package com.motofamdmn.you.fftdetector

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.realm.RealmResults

class CustomRecyclerViewAdapter (realmResults: RealmResults<myFiles>): RecyclerView.Adapter<ViewHolder>(){

    private val rResults: RealmResults<myFiles> = realmResults
    // リスナー格納変数
    lateinit var listener: OnItemClickListener

    override fun onCreateViewHolder(parent: ViewGroup, position: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.one_result, parent, false)
        val viewholder = ViewHolder(view)
        return viewholder
    }

    override fun getItemCount(): Int {
        val size: Int = rResults.size
        return size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val files = rResults[position]
        holder.fileNameText?.text = files?.fileName.toString()
        holder.idText?.text = files?.id.toString()
        holder.sampleRateText?.text = files?.sampleRate.toString()
        holder.dataBitText?.text = files?.dataBit.toString()
        holder.fileSizeText?.text = files?.fileSize.toString()
        holder.stereoMonoralText?.text = files?.stereoMonoral.toString()
        holder.itemView.setBackgroundColor(if (position % 2 == 0) Color.LTGRAY else Color.WHITE)

        // タップしたとき
        holder.itemView.setOnClickListener {
            listener.onItemClickListener(it, position)

        }

    }

    //インターフェースの作成
    interface OnItemClickListener{
        fun onItemClickListener(view: View, position: Int)
    }

    // リスナー
    fun setOnItemClickListener(listener: OnItemClickListener){
        this.listener = listener
    }

}