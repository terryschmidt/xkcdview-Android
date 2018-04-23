package com.transitiontose.xkcdviewand

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.util.Log
import java.lang.ref.WeakReference

internal class DownloadImageTask(private val xkcdActivity : WeakReference<XkcdActivity>?) : AsyncTask<String?, Int?, Bitmap?>() {
    override fun onPreExecute() {}
    override fun doInBackground(vararg urls: String?): Bitmap? {
        val urldisplay = urls[0]
        var image: Bitmap? = null
        try {
            val stream = java.net.URL(urldisplay).openStream()
            image = BitmapFactory.decodeStream(stream)
        } catch (e: Exception) {
            Log.e("Error", e.message)
            e.printStackTrace()
        }
        return image
    }
    override fun onPostExecute(result: Bitmap?) {
        xkcdActivity?.get()?.downloadImageFinished(result)
    }
}