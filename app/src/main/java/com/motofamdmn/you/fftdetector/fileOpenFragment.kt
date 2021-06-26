package com.motofamdmn.you.fftdetector

import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import io.realm.Realm
import io.realm.RealmResults
import io.realm.kotlin.where
import kotlinx.android.synthetic.main.fragment_file_open.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [fileOpenFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class fileOpenFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private lateinit var realm: Realm
    private lateinit var sch: RealmResults<myFiles>

    //全フラグメントからアクセス可能の共通データ
    private val cd = commonData.getInstance()

    private val HEADER_ONLY = 1
    private val READ_DATA = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
        realm = Realm.getDefaultInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_file_open, container, false)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment fileOpenFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            fileOpenFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        list.layoutManager = LinearLayoutManager(getActivity())
        sch = realm.where<myFiles>().findAll()
        val adapter = CustomRecyclerViewAdapter(sch)
        list.adapter = adapter

        // インターフェースの実装
        adapter.setOnItemClickListener(object:CustomRecyclerViewAdapter.OnItemClickListener{
            override fun onItemClickListener(view: View, position: Int) {
                val selectedMyFile = sch.get(position)
                val selectedFileName = selectedMyFile?.fileName
                if (selectedFileName != null) {
                    cd.cdFileName = selectedFileName
                }
                Toast.makeText(context, "${selectedFileName}がタップされました", Toast.LENGTH_LONG).show()

                //選択したwavファイルデータをshareWavYDataへ読込
                val mWav = mWavRead()
                //val selectedFileNamePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getPath() +"/testwave/"+selectedFileName
                if (selectedFileName != null) {
                    myWavRead(selectedFileName, READ_DATA)
                }  //HEADER_ONLYはデータ本体は読み込まない、READ_DATAでデータも読む
            }
        })


    }

    fun myWavRead(fileName : String, headerOnly : Int ) {

        //全フラグメントからアクセス可能の共通データ
        val cd = commonData.getInstance()

        var stereoMonoral : Int = 0  //0：モノラル、 1：ステレオ
        var sampleRate : Int = 0  //サンプリングレート
        var dataBit : Int = 0  //データが8ビットか16ビットか
        var wavDataSize = 0  //wavデータのサイズ

        //fun read(fileName : String, headerOnly : Int ) {

        // ByteArrayOutputStreamの生成
        val byteArrayOutputStream = ByteArrayOutputStream()
        val buffer4 = ByteArray(4) //4byte読込用
        val buffer2 = ByteArray(2) //2byte読込用
        var flg: Int = 0
        var bufTemp: Int = 0
        var bufTempStr = ""
        var readSize: Int = 0
        var idx: Int = 0
        var ddd: Int = 0
        var dat: Int = 0
        var fileReadProgress : Float = 0f
        fileReadProgressBar.progress = 0


        // WAVファイルを開く
        val fileNamePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getPath() +"/testwave/"+fileName
        val myFile = File(fileNamePath)
        val myFileSize = myFile.length()
        val file = FileInputStream(myFile)

        try {

            // RIFF識別子読込
            while (flg != 1) {

                // データの読み込み
                idx += readSize
                readSize = file.read(buffer4)
                bufTempStr = ""

                for (j in 0..3) {

                    bufTemp = buffer4[j].toInt() and 0xFF
                    bufTempStr += bufTemp.toString(16)
                    // ログの出力
                    Log.d("fmt:", bufTempStr)
                }

                if (bufTempStr == "52494646") {
                    //RIFF形式であることを検出"
                    flg = 1
                }
            }

            //ファイル全体サイズ
            idx += readSize
            readSize = file.read(buffer4)
            bufTempStr = ""
            for (j in 0..3) {

                bufTemp = buffer4[3 - j].toInt() and 0xFF
                bufTempStr += bufTemp.toString(16)

            }
            val fSize = bufTempStr.toInt(16) + 8

            flg = 0

            // WAVE識別子読込
            while (flg != 1) {

                // データの読み込み
                idx += readSize
                readSize = file.read(buffer4)
                bufTempStr = ""

                // fmt識別子読込
                for (j in 0..3) {

                    bufTemp = buffer4[j].toInt() and 0xFF
                    bufTempStr += bufTemp.toString(16)

                }

                if (bufTempStr == "57415645") {
                    //WAVE形式を検出
                    flg = 1
                }
            }

            flg = 0

            // fmt識別子読込
            while (flg != 1) {

                // データの読み込み
                idx += readSize
                readSize = file.read(buffer4)
                bufTempStr = ""

                // RIFF識別子読込
                for (j in 0..3) {

                    bufTemp = buffer4[j].toInt() and 0xFF
                    bufTempStr += bufTemp.toString(16)

                }

                if (bufTempStr == "666d7420") {
                    //fmt形式検出
                    flg = 1
                }
            }


            idx += readSize
            readSize = file.read(buffer4)
            bufTemp = buffer4[0].toInt()
            if (bufTemp == 16) {
                //リニアPCMを検出
            }

            idx += readSize
            readSize = file.read(buffer2)
            bufTemp = buffer2[0].toInt()
            if (bufTemp == 1) {
                //同じくリニアPCMを検出
            }

            idx += readSize
            readSize = file.read(buffer2)
            bufTemp = buffer2[0].toInt()
            if (bufTemp == 2) {
                stereoMonoral = 1
            } else if (bufTemp == 1) {
                stereoMonoral = 0
            }
            cd.stereoMonoral = stereoMonoral

            //サンプリング周波数
            idx += readSize
            readSize = file.read(buffer4)
            bufTempStr = ""
            for (j in 0..1) {

                bufTemp = buffer4[1 - j].toInt() and 0xFF
                bufTempStr += bufTemp.toString(16)

            }

            sampleRate = bufTempStr.toInt(16)
            cd.sampleRate = sampleRate

            //1秒あたりのバイト数平均はとばす
            idx += readSize
            readSize = file.read(buffer4)

            //ブロックサイズはとばす
            idx += readSize
            readSize = file.read(buffer2)

            //ビット数
            idx += readSize
            readSize = file.read(buffer2)
            bufTemp = buffer2[0].toInt()
            if (bufTemp == 16) {
                dataBit = 16
            } else if (bufTemp == 8) {
                dataBit = 8
            }
            cd.dataBits = dataBit

            flg = 0

            // data識別子読込
            while (flg != 1) {

                // データの読み込み
                idx += readSize
                readSize = file.read(buffer4)
                bufTempStr = ""

                // fmt識別子読込
                for (j in 0..3) {

                    bufTemp = buffer4[j].toInt() and 0xFF
                    bufTempStr += bufTemp.toString(16)
                    // ログの出力
                    Log.d("fmt:", bufTempStr)
                }

                if (bufTempStr == "64617461") {
                    //DATA形式を検出
                    flg = 1
                }
            }

            //波形データのバイト数
            idx += readSize
            readSize = file.read(buffer4)
            bufTemp = 0
            bufTempStr = ""
            for (j in 0..3) {

                bufTemp = buffer4[j].toInt() and 0xFF
                bufTempStr += bufTemp.toString(16)
                // ログの出力
                Log.d("fmt:", bufTempStr)
            }

            if(headerOnly!=1){

                flg = 0

                // data本体読込
                var j = 0
                idx += readSize
                wavDataSize = fSize - idx
                val dataPointsOf20Sec = idx + sampleRate * 20 * dataBit / 8  //20秒後のデータ位置
                var readMaxPoint = 0
                //ファイルサイズ-2byte（fSize-2）と20秒後のデータ位置（dataPointsOf20Sec）比較
                //小さいほうをreadMaxPointとしてそこまで読み込む
                if(fSize -2 > dataPointsOf20Sec) {
                    readMaxPoint = dataPointsOf20Sec
                }else{
                    readMaxPoint = fSize -2
                }

                //プログレスバーを初期化
                fileReadProgressBar.max = readMaxPoint
                fileReadProgressBar.progress = 0

                //データクリア
                cd.shareWavXData.clear()
                cd.shareWavYData.clear()
                while(idx < readMaxPoint) {
                    // データの読み込み
                    readSize = file.read(buffer2)
                    idx += readSize
                    bufTempStr = ""

                    bufTemp = buffer2[1].toInt() and 0xFF
                    if (bufTemp < 16) bufTempStr += "0"
                    bufTempStr += bufTemp.toString(16)
                    bufTemp = buffer2[0].toInt() and 0xFF
                    if (bufTemp < 16) bufTempStr += "0"
                    bufTempStr += bufTemp.toString(16)
                    ddd = bufTempStr.toInt(16)

                    bufTemp = ddd and 0x8000
                    dat = ddd and 0x7FFF
                    if (bufTemp == 0x8000) {
                        dat = -(32768 - dat)
                    }
                    cd.shareWavXData.add(j.toFloat() / sampleRate)
                    cd.shareWavYData.add(dat.toFloat() / 1000000.0f)
                    fileReadProgressBar.progress = idx
                    j += 1
                }
            }
            // close処理
            file.close()
            byteArrayOutputStream.close()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
