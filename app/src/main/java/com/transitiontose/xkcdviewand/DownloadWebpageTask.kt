package com.transitiontose.xkcdviewand

import android.os.AsyncTask
import android.view.View
import java.io.IOException
import java.io.InputStream
import java.lang.ref.WeakReference
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

internal class DownloadWebpageTask(private val xkcdActivity: WeakReference<XkcdActivity>?) : AsyncTask<String, Void, String>() {
    @Throws(IOException::class)
    private fun downloadUrl(myurl: String): String {
        var stream: InputStream? = null
        try {
            val url = URL(myurl)
            val conn = url.openConnection() as HttpURLConnection
            conn.readTimeout = 10000
            conn.connectTimeout = 15000
            conn.requestMethod = "GET"
            conn.doInput = true
            conn.connect()
            //int response = conn.getResponseCode();
            stream = conn.inputStream
            return convertStreamToString(stream)
        } finally {
            if (stream != null) {
                stream.close()
            }
        }
    }

    private fun convertStreamToString(inputStream: InputStream?): String {
        val scanner = Scanner(inputStream!!, "UTF-8").useDelimiter("\\A")
        return if (scanner.hasNext()) scanner.next() else ""
    }

    override fun onPreExecute() {
        if (xkcdActivity?.get()?.isInPortraitMode() == true) {
            xkcdActivity.get()?.progressBar?.visibility = View.VISIBLE
        }
    }

    override fun doInBackground(vararg urls: String): String {
        return try {
            downloadUrl(urls[0])
        } catch (e: IOException) {
            "Unable to retrieve web page."
        }
    }

    override fun onPostExecute(result: String) {
        xkcdActivity?.get()?.downloadWebpageFinished(result)
    }
}