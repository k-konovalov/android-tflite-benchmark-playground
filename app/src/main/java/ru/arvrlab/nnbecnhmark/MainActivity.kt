/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.arvrlab.nnbecnhmark

import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.view.PreviewView
import androidx.lifecycle.Observer
import com.android.example.cameraxbasic.R
import ru.arvrlab.nnbecnhmark.camera.CustomCameraX
import ru.arvrlab.nnbecnhmark.camera.ObjectDetectorAnalyzer
import ru.arvrlab.nnbecnhmark.mediapipe.MediaPipeHelper
import ru.arvrlab.nnbecnhmark.utils.ObjectDetectorConfigList
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers


/**
 * Main entry point into our app. This app follows the single-activity pattern, and all
 * functionality is implemented in the form of fragments.
 */
class MainActivity : AppCompatActivity() {
    private val cameraX: CustomCameraX by lazy(Dispatchers.Default) {CustomCameraX(this, previewView, applicationContext)}
    private val previewView by lazy { findViewById<PreviewView>(R.id.previewView) }
    private val nnConfigList = ObjectDetectorConfigList()
    private val tempList = nnConfigList.getNNSet7()
    private var report = ""
//    private val mediaPipeHelper = MediaPipeHelper()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        requestPermissions(arrayOf("android.permission.CAMERA"),1)
        previewView.apply {
            //preferredImplementationMode = PreviewView.ImplementationMode.SURFACE_VIEW
            post {
                cameraX.bindCamera(null)
            }
        }
        btnLaunchMediapipe.setOnClickListener {
           // mediaPipeHelper.initMediaPipe(applicationContext, applicationInfo, preview_display_layout)
           // mediaPipeHelper.onResume(this, applicationInfo)
        }

        //cameraX.bindCamera(this@MainActivity, previewView, this@MainActivity)

        btnTestTflite.setOnClickListener {
            report = "\n Report of NN Test. Total NN Count: ${tempList.size}\n"
            launchTestNN()
            //testPipeline()
        }

        btnLogLastTest.setOnClickListener {
            Log.e("MainActivity", report)
        }

        cameraX.nnTestReport.observe(this, Observer {
            it?.apply {
                report += this
            }

            launchTestNN()
        })
    }

    private fun launchTestNN() {
        txtInfo.text = "${tempList.size} left"
        if (tempList.isNotEmpty()) {
            try {
                cameraX.switchAnalyzer(
                        ObjectDetectorAnalyzer(
                                context = this,
                                config = tempList[0],
                                testReport = cameraX.nnTestReport
                        )
                )
                tempList.removeAt(0)
            } catch (e: Exception) {
                if (tempList.isNotEmpty()) {
                    val msg = "\n------------ Current Config:\nModel filename: ${tempList[0].modelFile}\nInit test is corrupted by ${e.message?.trim()}"
                    //Log.e("MainActivity", msg)
                    report += msg

                    tempList.removeAt(0)
                    launchTestNN()
                } else Log.e("MainActivity", report)
            }

        } else Log.d("MainActivity", report)
    }

    private fun testPipeline() {
        cameraX.apply {
            switchAnalyzer(ObjectDetectorAnalyzer(
                    context = this@MainActivity,
                    config = tempList[0],
                    secondConfig = tempList[1],
                    testReport = nnTestReport
            ))
        }
    }

    override fun onResume() {
        super.onResume()
        //mediaPipeHelper.onResume(this, applicationInfo)
    }

    override fun onPause() {
        super.onPause()
        //mediaPipeHelper.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraX.dispose()
    }
}


