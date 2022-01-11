package ru.`object`.detection.util

import android.content.res.AssetManager
import android.util.Size
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

object DetectorUtils {
    private const val IMAGE_MEAN = 128.0f
    private const val IMAGE_STD = 128.0f
    const val NUM_THREADS = 4
    private val PIXEL_SIZE = 3
    private val DIM_BATCH_SIZE = 1

    fun loadModelFile(assets: AssetManager, modelFilename: String): MappedByteBuffer {
        val fileDescriptor = assets.openFd(modelFilename)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun loadLabelsFile(assets: AssetManager, labelFilename: String): List<String> {
        return assets.open(labelFilename).bufferedReader().use { it.readLines() }
    }

    fun createImageBuffer(size: Size, isModelQuantized: Boolean): ByteBuffer {
        val numBytesPerChannel: Int = if (isModelQuantized) 1 else 4
        val buffer = ByteBuffer.allocateDirect( DIM_BATCH_SIZE * size.width * size.height * PIXEL_SIZE * numBytesPerChannel)

        buffer.order(ByteOrder.nativeOrder())
        buffer.rewind()

        return buffer
    }

    fun fillBuffer(
            imgData: ByteBuffer,
            pixels: IntArray,
            size: Size,
            isModelQuantized: Boolean
    ) {
        val width = size.width
        val height = size.height

        for (i in 0 until width) {
            for (j in 0 until height) {
                val pixelValue = pixels[i * width + j]
                if (isModelQuantized) {
                    imgData.put((pixelValue shr 16 and 0xFF).toByte())
                    imgData.put((pixelValue shr 8 and 0xFF).toByte())
                    imgData.put((pixelValue and 0xFF).toByte())
                } else {
                    imgData.putFloat(((pixelValue shr 16 and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                    imgData.putFloat(((pixelValue shr 8 and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                    imgData.putFloat(((pixelValue and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                }
            }
        }
    }
}