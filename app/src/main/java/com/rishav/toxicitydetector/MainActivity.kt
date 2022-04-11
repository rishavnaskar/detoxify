package com.rishav.toxicitydetector

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.Toast
import com.rishav.toxicitydetector.databinding.ActivityMainBinding
import kotlinx.coroutines.*
import java.util.*
import kotlin.coroutines.CoroutineContext

class MainActivity : AppCompatActivity(), CoroutineScope {
    private lateinit var activityMainBinding: ActivityMainBinding
    private lateinit var mJob: Job
    override val coroutineContext: CoroutineContext
        get() = mJob + Dispatchers.Main

    private val labels =
        listOf("Toxic", "Severe Toxic", "Obscene", "Threat", "Insult", "Identity Hate")
    private val thresholds = listOf(0.70, 0.30, 0.3, 0.15, 0.40, 0.20)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(activityMainBinding.root)
        mJob = Job()

        activityMainBinding.predictButton.isEnabled = false

        val classifier = ModelClassifier(this)

        async {
            withContext(Dispatchers.IO) {
                classifier.init()
                withContext(Dispatchers.Main) {
                    activityMainBinding.predictButton.isEnabled = true
                }
            }
        }

        activityMainBinding.predictButton.isHapticFeedbackEnabled = true
        activityMainBinding.predictButton.setOnClickListener {
            activityMainBinding.predictButton.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )

            if (activityMainBinding.inputText.text.isEmpty()) {
                Toast.makeText(this, "Text cant be empty", Toast.LENGTH_SHORT).show()
            } else {
                activityMainBinding.predictButton.isEnabled = false
                val text = activityMainBinding.inputText.text.toString().lowercase(Locale.ROOT).trim()
                val resArr = classifier.classifyText(text)
                activityMainBinding.predictButton.isEnabled = true
                updateResultUI(resArr)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateResultUI(res: FloatArray) {
        var isAboveThreshold = false

        val sortedRes = res.sorted().reversed()
        val labelList = mutableListOf<String>()
        val thresholdList = mutableListOf<Double>()
        for (i in 0..2) {
            val index = res.indexOfFirst { it == sortedRes[i] }
            labelList.add(labels[index])
            thresholdList.add(thresholds[index])
        }

        for (i in 0..2) {
            if (sortedRes[i] > thresholdList[i]) {
                isAboveThreshold = true
                break
            }
        }

        val percentSortedRes = sortedRes.map { it * 100 }

        if (isAboveThreshold) {
            activityMainBinding.resultLayout.visibility = View.VISIBLE
            activityMainBinding.cleanTextLayout.visibility = View.GONE
            val labelViews = listOf(
                activityMainBinding.firstLabel,
                activityMainBinding.secondLabel,
                activityMainBinding.thirdLabel
            )
            val valueViews = listOf(
                activityMainBinding.firstValue,
                activityMainBinding.secondValue,
                activityMainBinding.thirdValue
            )
            for (i in 0..2) {
                labelViews[i].text = labelList[i]
                valueViews[i].text = percentSortedRes[i].toString() + " %"
            }
        } else {
            activityMainBinding.cleanTextLayout.visibility = View.VISIBLE
            activityMainBinding.resultLayout.visibility = View.GONE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mJob.cancel()
    }
}