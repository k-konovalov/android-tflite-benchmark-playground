package ru.arvrlab.nnbecnhmark.camera

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Size
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.lifecycle.MutableLiveData
import ru.arvrlab.nnbecnhmark.detection.ObjectDetector
import ru.arvrlab.nnbecnhmark.utils.NNType
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import ru.`object`.detection.detection.DetectionResult
import ru.`object`.detection.util.DetectorUtils
import ru.`object`.detection.util.ImageUtil
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger


class ObjectDetectorAnalyzer(
        private val context: Context,
        private val config: Config,
        private val secondConfig: Config? = null,
        private val testReport: MutableLiveData<String>,
        private val contours: MutableLiveData<List<PointF>>? = null

) : ImageAnalysis.Analyzer {
    private val TAG = "ObjectDetectorAnalyzer"
    data class Config(
            val minimumConfidence: Float = 0.5f,
            val numDetection: Int = 10,
            val modelFile: String,
            val labelsFile: String = "",
            val nnType: NNType,
            val isGAN: Boolean = false,
            val isQuantized: Boolean = false,
            val isNnapiEnabled: Boolean = false,
            val isGpuEnabled: Boolean = false,
            val size: Size,
            val numThreads: Int = 4
    )
    data class Result(
            val objects: List<DetectionResult>,
            val imageWidth: Int,
            val imageHeight: Int,
            val imageRotationDegrees: Int
    )

    private val uiHandler = Handler(Looper.getMainLooper())
    private var iterationCounter = AtomicInteger(0)

    private val debugHelper = DebugHelper(
            saveResult = false,
            context = context,
            resultHeight = config.size.height,
            resultWidth = config.size.width
    )

    private var rgbArray: IntArray? = null
    private var inputArray = IntArray(config.size.width * config.size.height)
    private var inputArray2 = if (secondConfig == null) null else IntArray(secondConfig.size.width * secondConfig.size.height)

    private var rgbBitmap: Bitmap? = null
    private var resizedBitmap = Bitmap.createBitmap(config.size.width, config.size.height, Bitmap.Config.ARGB_8888)

    private var matrixToInput: Matrix? = null

    private lateinit var byteBuffer: ByteBuffer
    private lateinit var byteBuffer2: ByteBuffer

    // Specify the input size
    private val DIM_BATCH_SIZE = 1
    private val DIM_IMG_SIZE_X = config.size.width//224
    private val DIM_IMG_SIZE_Y = config.size.height//224
    private val DIM_PIXEL_SIZE = 3
    //4 * BATCH_SIZE * inputSize * inputSize * PIXEL_SIZE
    //val sizeForBuffer = DIM_BATCH_SIZE * DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y * DIM_PIXEL_SIZE

    private var objectDetector = if (secondConfig == null) initObjectDetectorWith(config) else initObjectDetectorWith(config, secondConfig)

    private val preProcessResults = mutableListOf<Long>()
    private val predictResults = mutableListOf<Long>()
    private val postProcessResults = mutableListOf<Long>()
    private val fullExecutionResults = mutableListOf<Long>()

    private var preProcessTime = 0L
    private var predictTime = 0L
    private var postProcessTime = 0L
    private var fullExecutionTime = 0L

    val customExecutor = Executors.newSingleThreadExecutor()
    init {
        Log.d(TAG, formatSettingsLog())
    }

    private fun initObjectDetectorWith(config: Config) = ObjectDetector(
            ObjectDetector.Config(
                    assetManager = context.assets,
                    isModelQuantized = config.isQuantized,
                    size = config.size,
                    labelFilename = config.labelsFile,
                    modelFilename = config.modelFile,
                    numDetections = config.numDetection,
                    minimumConfidence = config.minimumConfidence,
                    numThreads = config.numThreads,
                    useNnapi = config.isNnapiEnabled,
                    useGpu = config.isGpuEnabled)
    )

    private fun initObjectDetectorWith(config: Config, secondConfig: Config) = ObjectDetector(
            ObjectDetector.Config(
                    assetManager = context.assets,
                    isModelQuantized = config.isQuantized,
                    size = config.size,
                    labelFilename = config.labelsFile,
                    modelFilename = config.modelFile,
                    numDetections = config.numDetection,
                    minimumConfidence = config.minimumConfidence,
                    numThreads = config.numThreads,
                    useNnapi = config.isNnapiEnabled,
                    useGpu = config.isGpuEnabled),
            ObjectDetector.Config(
                    assetManager = context.assets,
                    isModelQuantized = secondConfig.isQuantized,
                    size = secondConfig.size,
                    labelFilename = secondConfig.labelsFile,
                    modelFilename = secondConfig.modelFile,
                    numDetections = secondConfig.numDetection,
                    minimumConfidence = secondConfig.minimumConfidence,
                    numThreads = secondConfig.numThreads,
                    useNnapi = secondConfig.isNnapiEnabled,
                    useGpu = secondConfig.isGpuEnabled)
    )

    override fun analyze(image: ImageProxy) {
        image.use {
            if (iterationCounter.get() < 101) {//101
                newWay(it)
                Log.i(TAG, "iteration: ${iterationCounter.get()}")
            }
            if ((iterationCounter.get() == 101 && predictResults.isNotEmpty())) {
                predictResults.removeAt(0)
                iterationCounter.incrementAndGet()

                testReport.postValue(formatSettingsLog() + formatAverageLog())
                Log.i(TAG, formatSettingsLog() + formatAverageLog())
            }
        }
    }

    private fun yuvToBitmap(imageProxy: ImageProxy): Bitmap? {
        val byteBufferY: ByteBuffer = imageProxy.planes[0].buffer.slice() //921600 for Y 1280x720 (width x height)
        val byteBufferU: ByteBuffer = imageProxy.planes[1].buffer.slice()
        val byteBufferV: ByteBuffer = imageProxy.planes[2].buffer.slice()

        val imageBytesY = ByteArray(byteBufferY.remaining())
        val imageBytesU = ByteArray(byteBufferU.remaining())
        val imageBytesV = ByteArray(byteBufferV.remaining())

        byteBufferY.get(imageBytesY)
        byteBufferU.get(imageBytesU)
        byteBufferV.get(imageBytesV)

        val yuvBytes = imageBytesY + imageBytesV + imageBytesU
        val out = ByteArrayOutputStream()
        val yuvImage = YuvImage(yuvBytes, ImageFormat.NV21, imageProxy.width, imageProxy.height, null)

        yuvImage.compressToJpeg(Rect(0, 0, imageProxy.width, imageProxy.height), 100, out)
        val imageBytes: ByteArray = out.toByteArray()
        out.close()

        val options = BitmapFactory.Options()
        options.inPreferredConfig = Bitmap.Config.RGB_565
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)
        val matrix = Matrix()
        matrix.preScale(-1.0f, 1.0f)
        matrix.postRotate(90f);
        val mirroredBitmap: Bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, false)
        return mirroredBitmap

    }

    private fun newWay(image: ImageProxy){
        val startTime = System.currentTimeMillis()

        preProcessTime = preProcessNew(image, startTime)
        predictTime = runInference()
        postProcessTime = postProcessNew()
        fullExecutionTime = System.currentTimeMillis() - startTime

        fillAndShowTimeLogs(true)
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    private fun preProcessNew(image: ImageProxy, startTime: Long): Long{
        val originalBitmap = FirebaseVisionImage
                .fromMediaImage(image.image!!, FirebaseVisionImageMetadata.ROTATION_90)
                .bitmap

        resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, config.size.width, config.size.height, false)
        //Log.e(TAG,"resizedBitmap bytes: "+resizedBitmap.byteCount.toString())

        ImageUtil.storePixels(resizedBitmap, inputArray)

        DetectorUtils.apply {
            byteBuffer = createImageBuffer(config.size, config.isQuantized)
            fillBuffer(
                    imgData = byteBuffer,
                    pixels = inputArray,
                    size = config.size,
                    isModelQuantized = config.isQuantized
            )
        }

        secondConfig?.let {
            resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, it.size.width, it.size.height, false)
            //Log.e(TAG,"resizedBitmap bytes: "+resizedBitmap.byteCount.toString())

            inputArray2?.apply {
                ImageUtil.storePixels(resizedBitmap, this)

                byteBuffer2 = DetectorUtils.createImageBuffer(it.size, it.isQuantized)
                DetectorUtils.fillBuffer(
                        imgData = byteBuffer2,
                        pixels = this,
                        size = it.size,
                        isModelQuantized = it.isQuantized
                )
            }
        }

        return System.currentTimeMillis() - startTime
    }

    private fun runInference(): Long {
        secondConfig?.let {
            return objectDetector.doubleDetect(
                    imgData1 = byteBuffer,
                    imgData2 = byteBuffer2,
                    NNType1 = config.nnType,
                    NNType2 = it.nnType
            )
        }
        return if (config.isGAN) detectGAN()
        else objectDetector.newDetect(byteBuffer, config.nnType)
    }

    private fun detectGAN(): Long{
        //DetectGAN
        val secondByteBuffer = DetectorUtils.createImageBuffer(config.size, config.isQuantized)
        DetectorUtils.fillBuffer(
                imgData = secondByteBuffer,
                pixels = inputArray,
                size = config.size,
                isModelQuantized = config.isQuantized
        )
        return objectDetector.detectGan(arrayOf(byteBuffer, secondByteBuffer), config.nnType)
    }

    private fun postProcessNew(): Long{
        val startTime = System.currentTimeMillis()
        iterationCounter.getAndIncrement()

        return System.currentTimeMillis() - startTime
    }

    private fun fillAndShowTimeLogs(showLog: Boolean = false){
        preProcessResults.add(preProcessTime)
        predictResults.add(predictTime)
        postProcessResults.add(postProcessTime)
        fullExecutionResults.add(fullExecutionTime)
        if(showLog) Log.i(TAG, formatExecutionLog())
    }

    fun testWays(it: ImageProxy){
        val startTime1 = System.currentTimeMillis()
        oldWay(it, 0)

        val endTime1 = System.currentTimeMillis()
        oldWayOptimized(it)

        Log.e(TAG, "Time:\n" +
                "OldWay time: ${endTime1 - startTime1}ms\n" +
                "OldWay2 time: ${System.currentTimeMillis() - endTime1}ms"
        )
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    private fun oldWayOptimized(image: ImageProxy){
        val startTime = System.currentTimeMillis()

        val fbImg = FirebaseVisionImage.fromMediaImage(image.image!!, FirebaseVisionImageMetadata.ROTATION_90)
        val fbTime = System.currentTimeMillis()


        val beforePreProcess = System.currentTimeMillis()
        //inputArray = preProcess(fbImg.bitmap)

        resizedBitmap = Bitmap.createScaledBitmap(fbImg.bitmap, config.size.width, config.size.height, false)
        /*Log.e(TAG, "scaled bytecount: " + resizedBitmap.byteCount.toString() + " sizeForBuffer: $sizeForBuffer")

        byteBuffer = ByteBuffer.allocateDirect(sizeForBuffer).also{
            it.order(ByteOrder.nativeOrder())
            it.rewind()
        }
        resizedBitmap.copyPixelsToBuffer(byteBuffer)*/
        ImageUtil.storePixels(resizedBitmap, inputArray)

        val beforeDetectTime = System.currentTimeMillis()


        objectDetector.oldDetect(byteBuffer!!, inputArray)

        /* objectDetector.newDetect(byteBuffer!!).also {
             val iteration = iterationCounter.getAndIncrement()
             Log.i(TAG, "$iteration) Inference Time: ${System.currentTimeMillis() - beforeDetectTime}ms")
             Log.d(TAG, "All inference: ${System.currentTimeMillis() - startTime}ms")
         }*/
        //Log.d(TAG, "Fb Time: ${fbTime - startTime}ms")
        //Log.d(TAG, "Preprocess Time: ${System.currentTimeMillis() - beforePreProcess}ms")
        //Log.d(TAG, "All Time: ${System.currentTimeMillis() - startTime}ms")
    }

    private fun oldWay(image: ImageProxy, rotationDegrees: Int){
        val startTime = System.currentTimeMillis()
        val iteration = iterationCounter.getAndIncrement()
        val rgbArray = convertYuvToRgb(image)
        val rgbBitmap = getRgbBitmap(rgbArray, image.width, image.height)
        val transformation = getTransformation(rotationDegrees, image.width, image.height)

        Canvas(resizedBitmap).drawBitmap(rgbBitmap, transformation, null)

        ImageUtil.storePixels(resizedBitmap, inputArray)
        val objects = objectDetector.oldDetect(byteBuffer!!, inputArray)

        /*objects.forEach {
            Log.d(TAG, "Title: "+ it.title + " " + it.text + it.confidence)
        }*/
        //debugHelper.saveResult(iteration, resizedBitmap, objects)
        Log.d(TAG, "detection objects($iteration): $objects ${objects.size}")


        Log.d(TAG, "Inference: ${System.currentTimeMillis() - startTime}ms")
        val result = Result(
                objects = objects,
                imageWidth = config.size.width,
                imageHeight = config.size.height,
                imageRotationDegrees = 0
        )

        uiHandler.post {
            //onDetectionResult.invoke(result)
        }
    }

    private fun getTransformation(rotationDegrees: Int, srcWidth: Int, srcHeight: Int): Matrix {
        var toInput = matrixToInput
        if (toInput == null) {
            toInput = ImageUtil.getTransformMatrix(rotationDegrees, srcWidth, srcHeight, config.size.width, config.size.height)
            matrixToInput = toInput
        }
        return toInput
    }

    private fun getRgbBitmap(pixels: IntArray, width: Int, height: Int): Bitmap {
        var bitmap = rgbBitmap
        if (bitmap == null) {
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888) as Bitmap
            rgbBitmap = bitmap
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)

        return bitmap
    }

    private fun convertYuvToRgb(image: ImageProxy): IntArray {
        var array = rgbArray
        if (array == null) {
            array = IntArray(image.width * image.height)
            rgbArray = array
        }
        ImageUtil.convertYuvToRgb(image, array)
        return array
    }

    private fun preProcess(originalBitmap: Bitmap?): IntArray {
        val bitmap = Bitmap.createScaledBitmap(originalBitmap!!,
                config.size.width,
                config.size.height, false)
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height) // The bitmap shape should be 32 x 32 x 3?
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        return pixels
    }

    private fun formatSettingsLog(): String {
        val sb = StringBuilder()
        sb.append("\n------------ Current Config: \n")
        sb.append("Model filename: ${config.modelFile}\n")
        sb.append("Input Image Size: ${config.size.toString()}\n")
        sb.append("NN Quantized: ${config.isQuantized}\n")
        sb.append("GPU enabled: ${config.isGpuEnabled}\n")
        sb.append("NN API enabled: ${config.isNnapiEnabled}\n")
        sb.append("Number of threads: ${config.numThreads}\n")
        return sb.toString()
    }

    private fun formatExecutionLog(): String {
        val sb = StringBuilder()
        sb.append("------------ Time\n")
        sb.append("Current iteration: ${iterationCounter.get()}\n")
        sb.append("Pre-process execution time: $preProcessTime ms\n")
        sb.append("Predicting execution time: $predictTime ms\n")
        sb.append("Post-process execution time: $postProcessTime ms\n")
        sb.append("Full execution time: $fullExecutionTime ms\n")
        return sb.toString()
    }

    private fun formatAverageLog(): String {
        val sb = StringBuilder()
        sb.append("------------ Average Time\n")
        sb.append("Iterations: ${iterationCounter.get() - 2}\n")
        sb.append("Pre-process execution time: ${preProcessResults.average().toInt()} ms\n")
        sb.append("Predicting average time: ${predictResults.average().toInt()} ms\n")
        sb.append("Post-process execution time: ${postProcessResults.average().toInt()} ms\n")
        sb.append("Full execution time: ${fullExecutionResults.average().toInt()} ms\n")
        return sb.toString()
    }

    fun disposeDetector() {
        objectDetector.closeAll()
    }
}