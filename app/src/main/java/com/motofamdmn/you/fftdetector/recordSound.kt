package com.motofamdmn.you.fftdetector


import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.*
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import io.realm.Realm
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import kotlinx.android.synthetic.main.fragment_record_sound.*
import kotlinx.android.synthetic.main.fragment_record_sound.stopBtnImage
import java.io.File
import java.io.IOException
import java.lang.Boolean.FALSE
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayList

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

    //ファイルリストのためのrealm
    private lateinit var realm: Realm

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
    private val dataFormat: SimpleDateFormat = SimpleDateFormat("HH:mm:ss", Locale.US)
    private var tempZeroDate : Date = dataFormat.parse("00:00:00")  //00:00:00表示のための基準
    private var tempZeroTime : Long = tempZeroDate.time
    private var count = 0
    private var period : Int = 100

    // 'Handler()' is deprecated as of API 30: Android 11.0 (R)
    private val handler: Handler = Handler(Looper.getMainLooper())

    //periodで設定した時間ごとに録音時間とプログレスバーを更新
    private val updateTime: Runnable = object : Runnable {
        override fun run() {
            markerWavText.text = dataFormat.format(tempZeroTime + xPosition*1000)
            //recordTimeBar.progress = (xPosition * 10).toInt()  //xPositionは0.1秒刻みなのでプログレスバー表示のためには10倍する
            recFileNameText.text = "  RECORD FILE NAME :  ${cd.cdFileName}"
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

        //ファイルリストのためのrealm
        realm = Realm.getDefaultInstance()

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

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        //fileName = "${context?.externalCacheDir?.absolutePath}/audiorecordtest.wav"
        var stopBtnFlg = 0  //録音停止か再生停止かのフラグ、0が録音停止、1が再生停止
        var newRecordFlg = 1  //新規の録音のときは1
        var recordContinueFlg = 0 //録音継続中は1

        //Recordingボタンを非表示に
        button.isVisible = false  //録音中ボタンの非表示
        button2.isVisible = true  //非録音中ボタンの表示

        //録音時間
        markerWavText.text = dataFormat.format(tempZeroDate)
        //totalRecordTimeText.text = dataFormat.format(tempZeroDate)

        //録音時間プログレスバー id:progressBar
        //recordTimeBar.max = 200
        //recordTimeBar.progress = 0

        //録音レベルプログレスバー id:soundLevelBar
        soundLevelBar.max = 1000
        soundLevelBar.progress = 0
        //soundLevelBar.secondaryProgress = 80  //secondaryは今は使わないのでコメントアウト

        //サンプリングレートとデータ数を表示
        recFileNameText.text = "  RECORD FILE NAME :  NEW FILE READY "
        textView4.text = "  SAMPLING RATE :  ${(cd.sampleRate/1000.0f).toString()} kHz "
        textView5.text = "  DATA BIT : ${cd.dataBits.toString()}  BIT "
        textView15.text = "  CHANNEL : MONORAL "
        maxRecordTimeText.text = "  MAX RECORD TIME : 20 SEC "

        // 長時間録音モード
        longRecordSwitch.isChecked = FALSE
        longRecordSwitch.setOnCheckedChangeListener { _, isChecked ->

                if (isChecked) {
                    wav1.longRecordFlg = 1
                    maxRecordTimeText.text = "  MAX RECORD TIME : NO LIMIT "
                } else {
                    wav1.longRecordFlg = 0
                    maxRecordTimeText.text = "  MAX RECORD TIME : 20 SEC "
                }

        }

        recordBtnImage.setOnClickListener {
            if(newRecordFlg == 1) {
                //AudioRecordの初期化
                initAudioRecord()
                xPosition = 0.0f
                //recordTimeBar.progress = 0
                newRecordFlg = 0
                markerWavText.text = dataFormat.format(tempZeroDate)
                //totalRecordTimeText.text = dataFormat.format(tempZeroDate)
            }
            stopBtnFlg = 0  //録音停止か再生停止か、0は録音停止を意味する
            button.isVisible = true  //録音中ボタンの表示
            button2.isVisible = false  //非録音中ボタンの非表示
            longRecordSwitch.isClickable = false //長時間録音スイッチを押せなくする
            handler.post(updateTime)
            startAudioRecord()
        }//レコードボタンリスナの設定

        stopBtnImage.setOnClickListener {
            if(stopBtnFlg == 0){  //0は録音停止
                if(cd.cdFileName != "") {
                    stopAudioRecord()
                    button.isVisible = false  //録音中ボタンの非表示
                    button2.isVisible = true  //非録音中ボタンの表示
                    handler.removeCallbacks(updateTime);
                    val tempWavDataTime = wav1.getWavDataTime()
                    cd.wavDataTime = tempWavDataTime
                    if(recordContinueFlg == 0){  //0は新規録音、1は追加録音
                        realm.executeTransaction { db: Realm ->
                            val maxId = db.where<myFiles>().max("id")
                            val nextId = (maxId?.toLong() ?: 0L) + 1
                            val myFile = db.createObject<myFiles>(nextId)
                            myFile.fileName = cd.cdFileName
                            myFile.fileSize = wav1.getDataSize().toLong() + 44
                            myFile.stereoMonoral = cd.stereoMonoral
                            myFile.sampleRate = cd.sampleRate
                            myFile.dataBit = cd.dataBits
                            myFile.wavDataTime = tempWavDataTime
                        }
                        recordContinueFlg = 1
                    }else{  //recordContinueFlgが1で追加録音の場合
                        realm.executeTransaction { db: Realm ->
                            val maxId = db.where<myFiles>().max("id")
                            val myFile = db.where<myFiles>().equalTo("id",maxId?.toLong()).findFirst()
                            myFile?.fileName = cd.cdFileName
                            myFile?.fileSize = wav1.getDataSize().toLong() + 44
                            myFile?.wavDataTime = tempWavDataTime
                        }
                    }
                }
            }else{  //stopBtnFlgが0ではなく1、つまり再生停止
                stopPlaying()
            }
        }

        playBtnImage.setOnClickListener {
            //wavファイルを再生する
            if(cd.cdFileName != "") {
                stopBtnFlg = 1
                startPlaying()
            }
        }

        newRecordBtnImage.setOnClickListener {
            //recordTimeBar.progress = 0
            soundLevelBar.progress = 0
            soundLevelBar.secondaryProgress = 0
            cd.cdFileName = ""

            count = 0
            newRecordFlg = 1
            recordContinueFlg = 0
            markerWavText.text = dataFormat.format(tempZeroDate)
            //totalRecordTimeText.text = dataFormat.format(tempZeroDate)
            //サンプリングレートとデータ数を更新
            recFileNameText.text = "  RECORD FILE NAME :  NEW FILE READY "
            textView4.text = " SAMPLINT RATE :  ${(cd.sampleRate/1000.0f).toString()} Hz "
            textView5.text = " DATA BIT : 16 BIT "
            textView15.text = " CHANNEL : MONORAL "

            longRecordSwitch.isClickable = true //長時間録音スイッチを押せるようにする

        }//新しいwavファイル作成

        // 外部ストレージ使用可否のﾌﾗｸﾞ
        var flag = 0
        var dirFlag = 0

        //外部ストレージ使用可否確認
        if(isExternalStorageReadable()){
            flag = 1
            Log.e(LOG_TAG, "外部ストレージ利用可能")
        }else{
            flag = 0
            Log.e(LOG_TAG, "外部ストレージ利用不可")
        }

        val mydirName = "testwave" // 保存フォルダー

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

    }


    //AudioRecordの初期化
    @RequiresApi(Build.VERSION_CODES.O)
    private fun initAudioRecord() {

        val DRAW_WAV_INTERVAL : Float = 0.1f  //wav波形を描画するインターバル（秒）兼プログレスバー更新のインターバル
        // nextPosition 次のプログレスバー更新の時間
        var nextXPosition : Float = 0.0f
        // wavのデータサイズ
        var dataSize : Int = 0

        val mydirName = "testwave" // 保存フォルダー

        wav1.apply{maxData = 0.0f}

        //現在日時をファイル名にする
        val currentLdt = LocalDateTime.now()
        val currentZdt: ZonedDateTime = currentLdt.atZone(ZoneId.of("Asia/Tokyo"))
        val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
        val formatted = currentZdt.format(formatter)
        val ExtFileName = formatted + ".wav"

        //fileName = myDir.toString() + ExtFileName
        fileName = extFilePath(mydirName, ExtFileName)
        cd.cdFileName = ExtFileName

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
                var tempStr: String = ""
                var tempAverageData: Float = 0.0f

                if (xPosition > nextXPosition) {

                    tempAverageData = wav1.get16PointsMaxWaveData(dataSize / 2) * 10  ////録音レベルをパーセンテージ表示、プログレスバーの1000が100%なので10倍する
                    if (tempAverageData > maxData) {
                        maxData = tempAverageData
                    }
                    if (tempAverageData >= 900.0f) {
                        soundLevelBar.progress = tempAverageData.toInt()  //録音レベルをパーセンテージ表示
                    } else {
                        soundLevelBar.progress = 0  //録音レベルをパーセンテージ表示
                        soundLevelBar.secondaryProgress = tempAverageData.toInt()  //録音レベルをパーセンテージ表示
                    }
                    nextXPosition = nextXPosition + DRAW_WAV_INTERVAL
                }

                if(longRecordSwitch.isChecked == FALSE) {  //長時間録音モードでないときは20秒でとめる
                    if (xPosition > 20.2) {
                        stopAudioRecord()  //20秒を超えたら録音停止
                        button.isVisible = false  //録音中ボタンの非表示
                        button2.isVisible = true  //非録音中ボタンの表示
                        handler.removeCallbacks(updateTime)
                        markerWavText.text = dataFormat.format(xPosition*1000)
                        val toast = Toast.makeText(context, "  20秒超、録音停止  ", Toast.LENGTH_LONG)
                        // 位置調整
                        toast.setGravity(Gravity.CENTER, 0, -400)
                        toast.show()
                        realm.executeTransaction { db: Realm ->
                            val maxId = db.where<myFiles>().max("id")
                            val nextId = (maxId?.toLong() ?: 0L) + 1
                            val myFile = db.createObject<myFiles>(nextId)
                            myFile.fileName = cd.cdFileName
                            myFile.fileSize = wav1.getDataSize().toLong() + 44
                            myFile.stereoMonoral = cd.stereoMonoral
                            myFile.sampleRate = cd.sampleRate
                            myFile.dataBit = cd.dataBits
                            myFile.wavDataTime = wav1.getWavDataTime()
                        }
                        cd.wavDataTime = wav1.getWavDataTime()
                    }
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

        if(cd.cdFileName != ""){
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
    fun extFilePath(mydirName: String, ExtFileName: String): String{
        val myDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getPath() +"/"+mydirName
        return  myDir+"/"+ ExtFileName
    }

    // 現在の外部ストレージのログ・ファイル名(パス含め)
    //fun extFilePath(mydirName : String, ExtFileName : String): String{
    //    val myDir = Environment.getExternalStorageDirectory().getPath() +"/"+mydirName
    //    return  myDir+"/"+ ExtFileName
    //}

}