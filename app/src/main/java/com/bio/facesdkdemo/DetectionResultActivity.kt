package com.bio.facesdkdemo

import android.graphics.Bitmap
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
class DetectionResultActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detection_result)

        val faceImage = intent.getParcelableExtra("face_image") as? Bitmap
        val livenessScore = intent.getFloatExtra("liveness", 0f)
        val yaw = intent.getFloatExtra("yaw", 0f)
        val roll = intent.getFloatExtra("roll", 0f)
        val pitch = intent.getFloatExtra("pitch", 0f)
        val face_quality = intent.getFloatExtra("face_quality", 0f)
        val face_luminance = intent.getFloatExtra("face_luminance", 0f)
        val left_eye_closed = intent.getFloatExtra("left_eye_closed", 0f)
        val right_eye_closed = intent.getFloatExtra("right_eye_closed", 0f)
        val face_occlusion = intent.getFloatExtra("face_occlusion", 0f)
        val mouth_opened = intent.getFloatExtra("mouth_opened", 0f)
        val age = intent.getIntExtra("age", 0)
        val gender = intent.getIntExtra("gender", 0)

        findViewById<ImageView>(R.id.imageFace).setImageBitmap(faceImage)

        findViewById<TextView>(R.id.textLivenessScore).text = String.format("%.04f", livenessScore)
        if (livenessScore > SettingsActivity.getLivenessThreshold(this))
            findViewById<TextView>(R.id.textLivenessResult).text = "Real"
        else
            findViewById<TextView>(R.id.textLivenessResult).text = "Spoof"

        findViewById<TextView>(R.id.textQualityScore).text = String.format("%.04f", face_quality)
        if (face_quality < 0.5f)
            findViewById<TextView>(R.id.textQualityResult).text = "Low"
        else if(face_quality < 0.75f)
            findViewById<TextView>(R.id.textQualityResult).text = "Medium"
        else
            findViewById<TextView>(R.id.textQualityResult).text = "High"

        if(gender == 0)
            findViewById<TextView>(R.id.textGenderResult).text = "Male"
        else
            findViewById<TextView>(R.id.textGenderResult).text = "Female"

        findViewById<TextView>(R.id.textAgeResult).text = String.format("%d", age)

        findViewById<TextView>(R.id.textEyeRightScore).text = String.format("%.04f", right_eye_closed)
        if (right_eye_closed > SettingsActivity.getEyecloseThreshold(this))
            findViewById<TextView>(R.id.textEyeRightResult).text = "Close"
        else
            findViewById<TextView>(R.id.textEyeRightResult).text = "Open"

        findViewById<TextView>(R.id.textEyeLeftScore).text = String.format("%.04f", left_eye_closed)
        if (left_eye_closed > SettingsActivity.getEyecloseThreshold(this))
            findViewById<TextView>(R.id.textEyeLeftResult).text = "Close"
        else
            findViewById<TextView>(R.id.textEyeLeftResult).text = "Open"

        findViewById<TextView>(R.id.textMouthScore).text = String.format("%.04f", mouth_opened)
        if (mouth_opened > SettingsActivity.getMouthopenThreshold(this))
            findViewById<TextView>(R.id.textMouthResult).text = "Open"
        else
            findViewById<TextView>(R.id.textMouthResult).text = "Close"

        findViewById<TextView>(R.id.textOcclusionScore).text = String.format("%.04f", face_occlusion)
        if (face_occlusion > SettingsActivity.getOcclusionThreshold(this))
            findViewById<TextView>(R.id.textOcclusionResult).text = "Yes"
        else
            findViewById<TextView>(R.id.textOcclusionResult).text = "No"

        findViewById<TextView>(R.id.textYawResult).text = String.format("%.04f", yaw)
        findViewById<TextView>(R.id.textRollResult).text = String.format("%.04f", roll)
        findViewById<TextView>(R.id.textPitchResult).text = String.format("%.04f", pitch)
    }
}