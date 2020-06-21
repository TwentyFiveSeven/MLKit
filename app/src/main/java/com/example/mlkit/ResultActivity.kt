package com.example.mlkit

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.get
import androidx.core.graphics.set
import androidx.databinding.DataBindingUtil
import com.example.mlkit.databinding.ActivityResultBinding


class ResultActivity : AppCompatActivity() {
    private lateinit var activityResultBinding: ActivityResultBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setUpDataBinding()

        val intent = intent /*데이터 수신*/
        val img = intent.getStringExtra("img")
        val rectList = intent.getParcelableArrayListExtra<RectPos>("result")

        val bitmap_read = BitmapFactory.decodeStream(openFileInput(img))
        var bitmap : Bitmap = bitmap_read.copy(Bitmap.Config.ARGB_8888,true)


        rectList.map {

            if(it.leftTop!!.x>0){
                it.leftTop!!.x--
            }
            if(it.leftTop!!.y>0){
                it.leftTop!!.y--
            }
            if(it.rightBottom!!.x>0){
                it.rightBottom!!.x--
            }
            if(it.rightBottom!!.y>0){
                it.rightBottom!!.y--
            }

            bitmap = drawRect(it.leftTop!!.x, it.leftTop!!.y, it.rightBottom!!.x, it.rightBottom!!.y, bitmap, it.color!!)

        }


        activityResultBinding.imageView.setImageBitmap(bitmap)

    }

    private fun setUpDataBinding() {
        activityResultBinding = DataBindingUtil.setContentView(this, R.layout.activity_result)
    }

    private fun drawRect(x1 : Int, y1 : Int, x2 : Int, y2 : Int, bitmap : Bitmap, c : Int):Bitmap {
        val temp : Int
        if(c ==0){
            temp = 0x00ff00
        }else if(c ==1){
            temp = 0xffff00
        }else{
            temp = 0xff0000
        }
        for (i in x1 until x2) {
            bitmap.setPixel(i,y1,temp)
            bitmap.setPixel(i,y2,temp)
        }

        for (i in y1 until y2) {
            bitmap.setPixel(x1,i,temp)
            bitmap.setPixel(x2,i,temp)
        }

        return bitmap
    }
}
