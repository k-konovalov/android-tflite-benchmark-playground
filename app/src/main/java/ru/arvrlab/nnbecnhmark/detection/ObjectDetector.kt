package ru.arvrlab.nnbecnhmark.detection

import android.content.res.AssetManager
import android.graphics.RectF
import android.util.Log
import android.util.Size
import ru.arvrlab.nnbecnhmark.utils.NNType
import kotlinx.coroutines.Dispatchers
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import ru.`object`.detection.detection.DetectionResult
import ru.`object`.detection.util.DetectorUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ObjectDetector (private val config: Config, private val secondConfig: Config? = null) {
    data class Config(
            val assetManager: AssetManager,
            val modelFilename: String,
            val labelFilename: String,
            val numThreads: Int = DetectorUtils.NUM_THREADS,
            val minimumConfidence: Float,
            val numDetections: Int = 1,
            val size: Size,
            val isModelQuantized: Boolean = false,
            val useNnapi: Boolean = false,
            val useGpu: Boolean = false
    )

    private val TAG = "ObjectDetector"
    private val interpreter: Interpreter by lazy(Dispatchers.Default) { initInterpreter(config.modelFilename) }
    private val secondInterpreter: Interpreter? by lazy(Dispatchers.Default) { secondConfig?.let { initInterpreter(secondConfig.modelFilename) } }

    private val gpuDelegate = initGpuDelegate()
    private val labels: List<String> = initLabels()
    private val interpreterResultHolder = initinterpreterResultHolder()

    private fun initGpuDelegate() = GpuDelegate(
            GpuDelegate.Options()
/*                    *//**
                     * Prefer maximizing the throughput. Same delegate will be used repeatedly on multiple inputs.
                     *//*
                    .setInferencePreference(GpuDelegate.Options.INFERENCE_PREFERENCE_SUSTAINED_SPEED)
                    *//**
                     * Sets whether precision loss is allowed.
                     *
                     * @param precisionLossAllowed When `true` (default), the GPU may quantify tensors, downcast
                     *     values, process in FP16. When `false`, computations are carried out in 32-bit floating
                     *     point.
                     *//*
                    .setPrecisionLossAllowed(true)*/
    )

    private fun initInterpreter(modelFilename: String): Interpreter{
        config.apply {
            val options = Interpreter.Options()
            if (!useGpu && !useNnapi) options.setNumThreads(numThreads)
            if (useGpu) options.addDelegate(gpuDelegate)
            if (useNnapi) options.setUseNNAPI(useNnapi)

            return Interpreter(
                    DetectorUtils.loadModelFile(assetManager, modelFilename),
                    options
            )
        }
    }

    private fun initLabels():List<String>{
        config.apply {
            return if (labelFilename.isEmpty()) emptyList()
            else DetectorUtils.loadLabelsFile(assetManager, labelFilename)
        }
    }

    private fun initinterpreterResultHolder(): InterpreterResultHolder{
        config.apply {
            return InterpreterResultHolder(width = size.width,height = size.height, detections = numDetections)
        }
    }

    fun oldDetect(byteBuffer: ByteBuffer,pixels: IntArray): List<DetectionResult> {
        val inputArray = arrayOf(byteBuffer)
        val resultHolder = InterpreterResultHolder(config.numDetections)

        val startTime = System.currentTimeMillis()
        interpreter.runForMultipleInputsOutputs(inputArray, resultHolder.createProperOutput(NNType.DETECT))
        Log.i(TAG, "Inference Time: ${System.currentTimeMillis() - startTime}ms")

        return collectDetectionResult(resultHolder)
    }

    private fun collectDetectionResult(holder: InterpreterResultHolder): List<DetectionResult> {
        // SSD Mobilenet V1 Model assumes class 0 is background class
        // in label file and class labels start from 1 to number_of_classes+1,
        // while outputClasses correspond to class index from 0 to number_of_classes

        val labelOffset = 1
        val result = mutableListOf<DetectionResult>()

        if (labels.isNotEmpty())
            for (i in 0 until holder.detections) {
                val confidence = holder.outputScores[0][i]
                if (confidence < config.minimumConfidence) continue

                val title = labels[holder.detectOutputClasses[0][i].toInt() + labelOffset]

                val inputSize = config.size.width

                val location = RectF(
                        holder.outputLocations[0][i][1] * inputSize,
                        holder.outputLocations[0][i][0] * inputSize,
                        holder.outputLocations[0][i][3] * inputSize,
                        holder.outputLocations[0][i][2] * inputSize
                )

                result.add(DetectionResult(id = i, title = title, confidence = confidence, location = location))
            }

        return result
    }
    /**
     * Runs model inference if the model takes only one input, and provides only one output.
     *
     * <p>Warning: The API runs much faster if {@link ByteBuffer} is used as input data type. Please
     * consider using {@link ByteBuffer} to feed input data for better performance.
     *
     * @param input an array or multidimensional array, or a {@link ByteBuffer} of primitive types
     *     including int, float, long, and byte. {@link ByteBuffer} is the preferred way to pass large
     *     input data. When {@link ByteBuffer} is used, its content should remain unchanged until
     *     model inference is done.
     * @param output a multidimensional array of output data, or a {@link ByteBuffer} of primitive
     *     types including int, float, long, and byte.
     */
    fun newDetect(imgData:ByteBuffer, NNType: NNType): Long {
        val input = arrayOf(imgData)
        val output = interpreterResultHolder.createProperOutput(NNType)

        val startTime = System.currentTimeMillis()
        interpreter.runForMultipleInputsOutputs(input, output)
        return System.currentTimeMillis() - startTime

        /*
        val objects = collectDetectionResult(interpreterResultHolder)
        var str: String = "Output: \n"
        objects.forEach {
            str += "Title: "+ it.title + " " + it.text +" "+ it.confidence + "\n"
        }
        Log.d(TAG,str)*/
    }

    fun doubleDetect(imgData1:ByteBuffer,imgData2:ByteBuffer, NNType1: NNType, NNType2: NNType): Long {
        val inputForOne = arrayOf(imgData1)
        val inputForTwo = arrayOf(imgData2)
        val outputForOne = interpreterResultHolder.createProperOutput(NNType1)
        val outputForTwo = interpreterResultHolder.createProperOutput(NNType2)

        val startTime = System.currentTimeMillis()
        interpreter.runForMultipleInputsOutputs(inputForOne, outputForOne)
        secondInterpreter?.runForMultipleInputsOutputs(inputForTwo, outputForTwo)
        /*
        val smth = outputForOne.values
        smth.forEach {
            (it as Array<*>).forEach {
                second->
                val smth2 = (second as Array<*>)
                Log.e(TAG, smth2.toString())
            }
        }*/

        //val smth = outputForTwo.values.filter {  }
        return System.currentTimeMillis() - startTime
    }

    fun detectGan(imgData:Array<ByteBuffer>, NNType: NNType): Long{
        val input = arrayOf(imgData[0], imgData[1])
        val output = interpreterResultHolder.createProperOutput(NNType)

        //Log.e(TAG, "Bytes: [0]&[1]: "+ input[0].remaining().toString() + " " + input[1].remaining().toString())
        val startTime = System.currentTimeMillis()
        interpreter.runForMultipleInputsOutputs(input, output)
        return System.currentTimeMillis() - startTime
    }

    fun closeAll(){
        interpreter.close()
        secondInterpreter?.close()
        gpuDelegate.close()
    }

    fun ByteBuffer.doubleBuffer(): ByteBuffer{
        val byteBuffer = ByteBuffer.allocateDirect(this.capacity()*2)
        byteBuffer.put(this.slice()).put(this.slice())
        byteBuffer.order(ByteOrder.nativeOrder())
        byteBuffer.rewind()

        return byteBuffer
    }
}