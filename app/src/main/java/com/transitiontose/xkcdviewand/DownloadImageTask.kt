package com.transitiontose.xkcdviewand

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import java.lang.ref.WeakReference

internal class DownloadImageTask(private val bmImage: WeakReference<ImageView>?, private val xkcdActivity : WeakReference<XkcdActivity>?) : AsyncTask<String?, Int?, Bitmap?>() {
    override fun onPreExecute() {}
    override fun doInBackground(vararg urls: String?): Bitmap? {
        val urldisplay = urls[0]
        var image: Bitmap? = null
        try {
            val `in` = java.net.URL(urldisplay).openStream()
            image = BitmapFactory.decodeStream(`in`)
        } catch (e: Exception) {
            Log.e("Error", e.message)
            e.printStackTrace()
        }
        return image
    }
    override fun onPostExecute(result: Bitmap?) {
        if (xkcdActivity?.get()?.isInPortraitMode() == true) {
            xkcdActivity.get()?.progressBar?.visibility = View.GONE
        }
        imageViewAnimatedChange(xkcdActivity?.get()?.applicationContext, bmImage, result)
    }

    private fun imageViewAnimatedChange(context: Context?, comicImageView: WeakReference<ImageView>?, newImage: Bitmap?) {
        val fadeFirstImageOut = AnimationUtils.loadAnimation(context, android.R.anim.fade_out)
        val fadeSecondImageIn = AnimationUtils.loadAnimation(context, android.R.anim.fade_in)
        fadeFirstImageOut.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {}
            override fun onAnimationRepeat(animation: Animation) {}
            override fun onAnimationEnd(animation: Animation) {
                comicImageView?.get()?.setImageBitmap(newImage)
                fadeSecondImageIn.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation) {}
                    override fun onAnimationRepeat(animation: Animation) {}
                    override fun onAnimationEnd(animation: Animation) {}
                })
                comicImageView?.get()?.startAnimation(fadeSecondImageIn)
            }
        })
        comicImageView?.get()?.startAnimation(fadeFirstImageOut)
    }
}