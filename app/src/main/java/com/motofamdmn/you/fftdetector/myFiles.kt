package com.motofamdmn.you.fftdetector

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class myFiles: RealmObject() {

    @PrimaryKey
    var id : Long = 0
    var fileName : String = ""
    var fileSize : Long = 0L
    var sampleRate : Int = 0
    var stereoMonoral : Int = 0
    var dataBit : Int = 0

}