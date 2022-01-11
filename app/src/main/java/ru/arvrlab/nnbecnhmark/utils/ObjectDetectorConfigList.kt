package ru.arvrlab.nnbecnhmark.utils

import android.util.Size
import ru.arvrlab.nnbecnhmark.camera.ObjectDetectorAnalyzer

class ObjectDetectorConfigList {
    private val nnSet4 = listOf(
            /*Pair(
                    NNType.MOBILENET,
                    ObjectDetectorAnalyzer.Config(
                            modelFile = "coco_sabanet_mobilenet_w1_coco_q-8345-od-ds100-soi8-it.tflite",
                            nnType = NNType.MOBILENET,
                            isQuantized = false,
                            isGpuEnabled = false,
                            isNnapiEnabled = false,
                            size = Size(192, 256)
                    )
            ),*/
            Pair(
                    NNType.MOBILENET,
                    ObjectDetectorAnalyzer.Config(
                            modelFile = "coco_sabanet_mobilenet_w1_coco_q-8550-od-ds100-soi8-it.tflite",
                            nnType = NNType.MOBILENET,
                            isQuantized = false,
                            isGpuEnabled = true,
                            isNnapiEnabled = false,
                            size = Size(192, 256)
                    )
            ),
            Pair(
                    NNType.MOBILENET,
                    ObjectDetectorAnalyzer.Config(
                            modelFile = "sabanet_mobilenet_w1_coco-8556-flt32.tflite",
                            nnType = NNType.MOBILENET,
                            isQuantized = false,
                            isGpuEnabled = false,
                            isNnapiEnabled = false,
                            size = Size(192, 256)
                    )
            )/*,
            Pair(
                    NNType.MOBILENET,
                    ObjectDetectorAnalyzer.Config(
                            modelFile = "sabanet_mobilenet_w1_coco-8556-od-ds100.tflite",
                            nnType = NNType.MOBILENET,
                            isQuantized = false,
                            isGpuEnabled = false,
                            isNnapiEnabled = false,
                            size = Size(192, 256)
                    )
            ),
            Pair(
                    NNType.MOBILENET,
                    ObjectDetectorAnalyzer.Config(
                            modelFile = "sabanet_mobilenet_w1_coco-8556-od-ds100-soi8.tflite",
                            nnType = NNType.MOBILENET,
                            isQuantized = false,
                            isGpuEnabled = false,
                            isNnapiEnabled = false,
                            size = Size(192, 256)
                    )
            ),
            Pair(
                    NNType.MOBILENET,
                    ObjectDetectorAnalyzer.Config(
                            modelFile = "sabanet_mobilenet_w1_coco-8556-od-ds100-soi8-it.tflite",
                            nnType = NNType.MOBILENET,
                            isQuantized = false,
                            isGpuEnabled = false,
                            isNnapiEnabled = false,
                            size = Size(192, 256)
                    )
            ),
            Pair(
                    NNType.MOBILENET,
                    ObjectDetectorAnalyzer.Config(
                            modelFile = "sabanet_mobilenet_w1_coco-8556-od-pure.tflite",
                            nnType = NNType.MOBILENET,
                            isQuantized = false,
                            isGpuEnabled = false,
                            isNnapiEnabled = false,
                            size = Size(192, 256)
                    )
            ),
            Pair(
                    NNType.MOBILENET,
                    ObjectDetectorAnalyzer.Config(
                            modelFile = "sabanet_mobilenet_w1_coco-8556-ofl-ds100.tflite",
                            nnType = NNType.MOBILENET,
                            isQuantized = false,
                            isGpuEnabled = false,
                            isNnapiEnabled = false,
                            size = Size(192, 256)
                    )
            ),
            Pair(
                    NNType.MOBILENET,
                    ObjectDetectorAnalyzer.Config(
                            modelFile = "sabanet_mobilenet_w1_coco-8556-ofl-pure.tflite",
                            nnType = NNType.MOBILENET,
                            isQuantized = false,
                            isGpuEnabled = false,
                            isNnapiEnabled = false,
                            size = Size(192, 256)
                    )
            )*/
    )

    private val nnSet5 = listOf(
            Pair(
                    NNType.MEDIAPIPE_FACE_LANDMARK,
                    ObjectDetectorAnalyzer.Config(
                            modelFile = "face_landmark.tflite",
                            nnType = NNType.MEDIAPIPE_FACE_LANDMARK,
                            isQuantized = false,
                            isGpuEnabled = true,
                            isNnapiEnabled = false,
                            size = Size(192, 192)
                    )),
            Pair(
                    NNType.MEDIAPIPE_FACE_DETECTION,
                    ObjectDetectorAnalyzer.Config(
                            modelFile = "face_detection_front.tflite",
                            nnType = NNType.MEDIAPIPE_FACE_DETECTION,
                            isQuantized = false,
                            isGpuEnabled = true,
                            isNnapiEnabled = false,
                            size = Size(128, 128)
                    )
            )
    )

    private val nnSet6 = listOf(
            Pair(
                    NNType.BEAUTY_GAN,
                    ObjectDetectorAnalyzer.Config(
                            modelFile = "beautygan-flt32.tflite",
                            nnType = NNType.BEAUTY_GAN,
                            isQuantized = false,
                            isGpuEnabled = true,
                            isNnapiEnabled = false,
                            isGAN = true,
                            size = Size(256, 256)
                    )
            ),
            Pair(
                    NNType.BEAUTY_GAN,
                    ObjectDetectorAnalyzer.Config(
                            modelFile = "BeautyGAN_quant_default_float16.tflite",
                            nnType = NNType.BEAUTY_GAN,
                            isQuantized = false,
                            isGpuEnabled = true,
                            isNnapiEnabled = false,
                            isGAN = true,
                            size = Size(256, 256)
                    )
            ),
            Pair(
                    NNType.BEAUTY_GAN,
                    ObjectDetectorAnalyzer.Config(
                            modelFile = "beautygan-od-ds9.tflite",
                            nnType = NNType.BEAUTY_GAN,
                            isQuantized = false,
                            isGpuEnabled = true,
                            isNnapiEnabled = false,
                            isGAN = true,
                            size = Size(256, 256)
                    )
            )
    )

    private val nnSet7 = listOf(
            Pair(
                    NNType.HAIRMATTENET,
                    ObjectDetectorAnalyzer.Config(
                            modelFile = "HairMatteNet_notrain.tflite",
                            nnType = NNType.HAIRMATTENET,
                            isQuantized = false,
                            isGpuEnabled = true,
                            isNnapiEnabled = false,
                            size = Size(224, 224)
                    )
            )
    )

    private val nnSet8 = listOf(
            Pair(
                    NNType.BLAZE_FACE,
                    ObjectDetectorAnalyzer.Config(
                            modelFile = "blaze_face.tflite",
                            nnType = NNType.BLAZE_FACE,
                            isQuantized = false,
                            isGpuEnabled = false,
                            isNnapiEnabled = false,
                            size = Size(128, 128)
                    )
            )
    )


    fun getNNSet4(): MutableList<ObjectDetectorAnalyzer.Config>{
        val listOfConfigs = mutableListOf<ObjectDetectorAnalyzer.Config>()
        nnSet4.forEach { listOfConfigs.add(it.second) }

        return listOfConfigs
    }

    fun getNNSet5(): MutableList<ObjectDetectorAnalyzer.Config>{
        val listOfConfigs = mutableListOf<ObjectDetectorAnalyzer.Config>()
        nnSet5.forEach { listOfConfigs.add(it.second) }

        return listOfConfigs
    }

    fun getNNSet6(): MutableList<ObjectDetectorAnalyzer.Config>{
        val listOfConfigs = mutableListOf<ObjectDetectorAnalyzer.Config>()
        nnSet6.forEach { listOfConfigs.add(it.second) }

        return listOfConfigs
    }

    fun getNNSet7(): MutableList<ObjectDetectorAnalyzer.Config>{
        val listOfConfigs = mutableListOf<ObjectDetectorAnalyzer.Config>()
        nnSet7.forEach { listOfConfigs.add(it.second) }

        return listOfConfigs
    }

    fun getOne(nnType: NNType): ObjectDetectorAnalyzer.Config? {
        return nnSet4.findLast { it.first == nnType }?.second
    }
}