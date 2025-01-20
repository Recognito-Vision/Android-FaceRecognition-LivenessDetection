package com.bio.facesdkdemo

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Size
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bio.facesdk.FaceBox
import com.bio.facesdk.FaceDetectionParam
import com.bio.facesdk.FaceSDK
import io.fotoapparat.Fotoapparat
import io.fotoapparat.parameter.Resolution
import io.fotoapparat.preview.Frame
import io.fotoapparat.preview.FrameProcessor
import io.fotoapparat.selector.front
import io.fotoapparat.view.CameraView
import java.util.Calendar
import java.util.Random
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class SmartCaptureActivityKt : AppCompatActivity(), SmartCaptureView.ViewModeChanged {

    val TAG = SmartCaptureActivityKt::class.java.simpleName
    val PREVIEW_WIDTH = 720
    val PREVIEW_HEIGHT = 1280

    private lateinit var fotoapparat: Fotoapparat
    private lateinit var context: Context

    private lateinit var cameraView: CameraView

    private lateinit var captureView: SmartCaptureView

    private lateinit var titleTxt: TextView

    private lateinit var warningTxt: TextView

    private lateinit var lytCaptureResult: ConstraintLayout

    private var capturedBitmap: Bitmap? = null

    private var capturedFace: FaceBox? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_smart_capture_kt)

        context = this
        cameraView = findViewById(R.id.preview)
        captureView = findViewById(R.id.captureView)
        titleTxt = findViewById(R.id.textTitle)
        warningTxt = findViewById(R.id.txtWarning)
        lytCaptureResult = findViewById(R.id.lytCaptureResult)

        findViewById<Button>(R.id.buttonRetake).setOnClickListener {
            val intent = intent // Get the current activity's intent
            finish() // Close the current activity
            startActivity(intent) // Restart the activity
        }

        captureView.setViewModeInterface(this)
        captureView.setViewMode(SmartCaptureView.VIEW_MODE.VIEW_WAIT)

        findViewById<View>(R.id.buttonEnroll).setOnClickListener {
            val faceImage = Utils.cropFace(capturedBitmap, capturedFace)
            val templates = FaceSDK.templateExtraction(capturedBitmap, capturedFace)

            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH) + 1 // Month starts from 0
            val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
            val hour = calendar.get(Calendar.HOUR_OF_DAY) // 24-hour format
            val minute = calendar.get(Calendar.MINUTE)
            val second = calendar.get(Calendar.SECOND)

            val dbManager = DBManager(context)
            dbManager.insertPerson("User " + year + month + dayOfMonth + hour + minute + second, faceImage, templates)
            Toast.makeText(this, getString(R.string.user_registered), Toast.LENGTH_SHORT).show()
            finish()
        }

        fotoapparat = Fotoapparat.with(this)
            .into(cameraView)
            .lensPosition(front())
            .frameProcessor(FaceFrameProcessor())
            .previewResolution { Resolution(PREVIEW_HEIGHT,PREVIEW_WIDTH) }
            .build()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_DENIED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 1)
        } else {
            fotoapparat.start()
        }
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            fotoapparat.start()
        }
    }

    override fun onPause() {
        super.onPause()
        fotoapparat.stop()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
            ) {
                fotoapparat.start()
            }
        }
    }

    override fun capture_finished() {
        lytCaptureResult.visibility = View.VISIBLE
    }

    fun checkFace(faceBoxes: List<FaceBox>?, context: Context?): FACE_CAPTURE_STATE {
        if (faceBoxes == null || faceBoxes.size == 0) return FACE_CAPTURE_STATE.NO_FACE

        if (faceBoxes.size > 1) {
            return FACE_CAPTURE_STATE.MULTIPLE_FACES
        }

        val faceBox = faceBoxes[0]
        var faceLeft = Float.MAX_VALUE
        var faceRight = 0f
        var faceBottom = 0f
        for (i in 0..67) {
            faceLeft = min(faceLeft.toDouble(), faceBox.landmarks_68[i * 2].toDouble()).toFloat()
            faceRight =
                max(faceRight.toDouble(), faceBox.landmarks_68[i * 2].toDouble()).toFloat()
            faceBottom =
                max(faceBottom.toDouble(), faceBox.landmarks_68[i * 2 + 1].toDouble()).toFloat()
        }

        val sizeRate = 0.30f
        val interRate = 0.03f
        val frameSize = Size(PREVIEW_WIDTH, PREVIEW_HEIGHT)
        val roiRect = SmartCaptureView.getROIRect(frameSize)
        val centerY = ((faceBox.y2 + faceBox.y1) / 2).toFloat()
        val topY = centerY - (faceBox.y2 - faceBox.y1) * 2 / 3
        val interX =
            (max(0.0, (roiRect.left - faceLeft).toDouble()) + max(
                0.0,
                (faceRight - roiRect.right).toDouble()
            )).toFloat()
        val interY =
            (max(0.0, (roiRect.top - topY).toDouble()) + max(
                0.0,
                (faceBottom - roiRect.bottom).toDouble()
            )).toFloat()
        if (interX / roiRect.width() > interRate || interY / roiRect.height() > interRate) {
            return FACE_CAPTURE_STATE.FIT_IN_CIRCLE
        }

        if (interX / roiRect.width() > interRate || interY / roiRect.height() > interRate) {
            return FACE_CAPTURE_STATE.FIT_IN_CIRCLE
        }

        if ((faceBox.y2 - faceBox.y1) * (faceBox.x2 - faceBox.x1) < roiRect.width() * roiRect.height() * sizeRate) {
            return FACE_CAPTURE_STATE.MOVE_CLOSER
        }

        if (abs(faceBox.yaw.toDouble()) > SettingsActivity.getYawThreshold(context!!) ||
            abs(faceBox.roll.toDouble()) > SettingsActivity.getRollThreshold(context!!) ||
            abs(faceBox.pitch.toDouble()) > SettingsActivity.getPitchThreshold(context!!)
        ) {
            return FACE_CAPTURE_STATE.NO_FRONT
        }

        if (faceBox.face_occlusion > SettingsActivity.getOcclusionThreshold(context!!)) {
            return FACE_CAPTURE_STATE.FACE_OCCLUDED
        }

        if (faceBox.left_eye_closed > SettingsActivity.getEyecloseThreshold(context!!) ||
            faceBox.right_eye_closed > SettingsActivity.getEyecloseThreshold(context!!)
        ) {
            return FACE_CAPTURE_STATE.EYE_CLOSED
        }

        if (faceBox.mouth_opened > SettingsActivity.getMouthopenThreshold(context!!)) {
            return FACE_CAPTURE_STATE.MOUTH_OPENED
        }

        return FACE_CAPTURE_STATE.CAPTURE_OK
    }

    inner class FaceFrameProcessor : FrameProcessor {

        override fun process(frame: Frame) {
            val bitmap = FaceSDK.yuv2Bitmap(frame.image, frame.size.width, frame.size.height, 7)

            val faceDetectionParam = FaceDetectionParam()
            faceDetectionParam.check_face_occlusion = true
            faceDetectionParam.check_eye_closeness = true
            faceDetectionParam.check_mouth_opened = true
            val faceBoxes = FaceSDK.faceDetection(bitmap, faceDetectionParam)

            val faceCaptureState = checkFace(faceBoxes, context)

            if (captureView.viewMode == SmartCaptureView.VIEW_MODE.VIEW_WAIT) {
                runOnUiThread {
                    captureView.setFrameSize(Size(bitmap.width, bitmap.height))
                    if (faceCaptureState == FACE_CAPTURE_STATE.NO_FACE) {
                        warningTxt.text = ""

                        captureView.setViewMode(SmartCaptureView.VIEW_MODE.VIEW_WAIT)
                    } else if (faceCaptureState == FACE_CAPTURE_STATE.MULTIPLE_FACES) warningTxt.text =
                        "Too many faces detected!"
                    else if (faceCaptureState == FACE_CAPTURE_STATE.FIT_IN_CIRCLE) warningTxt.text =
                        "Position your face inside the frame!"
                    else if (faceCaptureState == FACE_CAPTURE_STATE.MOVE_CLOSER) warningTxt.text =
                        "Too far away! Please move closer."
                    else if (faceCaptureState == FACE_CAPTURE_STATE.NO_FRONT) warningTxt.text =
                        "Turn your face to the front!"
                    else if (faceCaptureState == FACE_CAPTURE_STATE.FACE_OCCLUDED) warningTxt.text =
                        "Your face is occluded!"
                    else if (faceCaptureState == FACE_CAPTURE_STATE.EYE_CLOSED) warningTxt.text =
                        "Open your eyes!"
                    else if (faceCaptureState == FACE_CAPTURE_STATE.MOUTH_OPENED) warningTxt.text =
                        "Close your mouth!"
                    else if (faceCaptureState == FACE_CAPTURE_STATE.SPOOFED_FACE) warningTxt.text =
                        "Detected a spoof attempt!"
                    else {
                        warningTxt.text = ""
                        captureView.setViewMode(SmartCaptureView.VIEW_MODE.VIEW_CAPTURE)

                        capturedBitmap = bitmap
                        capturedFace = faceBoxes[0]
                        captureView.setCapturedBitmap(capturedBitmap)
                    }
                }
            } else if (captureView.viewMode == SmartCaptureView.VIEW_MODE.VIEW_CAPTURE) {
                if (faceCaptureState == FACE_CAPTURE_STATE.CAPTURE_OK) {
                    capturedBitmap = bitmap
                    capturedFace = faceBoxes[0]
                    captureView.setCapturedBitmap(capturedBitmap)
                }
            } else if (captureView.viewMode == SmartCaptureView.VIEW_MODE.VIEW_SUCCESS) {
                titleTxt.text = "Capture Result"
                runOnUiThread { fotoapparat.stop() }
            }
        }
    }
}