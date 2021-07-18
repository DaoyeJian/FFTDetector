package com.motofamdmn.you.fftdetector


import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import kotlinx.android.synthetic.main.fragment_fft_analisys.*
import kotlinx.android.synthetic.main.fragment_fft_analisys.markerWavText
import kotlinx.android.synthetic.main.fragment_record_sound.*

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [fftAnalisys.newInstance] factory method to
 * create an instance of this fragment.
 */
class fftAnalisys : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    //全フラグメントからアクセス可能の共通データ
    private val cd = commonData.getInstance()

    private var dataWavX = arrayListOf<Float>(0.0f, 20.0f)
    private var dataWavY = arrayListOf<Float>(0.0f, 0.0f)
    private var xPosition : Float = 0.0f  //現在のwav波形時間
    private var nextXPosition : Float = 0.0f  //次のwav波形を描画する時間
    private var wavMarkerTime : Float = 0.0f  //wav波形上のマーカー、FFTの分析位置兼ねる
    private var wavMarkerTimeStr : String = "0"  //wav波形上のマーカー、FFTの分析位置兼ねる
    private val SAMPLING_RATE = cd.sampleRate  //サンプリングレート
    private val FFT_DATA_POINTS = cd.dataPoints  //データ点数
    private val resolutionFreq : Float = (SAMPLING_RATE.toFloat() / FFT_DATA_POINTS.toFloat())
    private val DRAW_WAV_INTERVAL : Float = 0.1f  //wav波形を描画するインターバル（秒）兼プログレスバー更新のインターバル
    private var xWavMarker = arrayListOf(0.0f, 0.0f) //X軸マーカーデータ
    private var yWavMarker= arrayListOf(-150.0f, 150.0f) //Y軸マーカーデータ
    private var readPosition : Int = 0

    private val mFft = myFft()
    private var fftYResult = FloatArray(FFT_DATA_POINTS){0.0f}
    private var fftX = arrayListOf<Float>()  //FFTデータ,x軸
    private var fftY = arrayListOf<Float>()  //FFTデータ,y軸
    private var xFftMarker = arrayListOf(100.0f, 100.0f) //X軸マーカーデータ
    private var yFftMarker = arrayListOf(-60f, 100000.0f)//Y軸マーカーデータ
    private var markerFrq : Float = 100.0f
    private var markerStr : String = "100.0"
    private var sideFrq : Float = 0f
    private var sideStr : String = "0"
    private var frMarkerOn = java.lang.Boolean.TRUE  //frマーカーの表示、非表示
    private var fcMarkerOn = java.lang.Boolean.TRUE  //fcマーカーの表示、非表示
    private var zfcMarkerOn = java.lang.Boolean.TRUE  //zfcマーカーの表示、非表示
    private var zfiMarkerOn = java.lang.Boolean.TRUE  //zfiマーカーの表示、非表示
    private var fbx2MarkerOn = java.lang.Boolean.TRUE  //2fbマーカーの表示、非表示
    private var sideBandMarkerOn = java.lang.Boolean.FALSE  //Side Bandマーカーの表示、非表示

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_fft_analisys, container, false)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment fftAnalisys.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            fftAnalisys().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        //wavデータとグラフの初期化

        markerWavText.text = "0.0"
        faFileNameText.text = cd.cdFileName
        drawWav(dataWavX, dataWavY, xWavMarker, yWavMarker)
        lineChartWav.extraLeftOffset = 10.0f;  //Y軸数値が切れないようにオフセット

        if(cd.ffTYUnit == java.lang.Boolean.TRUE){
            yAxisLabelText.text = "dB"
        }else{
            yAxisLabelText.text = "Linear"
        }


        //窓関数のセット
        mFft.Wn = mFft.setWindowWn()

        //FFTのX軸周波数を設定とグラフ初期化
        var tempFrq: Float = 0.0f
        markerFreqText.text = "Marker %.1fHz".format(markerFrq)
        editMarker.setText(markerFrq.toString())
        sideFreqText.text = "Side Band %.1fHz".format(sideFrq)
        editSide.setText(sideFrq.toString())
        speedText.text = "Speed %.1frpm".format(cd.rotateSpeed)
        editSpeed.setText(cd.rotateSpeed.toString())
        cd.frFreq = cd.rotateSpeed/60f
        cd.fcFreq = cd.fcFreq18*cd.rotateSpeed/1800f
        cd.zfcFreq = cd.zfcFreq18*cd.rotateSpeed/1800f
        cd.zfiFreq = cd.zfiFreq18*cd.rotateSpeed/1800f
        cd.fbx2Freq = cd.fbx2Freq18*cd.rotateSpeed/1800f
        frSwitchText.text = "%.1fHz".format(cd.frFreq)
        fcSwitchText.text = "%.1fHz".format(cd.fcFreq)
        zfcSwitchText.text = "%.1fHz".format(cd.zfcFreq)
        zfiSwitchText.text = "%.1fHz".format(cd.zfiFreq)
        fbx2SwitchText.text = "%.1fHz".format(cd.fbx2Freq)
        for (i in 0..FFT_DATA_POINTS-1) {

            tempFrq = i * resolutionFreq
            fftX.add(tempFrq)
            fftY.add (0.0f)

        }

        lineChartFft.extraLeftOffset = 20.0f;  //Y軸数値が切れないようにオフセット
        drawFft(fftX, fftY, xFftMarker, yFftMarker)

        //wav波形とFFT波形グラフの初期化ここまで

        wavSeekBar.max = 200
        wavSeekBar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    wavSeekBar: SeekBar,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    wavMarkerTime = progress.toFloat()/10.0f  //wavSeekBarは最大200で1が0.1秒
                    wavMarkerTimeStr = wavMarkerTime.toString()
                    markerWavText.text = "%.1f".format(wavMarkerTime)
                    //wavマーカー設定
                    xWavMarker[0] = wavMarkerTime
                    xWavMarker[1] = wavMarkerTime

                    drawWav(dataWavX, dataWavY, xWavMarker, yWavMarker)

                    //FFT解析位置、wavSeekBarのポイントでFFT解析するために準備
                    readPosition = (wavMarkerTime * SAMPLING_RATE).toInt()

                }

                override fun onStartTrackingTouch(wavSeekBar: SeekBar?) {}

                override fun onStopTrackingTouch(wavSeekBar: SeekBar?) {}
            }
        )

        fftBtn.setOnClickListener {
            //解析位置、0.1はaveragingInterval
            val maxPoint = cd.shareWavXData.size - cd.averagingNum * 0.1f * SAMPLING_RATE - FFT_DATA_POINTS
            val dataPoints = cd.shareWavYData.size
            if (readPosition < maxPoint) {  //読込位置が読込可能最大位置未満のとき
                mFft.sendWavData(cd.shareWavYData)
                //mFft.readDataOfNowPosition(readPosition)  fftAveragingでデータ読み込むので不要になった
                fftYResult = mFft.fftAveraging(readPosition)
                fftY.clear()
                for (i in 0..FFT_DATA_POINTS-1) {
                    fftY.add(fftYResult[i])
                }

                drawFft(fftX, fftY, xFftMarker, yFftMarker)

            }else{  //読込位置が読込可能最大位置以上のとき、つまりデータ不足のとき
                val toast = Toast.makeText(context, "  この位置では解析できません  ", Toast.LENGTH_LONG)
                // 位置調整
                toast.setGravity(Gravity.CENTER, 0, -400)
                toast.show()
            }
        }

        upBtn.setOnClickListener {
            markerFrq += 1.0f
            markerFreqText.text = "Marker %.1fHz".format(markerFrq)
            editMarker.setText(markerFrq.toString())
            //マーカー周波数設定
            xFftMarker.set(0,markerFrq)
            xFftMarker.set(1,markerFrq)
            //マーカーサイドバンド周波数設定
            cd.sideFftXData1 = markerFrq - sideFrq * 3
            cd.sideFftXData2 = markerFrq - sideFrq * 2
            cd.sideFftXData3 = markerFrq - sideFrq * 1
            cd.sideFftXData4 = markerFrq + sideFrq * 1
            cd.sideFftXData5 = markerFrq + sideFrq * 2
            cd.sideFftXData6 = markerFrq + sideFrq * 3

            drawFft(fftX, fftY, xFftMarker, yFftMarker)
        }

        downBtn.setOnClickListener {
            markerFrq -= 1.0f
            markerFreqText.text = "Marker %.1fHz".format(markerFrq)
            editMarker.setText(markerFrq.toString())
            //マーカー周波数設定
            xFftMarker.set(0,markerFrq)
            xFftMarker.set(1,markerFrq)
            //マーカーサイドバンド周波数設定
            cd.sideFftXData1 = markerFrq - sideFrq * 3
            cd.sideFftXData2 = markerFrq - sideFrq * 2
            cd.sideFftXData3 = markerFrq - sideFrq * 1
            cd.sideFftXData4 = markerFrq + sideFrq * 1
            cd.sideFftXData5 = markerFrq + sideFrq * 2
            cd.sideFftXData6 = markerFrq + sideFrq * 3

            drawFft(fftX, fftY, xFftMarker, yFftMarker)
        }

        sideUpBtn.setOnClickListener {
            sideFrq += 1.0f
            sideFreqText.text = "Side Band %.1fHz".format(sideFrq)
            editSide.setText(sideFrq.toString())

            //マーカーサイドバンド周波数設定
            cd.sideFftXData1 = markerFrq - sideFrq * 3
            cd.sideFftXData2 = markerFrq - sideFrq * 2
            cd.sideFftXData3 = markerFrq - sideFrq * 1
            cd.sideFftXData4 = markerFrq + sideFrq * 1
            cd.sideFftXData5 = markerFrq + sideFrq * 2
            cd.sideFftXData6 = markerFrq + sideFrq * 3

            drawFft(fftX, fftY, xFftMarker, yFftMarker)
        }

        sideDownBtn.setOnClickListener {
            sideFrq -= 1.0f
            sideFreqText.text = "Side Band .1fHz".format(sideFrq)
            editSide.setText(sideFrq.toString())

            //マーカーサイドバンド周波数設定
            cd.sideFftXData1 = markerFrq - sideFrq * 3
            cd.sideFftXData2 = markerFrq - sideFrq * 2
            cd.sideFftXData3 = markerFrq - sideFrq * 1
            cd.sideFftXData4 = markerFrq + sideFrq * 1
            cd.sideFftXData5 = markerFrq + sideFrq * 2
            cd.sideFftXData6 = markerFrq + sideFrq * 3

            drawFft(fftX, fftY, xFftMarker, yFftMarker)
        }

        speedUpBtn.setOnClickListener {
            cd.rotateSpeed += 1.0f
            editSpeed.setText(cd.rotateSpeed.toString())
            speedText.text = "Speed %.1frpm".format(cd.rotateSpeed)
            cd.frFreq = cd.rotateSpeed/60f
            cd.fcFreq = cd.fcFreq18*cd.rotateSpeed/1800f
            cd.zfcFreq = cd.zfcFreq18*cd.rotateSpeed/1800f
            cd.zfiFreq = cd.zfiFreq18*cd.rotateSpeed/1800f
            cd.fbx2Freq = cd.fbx2Freq18*cd.rotateSpeed/1800f
            frSwitchText.text = "%.1fHz".format(cd.frFreq)
            fcSwitchText.text = "%.1fHz".format(cd.fcFreq)
            zfcSwitchText.text = "%.1fHz".format(cd.zfcFreq)
            zfiSwitchText.text = "%.1fHz".format(cd.zfiFreq)
            fbx2SwitchText.text = "%.1fHz".format(cd.fbx2Freq)

            drawFft(fftX, fftY, xFftMarker, yFftMarker)
        }

        speedDownBtn.setOnClickListener {
            cd.rotateSpeed -= 1.0f
            editSpeed.setText(cd.rotateSpeed.toString())
            speedText.text = "Speed %.1frpm".format(cd.rotateSpeed)
            cd.frFreq = cd.rotateSpeed/60f
            cd.fcFreq = cd.fcFreq18*cd.rotateSpeed/1800f
            cd.zfcFreq = cd.zfcFreq18*cd.rotateSpeed/1800f
            cd.zfiFreq = cd.zfiFreq18*cd.rotateSpeed/1800f
            cd.fbx2Freq = cd.fbx2Freq18*cd.rotateSpeed/1800f
            frSwitchText.text = "%.1fHz".format(cd.frFreq)
            fcSwitchText.text = "%.1fHz".format(cd.fcFreq)
            zfcSwitchText.text = "%.1fHz".format(cd.zfcFreq)
            zfiSwitchText.text = "%.1fHz".format(cd.zfiFreq)
            fbx2SwitchText.text = "%.1fHz".format(cd.fbx2Freq)

            drawFft(fftX, fftY, xFftMarker, yFftMarker)
        }

        editSpeed.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                if (editSpeed.text.toString() != "") {
                    cd.rotateSpeed = editSpeed.text.toString().toFloat()
                    speedText.text = "Speed %.1frpm".format(cd.rotateSpeed)
                    cd.frFreq = cd.rotateSpeed/60f
                    cd.fcFreq = cd.fcFreq18*cd.rotateSpeed/1800f
                    cd.zfcFreq = cd.zfcFreq18*cd.rotateSpeed/1800f
                    cd.zfiFreq = cd.zfiFreq18*cd.rotateSpeed/1800f
                    cd.fbx2Freq = cd.fbx2Freq18*cd.rotateSpeed/1800f
                    frSwitchText.text = "%.1fHz".format(cd.frFreq)
                    fcSwitchText.text = "%.1fHz".format(cd.fcFreq)
                    zfcSwitchText.text = "%.1fHz".format(cd.zfcFreq)
                    zfiSwitchText.text = "%.1fHz".format(cd.zfiFreq)
                    fbx2SwitchText.text = "%.1fHz".format(cd.fbx2Freq)
                    drawFft(fftX, fftY, xFftMarker, yFftMarker)

                } else {
                    cd.frFreq = 0f
                    cd.fcFreq = 0f
                    cd.zfcFreq = 0f
                    cd.zfiFreq = 0f
                    cd.fbx2Freq = 0f
                    frSwitchText.text = "0Hz"
                    fcSwitchText.text = "0Hz"
                    zfcSwitchText.text = "0Hz"
                    zfiSwitchText.text = "0Hz"
                    fbx2SwitchText.text = "0Hz"
                    drawFft(fftX, fftY, xFftMarker, yFftMarker)
                }
            }

        })

        editMarker.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                if (editMarker.text.toString() != "") {
                    markerFrq = editMarker.text.toString().toFloat()
                    markerStr = markerFrq.toString()
                    markerFreqText.text = "Marker %.1fHz".format(markerFrq)
                    //マーカー周波数設定
                    xFftMarker.set(0,markerFrq)
                    xFftMarker.set(1,markerFrq)
                    //マーカーサイドバンド周波数設定
                    cd.sideFftXData1 = markerFrq - sideFrq * 3
                    cd.sideFftXData2 = markerFrq - sideFrq * 2
                    cd.sideFftXData3 = markerFrq - sideFrq * 1
                    cd.sideFftXData4 = markerFrq + sideFrq * 1
                    cd.sideFftXData5 = markerFrq + sideFrq * 2
                    cd.sideFftXData6 = markerFrq + sideFrq * 3

                    drawFft(fftX, fftY, xFftMarker, yFftMarker)

                } else {
                    markerFrq = 0f
                    markerStr = markerFrq.toString()
                    markerFreqText.text = "Marker %.1fHz".format(markerFrq)
                    //マーカー周波数設定
                    xFftMarker.set(0,markerFrq)
                    xFftMarker.set(1,markerFrq)
                    //マーカーサイドバンド周波数設定
                    cd.sideFftXData1 = 0f
                    cd.sideFftXData2 = 0f
                    cd.sideFftXData3 = 0f
                    cd.sideFftXData4 = 0f
                    cd.sideFftXData5 = 0f
                    cd.sideFftXData6 = 0f

                    drawFft(fftX, fftY, xFftMarker, yFftMarker)
                }
            }

        })

        editSide.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                if (editSide.text.toString() != "") {
                    sideFrq = editSide.text.toString().toFloat()
                    sideStr = sideFrq.toString()
                    sideFreqText.text = "Side Band %.1fHz".format(sideFrq)

                    //マーカーサイドバンド周波数設定
                    cd.sideFftXData1 = markerFrq - sideFrq * 3
                    cd.sideFftXData2 = markerFrq - sideFrq * 2
                    cd.sideFftXData3 = markerFrq - sideFrq * 1
                    cd.sideFftXData4 = markerFrq + sideFrq * 1
                    cd.sideFftXData5 = markerFrq + sideFrq * 2
                    cd.sideFftXData6 = markerFrq + sideFrq * 3

                    drawFft(fftX, fftY, xFftMarker, yFftMarker)

                } else {
                    sideFrq = 0f
                    sideStr = sideFrq.toString()
                    sideFreqText.text = "Side Band %.1fHz".format(sideFrq)

                    //マーカーサイドバンド周波数設定
                    cd.sideFftXData1 = 0f
                    cd.sideFftXData2 = 0f
                    cd.sideFftXData3 = 0f
                    cd.sideFftXData4 = 0f
                    cd.sideFftXData5 = 0f
                    cd.sideFftXData6 = 0f

                    drawFft(fftX, fftY, xFftMarker, yFftMarker)
                }
            }

        })

        // brgマーカーの表示、非表示スイッチ
        frSwitch.isChecked = frMarkerOn  //デフォルトではON
        frSwitch.setOnCheckedChangeListener { _, isChecked ->

            if (isChecked) {
                frMarkerOn = java.lang.Boolean.TRUE
            } else {
                frMarkerOn = java.lang.Boolean.FALSE
            }
            drawFft(fftX, fftY, xFftMarker, yFftMarker)

        }

        fcSwitch.isChecked = fcMarkerOn  //デフォルトではON
        fcSwitch.setOnCheckedChangeListener { _, isChecked ->

            if (isChecked) {
                fcMarkerOn = java.lang.Boolean.TRUE
            } else {
                fcMarkerOn = java.lang.Boolean.FALSE
            }
            drawFft(fftX, fftY, xFftMarker, yFftMarker)

        }

        zfcSwitch.isChecked = zfcMarkerOn  //デフォルトではON
        zfcSwitch.setOnCheckedChangeListener { _, isChecked ->

            if (isChecked) {
                zfcMarkerOn = java.lang.Boolean.TRUE
            } else {
                zfcMarkerOn = java.lang.Boolean.FALSE
            }
            drawFft(fftX, fftY, xFftMarker, yFftMarker)

        }

        zfiSwitch.isChecked = zfiMarkerOn  //デフォルトではON
        zfiSwitch.setOnCheckedChangeListener { _, isChecked ->

            if (isChecked) {
                zfiMarkerOn = java.lang.Boolean.TRUE
            } else {
                zfiMarkerOn = java.lang.Boolean.FALSE
            }
            drawFft(fftX, fftY, xFftMarker, yFftMarker)

        }

        fbx2Switch.isChecked = fbx2MarkerOn  //デフォルトではON
        fbx2Switch.setOnCheckedChangeListener { _, isChecked ->

            if (isChecked) {
                fbx2MarkerOn = java.lang.Boolean.TRUE
            } else {
                fbx2MarkerOn = java.lang.Boolean.FALSE
            }
            drawFft(fftX, fftY, xFftMarker, yFftMarker)

        }

        sideBandSwitch.isChecked = sideBandMarkerOn  //デフォルトではON
        sideBandSwitch.setOnCheckedChangeListener { _, isChecked ->

            if (isChecked) {
                sideBandMarkerOn = java.lang.Boolean.TRUE
            } else {
                sideBandMarkerOn = java.lang.Boolean.FALSE
            }
            drawFft(fftX, fftY, xFftMarker, yFftMarker)

        }

    }

    override fun onStart() {
        super.onStart()

        val dataPoints = cd.shareWavYData.size
        dataWavX.clear()
        dataWavY.clear()

        for (i in 0..dataPoints - 1) {
            if ((i % 100) == 0) {
                dataWavX.add(cd.shareWavXData[i])
                dataWavY.add(cd.shareWavYData[i])
            }
        }

        //解析位置、0.1はaveragingInterval
        val maxPoint = cd.shareWavXData.size - cd.averagingNum * 0.1f * SAMPLING_RATE - FFT_DATA_POINTS
        if (readPosition < maxPoint) {  //読込位置が読込可能最大位置未満のとき
            mFft.sendWavData(cd.shareWavYData)
            //mFft.readDataOfNowPosition(readPosition)  fftAveragingでデータ読み込むので不要になった
            fftYResult = mFft.fftAveraging(readPosition)
            fftY.clear()
            for (i in 0..FFT_DATA_POINTS-1) {
                fftY.add(fftYResult[i])
            }
        }else{  //読込位置が読込可能最大位置以上のとき、つまりデータ不足のとき
            val toast = Toast.makeText(context, "  この位置では解析できません  ", Toast.LENGTH_LONG)
            // 位置調整
            toast.setGravity(Gravity.CENTER, 0, -400)
            toast.show()
        }
        drawWav(dataWavX, dataWavY, xWavMarker, yWavMarker)
        drawFft(fftX, fftY, xFftMarker, yFftMarker)

    }

    private fun drawWav(xx: List<Float>, yy: List<Float>, xout: List<Float>, yout: List<Float>) {

        //①Entryにデータ格納
        val entryList = mutableListOf<Entry>()//1本目の線
        val entryListm = mutableListOf<Entry>()//マーカーの線

        for (i in xx.indices) {
            entryList.add(
                Entry(xx[i], yy[i])
            )
        }

        entryListm.add(
            Entry(xout[0], yout[0])
        )
        entryListm.add(
            Entry(xout[1], yout[1])
        )

        //LineDataSetのList
        val lineDataSets = mutableListOf<ILineDataSet>()
        //②DataSetにデータ格納
        val lineDataSet = LineDataSet(entryList, "frequency")

        val lineDataSetsm = mutableListOf<ILineDataSet>()
        //②DataSetにデータ格納
        val lineDataSetm = LineDataSet(entryListm, "marker")
        //③DataSetにフォーマット指定(3章で詳説)
        lineDataSet.color = Color.BLUE
        lineDataSetm.color = Color.RED
        lineDataSet.setDrawCircles(false)  //折れ線上のポイントサークルを描画しない
        lineDataSetm.setDrawCircles(false)  //折れ線上のポイントサークルを描画しない

        //リストに格納
        lineDataSets.add(lineDataSet)
        lineDataSets.add(lineDataSetm)

        //④LineDataにLineDataSet格納
        val lineData = LineData(lineDataSets)
        //⑤LineChartにLineData格納
        lineChartWav.data = lineData
        //⑥Chartのフォーマット指定(3章で詳説)
        //X軸の設定
        lineChartWav.xAxis.apply {
            isEnabled = true
            textColor = Color.WHITE
            setDrawGridLines(true)
            axisMaximum = 20.1f
            position = XAxis.XAxisPosition.BOTTOM
        }

        val ymax = yy.maxOrNull()  //y軸最大値設定のためyの最大値を求める
        val ymin = yy.minOrNull()
        //Y軸の設定
        lineChartWav.axisLeft.apply {
            isEnabled = true
            textColor = Color.WHITE
            setDrawGridLines(true)
            if (ymax != null) {
                axisMaximum = ymax * 1.5f  //y軸最大値はwav結果のy最大値の1.5倍とする
            } else {
                axisMaximum = 100.0f
            }
            if (ymin != null) {
                axisMinimum = -ymax!! * 1.5f  //y軸最小値はwav結果のy最大値の1.5倍とする
            } else{
                axisMinimum = -100.0f
            }
        }

        lineChartWav.axisRight.apply {
            isEnabled = false
        }
        //⑦linechart更新
        lineChartWav.legend.isEnabled = false  //ラベルを表示しない
        lineChartWav.description.isEnabled = false  //Description label を表示しない
        lineChartWav.setBackgroundColor(Color.DKGRAY)
        lineChartWav.invalidate()
    }


    //Fftの描画
    fun drawFft(xx: List<Float>, yy: List<Float>, xout: List<Float>, yout: List<Float>) {

        //①Entryにデータ格納
        val entryList = mutableListOf<Entry>()//1本目の線
        val entryListm = mutableListOf<Entry>()//ユーザーマーカーの線
        val entryListmfr = mutableListOf<Entry>()//frマーカーの線
        val entryListmfc = mutableListOf<Entry>()//fcマーカーの線
        val entryListmzfc = mutableListOf<Entry>()//zfcマーカーの線
        val entryListmzfi = mutableListOf<Entry>()//zfiマーカーの線
        val entryListmfbx2 = mutableListOf<Entry>()//2fbマーカーの線
        val entryListms1 = mutableListOf<Entry>()//マーカーのサイドバンド1本目
        val entryListms2 = mutableListOf<Entry>()//マーカーのサイドバンド2本目
        val entryListms3 = mutableListOf<Entry>()//マーカーのサイドバンド3本目
        val entryListms4 = mutableListOf<Entry>()//マーカーのサイドバンド4本目
        val entryListms5 = mutableListOf<Entry>()//マーカーのサイドバンド5本目
        val entryListms6 = mutableListOf<Entry>()//マーカーのサイドバンド6本目

        //for (i in xx.indices) {
        for (i in 0..FFT_DATA_POINTS-1) {
            entryList.add(
                Entry(xx[i], yy[i])
            )
        }

        //ユーザーマーカー
        entryListm.add(
            Entry(xout[0], yout[0])
        )
        entryListm.add(
            Entry(xout[1], yout[1])
        )

        //fcマーカー
        entryListmfr.add(Entry(cd.frFreq, yout[0]))
        entryListmfr.add(Entry(cd.frFreq, yout[1]))

        //fcマーカー
        entryListmfc.add(Entry(cd.fcFreq, yout[0]))
        entryListmfc.add(Entry(cd.fcFreq, yout[1]))

        //zfcマーカー
        entryListmzfc.add(Entry(cd.zfcFreq, yout[0]))
        entryListmzfc.add(Entry(cd.zfcFreq, yout[1]))

        //zfiマーカー
        entryListmzfi.add(Entry(cd.zfiFreq, yout[0]))
        entryListmzfi.add(Entry(cd.zfiFreq, yout[1]))

        //2fbマーカー
        entryListmfbx2.add(Entry(cd.fbx2Freq, yout[0]))
        entryListmfbx2.add(Entry(cd.fbx2Freq, yout[1]))

        //6本のサイドバンドマーカー
        entryListms1.add(Entry(cd.sideFftXData1, yout[0]))
        entryListms1.add(Entry(cd.sideFftXData1, yout[1]))
        entryListms2.add(Entry(cd.sideFftXData2, yout[0]))
        entryListms2.add(Entry(cd.sideFftXData2, yout[1]))
        entryListms3.add(Entry(cd.sideFftXData3, yout[0]))
        entryListms3.add(Entry(cd.sideFftXData3, yout[1]))
        entryListms4.add(Entry(cd.sideFftXData4, yout[0]))
        entryListms4.add(Entry(cd.sideFftXData4, yout[1]))
        entryListms5.add(Entry(cd.sideFftXData5, yout[0]))
        entryListms5.add(Entry(cd.sideFftXData5, yout[1]))
        entryListms6.add(Entry(cd.sideFftXData6, yout[0]))
        entryListms6.add(Entry(cd.sideFftXData6, yout[1]))

        //LineDataSetのList
        val lineDataSets = mutableListOf<ILineDataSet>()
        //②DataSetにデータ格納
        val lineDataSet = LineDataSet(entryList, "")

        val lineDataSetsm = mutableListOf<ILineDataSet>()
        //②DataSetにデータ格納
        val lineDataSetm = LineDataSet(entryListm, "")

        val lineDataSetsmfr = mutableListOf<ILineDataSet>()
        //②DataSetにデータ格納
        val lineDataSetmfr = LineDataSet(entryListmfr, "fr")

        val lineDataSetsmfc = mutableListOf<ILineDataSet>()
        //②DataSetにデータ格納
        val lineDataSetmfc = LineDataSet(entryListmfc, "fc")

        val lineDataSetsmzfc = mutableListOf<ILineDataSet>()
        //②DataSetにデータ格納
        val lineDataSetmzfc = LineDataSet(entryListmzfc, "zfc")

        val lineDataSetsmzfi = mutableListOf<ILineDataSet>()
        //②DataSetにデータ格納
        val lineDataSetmzfi = LineDataSet(entryListmzfi, "zfi")

        val lineDataSetsmfbx2 = mutableListOf<ILineDataSet>()
        //②DataSetにデータ格納
        val lineDataSetmfbx2 = LineDataSet(entryListmfbx2, "2fb")

        val lineDataSetsms1 = mutableListOf<ILineDataSet>()
        //②DataSetにデータ格納
        val lineDataSetms1 = LineDataSet(entryListms1, "s1")
        val lineDataSetsms2 = mutableListOf<ILineDataSet>()
        //②DataSetにデータ格納
        val lineDataSetms2 = LineDataSet(entryListms2, "s2")
        val lineDataSetsms3 = mutableListOf<ILineDataSet>()
        //②DataSetにデータ格納
        val lineDataSetms3 = LineDataSet(entryListms3, "s3")
        val lineDataSetsms4 = mutableListOf<ILineDataSet>()
        //②DataSetにデータ格納
        val lineDataSetms4 = LineDataSet(entryListms4, "s4")
        val lineDataSetsms5 = mutableListOf<ILineDataSet>()
        //②DataSetにデータ格納
        val lineDataSetms5 = LineDataSet(entryListms5, "s5")
        val lineDataSetsms6 = mutableListOf<ILineDataSet>()
        //②DataSetにデータ格納
        val lineDataSetms6 = LineDataSet(entryListms6, "s6")

        //③DataSetにフォーマット指定(3章で詳説)
        lineDataSet.color = Color.BLUE
        lineDataSetm.color = Color.RED
        lineDataSetmfr.color = Color.parseColor("#7fffd4")
        lineDataSetmfc.color = Color.parseColor("#00FF00")
        lineDataSetmzfc.color = Color.parseColor("#bdb76b")
        lineDataSetmzfi.color = Color.parseColor("#ff8c00")
        lineDataSetmfbx2.color = Color.parseColor("#b22222")
        lineDataSetms1.color = Color.MAGENTA
        lineDataSetms2.color = Color.MAGENTA
        lineDataSetms3.color = Color.MAGENTA
        lineDataSetms4.color = Color.MAGENTA
        lineDataSetms5.color = Color.MAGENTA
        lineDataSetms6.color = Color.MAGENTA
        lineDataSet.setDrawCircles(false)  //折れ線上のポイントサークルを描画しない
        lineDataSetm.setDrawCircles(false)  //折れ線上のポイントサークルを描画しない
        lineDataSetmfr.setDrawCircles(false)  //折れ線上のポイントサークルを描画しない
        lineDataSetmfc.setDrawCircles(false)  //折れ線上のポイントサークルを描画しない
        lineDataSetmzfc.setDrawCircles(false)  //折れ線上のポイントサークルを描画しない
        lineDataSetmzfi.setDrawCircles(false)  //折れ線上のポイントサークルを描画しない
        lineDataSetmfbx2.setDrawCircles(false)  //折れ線上のポイントサークルを描画しない
        lineDataSetms1.setDrawCircles(false)  //折れ線上のポイントサークルを描画しない
        lineDataSetms2.setDrawCircles(false)  //折れ線上のポイントサークルを描画しない
        lineDataSetms3.setDrawCircles(false)  //折れ線上のポイントサークルを描画しない
        lineDataSetms4.setDrawCircles(false)  //折れ線上のポイントサークルを描画しない
        lineDataSetms5.setDrawCircles(false)  //折れ線上のポイントサークルを描画しない
        lineDataSetms6.setDrawCircles(false)  //折れ線上のポイントサークルを描画しない

        lineDataSetmfr.enableDashedLine(10.0f,10.0f,0.0f)  //ベアリングマーカーを点線とする
        lineDataSetmfc.enableDashedLine(10.0f,10.0f,0.0f)  //ベアリングマーカーを点線とする
        lineDataSetmzfc.enableDashedLine(10.0f,10.0f,0.0f)  //ベアリングマーカーを点線とする
        lineDataSetmzfi.enableDashedLine(10.0f,10.0f,0.0f)  //ベアリングマーカーを点線とする
        lineDataSetmfbx2.enableDashedLine(10.0f,10.0f,0.0f)  //ベアリングマーカーを点線とする

        lineDataSetms1.enableDashedLine(15.0f,15.0f,0.0f)  //サイドバンドマーカーを点線とする
        lineDataSetms2.enableDashedLine(15.0f,15.0f,0.0f)  //サイドバンドマーカーを点線とする
        lineDataSetms3.enableDashedLine(15.0f,15.0f,0.0f)  //サイドバンドマーカーを点線とする
        lineDataSetms4.enableDashedLine(15.0f,15.0f,0.0f)  //サイドバンドマーカーを点線とする
        lineDataSetms5.enableDashedLine(15.0f,15.0f,0.0f)  //サイドバンドマーカーを点線とする
        lineDataSetms6.enableDashedLine(15.0f,15.0f,0.0f)  //サイドバンドマーカーを点線とする


        //リストに格納
        lineDataSets.add(lineDataSet)
        lineDataSets.add(lineDataSetm)
        if(frMarkerOn){lineDataSets.add(lineDataSetmfr)}
        if(fcMarkerOn){lineDataSets.add(lineDataSetmfc)}
        if(zfcMarkerOn){lineDataSets.add(lineDataSetmzfc)}
        if(zfiMarkerOn){lineDataSets.add(lineDataSetmzfi)}
        if(fbx2MarkerOn){lineDataSets.add(lineDataSetmfbx2)}
        if(sideBandMarkerOn) {
            lineDataSets.add(lineDataSetms1)
            lineDataSets.add(lineDataSetms2)
            lineDataSets.add(lineDataSetms3)
            lineDataSets.add(lineDataSetms4)
            lineDataSets.add(lineDataSetms5)
            lineDataSets.add(lineDataSetms6)
        }

        //④LineDataにLineDataSet格納
        val lineData = LineData(lineDataSets)
        //⑤LineChartにLineData格納
        lineChartFft.data = lineData
        //⑥Chartのフォーマット指定(3章で詳説)
        //X軸の設定
        lineChartFft.xAxis.apply {
            isEnabled = true
            textColor = Color.WHITE
            position = XAxis.XAxisPosition.BOTTOM
            axisMinimum = 0.0f
            axisMaximum = 10000.0f
        }

        val ymax = yy.maxOrNull()  //y軸最大値設定のためyの最大値を求める
        val ymin= yy.minOrNull()  //y軸最小値設定のためyの最大値を求める
        //Y軸の設定
        lineChartFft.axisLeft.apply {
            isEnabled = true
            textColor = Color.WHITE

            if(cd.ffTYUnit) {  //Y軸がdb表示の場合
                if (ymin != null) {
                    axisMaximum = 0f
                    axisMinimum = ymin * 1.2f
                }else{
                    axisMinimum = -50.0f
                }
            }else{  //Y軸がリニア表示の場合
                if (ymax != null) {
                    axisMaximum = ymax * 1.2f  //y軸最大値はFFT結果のy最大値の1.2倍とする
                    axisMinimum = 0.0f
                }else{
                    axisMaximum = 10000.0f
                    axisMinimum = 0.0f
                }
            }

        }


        lineChartFft.axisRight.apply {
            isEnabled = false
        }
        //⑦linechart更新
        lineChartFft.legend.isEnabled = false  //ラベルを表示しない
        lineChartFft.description.isEnabled = false  //Description label を表示しない
        lineChartFft.description.text = "Frequency"
        lineChartFft.setBackgroundColor(Color.DKGRAY)
        lineChartFft.invalidate()
    }

}