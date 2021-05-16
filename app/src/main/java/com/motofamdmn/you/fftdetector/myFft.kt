package com.motofamdmn.you.fftdetector

import kotlin.math.*

class myFft {


    //全フラグメントからアクセス可能の共通データ
    private val cd = commonData.getInstance()

    var wavData = arrayListOf(0.0f)
    var XR = FloatArray(cd.dataPoints) { 0f }
    var XI = FloatArray(cd.dataPoints) { 0f }
    var Wn = setWindowWn()  //ハニング窓関数の係数
    var nowPosition = 0  //wavデータの中で現在位置が何点目か
    var wavDataSize = 0  //wavデータがサイズ（点数）
    lateinit var fftData : List<FloatArray>

    //N:サンプリング数（データ数）、L:ビット数
    val N: Int = cd.dataPoints
    var L = cd.dataBit

    fun setWindowWn() : FloatArray {
        var tempWn = FloatArray( N ){ 0f }

        when(cd.window){
            1 -> {  //ハニング窓の場合
                for(i in 0..N-1){
                    tempWn[i] = (0.5 - 0.5 * cos (2 * 3.14159265 * i / N)).toFloat()
                }
            }

            else -> {  //矩形窓の場合
                for(i in 0..N-1){
                    tempWn[i] = 1.0f
                }
            }
        }

        return tempWn
    }

    fun sendWavData(waveData : ArrayList<Float>){
        wavData = ArrayList<Float>(waveData)
        wavDataSize = wavData.size
    }

    //wavデータ全体から現在の位置よりデータ点数N点のデータを読込、ただしfftAveragingでデータ読み込むので不要になった
    fun readDataOfNowPosition(position : Int){
        var ii : Int = 0
        nowPosition = position
        val lastPosition = position+N-1

        for (i in position..lastPosition) {
            XR[ii] = wavData[i]
            XI[ii] = 0.0f
            ii++
        }
        ii = XR.size

    }

    fun fftAveraging(position : Int) : FloatArray {
        var ii : Int = 0
        var fftAverageYData = FloatArray(N){0.0f}
        var fftYData = FloatArray(N){0.0f}
        val averagingInterval : Float = 0.1f
        val averagingIntervalPoints = (cd.sampleRate * averagingInterval).toInt()
        var lastPosition = 0
        var nowPosition = 0
        for(j in 0 until cd.averagingNum) {
            nowPosition = position + j * averagingIntervalPoints
            lastPosition = nowPosition+N-1
            ii = 0
            for (i in nowPosition..lastPosition) {
                XR[ii] = wavData[i] * Wn[ii]  //Wnは窓関数
                XI[ii] = 0.0f
                ii++
            }
            fftYData = fftAnalyze()
            for(i in 0 until N){
                fftAverageYData[i] = 1/ (j.toFloat() + 1) * fftYData[i] + j.toFloat()/ (j.toFloat() + 1) * fftAverageYData[i]
            }
        }

        return(fftAverageYData)

    }

    fun fftAnalyze() : FloatArray {

        var fftYData = FloatArray(N){0.0f}
        var A: Float = 0f
        var B: Float = 0f
        var C: Float = 0f
        var DEG: Float = 0f
        var S: Float = 0f
        var XXR: Float = 0f
        var XXI: Float = 0f
        var IR: Int = 1
        var J2: Int = N
        var K: Int = 0
        var K1: Int = 0
        var KK: Int = 0
        var L1: Int = 0
        var R: Int = L  //データ点数の2の何乗か、例：65536 なら 2^16　で16
        var pp: Int = 0
        var pole: Int = 0
        var BIT: Int = 0
        var ARG: Float = 0F
        val fftYDataWt : Float = (N.toFloat() / 2.0f * sqrt(2.0f))  //Y軸レベルを適正化、例：65536/2*sqrt(2) = 46340.9500

        IR = 1    //IR = 1 FFT, IR = -1 IFFT
        J2 = N
        DEG = (2 * 3.14159 / N).toFloat()
        K = 0
        L1 = R - 1
        for (L in 1..R) {
            J2 /= 2
            while (K < N) {   //X_CAL:
                for (i in 1..J2) {
                    pole = K.shr(L1)
                    BIT = bitGyaku(pole)
                    ARG = DEG * BIT
                    C = cos(ARG)
                    S = IR * sin(ARG)
                    K1 = K + J2
                    A = XR[K1] * C + XI[K1] * S
                    B = XI[K1] * C - XR[K1] * S
                    XR[K1] = XR[K] - A
                    XI[K1] = XI[K] - B
                    XR[K] = XR[K] + A
                    XI[K] = XI[K] + B
                    K = K + 1
                }
                K = K + J2
            }// GoTo X_CAL
            K = 0
            L1 = L1 - 1
        }//Next L

        KK = 0
        for (KK in 0..N - 1) {
            pp = KK
            BIT = bitGyaku(pp)
            if (BIT > KK) {  //Then GoTo KK_SKIP
                XXR = XR[KK]
                XR[KK] = XR[BIT]
                XR[BIT] = XXR
                XXI = XI[KK]
                XI[KK] = XI[BIT]
                XI[BIT] = XXI
            }
        }

        for (i in 0..N-1) {
            fftYData[i] = sqrt(XR[i].pow(2) + XI[i].pow(2)) /fftYDataWt
        }

        //FFTのY軸スケールをdB表示とする場合、基準は1.0x10^-6　→　改め基準は65536
        if(cd.ffTYUnit){
            for(i in 0..N-1){
                //fftYData[i] = log10(fftYData[i]*10.0f.pow(6))
                fftYData[i] = log10(fftYData[i]/65536f)
                //fftYData[i] = log10(fftYData[i]/fftYDataWt)
            }
        }

        return fftYData

    }


    fun bitGyaku(pole: Int): Int {

        //ビット反転のプラグラム

        var B1: Int = 0
        var B2: Int = 0
        var D1: Int = 0
        var BIT: Int = 0

        B1 = pole   //B1にビット反転前のデータをセット
        BIT = 0
        for (k in 1..L) {
            D1 = B1 % 2
            B2 = (B1 - D1) / 2
            BIT = BIT * 2 + B1 - 2 * B2
            B1 = B2
        }

        return (BIT)

    }

}