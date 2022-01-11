package ru.arvrlab.nnbecnhmark.detection

import ru.arvrlab.nnbecnhmark.utils.NNType

class InterpreterResultHolder(
        val detections: Int = 10,
        val width: Int = 0,
        val height: Int = 0
) {
    val outputLocations = Array(1) { Array(detections) { FloatArray(4) } }
    val detectOutputClasses = Array(1) { FloatArray(detections) }
    val outputScores = Array(1) { FloatArray(detections) }
    val numDetections = FloatArray(1) // 1
    
    //Java - float[][] outputs = new float[1][num_classes]
    //Kotlin [1 18]
    private val outputClasses = Array(1) {
        FloatArray(18)
    }
    
    //<class 'numpy.float32'>
    private val outputFloatExample = Array(1) {//1
        Array(width) {//192
            Array(height) {//256
                FloatArray(18)
            }//18
        }
    }

    //<class 'numpy.int32'>
    private val outputIntExample = Array(1) {//1
        Array(width) {//192
            Array(height) {//256
                IntArray(18)
            }//18
        }
    }

    //<class 'numpy.uint8'>
    private val outputByteExample = Array(1) {//1
        Array(width) {//192
            Array(height) {//256
                ByteArray(18)
            }//18
        }
    }

    //Output [1 192 256 18] <class 'numpy.float32'>
    private val mobileNetOutput = Array(1) {//1
        Array(width) {//192
            Array(height) {//256
                FloatArray(18)
            }//18
        }
    }

    private val prnetOutput = Array(1) {//1
        Array(width) {//256
            Array(height) {//256
                FloatArray(3)//3
            }
        }
    }

    private val cocoSsdMobilenetOutput = Array(1) {//1
        Array(10) {//10
            FloatArray(4)//4
        }
    }

    private val mobilenet_v1_1_0_224Output = Array(1) {//1
            ByteArray(1001)//1001
        }

    private val mobile_ssd_v2_Output = Array(1) {//1
        Array(2034){//2034
            FloatArray(4) //4
        }
    }

    //[1 23 17 17] <class 'numpy.float32'>
    private val multiPersonMobilenet_v1_075_Output = Array(1){
        Array(23){
            Array(17){
                FloatArray(17)
            }
        }
    }

    //[1 257 257 21] <class 'numpy.float32'>
    private val deeplabV3_257_MV_GPU_Output = Array(1){
        Array(257){
            Array(257){
                FloatArray(21)
            }
        }
    }

    //[1 2034 4]<class 'numpy.float32'>
    private val mobile_SSD_V2_Float_Coco_GPU = Array(1) {//1
        Array(2034){//2034
            FloatArray(4) //4
        }
    }

    private val mobilenet_v1_1_0_224_GPU_Output = Array(1) {//1
        FloatArray(1001)//1001
    }

    private val MULTI_PERSON_MOBILENET_v1_075_GPU = Array(1){
        Array(23){
            Array(17){
                FloatArray(17)
            }
        }
    }

    private val BEAUTY_GAN_Output = Array(1){
        Array(256){
            Array(256){
                FloatArray(3)
            }
        }
    }

    private val HAIR_Detection_Output = Array(1){
        Array(512){
            Array(512){
                FloatArray(2)
            }
        }
    }

    private val MEDIAPIPE_FACE_LANDMARK_Output1 = Array(1){
        Array(1){
            Array(1){
                FloatArray(1404)
            }
        }
    }

    private val MEDIAPIPE_FACE_LANDMARK_Output2 = Array(1){
        Array(1){
            Array(1){
                FloatArray(1)
            }
        }
    }

    private val MEDIAPIPE_FACE_DETECTION_Output1 = Array(1) {
        Array(896) {
            FloatArray(16)
        }
    }

    private val MEDIAPIPE_FACE_DETECTION_Output2 = Array(1) {
        Array(896) {
            FloatArray(1)
        }
    }

    private val HAIRMATTENET_Output = Array(1){
        Array(224) {
            Array(224){
                FloatArray(2)
            }
        }
    }

    private val BLAZE_FACE_Output1 = Array(1) {
        Array(896) {
            FloatArray(16)
        }
    }

    private val BLAZE_FACE_Output2 = Array(1) {
        Array(896) {
            FloatArray(1)
        }
    }

    private fun createMobileNetOutput() = mapOf(
            0 to mobileNetOutput
    )

    private fun createDetectOutputMap() = mapOf(
            0 to outputLocations,
            1 to detectOutputClasses,
            2 to outputScores,
            3 to numDetections
    )

    private fun createPrNetOutput() = mapOf(
            0 to prnetOutput
    )

    private fun createCocoSsdMobilenet_Output() = mapOf(
            0 to cocoSsdMobilenetOutput
    )

    private fun createMOBILENET_v1_1_0_224_Output() = mapOf(
            0 to mobilenet_v1_1_0_224Output
    )

    private fun createMobile_ssd_v2_Output() = mapOf(
            0 to mobile_ssd_v2_Output
    )

    private fun createMultiPersonMobilenet_v1_075_Output() = mapOf(
            0 to multiPersonMobilenet_v1_075_Output
    )

    private fun createDeeplabV3_257_MV_GPU_Output() = mapOf(
            0 to deeplabV3_257_MV_GPU_Output
    )

    private fun createMobile_SSD_V2_Float_Coco_GPU_Output() = mapOf(
            0 to mobile_SSD_V2_Float_Coco_GPU
    )

    private fun createMOBILENET_v1_1_0_224_GPU_Output() = mapOf(
            0 to mobilenet_v1_1_0_224_GPU_Output
    )

    private fun createMULTI_PERSON_MOBILENET_v1_075_GPU_Output() = mapOf(
            0 to MULTI_PERSON_MOBILENET_v1_075_GPU
    )

    private fun createBEAUTY_GAN_Output() = mapOf(
            0 to BEAUTY_GAN_Output
    )

    private fun createBEAUTY_GAN_QUANT_Output() = mapOf(
            0 to BEAUTY_GAN_Output
    )

    private fun createHAIR_Detection_Output() = mapOf(
            0 to HAIR_Detection_Output
    )

    private fun createMEDIAPIPE_FACE_LANDMARK_Output() = mapOf(
            0 to MEDIAPIPE_FACE_LANDMARK_Output1,
            1 to MEDIAPIPE_FACE_LANDMARK_Output2
    )

    private fun createMEDIAPIPE_FACE_DETECTION_Output() = mapOf(
            0 to MEDIAPIPE_FACE_DETECTION_Output1,
            1 to MEDIAPIPE_FACE_DETECTION_Output2
    )

    private fun createHAIRMATTENET_Output() = mapOf(
            0 to HAIRMATTENET_Output
    )

    private fun createBLAZE_FACE_Output() = mapOf(
            0 to BLAZE_FACE_Output1,
            1 to BLAZE_FACE_Output2
    )

    fun createProperOutput(nnType: NNType): Map<Int,Any>{
        return when(nnType){
            NNType.DETECT-> createDetectOutputMap()
            NNType.MOBILENET -> createMobileNetOutput()
            NNType.PRNET -> createPrNetOutput()
            NNType.COCO_SSD_MOBILENET -> createCocoSsdMobilenet_Output()
            NNType.MOBILENET_v1_1_0_224 -> createMOBILENET_v1_1_0_224_Output()
            NNType.MOBILE_SSD_v2 -> createMobile_ssd_v2_Output()
            NNType.MULTI_PERSON_MOBILENET_v1_075 -> createMultiPersonMobilenet_v1_075_Output()
            NNType.DEEPLABV3_257_MV_GPU -> createDeeplabV3_257_MV_GPU_Output()
            NNType.MOBILE_SSD_V2_FLOAT_COCO_GPU -> createMobile_SSD_V2_Float_Coco_GPU_Output()
            NNType.MOBILENET_v1_1_0_224_GPU -> createMOBILENET_v1_1_0_224_GPU_Output()
            NNType.MULTI_PERSON_MOBILENET_v1_075_GPU -> createMULTI_PERSON_MOBILENET_v1_075_GPU_Output()
            NNType.BEAUTY_GAN -> createBEAUTY_GAN_Output()
            NNType.HAIR_DETECTION -> createHAIR_Detection_Output()
            NNType.BEAYTY_GAN_QUANT -> createBEAUTY_GAN_QUANT_Output()
            NNType.MEDIAPIPE_FACE_LANDMARK -> createMEDIAPIPE_FACE_LANDMARK_Output()
            NNType.MEDIAPIPE_FACE_DETECTION -> createMEDIAPIPE_FACE_DETECTION_Output()
            NNType.HAIRMATTENET -> createHAIRMATTENET_Output()
            NNType.BLAZE_FACE -> createBLAZE_FACE_Output()
        }
    }
}