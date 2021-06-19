package com.motofamdmn.you.fftdetector


import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import java.io.IOException
import kotlin.collections.ArrayList
import kotlinx.android.synthetic.main.fragment_record_sound.*
import java.io.File

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [recordSound.newInstance] factory method to
 * create an instance of this fragment.
 */
class recordSound : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private val LOG_TAG = "AudioRecordTest"

    //全フラグメントからアクセス可能の共通データ
    private val cd = commonData.getInstance()

    //現在の録音位置
    var xPosition : Float =  0.0f

    var audioRecord //録音用のオーディオレコードクラス
            : AudioRecord? = null
    val SAMPLING_RATE = cd.sampleRate //オーディオレコード用サンプリング周波数

    private var bufSize //オーディオレコード用バッファのサイズ
            = 0

    private lateinit var shortData: ShortArray //オーディオレコード用バッファ

    private val wav1: myWaveFile = myWaveFile()
    // wavデータの初期化
    private var dataWav = Pair(arrayListOf<Float>(), arrayListOf<Float>())

    private lateinit var myDir : File
    private var fileName: String = ""

    private var player: MediaPlayer? = null

    var maxData = 0.0f

    //録音時間のカウント用、Handlerを使って定期的に表示処理をする
    //private val dataFormat: SimpleDateFormat = SimpleDateFormat("mm:ss.S", Locale.US)
    private var count = 0
    private var period : Int = 100

    // 'Handler()' is deprecated as of API 30: Android 11.0 (R)
    private val handler: Handler = Handler(Looper.getMainLooper())

    //periodで設定した時間ごとに録音時間とプログレスバーを更新
    private val updateTime: Runnable = object : Runnable {
        override fun run() {
            markerWavText.text = "%.0f".format(xPosition)
            recordTimeBar.progress = (xPosition * 10).toInt()  //xPositionは0.1秒刻みなのでプログレスバー表示のためには10倍する

            handler.postDelayed(this, period.toLong())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }

        //描画用wavデータの初期化、cdはグローバル変数で全フラグメントからアクセス可能
        cd.shareWavXData.clear()
        cd.shareWavYData.clear()
        cd.shareWavXData.add(0.0f)
        cd.shareWavXData.add(20.0f)
        cd.shareWavYData.add(0.0f)
        cd.shareWavYData.add(0.0f)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_record_sound, container, false)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment recordSound.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            recordSound().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        //fileName = "${context?.externalCacheDir?.absolutePath}/audiorecordtest.wav"
        var stopBtnFlg = 0  //録音停止か再生停止かのフラグ、0が録音停止、1が再生停止
        var newRecordFlg = 1  //新規の録音のときは1

        //Recordingボタンを非表示に
        button.isVisible = false  //録音中ボタンの非表示
        button2.isVisible = true  //非録音中ボタンの表示

        //録音時間
        markerWavText.text = "%.0f".format(0f)

        //録音時間プログレスバー id:progressBar
        recordTimeBar.max = 200
        recordTimeBar.progress = 0

        //録音レベルプログレスバー id:soundLevelBar
        soundLevelBar.max = 1000
        soundLevelBar.progress = 0
        //soundLevelBar.secondaryProgress = 80  //secondaryは今は使わないのでコメントアウト

        //サンプリングレートとデータ数を表示
        textView4.text = "  SAMPLING RATE :  ${(cd.sampleRate/1000.0f).toString()} kHz "
        textView5.text = "  DATA BIT : 16 BIT "
        textView15.text = "  CHANNEL : MONORAL "
        textView16.text = "  MAX RECORD TIME : 20 SEC "

        //AudioRecordの初期化
        initAudioRecord()

        recordBtnImage.setOnClickListener {
            if(newRecordFlg == 1) {
                xPosition = 0.0f
                recordTimeBar.progress = 0
                newRecordFlg = 0
                markerWavText.text = "%.0f".format(0f)
            }
            stopBtnFlg = 0  //録音停止か再生停止か、0は録音停止を意味する
            button.isVisible = true  //録音中ボタンの表示
            button2.isVisible = false  //非録音中ボタンの非表示
            handler.post(updateTime)
            startAudioRecord()
        }//レコードボタンリスナの設定

        stopBtnImage.setOnClickListener {
            if(stopBtnFlg == 0){
                stopAudioRecord()
                button.isVisible = false  //録音中ボタンの非表示
                button2.isVisible = true  //非録音中ボタンの表示
                handler.removeCallbacks(updateTime);
            }else{
                stopPlaying()
            }
        }

        playBtnImage.setOnClickListener {
            //wavファイルを再生する
            stopBtnFlg = 1
            startPlaying()
        }

        newRecordBtnImage.setOnClickListener {
            recordTimeBar.progress = 0
            soundLevelBar.progress = 0
            soundLevelBar.secondaryProgress = 0

            initAudioRecord()
            count = 0
            newRecordFlg = 1
            markerWavText.text = "%.0f".format(0f)
            //サンプリングレートとデータ数を更新
            textView4.text = " SAMPLINT RATE :  ${(cd.sampleRate/1000.0f).toString()} Hz "
            textView5.text = " DATA BIT : 16 BIT "
            textView15.text = " CHANNEL : MONORAL "

        }//新しいwavファイル作成

    }


    //AudioRecordの初期化
    private fun initAudioRecord() {

        val DRAW_WAV_INTERVAL : Float = 0.1f  //wav波形を描画するインターバル（秒）兼プログレスバー更新のインターバル
        // nextPosition 次のプログレスバー更新の時間
        var nextXPosition : Float = 0.0f
        // wavのデータサイズ
        var dataSize : Int = 0
        // 外部ストレージ使用可否のﾌﾗｸﾞ
        var flag = 0

        wav1.apply{maxData = 0.0f}

        //外部ストレージ使用可否確認
        if(isExternalStorageReadable()){
            flag = 1
            Log.e(LOG_TAG, "外部ストレージ利用可能")
        }else{
            flag = 0
            Log.e(LOG_TAG, "外部ストレージ利用不可")
        }

        val mydirName = "testwave" // 保存フォルダー
        val ExtFileName = "sample.wav" // ファイル名

        var dirFlag = 0

        // フォルダーを使用する場合、あるかを確認
        myDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), mydirName)
        if (!myDir.exists()) {
            // なければ、フォルダーを作る
            if(myDir.mkdirs()){
                dirFlag = 1
                Log.e(LOG_TAG, "フォルダ作成成功")
            }else{
                dirFlag = 0
                Log.e(LOG_TAG, "フォルダ作成失敗")
            }
            Log.e(LOG_TAG, "フォルダ作成")
        }

        //fileName = myDir.toString() + ExtFileName
        fileName = extFilePath(mydirName, ExtFileName)
        cd.cdFileName = fileName

        //mydirNameディレクトリのファイルリスト作成
        val myFileList = myDir.list()

        wav1.createFile(fileName)
        //wav1.createFile(SoundDefine.filePath)
        // AudioRecordオブジェクトを作成
        bufSize = AudioRecord.getMinBufferSize(
            SAMPLING_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLING_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufSize
        )
        shortData = ShortArray(bufSize / 2)

        // コールバックを指定
        audioRecord!!.setRecordPositionUpdateListener(object :
            AudioRecord.OnRecordPositionUpdateListener {
            // フレームごとの処理
            override fun onPeriodicNotification(recorder: AudioRecord) {
                // TODO Auto-generated method stub
                audioRecord!!.read(shortData, 0, bufSize / 2) // 読み込む
                wav1.addBigEndianData(shortData) // ファイルに書き出す

                dataSize = wav1.getDataSize()
                // xPosition 現在のwav波形時間
                xPosition = dataSize.toFloat() / (SAMPLING_RATE * 2)  //1秒はサンプリング周波数 x 2 byte　なのでxPositionは0.1秒刻み
                var tempStr : String = ""
                var tempAverageData : Float = 0.0f

                if (xPosition > nextXPosition) {

                    tempAverageData = wav1.get16PointsMaxWaveData(dataSize/2) * 10  ////録音レベルをパーセンテージ表示、プログレスバーの1000が100%なので10倍する
                    if(tempAverageData > maxData) {
                        maxData = tempAverageData
                    }
                    if(tempAverageData >= 900.0f){
                        soundLevelBar.progress = tempAverageData.toInt()  //録音レベルをパーセンテージ表示
                    }else {
                        soundLevelBar.progress = 0  //録音レベルをパーセンテージ表示
                        soundLevelBar.secondaryProgress = tempAverageData.toInt()  //録音レベルをパーセンテージ表示
                    }
                    nextXPosition = nextXPosition + DRAW_WAV_INTERVAL
                }

                if(xPosition > 300.0){
                    stopAudioRecord()  //20秒を超えたら録音停止
                    handler.removeCallbacks(updateTime);
                    val toast = Toast.makeText(context, "  20秒超、録音停止  ", Toast.LENGTH_LONG)
                    // 位置調整
                    toast.setGravity(Gravity.CENTER, 0, -400)
                    toast.show()
                }

            }

            override fun onMarkerReached(recorder: AudioRecord) {
                // TODO Auto-generated method stub
            }
        })
        // コールバックが呼ばれる間隔を指定
        audioRecord!!.positionNotificationPeriod = bufSize / 2
    }

    //オーディオレコードを開始する
    private fun startAudioRecord() {
        audioRecord!!.startRecording()
        audioRecord!!.read(shortData, 0, bufSize / 2)
    }

    //オーディオレコードを停止する
    private fun stopAudioRecord() {
        audioRecord!!.stop()
        dataWav  = wav1.getWaveData()  //dataWavにwavデータを保管する
        //wavデータを全フラグメントがアクセスできる変数に保管

        cd.shareWavXData.clear()
        cd.shareWavYData.clear()
        cd.shareWavXData = ArrayList<Float>(dataWav.first)
        cd.shareWavYData = ArrayList<Float>(dataWav.second)

    }

    //再生する
    private fun startPlaying() {

        fileName = myDir.toString()+"/"+ cd.cdFileName

        //wavファイルを再生する
        player = MediaPlayer().apply {
            try {
                setDataSource(fileName)
                prepare()
                start()
            } catch (e: IOException) {
                Log.e(LOG_TAG, "prepare() failed")
            }
        }
    }

    private fun stopPlaying() {
        player?.release()
        player = null
    }

    // 外部ストレージが読み取り可能かどうかをチェック
    fun isExternalStorageReadable(): Boolean{
        val state = Environment.getExternalStorageState()
        return Environment.MEDIA_MOUNTED == state || Environment.MEDIA_MOUNTED_READ_ONLY == state
    }

    // 現在の外部MUSICストレージのログ・ファイル名(パス含め)
    fun extFilePath(mydirName : String, ExtFileName : String): String{
        val myDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getPath() +"/"+mydirName
        return  myDir+"/"+ ExtFileName
    }

    // 現在の外部ストレージのログ・ファイル名(パス含め)
    //fun extFilePath(mydirName : String, ExtFileName : String): String{
    //    val myDir = Environment.getExternalStorageDirectory().getPath() +"/"+mydirName
    //    return  myDir+"/"+ ExtFileName
    //}

}