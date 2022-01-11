package ru.arvrlab.nnbecnhmark.camera

import android.content.Context
import android.util.DisplayMetrics
import android.util.Log
import android.util.Size
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class CustomCameraX(val viewLifecycleOwner: LifecycleOwner, val previewView: PreviewView, val appContext: Context) {
    private val TAG = "CustomCameraX"
    //Internal Camera
    private var camera: Camera? = null
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private val analysisExecutor: Executor =  Executors.newSingleThreadExecutor()
    private lateinit var mainExecutor: Executor
    private var cameraProvider: ProcessCameraProvider? = null

    //Internal Camera Settings
    private var displayId: Int = -1
    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private val RATIO_4_3_VALUE = 4.0 / 3.0
    private val RATIO_16_9_VALUE = 16.0 / 9.0

    val nnTestReport = MutableLiveData<String>()

    fun bindCamera(objectDetectorAnalyzer: ImageAnalysis.Analyzer? = null) {
        mainExecutor = ContextCompat.getMainExecutor(appContext)
        // Get screen metrics used to setup camera for full screen resolution
        val metrics = DisplayMetrics().also { previewView.display.getRealMetrics(it) }
        val screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels)
        val rotation = previewView.display.rotation

        Log.d(TAG, "Screen metrics: ${metrics.widthPixels} x ${metrics.heightPixels}")
        Log.d(TAG, "Preview aspect ratio: $screenAspectRatio")

        // Bind the CameraProvider to the LifeCycleOwner

        //val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

        Log.e(TAG, "---- CameraProvider Report Start")
        val cameraProviderFuture = ProcessCameraProvider.getInstance(appContext)
        cameraProviderFuture.addListener(
            futureListener(
                cameraProviderFuture,
                screenAspectRatio,
                rotation,
                cameraSelector,
                    objectDetectorAnalyzer
            ), mainExecutor
        )
    }

    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    private fun futureListener(
        cameraProviderFuture: ListenableFuture<ProcessCameraProvider>,
        screenAspectRatio: Int,
        rotation: Int,
        cameraSelector: CameraSelector,
        objectDetectorAnalyzer: ImageAnalysis.Analyzer?
    ) = Runnable {
        cameraProvider = cameraProviderFuture.get()

        preview = Preview.Builder()
                //.setTargetAspectRatio(screenAspectRatio)
                .setTargetResolution(Size(480,640))
                .setTargetRotation(rotation)
                .build()

        imageAnalyzer = imageAnalyzer ?: ImageAnalysis.Builder()
                //.setTargetAspectRatio(screenAspectRatio)
                .setTargetResolution(Size(480,640))
                .setTargetRotation(rotation)
                .build()

        objectDetectorAnalyzer?.apply {
            imageAnalyzer?.setAnalyzer(analysisExecutor, this)
        }

        /*
        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(rotation)
            .build()
            */

        bindUseCases(viewLifecycleOwner, cameraSelector, previewView)
    }

    private fun bindUseCases(
            viewLifecycleOwner: LifecycleOwner,
            cameraSelector: CameraSelector,
            internalCameraView: PreviewView){
        // Must unbind the use-cases before rebinding them.
        cameraProvider?.unbindAll()
        try {
            // A variable number of use-cases can be passed here - camera provides access to CameraControl & CameraInfo
            camera = cameraProvider?.bindToLifecycle(
                    viewLifecycleOwner, cameraSelector, preview, imageAnalyzer
            ) //imageCapture,

            val captureSize = imageCapture?.attachedSurfaceResolution ?: Size(0, 0)
            val previewSize = preview?.attachedSurfaceResolution ?: Size(0, 0)
            val analyzeSize = imageAnalyzer?.attachedSurfaceResolution ?: Size(0, 0)

            Log.e(TAG, "Use case res: capture_$captureSize preview_$previewSize analyze_$analyzeSize")

            preview?.setSurfaceProvider(internalCameraView.surfaceProvider)
        } catch (exc: Exception) {
            Log.e(TAG, "Preview binding failed. Attempt to use without", exc)
            try {
                camera = cameraProvider?.bindToLifecycle(viewLifecycleOwner, cameraSelector, imageAnalyzer)
            } catch (e: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }
    }

    fun switchAnalyzer(objectDetectorAnalyzer: ImageAnalysis.Analyzer) {
        imageAnalyzer?.clearAnalyzer()
        bindCamera(objectDetectorAnalyzer)
    }

    fun dispose(){
        cameraProvider?.unbindAll()
        //objectDetectorAnalyzer.disposeDetector()
    }
}