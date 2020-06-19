package com.example.mlkit

import android.Manifest
import android.R.attr
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Point
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
import kotlinx.android.synthetic.main.activity_main.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


class MainActivity : AppCompatActivity() {
    private val TAG = "MyActivity"
    val MY_PERMISSIONS_REQUEST_READ_CONTACTS = 1
    private lateinit var file : File
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkPermissionCamera() //Camera Permission 확인
        val sdcard : File? = getExternalFilesDir("/sdcard")
        file = File(sdcard, "capture.pjg")
        button.setOnClickListener {  //버튼누르면 카메라 실행 => 사진찍고 확인=> 사진에서 글자분석
            capture()
        }
    }

    private fun checkPermissionCamera(){
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
            Log.d(TAG,"PermissionError")
            // Permission has already been granted
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
            var stri : String = "" //화면에 보여줄 String값

            //var p = Array<IntArray>(4,{ IntArray(2) })
            var rectList: ArrayList<RectPos> = ArrayList()

            val result = detector.processImage(image).addOnSuccessListener { firebaseVisionText -> //사진에서 글자인식하고 return한 값 분석
                val resultText = firebaseVisionText.text
                for (block in firebaseVisionText.textBlocks) {
                    val blockText = block.text
                    val blockConfidence = block.confidence
                    val blockLanguages = block.recognizedLanguages
                    val blockCornerPoints = block.cornerPoints

                    val blockFrame = block.boundingBox
                    for (line in block.lines) { //라인 단위로 끊어서 확인
                        val lineText = line.text
                        val lineConfidence = line.confidence
                        val lineLanguages = line.recognizedLanguages
                        val lineCornerPoints = line.cornerPoints
                        val lineFrame = line.boundingBox
                        stri = stri + lineText +'\n' //화면에 출력되는 문자는 line의 text값들이다.
                        rectList.add(RectPos(Point(lineCornerPoints?.get(0)?.x!!,lineCornerPoints?.get(0)?.y!!),
                            Point(lineCornerPoints?.get(2)?.x!!,lineCornerPoints?.get(2)?.y!!), 0))
                    }

                }
                TextV.text = stri


                //------------------------
                var fileName: String? = "myImage" //no .png or .jpg needed

                try {
                    val bytes = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
                    val fo: FileOutputStream = openFileOutput(fileName, Context.MODE_PRIVATE)
                    fo.write(bytes.toByteArray())
                    // remember close file output
                    fo.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                    fileName = null
                }
                //-----------------------

                intent = Intent(applicationContext,ResultActivity::class.java)
                intent.putParcelableArrayListExtra("result",rectList)
                intent.putExtra("img", fileName)

                startActivity(intent)
            }
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
}
