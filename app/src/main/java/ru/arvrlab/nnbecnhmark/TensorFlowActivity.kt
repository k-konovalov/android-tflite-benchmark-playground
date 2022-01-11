package ru.arvrlab.nnbecnhmark

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.nnapi.NnApiDelegate
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder


class TensorFlowActivity : AppCompatActivity() {
    val TAG = "TensorFlowActivity"
    // Specify the input size
    private val DIM_BATCH_SIZE = 1
    private val DIM_IMG_SIZE_X = 224
    private val DIM_IMG_SIZE_Y = 224
    private val DIM_PIXEL_SIZE = 3
    // Number of bytes to hold a float (32 bits / float) / (8 bits / byte) = 4 bytes / float
    private val BYTE_SIZE_OF_FLOAT = 4

    // Initialize interpreter with NNAPI delegate for Android Pie or above
    val options: Interpreter.Options = Interpreter.Options()
    var nnApiDelegate: NnApiDelegate? = NnApiDelegate().also {
        options.addDelegate(it)
        options.setUseNNAPI(true)
    }
    var tfLite: Interpreter? = null

    val btnRunWithNPU by lazy { findViewById<Button>(R.id.btnRunWithNPU) }
    val txtInference by lazy { findViewById<TextView>(R.id.txtInference) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tensor_flow)

        btnRunWithNPU.setOnClickListener {
            runInference()
        }
        try {
            val model = loadFileFromAssets(assets, "efficientnet-lite0-fp32.tflite")
            Log.d("Tensor", model.absolutePath +" name: "+model.name)
            tfLite = Interpreter(model, options)
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tfLite?.close();
        nnApiDelegate?.close();
    }

    private fun loadFileFromAssets(
            assetsManager: AssetManager,
            modelFilename: String
    ): File = File(this.cacheDir, modelFilename).also {
        it.outputStream().use { cache ->
            assetsManager.open(modelFilename).use { inpStr ->
                inpStr.copyTo(cache)
            }
        }
    }

    private fun runInference(){
        val startTime = System.currentTimeMillis()
        val bitmap = BitmapFactory.decodeStream(assets.open("cat.jpeg"))
        Log.e(TAG, "ByteCount of bitmap:" + bitmap.byteCount)
        val goodByteBuffer = preProcess(bitmap)

        goodByteBuffer?.apply {
            //if(checkByteBuffer(this)) tfLite?.runForMultipleInputsOutputs(this,"")//tfLite?.run(this, "")
            txtInference.text = "(${startTime - System.currentTimeMillis()})"

        }
    }

    private fun checkByteBuffer(byteBuffer: ByteBuffer): Boolean {
            if (byteBuffer.order() != ByteOrder.nativeOrder()) {
                throw Exception("Input Bytebuffers should use native order")
            }
            if (!byteBuffer.isDirect) {
                throw Exception("Input ByteBuffer should be direct ByteBuffer")
            }
            return true
    }

    private fun preProcess(originalBitmap: Bitmap?): ByteBuffer? {
        if (originalBitmap == null) {
            return null
        }

        val bitmap = Bitmap.createScaledBitmap(originalBitmap,
                DIM_IMG_SIZE_X,
                DIM_IMG_SIZE_Y, false)
        val width = bitmap.width
        val height = bitmap.height
        val inputBuffer = ByteBuffer
                .allocateDirect(BYTE_SIZE_OF_FLOAT * DIM_BATCH_SIZE * DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y * DIM_PIXEL_SIZE)
                .also {
                    it.order(ByteOrder.nativeOrder())
                    it.rewind()
                }
        val startTime = System.currentTimeMillis()
        val pixels = IntArray(width * height) // The bitmap shape should be 32 x 32 x 3?

        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // Set 0 for white and 255 for black pixels
        pixels.forEach {
            val channel = it and 0xff //???
            //-4344167 vs 255 = 153
            //Log.e(TAG,"$it vs ${0xff} = $channel") //???
            // The color of the input is black so the blue channel will be 0xFF.
            inputBuffer.putFloat((0xff - channel).toFloat())
        }

        val endTime = System.currentTimeMillis()
        Log.d("TensorFLow", "Time cost to put values into ByteBuffer: ${((endTime - startTime))}")

        return inputBuffer
    }
}