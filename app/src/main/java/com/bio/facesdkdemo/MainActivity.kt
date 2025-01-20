package com.bio.facesdkdemo

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bio.facesdk.FaceBox
import com.bio.facesdk.FaceDetectionParam
import com.bio.facesdk.FaceSDK
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    companion object {
        private val SELECT_ADD_USER_REQUEST_CODE = 1
        private val SELECT_DETECT_FROM_IMAGE_REQUEST_CODE = 2
        private val SELECT_DETECT_FROM_CAMERA_REQUEST_CODE = 3
    }

    private lateinit var dbManager: DBManager
    private lateinit var textWarning: TextView
    private lateinit var personAdapter: PersonAdapter

    private lateinit var currentPhotoPath: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textWarning = findViewById<TextView>(R.id.textWarning)

        val license_str = application.assets.open("license").bufferedReader().use{
            it.readText()
        }

        var ret = FaceSDK.setActivation(license_str)

        if (ret == FaceSDK.SDK_SUCCESS) {
            ret = FaceSDK.init(assets)
        }

        if (ret != FaceSDK.SDK_SUCCESS) {
            textWarning.setVisibility(View.VISIBLE)
            if (ret == FaceSDK.SDK_LICENSE_KEY_ERROR) {
                textWarning.setText("License key error!")
            } else if (ret == FaceSDK.SDK_LICENSE_APPID_ERROR) {
                textWarning.setText("App ID error!")
            } else if (ret == FaceSDK.SDK_LICENSE_EXPIRED) {
                textWarning.setText("License key expired!")
            } else if (ret == FaceSDK.SDK_NO_ACTIVATED) {
                textWarning.setText("Activation failed!")
            } else if (ret == FaceSDK.SDK_INIT_ERROR) {
                textWarning.setText("Engine init error!")
            }
        }

        dbManager = DBManager(this)
        dbManager.loadPerson()

        personAdapter = PersonAdapter(this, DBManager.personList)
        val listView: ListView = findViewById<View>(R.id.listPerson) as ListView
        listView.setAdapter(personAdapter)

        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
        )

        val requestCode = 1

        val deniedPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_DENIED
        }.toTypedArray()

        if (deniedPermissions.isNotEmpty()) {
            // Request all denied permissions at once
            ActivityCompat.requestPermissions(this, deniedPermissions, requestCode)
        }

        findViewById<Button>(R.id.buttonDetect).setOnClickListener {
            // Show a dialog with options to choose from
            val options = arrayOf("Select Image", "Capture Image")
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Choose an option")
            builder.setItems(options) { dialog, which ->
                when (which) {
                    0 -> {
                        // Option 1: Select image from gallery
                        val intent = Intent(Intent.ACTION_PICK)
                        intent.type = "image/*"
                        startActivityForResult(Intent.createChooser(intent, getString(R.string.choose_photo)), SELECT_DETECT_FROM_IMAGE_REQUEST_CODE)
                    }
                    1 -> {
                        // Option 2: Capture image with the camera
                        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                        if (intent.resolveActivity(packageManager) != null) {
                            // Create a file to store the captured image
                            val photoFile: File? = createImageFile()
                            photoFile?.also {
                                val photoURI: Uri = FileProvider.getUriForFile(
                                    this,
                                    "${applicationContext.packageName}.fileprovider",
                                    it
                                )
                                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                                startActivityForResult(intent, SELECT_DETECT_FROM_CAMERA_REQUEST_CODE)
                            }
                        }
                    }
                }
            }
            builder.show()
        }

        findViewById<Button>(R.id.buttonCapture).setOnClickListener {
            startActivity(Intent(this, SmartCaptureActivityKt::class.java))
        }

        findViewById<Button>(R.id.buttonEnroll).setOnClickListener {
            val intent = Intent()
            intent.setType("image/*")
            intent.setAction(Intent.ACTION_PICK)
            startActivityForResult(Intent.createChooser(intent, getString(R.string.choose_photo)), SELECT_ADD_USER_REQUEST_CODE)
        }

        findViewById<Button>(R.id.buttonIdentify).setOnClickListener {
            startActivity(Intent(this, CameraActivityKt::class.java))
        }

        findViewById<ImageButton>(R.id.buttonSetting).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun createImageFile(): File? {
        return try {
            val timestamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val storageDir: File = getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: return null
            val imageFile = File.createTempFile("JPEG_${timestamp}_", ".jpg", storageDir)
            currentPhotoPath = imageFile.absolutePath

            imageFile
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    override fun onResume() {
        super.onResume()

        personAdapter.notifyDataSetChanged()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == SELECT_ADD_USER_REQUEST_CODE && resultCode == RESULT_OK) {
            try {
                var bitmap: Bitmap = Utils.getCorrectlyOrientedImage(this, data?.data!!)

                val faceDetectionParam = FaceDetectionParam()
                faceDetectionParam.check_liveness = true
                faceDetectionParam.check_liveness_level = SettingsActivity.getLivenessModelType(this)
                var faceBoxes: List<FaceBox>? = FaceSDK.faceDetection(bitmap, faceDetectionParam)

                if(faceBoxes.isNullOrEmpty()) {
                    Toast.makeText(this, getString(R.string.no_face), Toast.LENGTH_SHORT).show()
                } else if (faceBoxes.size > 1) {
                    Toast.makeText(this, getString(R.string.multiple_face_detected), Toast.LENGTH_SHORT).show()
                } else {
                    val faceImage = Utils.cropFace(bitmap, faceBoxes[0])
                    val templates = FaceSDK.templateExtraction(bitmap, faceBoxes[0])

                    val calendar = Calendar.getInstance()
                    val year = calendar.get(Calendar.YEAR)
                    val month = calendar.get(Calendar.MONTH) + 1 // Month starts from 0
                    val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
                    val hour = calendar.get(Calendar.HOUR_OF_DAY) // 24-hour format
                    val minute = calendar.get(Calendar.MINUTE)
                    val second = calendar.get(Calendar.SECOND)

                    dbManager.insertPerson("User " + year + month + dayOfMonth + hour + minute + second, faceImage, templates)
                    personAdapter.notifyDataSetChanged()
                    Toast.makeText(this, getString(R.string.user_registered), Toast.LENGTH_SHORT).show()
                }
            } catch (e: java.lang.Exception) {
                //handle exception
                e.printStackTrace()
            }
        } else if ((requestCode == SELECT_DETECT_FROM_IMAGE_REQUEST_CODE || requestCode == SELECT_DETECT_FROM_CAMERA_REQUEST_CODE) && resultCode == RESULT_OK) {
            try {
                var bitmap: Bitmap? = null
                if (requestCode == SELECT_DETECT_FROM_IMAGE_REQUEST_CODE) {
                    bitmap = Utils.getCorrectlyOrientedImage(this, data?.data!!)
                } else if (requestCode == SELECT_DETECT_FROM_CAMERA_REQUEST_CODE) {
                    val file = File(currentPhotoPath)
                    if (!file.exists()) {
                        Log.e("", "File does not exist at path: $currentPhotoPath")
                        return
                    }
                    val contentUri = FileProvider.getUriForFile(this,this.getPackageName() + ".fileprovider", file)

                    bitmap = Utils.getCorrectlyOrientedImage(this, contentUri)
                } else
                    return

                val param = FaceDetectionParam()
                param.check_liveness = true
                param.check_liveness_level = SettingsActivity.getLivenessModelType(this)
                param.check_eye_closeness = true
                param.check_face_occlusion = true
                param.check_mouth_opened = true
                param.estimate_age_gender = true
                var faceBoxes: List<FaceBox>? = FaceSDK.faceDetection(bitmap, param)

                if(faceBoxes.isNullOrEmpty()) {
                    Toast.makeText(this, getString(R.string.no_face), Toast.LENGTH_SHORT).show()
                } else if (faceBoxes.size > 1) {
                    Toast.makeText(this, getString(R.string.multiple_face_detected), Toast.LENGTH_SHORT).show()
                } else {
                    val faceImage = Utils.cropFace(bitmap, faceBoxes[0])

                    val intent = Intent(this, DetectionResultActivity::class.java)
                    intent.putExtra("face_image", faceImage)
                    intent.putExtra("yaw", faceBoxes[0].yaw)
                    intent.putExtra("roll", faceBoxes[0].roll)
                    intent.putExtra("pitch", faceBoxes[0].pitch)
                    intent.putExtra("face_quality", faceBoxes[0].face_quality)
                    intent.putExtra("face_luminance", faceBoxes[0].face_luminance)
                    intent.putExtra("liveness", faceBoxes[0].liveness)
                    intent.putExtra("left_eye_closed", faceBoxes[0].left_eye_closed)
                    intent.putExtra("right_eye_closed", faceBoxes[0].right_eye_closed)
                    intent.putExtra("face_occlusion", faceBoxes[0].face_occlusion)
                    intent.putExtra("mouth_opened", faceBoxes[0].mouth_opened)
                    intent.putExtra("age", faceBoxes[0].age)
                    intent.putExtra("gender", faceBoxes[0].gender)

                    startActivity(intent)
                }
            } catch (e: java.lang.Exception) {
                //handle exception
                e.printStackTrace()
            }
        }
    }
}