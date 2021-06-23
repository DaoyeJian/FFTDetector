package com.motofamdmn.you.fftdetector

import android.os.Environment
import android.util.Log
import io.realm.Realm
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream

class myWavRead {

    //全フラグメントからアクセス可能の共通データ
    private val cd = commonData.getInstance()

    private var stereoMonoral : Int = 0  //0：モノラル、 1：ステレオ
    private var sampleRate : Int = 0  //サンプリングレート
    private var dataBit : Int = 0  //データが8ビットか16ビットか
    private var wavDataSize = 0  //wavデータのサイズ

    fun read(fileName : String ) {

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

            flg = 0

            // data本体読込
            idx += readSize
            wavDataSize = fSize - idx
            val dataPointsOf20Sec = sampleRate * 3  //3秒分のデータ数
            for(i in 0..65535){  //今の位置から65536*2バイト分読み込む（buffer2は2バイト分の読込）
                readSize = file.read(buffer2)
                idx += readSize
            }

            //データクリア
            cd.shareWavXData.clear()
            cd.shareWavYData.clear()
            //while(idx < fSize) {
            for (j in 0..dataPointsOf20Sec - 1) {
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
            }
//            }
            // close処理
            file.close()
            byteArrayOutputStream.close()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}