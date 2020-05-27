package com.example.mlkit

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.IOException


class MainActivity : AppCompatActivity() {
    private val TAG = "MyActivity"
    val MY_PERMISSIONS_REQUEST_READ_CONTACTS = 1
    private lateinit var file : File
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (ContextCompat.checkSelfPermission(this@MainActivity,
                Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED) {

            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this@MainActivity,
                    Manifest.permission.READ_CONTACTS)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this@MainActivity,
                    arrayOf(Manifest.permission.READ_CONTACTS),
                    MY_PERMISSIONS_REQUEST_READ_CONTACTS)

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
            // Permission has already been granted
        }
        val sdcard : File? = getExternalFilesDir("/sdcard")
        file = File(sdcard, "capture.pjg")
        button.setOnClickListener {
            capture()
        }
    }

    private fun capture(){
        intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, FileProvider.getUriForFile(this, "com.bignerdranch.android.test.fileprovider", file))
        startActivityForResult(intent,101)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == 101 && resultCode == Activity.RESULT_OK){
            val options : BitmapFactory.Options = BitmapFactory.Options()
            options.inSampleSize = 8;
            var bitmap : Bitmap = BitmapFactory.decodeFile(file.absolutePath,options)
            var exif: ExifInterface? = null
            try {
                exif = ExifInterface(file.absolutePath)
            } catch (e: IOException) {
                e.printStackTrace()
            }
            val exifOrientation: Int
            val exifDegree: Int

            if (exif != null) {
                exifOrientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
                exifDegree = exifOrientationToDegrees(exifOrientation)
            } else {
                exifDegree = 0
            }
            val image = FirebaseVisionImage.fromBitmap(rotate(bitmap, exifDegree.toFloat()))
            val detector = FirebaseVision.getInstance().onDeviceTextRecognizer
            Log.d("whatis?","before")
            val result = detector.processImage(image)
            Log.d("whatis",result.result.toString())

            val resultText = result.result
            if (resultText != null) {
                for (block in resultText.textBlocks) {
                    val blockText = block.text
                    val blockConfidence = block.confidence
                    val blockLanguages = block.recognizedLanguages
                    val blockCornerPoints = block.cornerPoints
                    val blockFrame = block.boundingBox
                    for (line in block.lines) {
                        val lineText = line.text
                        val lineConfidence = line.confidence
                        val lineLanguages = line.recognizedLanguages
                        val lineCornerPoints = line.cornerPoints
                        val lineFrame = line.boundingBox
                        for (element in line.elements) {
                            val elementText = element.text
                            val elementConfidence = element.confidence
                            val elementLanguages = element.recognizedLanguages
                            val elementCornerPoints = element.cornerPoints
                            val elementFrame = element.boundingBox
                            Log.d("whatis",elementText)
                        }
                    }
                }
            }

//                .addOnSuccessListener { firebaseVisionText ->
//                    Log.d(TAG,"success")
//                    // Task completed successfully
//                    // ...
//                }
//                .addOnFailureListener { e ->
//                    Log.e(TAG,e.toString())
//                    // Task failed with an exception
//                    // ...
//                }
//            val resultText = result.result?.text
            TextV.text = "resultText"
        }
    }
    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_READ_CONTACTS -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return
            }

            // Add other 'when' lines to check for other
            // permissions this app might request.
            else -> {
                // Ignore all other requests.
            }
        }
    }
    private fun exifOrientationToDegrees(exifOrientation: Int): Int {
        if(exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) {
            return 90
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {
            return 180
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {
            return 270
        }
        return 0
    }

    private fun rotate(bitmap: Bitmap, degree: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degree)
        return Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )
    }
//    private class YourImageAnalyzer : ImageAnalysis.Analyzer {
//        private fun degreesToFirebaseRotation(degrees: Int): Int = when(degrees) {
//            0 -> FirebaseVisionImageMetadata.ROTATION_0
//            90 -> FirebaseVisionImageMetadata.ROTATION_90
//            180 -> FirebaseVisionImageMetadata.ROTATION_180
//            270 -> FirebaseVisionImageMetadata.ROTATION_270
//            else -> throw Exception("Rotation must be 0, 90, 180, or 270.")
//        }
//
//        override fun analyze(imageProxy: Bitmap?, degrees: Int) {
//            val mediaImage = imageProxy
//            val imageRotation = degreesToFirebaseRotation(degrees)
//            if (mediaImage != null) {
//                val image = FirebaseVisionImage.fromMediaImage(mediaImage, imageRotation)
//                // Pass image to an ML Kit Vision API
//                // ...
//            }
//        }
//    }
}
