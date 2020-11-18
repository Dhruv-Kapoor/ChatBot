package com.example.chatbot

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import androidx.preference.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.io.IOException
import kotlin.random.Random.Default.nextInt

private const val TAG = "Classifier"

const val DIC_FILE_NAME = "dicFileName"
const val CLASSES_FILE_NAME = "classesFileName"
const val DATASET_FILE_NAME = "datasetFileName"
const val MODEL_FILE_NAME = "modelFileName"

class Classifier(private val context: Context) {

    private val preferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(context)
    }

    private var words: ArrayList<String>? = null
    private var classes: ArrayList<String>? = null
    private var dataset: JSONObject? = null
    private var interpreter: Interpreter? = null
    private val dicFileName = preferences.getString(DIC_FILE_NAME, "words_covid.json")?:"words_covid.json"
    private val classesFileName = preferences.getString(CLASSES_FILE_NAME, "classes_covid.json")?:"classes_covid.json"
    private val datasetFileName = preferences.getString(DATASET_FILE_NAME, "dataset_covid.json")?:"dataset_covid.json"
    private val modelFileName = preferences.getString(MODEL_FILE_NAME, "model_covid.tflite")?:"model_covid.tflite"

    init {
        loadData()
    }

    private fun bow(input: String): IntArray {
        Log.d(TAG, "bow: ")
        var str = ""

        //Remove punctuations
        for (c in input.toCharArray()) {
            if (c.isLetter()) {
                str += c.toLowerCase()
            } else {
                str += " "
            }
        }

        val bag = IntArray(words!!.size)
        val sentenceWords = str.split(" ")
        for (i in sentenceWords.indices) {
            for (j in words!!.indices) {
                if (sentenceWords[i] == words!![j]) {
                    bag[j] = 1
                    break
                }
            }
        }

        return bag

    }

    private fun predictClass(input: String): String? {
        Log.d(TAG, "predictClass: ")
        if (words.isNullOrEmpty() || classes.isNullOrEmpty() || dataset == null) {
            return null
        }

        val bag = bow(input)
        val inputs: Array<FloatArray> = arrayOf(bag.map { it.toFloat() }.toFloatArray())
        val outputs: Array<FloatArray> = arrayOf(FloatArray(classes!!.size))
        interpreter!!.run(inputs, outputs)

        var maxProb = 0f
        var maxProbIndex = -1
        for (i in outputs[0].indices) {
            if (outputs[0][i] > maxProb) {
                maxProb = outputs[0][i]
                maxProbIndex = i
            }
        }
        Log.d(TAG, "predictClass: maxProbabilityIndex $maxProbIndex")
        return classes!![maxProbIndex]
    }

    fun chatBotResponse(userInput: String): String? {
        Log.d(TAG, "chatBotResponse: ")
        val tag = predictClass(userInput) ?: return null
        if(tag=="Report") return "report"
        val intents = dataset!!.getJSONArray("intents")
        var result: String? = null
        for (i in 0 until intents.length()) {
            val intent = intents.getJSONObject(i)
            if (intent.getString("tag") == tag) {
                val responses = intent.getJSONArray("response")
                result = responses.getString(nextInt(responses.length()))
            }
        }
        return result
    }

    private fun loadData() {
        Log.d(TAG, "loadData: ")

        GlobalScope.launch(Dispatchers.IO) {

            //Dictionary
            launch {
                val jsonObject = JSONObject(loadJsonFromAsset(dicFileName))
                val iterator: Iterator<String> = jsonObject.keys()
                val data = ArrayList<String>()
                while (iterator.hasNext()) {
                    val key = iterator.next()
                    data.add(key)
                }
                words = data
            }

//            Classes
            launch {
                val jsonObject = JSONObject(loadJsonFromAsset(classesFileName))
                val iterator: Iterator<String> = jsonObject.keys()
                val data = ArrayList<String>()
                while (iterator.hasNext()) {
                    val key = iterator.next()
                    data.add(key)
                }
                classes = data
            }

//            DataSet
            launch {
                dataset = JSONObject(loadJsonFromAsset(datasetFileName))
            }
        }

        loadModel()
    }

    private fun unload() {
        words?.clear()
        interpreter?.close()
    }

    private suspend fun loadJsonFromAsset(fileName: String): String? {
        Log.d(TAG, "loadJsonFromAsset: $fileName")
        var json: String? = null
        try {
            val inputStream = context.assets.open(fileName, AssetManager.ACCESS_STREAMING)
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            json = String(buffer)
        } catch (ex: IOException) {
            ex.printStackTrace()
            return null
        }
        return json
    }

    private fun loadModel() {
        try {
            interpreter = Interpreter(
                FileUtil.loadMappedFile(context, modelFileName)
            )

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    fun destroy() {
        unload()
        words = null
        classes = null
        dataset = null
        interpreter = null
    }

}