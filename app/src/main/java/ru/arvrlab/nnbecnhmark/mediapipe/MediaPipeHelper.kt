package ru.arvrlab.nnbecnhmark.mediapipe

import android.app.Activity
import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.SurfaceTexture
import android.util.Size
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.google.mediapipe.components.CameraHelper.CameraFacing
import com.google.mediapipe.components.CameraXPreviewHelper
import com.google.mediapipe.components.ExternalTextureConverter
import com.google.mediapipe.components.FrameProcessor
import com.google.mediapipe.components.PermissionHelper
import com.google.mediapipe.framework.AndroidAssetUtil
import com.google.mediapipe.glutil.EglManager

class MediaPipeHelper {
    init {
        // Load all native libraries needed by the app.
//        System.loadLibrary("mediapipe_jni")
        try {
           System.loadLibrary("opencv_java3")
        } catch (e:Exception) {
            // Some example apps (e.g. template matching) require OpenCV 4.
            System.loadLibrary("opencv_java4")
        }
    }

    // Flips the camera-preview frames vertically before sending them into FrameProcessor to be
    // processed in a MediaPipe graph, and flips the processed frames back when they are displayed.
    // This is needed because OpenGL represents images assuming the image origin is at the bottom-left
    // corner, whereas MediaPipe in general assumes the image origin is at top-left.
    private val FLIP_FRAMES_VERTICALLY = true

    // Sends camera-preview frames into a MediaPipe graph for processing, and displays the processed
    // frames onto a {@link Surface}.
    var processor: FrameProcessor? = null

    // Handles camera access via the {@link CameraX} Jetpack support library.
    var cameraHelper: CameraXPreviewHelper = CameraXPreviewHelper()

    // {@link SurfaceTexture} where the camera-preview frames can be accessed.
    private var previewFrameTexture: SurfaceTexture? = null

    // {@link SurfaceView} that displays the camera-preview frames processed by a MediaPipe graph.
    private var previewDisplayView: SurfaceView? = null

    // Creates and manages an {@link EGLContext}.
    private var eglManager = EglManager(null)

    // Converts the GL_TEXTURE_EXTERNAL_OES texture from Android camera into a regular texture to be
    // consumed by {@link FrameProcessor} and the underlying MediaPipe graph.
    private var converter = ExternalTextureConverter(eglManager.context)


    fun initMediaPipe(context: Context, applicationInfo: ApplicationInfo, preview_display_layout: FrameLayout){
        // Initialize asset manager so that MediaPipe native libraries can access the app assets, e.g.,
        // binary graphs.
        AndroidAssetUtil.initializeNativeAssetManager(context)

        previewDisplayView = SurfaceView(context)
        setupPreviewDisplayView(preview_display_layout)

        processor = FrameProcessor(
                context,
                eglManager.nativeContext,
                "hairsegmentationgpu.binarypb",//applicationInfo.metaData.getString("binaryGraphName"),
                applicationInfo.metaData.getString("inputVideoStreamName"),
                applicationInfo.metaData.getString("outputVideoStreamName")
        )
                .also {
                    it.videoSurfaceOutput.setFlipY(FLIP_FRAMES_VERTICALLY)
                }
    }

    private fun setupPreviewDisplayView(viewGroup: ViewGroup) {
        previewDisplayView!!.visibility = View.GONE
        //val viewGroup: ViewGroup = findViewById<ViewGroup>(R.id.preview_display_layout)
        viewGroup.addView(previewDisplayView)
        previewDisplayView!!
                .holder
                .addCallback(
                        object : SurfaceHolder.Callback {
                            override fun surfaceCreated(holder: SurfaceHolder) {
                                processor!!.videoSurfaceOutput.setSurface(holder.surface)
                            }

                            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                                // (Re-)Compute the ideal size of the camera-preview display (the area that the
                                // camera-preview frames get rendered onto, potentially with scaling and rotation)
                                // based on the size of the SurfaceView that contains the display.
                                val viewSize = Size(width, height)
                                val displaySize = cameraHelper.computeDisplaySizeFromViewSize(viewSize)
                                val isCameraRotated = cameraHelper.isCameraRotated

                                // Connect the converter to the camera-preview frames as its input (via
                                // previewFrameTexture), and configure the output width and height as the computed
                                // display size.
                                converter.setSurfaceTextureAndAttachToGLContext(
                                        previewFrameTexture,
                                        if (isCameraRotated) displaySize.height else displaySize.width,
                                        if (isCameraRotated) displaySize.width else displaySize.height)
                            }

                            override fun surfaceDestroyed(holder: SurfaceHolder) {
                                processor!!.videoSurfaceOutput.setSurface(null)
                            }
                        })
    }

    private fun startCamera(context: Activity, applicationInfo:ApplicationInfo) {
        cameraHelper.setOnCameraStartedListener { surfaceTexture: SurfaceTexture? -> onCameraStarted(surfaceTexture!!) }
        val cameraFacing = if (applicationInfo.metaData.getBoolean("cameraFacingFront", false)) CameraFacing.FRONT else CameraFacing.BACK
        cameraHelper.startCamera(context, cameraFacing,  /*surfaceTexture=*/null)
    }

    private fun onCameraStarted(surfaceTexture: SurfaceTexture) {
        previewFrameTexture = surfaceTexture
        // Make the display view visible to start showing the preview. This triggers the
        // SurfaceHolder.Callback added to (the holder of) previewDisplayView.
        previewDisplayView!!.visibility = View.VISIBLE
    }

    fun onResume(context: Activity, applicationInfo: ApplicationInfo) {
        converter.setFlipY(FLIP_FRAMES_VERTICALLY)
        converter.setConsumer(processor)
        if (PermissionHelper.cameraPermissionsGranted(context)) {
            startCamera(context, applicationInfo)
        }
    }

    fun onPause() {
        converter.close()
    }
}