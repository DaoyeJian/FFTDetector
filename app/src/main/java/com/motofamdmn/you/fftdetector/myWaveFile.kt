package com.motofamdmn.you.fftdetector

import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.RandomAccessFile

class myWaveFile {


    //全フラグメントからアクセス可能の共通データ
    private val cd = commonData.getInstance()

    private val FILESIZE_SEEK = 4
    private val DATASIZE_SEEK = 40
    private val SAMPLING_RATE = cd.sampleRate


    private var raf //リアルタイム処理なのでランダムアクセスファイルクラスを使用する
            : RandomAccessFile? = null
    private var recFile //録音後の書き込み、読み込みようファイル
            : File? = null
    //private var fileName = "/sdcard/teruuuSound.wav" //録音ファイルのパス

    private var count : Int = 0  //何点目のデータかを格納
    private val wavXData = arrayListOf<Float>()//wavのX軸データを保管、MPAndroidchartで描画できるようにFloat型になっている
    private val wavYData = arrayListOf<Float>() //wavのY軸データを保管、MPAndroidchartで描画できるようにFloat型になっている
    private var temp16Data = Array<Float>(16){ 0f }  //16点平均用のデータ

    private val RIFF = byteArrayOf(
        'R'.toByte(),
        'I'.toByte(),
        'F'.toByte(),
        'F'.toByte()
    ) //wavファイルリフチャンクに書き込むチャンクID用

    private var fileSize = 36
    private var wavDataTime = 0
    private val WAVE =
        byteArrayOf('W'.toByte(), 'A'.toByte(), 'V'.toByte(), 'E'.toByte()) //WAV形式でRIFFフォーマットを使用する

    private val fmt =
        byteArrayOf('f'.toByte(), 'm'.toByte(), 't'.toByte(), ' '.toByte()) //fmtチャンク　スペースも必要

    private val fmtSize = 16 //fmtチャンクのバイト数

    private val fmtID = byteArrayOf(1, 0) // フォーマットID リニアPCMの場合01 00 2byte

    private val chCount: Short = 1 //チャネルカウント モノラルなので1 ステレオなら2にする

    private val bytePerSec: Int = SAMPLING_RATE * (fmtSize / 8) * chCount //データ速度

    private val blockSize =
        (fmtSize / 8 * chCount).toShort() //ブロックサイズ (Byte/サンプリングレート * チャンネル数)

    private val bitPerSample: Short = 16 //サンプルあたりのビット数 WAVでは8bitか16ビットが選べる

    private val data =
        byteArrayOf('d'.toByte(), 'a'.toByte(), 't'.toByte(), 'a'.toByte()) //dataチャンク

    private var dataSize = 0 //波形データのバイト数

    var longRecordFlg : Int = 0

    fun createFile(fName: String) {
        var fileName = fName
        wavXData.clear()
        wavYData.clear()
        count = 0

        //	ファイルを作成
        recFile = File(fileName)
        if (recFile!!.exists()) {
            recFile!!.delete()
        }
        try {
            recFile!!.createNewFile()
        } catch (e: IOException) {
            //	TODO	Auto-generated	catch	block
            e.printStackTrace()
        }
        try {
            raf = RandomAccessFile(recFile, "rw")
        } catch (e: FileNotFoundException) {
            //	TODO	Auto-generated	catch	block
            e.printStackTrace()
        }

        //wavのヘッダを書き込み
        try {
            raf!!.seek(0)
            raf!!.write(RIFF)
            raf!!.write(littleEndianInteger(fileSize))
            raf!!.write(WAVE)
            raf!!.write(fmt)
            raf!!.write(littleEndianInteger(fmtSize))
            raf!!.write(fmtID)
            raf!!.write(littleEndianShort(chCount))
            raf!!.write(littleEndianInteger(SAMPLING_RATE)) //サンプリング周波数
            raf!!.write(littleEndianInteger(bytePerSec))
            raf!!.write(littleEndianShort(blockSize))
            raf!!.write(littleEndianShort(bitPerSample))
            raf!!.write(data)
            raf!!.write(littleEndianInteger(dataSize))
        } catch (e: IOException) {
            //	TODO	Auto-generated	catch	block
            e.printStackTrace()
        }
    }


    private fun littleEndianInteger(i: Int): ByteArray {
        val buffer = ByteArray(4)
        buffer[0] = i.toByte()
        buffer[1] = (i shr 8).toByte()
        buffer[2] = (i shr 16).toByte()
        buffer[3] = (i shr 24).toByte()
        return buffer
    }

    // PCMデータを追記するメソッド
    fun addBigEndianData(shortData: ShortArray) {

        // ファイルにデータを追記
        try {
            raf!!.seek(raf!!.length())
            raf!!.write(littleEndianShorts(shortData))
        } catch (e: IOException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }

        var tempX : Float = 0.0f
        var tempY : Float = 0.0f

        // wav波形を描画するためにX軸とY軸データを格納
        if(longRecordFlg == 0) {  //長時間録音モードでないならばデータ格納
            for (i in shortData.indices) {
                count++
                tempX = count.toFloat() / SAMPLING_RATE.toFloat()  //tempには現在のX軸の時間を格納、単位100msec
                tempY = shortData[i] * 100 / 32768.toFloat()  //wavデータ格納、符号付き16bit(-32768～32767)のパーセンテージとする
                temp16Data[count % 16] = tempY
                wavXData.add(tempX)
                wavYData.add(tempY)
            }  //wavデータ格納
        }else{
            for (i in shortData.indices) {
                count++
                //tempX = count.toFloat() / SAMPLING_RATE.toFloat()  //tempには現在のX軸の時間を格納、単位100msec
                tempY = shortData[i] * 100 / 32768.toFloat()  //wavデータ格納、符号付き16bit(-32768～32767)のパーセンテージとする
                temp16Data[count % 16] = tempY
            }
        }

        // ファイルサイズを更新
        updateFileSize()

        // データサイズを更新
        updateDataSize()
    }

    // ファイルサイズを更新
    private fun updateFileSize() {
        fileSize = (recFile!!.length() - 8).toInt()
        val fileSizeBytes = littleEndianInteger(fileSize)
        try {
            raf!!.seek(FILESIZE_SEEK.toLong())
            raf!!.write(fileSizeBytes)
        } catch (e: IOException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }
    }

    // データサイズを取得
    fun getDataSize(): Int {
        dataSize = (recFile!!.length() - 44).toInt()  //dataSizeはビッグエンディアンで最初が上位バイト
        return(dataSize)

    }

    // WAVデータの時間を取得（秒で返す）
    fun getWavDataTime(): Int {
        wavDataTime = ((recFile!!.length())/(bitPerSample/8)/chCount/SAMPLING_RATE).toInt()
        return(wavDataTime)

    }

    // wavDataを取得、MPAndroidchartで描画できるようにFloat型になっている
    fun getWaveData(): Pair<ArrayList<Float>,ArrayList<Float>> {
        return Pair(wavXData, wavYData)
    }

    // wavDataの16点平均を取得する
    fun get16PointsMaxWaveData(position : Int): Float {  //positionは現在位置

        var wavY16PointsMaxData = 0.0f

        if (count < 16) {  //データ数が16未満の場合
            if (count != 0) {  ////データ数が1～15の場合
                for (i in 0..count) {
                    if (kotlin.math.abs(temp16Data[i]) > wavY16PointsMaxData) {
                        wavY16PointsMaxData = kotlin.math.abs(temp16Data[i])
                    }
                }
            } else {
                wavY16PointsMaxData = 0.0f  //データ数が0の場合
            }
        } else {  //データ数が16以上の場合
            for (i in 0..15) {
                if (kotlin.math.abs(temp16Data[i]) > wavY16PointsMaxData) {
                    wavY16PointsMaxData = kotlin.math.abs(temp16Data[i])
                }
            }

        }
        return wavY16PointsMaxData
    }

    // データサイズを更新
    private fun updateDataSize() {
        dataSize = (recFile!!.length() - 44).toInt()
        val dataSizeBytes = littleEndianInteger(dataSize)
        try {
            raf!!.seek(DATASIZE_SEEK.toLong())
            raf!!.write(dataSizeBytes)
        } catch (e: IOException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }

    }

    // short型変数をリトルエンディアンのbyte配列に変更
    private fun littleEndianShort(s: Short): ByteArray? {
        val buffer = ByteArray(2)
        buffer[0] = s.toByte()
        buffer[1] = (s.toInt() shr 8) .toByte()
        return buffer
    }

    // short型配列をリトルエンディアンのbyte配列に変更
    private fun littleEndianShorts(s: ShortArray): ByteArray? {
        val buffer = ByteArray(s.size * 2)
        var i: Int
        i = 0
        while (i < s.size) {
            buffer[2 * i] = s[i].toByte()
            buffer[2 * i + 1] = (s[i].toInt() shr 8).toByte()
            i++
        }
        return buffer
    }


    // ファイルを閉じる
    fun close() {
        try {
            raf!!.close()
        } catch (e: IOException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }
    }


}