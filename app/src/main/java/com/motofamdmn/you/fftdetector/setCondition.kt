package com.motofamdmn.you.fftdetector

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_set_condition.*


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [setCondition.newInstance] factory method to
 * create an instance of this fragment.
 */
class setCondition : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private val cd = commonData.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_set_condition, container, false)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment setCondition.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            setCondition().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        //データ数設定 -> 分解能になった
        when (cd.dataPoints) {
            65536 -> {
                radio65536Btn.isChecked = true
            }
            16384 -> {
                radio32768Btn.isChecked = true
            }
            else -> {
                radio16384Btn.isChecked = true
            }
        }

        //データ数が2の何乗かをセット、FFT解析で使用する
        when (cd.dataPoints) {
            65536 -> {
                cd.dataBit = 16
            }
            16384 -> {
                cd.dataBit = 14
            }
            else -> {
                cd.dataBit = 14
            }
        }

        //FFTの平均化回数
        when (cd.averagingNum) {
            1 -> {
                av1radioButton.isChecked = true
            }
            8 -> {
                av8radioButton.isChecked = true
            }
            else -> {
                av16radioButton.isChecked = true
            }

        }

        //窓関数
        when (cd.window) {
            1 -> {
                hanningradioButton.isChecked = true
            }
            else -> {
                rectradioButton.isChecked = true
            }

        }

        dataPointRadioGroup.setOnCheckedChangeListener { group, checkedId ->
            // checkedIdから、選択されたRadioButtonを取得
            val radioButton = dataPointRadioGroup.checkedRadioButtonId
            when (radioButton) {
                radio65536Btn.id -> {
                    cd.dataPoints = 65536
                    cd.dataBit = 16
                }
                radio32768Btn.id -> {
                    cd.dataPoints = 16384
                    cd.dataBit = 14
                }
                else -> {
                    cd.dataPoints = 4096
                    cd.dataBit = 12
                }
            }
        }

        // FFTのY軸スケールをdB表示にするかどうか、TRUEでdB表示
        dbUnitSwitch.isChecked = cd.ffTYUnit
        dbUnitSwitch.setOnCheckedChangeListener { _, isChecked ->

            if (isChecked) {
                cd.ffTYUnit = java.lang.Boolean.TRUE
            } else {
                cd.ffTYUnit = java.lang.Boolean.FALSE
            }

        }

        averagingRadioGroup.setOnCheckedChangeListener { group, checkedId ->
            // checkedIdから、選択されたRadioButtonを取得
            val radioButton = averagingRadioGroup.checkedRadioButtonId
            when (radioButton) {
                av1radioButton.id -> {
                    cd.averagingNum = 1
                }
                av8radioButton.id -> {
                    cd.averagingNum = 4
                }
                else -> {
                    cd.averagingNum = 8
                }
            }
        }

        windowRadioGroup.setOnCheckedChangeListener { group, checkedId ->
            // checkedIdから、選択されたRadioButtonを取得
            val radioButton = windowRadioGroup.checkedRadioButtonId

            when (radioButton) {
                hanningradioButton.id -> {
                    hanningradioButton.isChecked = true
                    cd.window = 1
                }
                else -> {
                    rectradioButton.isChecked = true
                    cd.window = 0
                }

            }

        }

        //freq1Text入力時のfcFreq書き換え
        freq1Text.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                if (freq1Text.text.toString() != "") {
                    cd.fcFreq18 = freq1Text.text.toString().toFloat()
                } else {
                    cd.fcFreq18 = 0f
                }
            }
        })

        //freq2Text入力時のzfcFreq書き換え
        freq2Text.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                if (freq2Text.text.toString() != "") {
                    cd.zfcFreq18 = freq2Text.text.toString().toFloat()
                } else {
                    cd.zfcFreq18 = 0f
                }
            }
        })

        //freq3Text入力時のzfiFreq書き換え
        freq3Text.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                if (freq3Text.text.toString() != "") {
                    cd.zfiFreq18 = freq3Text.text.toString().toFloat()
                } else {
                    cd.zfiFreq18 = 0f
                }
            }
        })

        //freq4Text入力時のfbx2Freq書き換え
        freq4Text.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                if (freq4Text.text.toString() != "") {
                    cd.fbx2Freq18 = freq4Text.text.toString().toFloat()
                } else {
                    cd.fbx2Freq18 = 0f
                }
            }

        })

    }
}