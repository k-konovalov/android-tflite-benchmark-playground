package ru.arvrlab.nnbecnhmark.utils

import android.app.Activity
import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.SystemClock
import android.util.DisplayMetrics
import android.util.Log
import android.util.Size
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import com.google.mediapipe.components.CameraHelper
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class CustomCameraHelper: CameraHelper(){
    private val TAG = "CameraXPreviewHelper"
    private val TARGET_SIZE = Size(1280, 720)
    private val CLOCK_OFFSET_CALIBRATION_ATTEMPTS = 3
    private var frameSize: Size? = null
    private var frameRotation = 0
    private var cameraCharacteristics: CameraCharacteristics? = null
    private var focalLengthPixels = 1.4E-45f
    private var cameraTimestampSource = 0

    private var camera: Camera? = null
    private var imageCapture: ImageCapture? = null
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private val analysisExecutor: Executor =  Executors.newSingleThreadExecutor()
    private lateinit var mainExecutor: Executor
    private var cameraProvider: ProcessCameraProvider? = null

    private val RATIO_4_3_VALUE = 4.0 / 3.0
    private val RATIO_16_9_VALUE = 16.0 / 9.0

    fun CameraXPreviewHelper() {}

    override fun startCamera(context: Activity, cameraFacing: CameraFacing, surfaceTexture: SurfaceTexture?) {
        this.startCamera(context, cameraFacing, surfaceTexture, TARGET_SIZE)
    }

    fun startCamera(viewLifecycleOwner: LifecycleOwner, internalCameraView: PreviewView, context: Context) {
        bindCamera(viewLifecycleOwner, internalCameraView, context)
    }

    private fun startCamera(context: Activity, cameraFacing: CameraFacing, surfaceTexture: SurfaceTexture?, targetSize: Size) {

/*        preview.setOnPreviewOutputUpdateListener { previewOutput ->
            if (!previewOutput.getTextureSize().equals(frameSize)) {
                frameSize = previewOutput.getTextureSize()
                frameRotation = previewOutput.getRotationDegrees()
                if (frameSize!!.width == 0 || frameSize!!.height == 0) {
                    Log.d("CameraXPreviewHelper", "Invalid frameSize.")
                    return@setOnPreviewOutputUpdateListener
                }
            }
            val selectedLensFacing = if (cameraFacing == CameraFacing.FRONT) 0 else 1
            cameraCharacteristics = getCameraCharacteristics(context, selectedLensFacing)
            if (cameraCharacteristics != null) {
                cameraTimestampSource = cameraCharacteristics!!.get(CameraCharacteristics.SENSOR_INFO_TIMESTAMP_SOURCE)!!
                focalLengthPixels = calculateFocalLengthInPixels()
            }
            if (this.onCameraStartedListener != null) {
                this.onCameraStartedListener.onCameraStarted(previewOutput.getSurfaceTexture())
            }
        }
        CameraX.bindToLifecycle((context as LifecycleOwner), arrayOf<UseCase?>(preview))*/
    }

    fun bindCamera(viewLifecycleOwner: LifecycleOwner, internalCameraView: PreviewView, context: Context) {
        mainExecutor = ContextCompat.getMainExecutor(context)
        // Get screen metrics used to setup camera for full screen resolution
        val metrics = DisplayMetrics().also { internalCameraView.display.getRealMetrics(it) }
        val screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels)
        val rotation = internalCameraView.display.rotation

        Log.d(TAG, "Screen metrics: ${metrics.widthPixels} x ${metrics.heightPixels}")
        Log.d(TAG, "Preview aspect ratio: $screenAspectRatio")

        // Bind the CameraProvider to the LifeCycleOwner

        //val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA


        Log.e(TAG, "---- CameraProvider Report Start")
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener(
                futureListener(
                        cameraProviderFuture,
                        screenAspectRatio,
                        rotation,
                        cameraSelector,
                        internalCameraView,
                        viewLifecycleOwner
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
            internalCameraView: PreviewView,
            viewLifecycleOwner: LifecycleOwner
    ) = Runnable {
        cameraProvider = cameraProviderFuture.get()

        preview = Preview.Builder()
                //.setTargetAspectRatio(screenAspectRatio)
                .setTargetRotation(rotation)
                .setTargetResolution(TARGET_SIZE)
                .build()

        cameraProvider?.unbindAll()
        try {
            // A variable number of use-cases can be passed here - camera provides access to CameraControl & CameraInfo
            camera = cameraProvider?.bindToLifecycle(
                    viewLifecycleOwner, cameraSelector, preview
            ) //imageCapture,
            //preview?.setSurfaceProvider(internalCameraView.createSurfaceProvider())
        } catch (exc: Exception) {
            Log.e(TAG, "Preview binding failed. Attempt to use without", exc)
        }
    }

    override fun isCameraRotated(): Boolean {
        return frameRotation % 180 == 90
    }

    override fun computeDisplaySizeFromViewSize(viewSize: Size): Size {
        frameSize?.apply {
            val optimalSize = getOptimalViewSize(viewSize)
            if(optimalSize.isNotZero()){
                return optimalSize
            } else {
                Log.d("CameraXPreviewHelper", "viewSize or frameSize is zero.")
            }
        }
         return Size(0,0)
    }

    private fun getOptimalViewSize(targetSize: Size): Size {
        if (cameraCharacteristics != null) {
            val outputSizes = cameraCharacteristics!!.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!.getOutputSizes(SurfaceTexture::class.java)
            var selectedWidth = -1
            var selectedHeight = -1
            var selectedAspectRatioDifference = 1000.0f
            val targetAspectRatio = targetSize.width.toFloat() / targetSize.height.toFloat()
            val var9 = outputSizes.size
            for (var10 in 0 until var9) {
                val size = outputSizes[var10]
                val aspectRatio = size.width.toFloat() / size.height.toFloat()
                val aspectRatioDifference = Math.abs(aspectRatio - targetAspectRatio)
                if (aspectRatioDifference <= selectedAspectRatioDifference && (selectedWidth == -1 && selectedHeight == -1 || size.width <= selectedWidth && size.width >= frameSize!!.width && size.height <= selectedHeight && size.height >= frameSize!!.height)) {
                    selectedWidth = size.width
                    selectedHeight = size.height
                    selectedAspectRatioDifference = aspectRatioDifference
                }
            }
            if (selectedWidth != -1 && selectedHeight != -1) {
                return Size(selectedWidth, selectedHeight)
            }
        }
        return Size(0,0)
    }

    fun getTimeOffsetToMonoClockNanos(): Long {
        return if (cameraTimestampSource == 1) getOffsetFromRealtimeTimestampSource() else getOffsetFromUnknownTimestampSource()
    }

    private fun getOffsetFromUnknownTimestampSource(): Long {
        return 0L
    }

    private fun getOffsetFromRealtimeTimestampSource(): Long {
        var offset = 9223372036854775807L
        var lowestGap = 9223372036854775807L
        for (i in 0..2) {
            val startMonoTs = System.nanoTime()
            val realTs = SystemClock.elapsedRealtimeNanos()
            val endMonoTs = System.nanoTime()
            val gapMonoTs = endMonoTs - startMonoTs
            if (gapMonoTs < lowestGap) {
                lowestGap = gapMonoTs
                offset = (startMonoTs + endMonoTs) / 2L - realTs
            }
        }
        return offset
    }

    fun getFocalLengthPixels(): Float {
        return focalLengthPixels
    }

    fun getFrameSize(): Size? {
        return frameSize
    }

    private fun calculateFocalLengthInPixels(): Float {
        val focalLengthMm = cameraCharacteristics!!.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)!![0]
        val sensorWidthMm = cameraCharacteristics!!.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)!!.width
        return frameSize!!.width.toFloat() * focalLengthMm / sensorWidthMm
    }

    private fun getCameraCharacteristics(context: Activity, lensFacing: Int): CameraCharacteristics? {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            val cameraList = Arrays.asList(*cameraManager.cameraIdList)
            val var4: Iterator<*> = cameraList.iterator()
            while (var4.hasNext()) {
                val availableCameraId = var4.next() as String
                val availableCameraCharacteristics = cameraManager.getCameraCharacteristics(availableCameraId)
                val availableLensFacing = availableCameraCharacteristics.get(CameraCharacteristics.LENS_FACING)
                if (availableLensFacing != null && availableLensFacing == lensFacing) {
                    return availableCameraCharacteristics
                }
            }
        } catch (var8: CameraAccessException) {
            Log.e("CameraXPreviewHelper", "Accessing camera ID info got error: $var8")
        }
        return null
    }

    fun Size.isNotZero():Boolean{
        return this.width!=0 && this.height !=0
    }
}