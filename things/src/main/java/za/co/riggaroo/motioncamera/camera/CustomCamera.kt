package za.co.riggaroo.motioncamera.camera

import android.content.ContentValues
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Handler
import android.util.Log

/**
 * @author rebeccafranks
 * @since 2017/09/15.
 */
class CustomCamera : AutoCloseable {

    private var mImageReader: ImageReader? = null
    private var mCameraDevice: CameraDevice? = null
    private var mCaptureSession: CameraCaptureSession? = null

    companion object InstanceHolder {
        val IMAGE_WIDTH = 640
        val IMAGE_HEIGHT = 480
        val MAX_IMAGES = 1
        private val mCamera = CustomCamera()

        fun getInstance(): CustomCamera {
            return mCamera
        }
    }

    fun initializeCamera(context: Context, backgroundHandler: Handler, imageListener: ImageReader.OnImageAvailableListener) {
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        var camIds = emptyArray<String>()
        try {
            camIds = manager.cameraIdList
        } catch (e: CameraAccessException) {
            Log.d(ContentValues.TAG, "Cam access exception getting ids", e)
        }
        if (camIds.isEmpty()) {
            Log.d(ContentValues.TAG, "No cameras found")
            return
        }

        val id = camIds[0]
        mImageReader = ImageReader.newInstance(IMAGE_WIDTH, IMAGE_HEIGHT,
                ImageFormat.JPEG, MAX_IMAGES)
        mImageReader?.setOnImageAvailableListener(imageListener, backgroundHandler)
        try {
            manager.openCamera(id, mStateCallback, backgroundHandler)
        } catch (cae: Exception) {
            Log.d(ContentValues.TAG, "Camera access exception", cae)
        }
    }

    fun takePicture() {
        mCameraDevice?.createCaptureSession(
                arrayListOf(mImageReader?.surface),
                mSessionCallback,
                null)
    }

    private fun triggerImageCapture() {
        val captureBuilder = mCameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
        captureBuilder?.addTarget(mImageReader!!.surface)
        captureBuilder?.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
        Log.d(ContentValues.TAG, "Session initialized.")
        mCaptureSession?.capture(captureBuilder?.build(), mCaptureCallback, null)
    }

    private val mCaptureCallback = object : CameraCaptureSession.CaptureCallback() {

        override fun onCaptureProgressed(session: CameraCaptureSession?, request: CaptureRequest?, partialResult: CaptureResult?) {
            Log.d(ContentValues.TAG, "Partial result")
        }

        override fun onCaptureFailed(session: CameraCaptureSession?, request: CaptureRequest?, failure: CaptureFailure?) {
            Log.d(ContentValues.TAG, "Capture session failed")
        }

        override fun onCaptureCompleted(session: CameraCaptureSession?, request: CaptureRequest?, result: TotalCaptureResult?) {
            session?.close()
            mCaptureSession = null
            Log.d(ContentValues.TAG, "Capture session closed")
        }
    }

    private val mSessionCallback = object : CameraCaptureSession.StateCallback() {
        override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession?) {
            Log.w(ContentValues.TAG, "Failed to configure camera")
        }

        override fun onConfigured(cameraCaptureSession: CameraCaptureSession?) {
            if (mCameraDevice == null) {
                return
            }
            mCaptureSession = cameraCaptureSession
            triggerImageCapture()
        }

    }

    private val mStateCallback = object : CameraDevice.StateCallback() {
        override fun onError(cameraDevice: CameraDevice, code: Int) {
            Log.d(ContentValues.TAG, "Camera device error, closing")
            cameraDevice.close()
        }

        override fun onOpened(cameraDevice: CameraDevice) {
            Log.d(ContentValues.TAG, "Opened camera.")
            mCameraDevice = cameraDevice
        }

        override fun onDisconnected(cameraDevice: CameraDevice) {
            Log.d(ContentValues.TAG, "Camera disconnected, closing")
            cameraDevice.close()
        }

        override fun onClosed(camera: CameraDevice) {
            Log.d(ContentValues.TAG, "Closed camera, releasing")
            mCameraDevice = null
        }
    }

    override fun close() {
        mCameraDevice?.close()
    }
}