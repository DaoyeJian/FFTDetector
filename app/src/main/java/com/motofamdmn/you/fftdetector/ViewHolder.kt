package com.motofamdmn.you.fftdetector

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.one_result.view.*

class ViewHolder (itemView: View): RecyclerView.ViewHolder(itemView) {

    var fileNameText: TextView? = null
    var idText: TextView? = null
    var sampleRateText: TextView? = null
    var stereoMonoralText: TextView? = null
    var dataBitText: TextView? = null
    var fileSizeText : TextView? = null
    var wavDataTimeText : TextView? = null

    init{

        fileNameText = itemView.fileName_text_list
        idText = itemView.id_text_list
        sampleRateText = itemView.sampleRate_text_list
        dataBitText = itemView.dataBit_text_list
        stereoMonoralText = itemView.stereoMonoral_text_list
        fileSizeText = itemView.fileSize_text_list
        wavDataTimeText = itemView.wavDataTime_text_list


    }

}