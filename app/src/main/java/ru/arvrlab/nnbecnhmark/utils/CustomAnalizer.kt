package ru.arvrlab.nnbecnhmark.utils

import android.graphics.ImageFormat
import android.graphics.YuvImage
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer

object CustomAnalizer: ImageAnalysis.Analyzer {
    private val TAG = "CustomAnalizer"
    private var frameCount = 0

    override fun analyze(image: ImageProxy) {
        frameCount++
        //Analyze every 30 frames
        if (frameCount == 30) {
            val imageProxySize = image.planes[0].buffer.remaining() + image.planes[1].buffer.remaining() + image.planes[2].buffer.remaining()
            val byteBuffer = ByteBuffer.allocateDirect(imageProxySize)
            image.planes.forEach { byteBuffer.put(it.buffer) }

            val stride = intArrayOf(image.width, image.width / 2, image.width / 2)
            val yuvImage = YuvImage(byteBuffer.toByteArray(), ImageFormat.NV21, image.width, image.height, stride)
            val inYuvY = yuvImage.yuvData

            var remaining = inYuvY.size
            //Log.d(TAG, "Image bytes: $remaining")
            Log.d(TAG,"Before change byte: ${inYuvY[0]}")

            remaining = inYuvY.size
            //Log.d(TAG, "Custom After change image bytes: $remaining")
            Log.d(TAG,"Custom change byte: ${inYuvY[0]}")

            frameCount = 0
        }
        //Important to close image for prevent block analyze
        image.close()
    }

    /**
     * Helper extension function used to extract a byte array from an image plane buffer
     */
    private fun ByteBuffer.toByteArray(): ByteArray {
        rewind()    // Rewind the buffer to zero
        val data = ByteArray(remaining())
        get(data)   // Copy the buffer into a byte array
        return data // Return the byte array
    }
}