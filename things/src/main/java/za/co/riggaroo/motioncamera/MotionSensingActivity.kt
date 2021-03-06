package za.co.riggaroo.motioncamera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ImageReader
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.ImageView
import com.google.android.things.pio.Gpio
import com.google.android.things.pio.PeripheralManagerService
import za.co.riggaroo.motioncamera.camera.CustomCamera


class MotionSensingActivity : AppCompatActivity(), MotionSensor.MotionListener {


    private val ACT_TAG: String = "MotionSensingActivity"

    private lateinit var ledGpio: Gpio

    private lateinit var camera: CustomCamera
    private lateinit var motionImage: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_motion_sensing)

        setupUIElements()
        setupCamera()

        val peripheralManagerService = PeripheralManagerService()

        ledGpio = peripheralManagerService.openGpio("GPIO_174")
        ledGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)

        val motionSensorPin = peripheralManagerService.openGpio("GPIO_35")
        lifecycle.addObserver(MotionSensor(this, motionSensorPin))

    }

    private fun setupUIElements() {
        motionImage = findViewById(R.id.image_view_motion)

    }

    private val onImageAvailableListener = ImageReader.OnImageAvailableListener { reader ->
        val image = reader.acquireLatestImage()
        val imageBuffer = image.planes[0].buffer
        val imageBytes = ByteArray(imageBuffer.remaining())
        imageBuffer.get(imageBytes)
        image.close()

        motionImage.setImageBitmap(getBitmapFromByteArray(imageBytes))
        //write log to firebase

    }

    private fun getBitmapFromByteArray(imageBytes: ByteArray): Bitmap {
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        val matrix = Matrix()
        matrix.postRotate(180f)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun setupCamera() {
        camera = CustomCamera.getInstance()
        camera.initializeCamera(this, Handler(), onImageAvailableListener)
    }

    override fun onMotionDetected() {
        Log.d(ACT_TAG, "onMotionDetected")
        camera.takePicture()
        ledGpio.value = true
    }

    override fun onMotionStopped() {
        Log.d(ACT_TAG, "onMotionStopped")
        ledGpio.value = false
    }
}
