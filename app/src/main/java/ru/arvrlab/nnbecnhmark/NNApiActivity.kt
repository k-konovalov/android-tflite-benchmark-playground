/**
 * Copyright 2017 The Android Open Source Project
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.arvrlab.nnbecnhmark

import android.app.Activity
import android.content.res.AssetManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NNApiActivity : Activity() {
    init {
        System.loadLibrary("basic")
    }

    private val LOG_TAG = "NNAPI_BASIC"
    private var modelHandle: Long = 0
    private val tv by lazy { findViewById<View>(R.id.textView) as TextView }

    private external fun initModel(assetManager: AssetManager?, assetName: String?): Long
    private external fun startCompute(modelHandle: Long, input1: Float, input2: Float): Float
    private external fun destroyModel(modelHandle: Long)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nnapi)
        //InitModelTask().execute("model_data.bin")
        initModelTask(arrayOf("model_data.bin"))

        val compute = findViewById<View>(R.id.button) as Button
        compute.setOnClickListener {
            if (modelHandle != 0L) {
                val edt1 = findViewById<View>(R.id.inputValue1) as EditText
                val edt2 = findViewById<View>(R.id.inputValue2) as EditText
                val inputValue1 = edt1.text.toString()
                val inputValue2 = edt2.text.toString()
                if (inputValue1 != "" && inputValue2 != "") {
                    Toast.makeText(applicationContext, "Computing",Toast.LENGTH_SHORT).show()
                    /*ComputeTask().execute(
                            java.lang.Float.valueOf(inputValue1),
                            java.lang.Float.valueOf(inputValue2))*/
                    try {
                        computeTask(arrayOf(inputValue1.toFloat(),inputValue2.toFloat()))
                    } catch (e:Exception){
                        Log.e(LOG_TAG, "Something happened during compute", e)
                    }
                }
            } else {
                Toast.makeText(applicationContext, "Model initializing, please wait",Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        if (modelHandle != 0L) {
            destroyModel(modelHandle)
            modelHandle = 0
        }
        super.onDestroy()
    }

    private fun initModelTask(modelName: Array<String>) {
        // Prepare the model in a separate thread.
        CoroutineScope(Dispatchers.Default).launch {
            if (modelName.size != 1) {
                Log.e(LOG_TAG, "Incorrect number of model files")
                return@launch
            }

            modelHandle = initModel(assets, modelName[0])
        }
    }

    private fun computeTask(inputs: Array<Float>){
        // Reusing the same prepared model with different inputs.
        CoroutineScope(Dispatchers.Default).launch {
            if (inputs.size != 2) {
                Log.e(LOG_TAG, "Incorrect number of input values")
                return@launch
            }

            tv.text = startCompute(modelHandle, inputs[0], inputs[1]).toString()
        }
    }

    /*private inner class InitModelTask : AsyncTask<String?, Void?, Long>() {
        override fun doInBackground(vararg modelName: String?): Long {
            if (modelName.size != 1) {
                Log.e(LOG_TAG, "Incorrect number of model files")
                return 0L
            }
            // Prepare the model in a separate thread.
            return initModel(assets, modelName[0])
        }

        override fun onPostExecute(result: Long) {
            modelHandle = result
        }
    }*/

    /*private inner class ComputeTask : AsyncTask<Float?, Void?, Float>() {
       override fun doInBackground(vararg inputs: Float?): Float {
            if (inputs.size != 2) {
                Log.e(LOG_TAG, "Incorrect number of input values")
                return 0.0f
            }
            // Reusing the same prepared model with different inputs.
            return startCompute(modelHandle, inputs[0]!!, inputs[1]!!)
        }

        override fun onPostExecute(result: Float) {
            tv.text = result.toString()
        }
    }*/
}