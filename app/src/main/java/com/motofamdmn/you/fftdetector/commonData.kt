package com.motofamdmn.you.fftdetector

import java.lang.Boolean

class commonData {

    var cdFileName : String = ""
    var stereoMonoral : Int = 0
    var dataBits : Int = 0
    var wavDataTime : Int = 0

    var shareWavXData = arrayListOf<Float>()
    var shareWavYData = arrayListOf<Float>()
    var sampleRate = 44100
    var dataPoints = 16384
    var dataBit = 14  //dataPointsが2の何乗か、FFT解析で使用する
    var ffTYUnit = Boolean.TRUE  //FFTのY軸表示単位、1がdB表示でリファレンスは1.0x10^(-6)
    var averagingNum : Int = 1  //FFTの平均化回数
    var window = 1  //窓関数　0:矩形窓、1:ハニング窓、係数はmyFft内のsetWindowWnメソッドで設定

    //ベアリングの周波数（FFT描画用）
    var rotateSpeed = 1800.0f
    var frFreq = 30f
    var fcFreq = 0f
    var zfcFreq = 0f
    var zfiFreq = 0f
    var fbx2Freq = 0f

    //ベアリングの周波数（基準となる1800rpmの周波数）
    var frFreq18 = 30f
    var fcFreq18 = 0f
    var zfcFreq18 = 0f
    var zfiFreq18 = 0f
    var fbx2Freq18 = 0f

    //FFTのマーカーサイドバンド描画用
    var sideFftXData1 = 0f
    var sideFftXData2 = 0f
    var sideFftXData3 = 0f
    var sideFftXData4 = 0f
    var sideFftXData5 = 0f
    var sideFftXData6 = 0f


    companion object {

        private var instance : commonData? = null

        fun  getInstance(): commonData {
            if (instance == null)
                instance = commonData()

            return instance!!
        }
    }

}