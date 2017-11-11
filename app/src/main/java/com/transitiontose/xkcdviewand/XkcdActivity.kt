package com.transitiontose.xkcdviewand

//import retrofit2.Retrofit;
import android.Manifest
import android.R.color.white
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.view.animation.AnimationUtils
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

class XkcdActivity : Activity() {

    private var relativeLayout: RelativeLayout? = null
    private var getSpecificComicButton: Button? = null
    private var numberTextView: TextView? = null
    private var dateTextView: TextView? = null
    private var titleTextView: TextView? = null
    private var comicImageView: ImageView? = null
    private var comicNumTaker: EditText? = null
    private var progressBar: ProgressBar? = null
    private var shareIcon: ImageView? = null
    private var maximumComicNumber = 1810
    private var counter = 1800
    private var URLtoRequestDataFrom: String? = "https://xkcd.com/info.0.json"
    private var json: JSONObject? = null
    private var isFirstQuery = true
    private var player: MediaPlayer? = null
    private var shouldPlaySound = true

    private val networkInfo: NetworkInfo?
        get() {
            val connMgr = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            return connMgr.activeNetworkInfo
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("XkcdActivity", "onCreate")
        setContentView(R.layout.activity_main)

        relativeLayout = findViewById<View>(R.id.relativeLayout) as RelativeLayout
        relativeLayout!!.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                hideKeyboard()
            }
        }

        getSpecificComicButton = findViewById<View>(R.id.getSpecificComic) as Button
        getSpecificComicButton!!.isEnabled = false
        comicImageView = findViewById<View>(R.id.comicImageView) as ImageView
        comicNumTaker = findViewById<View>(R.id.comicNumTaker) as EditText
        comicNumTaker!!.isEnabled = false
        progressBar = findViewById<View>(R.id.progressBar) as ProgressBar
        progressBar!!.indeterminateDrawable.setColorFilter(resources.getColor(white), android.graphics.PorterDuff.Mode.SRC_IN)
        setEditTextOptions()
        comicNumTaker!!.background.mutate().setColorFilter(resources.getColor(white), PorterDuff.Mode.SRC_ATOP)
        shareIcon = findViewById<View>(R.id.shareIcon) as ImageView
        numberTextView = findViewById<View>(R.id.numberTextView) as TextView
        dateTextView = findViewById<View>(R.id.dateTextView) as TextView
        titleTextView = findViewById<View>(R.id.titleTextView) as TextView
        player = MediaPlayer()
        json = JSONObject()

        val initialURL = "https://xkcd.com/info.0.json"
        val networkInfo = networkInfo

        if (networkInfo != null && networkInfo.isConnected && savedInstanceState != null) {
            maximumComicNumber = savedInstanceState.getInt("oldMaximumComicNumber")
            counter = savedInstanceState.getInt("oldCounter")
            URLtoRequestDataFrom = savedInstanceState.getString("oldURLtoRequestDataFrom")
            isFirstQuery = savedInstanceState.getBoolean("oldisFirstQuery")
            getSpecificComicButton!!.isEnabled = true
            comicNumTaker!!.isEnabled = true
            DownloadWebpageTask().execute(URLtoRequestDataFrom)
        } else if (networkInfo != null && networkInfo.isConnected && savedInstanceState == null) {
            DownloadWebpageTask().execute(initialURL)
        } else if (networkInfo == null) {
            networkToast()
        }
    }

    override fun onDestroy() {
        Log.d("XkcdActivity", "onDestroy")
        super.onDestroy()
    }

    override fun onPause() {
        super.onPause()
        Log.d("XkcdActivity", "onPause")
    }

    override fun onResume() {
        super.onResume()
        Log.d("XkcdActivity", "onResume")
    }

    override fun onRestart() {
        super.onRestart()
        Log.d("XkcdActivity", "onRestart")
    }

    override fun onStart() {
        super.onStart()
        Log.d("XkcdActivity", "onStart")
    }

    override fun onStop() {
        super.onStop()
        Log.d("XkcdActivity", "onStop")
    }

    private fun setEditTextOptions() {
        comicNumTaker!!.imeOptions = EditorInfo.IME_ACTION_DONE // collapse keyboard when done is pressed

        comicNumTaker!!.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                comicNumTaker!!.clearFocus()
                relativeLayout!!.requestFocus()
            }
            false
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("oldMaximumComicNumber", maximumComicNumber)
        outState.putInt("oldCounter", counter)
        outState.putString("oldURLtoRequestDataFrom", URLtoRequestDataFrom)
        outState.putBoolean("oldisFirstQuery", isFirstQuery!!)
    }

    fun audioPressed(v: View) {
        shouldPlaySound = !shouldPlaySound
        Toast.makeText(this, "Audio toggled.", Toast.LENGTH_SHORT).show()
    }

    fun sharePressed(v: View?) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            val drawable = comicImageView!!.drawable
            if (drawable != null) {
                val bitmap = (drawable as BitmapDrawable).bitmap
                val path = MediaStore.Images.Media.insertImage(contentResolver, bitmap, "Image I want to share", null)
                val uri = Uri.parse(path)
                val shareIntent = Intent()
                shareIntent.action = Intent.ACTION_SEND
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
                shareIntent.type = "image/*"
                if (shareIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(Intent.createChooser(shareIntent, "Share Image"))
                }
            }
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 14)
        }
    }

    fun randomComic(v: View) {
        val networkInfo = networkInfo

        if (networkInfo != null && networkInfo.isConnected) {
            // fetch data
            if (shouldPlaySound) {
                playSound()
            }
            counter = randomInteger(1, maximumComicNumber)
            URLtoRequestDataFrom = "https://xkcd.com/$counter/info.0.json"
            if (isFirstQuery!!) {
                URLtoRequestDataFrom = "https://xkcd.com/info.0.json"
            }
            getData()
        } else {
            networkToast()
        }
    }

    private fun randomInteger(min: Int, max: Int): Int {
        return Random().nextInt(max - min + 1) + min
    }

    fun leftPressed(v: View) {
        val networkInfo = networkInfo

        if (networkInfo != null && networkInfo.isConnected) {
            if (counter >= 2 && counter <= maximumComicNumber) {
                counter--
                if (shouldPlaySound) {
                    playSound()
                }
                URLtoRequestDataFrom = "https://xkcd.com/$counter/info.0.json"
                if (isFirstQuery!!) {
                    URLtoRequestDataFrom = "https://xkcd.com/info.0.json"
                }
                getData()
            } else {
                invalidToast()
            }
        } else {
            networkToast()
        }
    }

    fun rightPressed(v: View) {
        val networkInfo = networkInfo

        if (networkInfo != null && networkInfo.isConnected) {
            if (counter >= 1 && counter <= maximumComicNumber - 1) {
                counter++
                if (shouldPlaySound) {
                    playSound()
                }
                URLtoRequestDataFrom = "https://xkcd.com/$counter/info.0.json"
                if (isFirstQuery!!) {
                    URLtoRequestDataFrom = "https://xkcd.com/info.0.json"
                }
                getData()
            } else {
                invalidToast()
            }
        } else {
            networkToast()
        }
    }

    fun savePressed(v: View) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            val drawable = comicImageView!!.drawable
            if (drawable != null) {
                val bitmap = (comicImageView!!.drawable as BitmapDrawable).bitmap
                if (bitmap != null) {
                    saveImage(bitmap)
                    Toast.makeText(this, "Image saved to ~/xkcdview", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Cannot save before image loads.", Toast.LENGTH_SHORT).show()
            }
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 13)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            13 -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    val bitmap = (comicImageView!!.drawable as BitmapDrawable).bitmap
                    saveImage(bitmap)
                    Toast.makeText(this, "Image saved to ~/xkcdview", Toast.LENGTH_SHORT).show()
                } else {
                    // permission denied, boo!
                    Toast.makeText(this, "This permission is required in order to save image to your device.", Toast.LENGTH_SHORT).show()
                }
            }

            14 -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    sharePressed(null)
                } else {
                    // permission denied, boo!
                    Toast.makeText(this, "This permission is required in order to share image.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveImage(finalBitmap: Bitmap) {
        val root = Environment.getExternalStorageDirectory().toString()
        val myDir = File(root + "/xkcdview")
        myDir.mkdirs()
        val fname = "Image-$counter.jpg"
        val file = File(myDir, fname)
        if (file.exists()) {
            file.delete()
        }
        try {
            val out = FileOutputStream(file)
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            out.flush()
            out.close()

        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    fun getSpecificComic(v: View) {
        val comicToGet: Int

        try {
            comicToGet = Integer.parseInt(comicNumTaker!!.text.toString())
        } catch (e: IllegalArgumentException) {
            relativeLayout!!.requestFocus()
            invalidToast()
            return
        }

        val networkInfo = networkInfo

        if (networkInfo != null && networkInfo.isConnected) {
            if (comicToGet >= 1 && comicToGet <= maximumComicNumber) {
                counter = comicToGet
                if (shouldPlaySound) {
                    playSound()
                }
                URLtoRequestDataFrom = "https://xkcd.com/$counter/info.0.json" // update the URL
                if (isFirstQuery!!) {
                    URLtoRequestDataFrom = "https://xkcd.com/info.0.json"
                }
                getData()
            } else {
                invalidToast()
            }
        } else {
            networkToast()
        }

        relativeLayout!!.requestFocus()
    }

    private fun hideKeyboard() {
        val view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    fun getData() {
        DownloadWebpageTask().execute(URLtoRequestDataFrom)
    }

    private fun convertStreamToString(`is`: InputStream?): String {
        val scanner = Scanner(`is`!!, "UTF-8").useDelimiter("\\A")
        return if (scanner.hasNext()) scanner.next() else ""
    }

    private fun getComicImage(jsonArg: JSONObject) {
        var imageURLtoFetch = ""
        try {
            imageURLtoFetch = jsonArg.getString("img")
        } catch (j: JSONException) {
            j.printStackTrace()
        }

        DownloadImageTask(findViewById<View>(R.id.comicImageView) as ImageView).execute(imageURLtoFetch)
    }

    private fun getComicDate(jsonArg: JSONObject) {
        var day = ""
        var month = ""
        var year = ""
        try {
            day = jsonArg.getString("day")
            month = jsonArg.getString("month")
            year = jsonArg.getString("year")
        } catch (j: org.json.JSONException) {
            j.printStackTrace()
        }

        dateTextView!!.text = "$month/$day/$year"
    }

    private fun getComicTitle(jsonArg: JSONObject) {
        var title = ""

        try {
            title = jsonArg.getString("title")
        } catch (j: org.json.JSONException) {
            Log.d("getComicTitle", "Can't parse title.")
        }

        titleTextView!!.text = title
    }

    private fun getComicNumber(jsonArg: JSONObject) {
        var num = -1

        try {
            num = jsonArg.getInt("num")
        } catch (j: org.json.JSONException) {
            Log.d("getComicNumber", "Can't parse number.")
        }

        numberTextView!!.text = "comic #: " + num
    }

    private fun firstQueryWork(jsonArg: JSONObject) {
        var max = -1

        try {
            max = jsonArg.getInt("num")
        } catch (j: org.json.JSONException) {
            Log.d("firstQueryWork", "Can't parse number.")
        }

        maximumComicNumber = max
        counter = maximumComicNumber
    }

    private fun invalidToast() {
        Toast.makeText(this, "Invalid comic number.", Toast.LENGTH_SHORT).show()
    }

    private fun networkToast() {
        Toast.makeText(this, "Network unavailable.", Toast.LENGTH_SHORT).show()
    }

    private fun playSound() {
        if (player!!.isPlaying) {
            player!!.stop()
        }

        try {
            player!!.reset()
            val afd: AssetFileDescriptor
            afd = assets.openFd("sound.wav")
            player!!.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            player!!.prepare()
            player!!.start()
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    // task for downloading json from webpage
    private inner class DownloadWebpageTask : AsyncTask<String, Void, String>() {

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

        override fun onPreExecute() {
            progressBar!!.visibility = View.VISIBLE
        }

        override fun doInBackground(vararg urls: String): String {

            try {
                return downloadUrl(urls[0])
            } catch (e: IOException) {
                return "Unable to retrieve web page."
            }

        }

        override fun onPostExecute(result: String) {
            try {
                json = JSONObject(result)
            } catch (j: org.json.JSONException) {
                Log.d("downloadUrl", "JSONObject creation failed.")
                Toast.makeText(applicationContext, "Could not fetch comic.", Toast.LENGTH_SHORT).show()
                j.printStackTrace()
                return
            }

            if (isFirstQuery!!) {
                val tempjson = json
                if (tempjson != null) {
                    firstQueryWork(tempjson)
                }
                //firstQueryWork(json)
                isFirstQuery = false
                getSpecificComicButton!!.isEnabled = true
                comicNumTaker!!.isEnabled = true
            }

            val tempjson = json
            if (tempjson != null) {
                getComicImage(tempjson)
                getComicNumber(tempjson)
                getComicTitle(tempjson)
                getComicDate(tempjson)
            }
        }
    }

    // task for downloading the comic image
    private inner class DownloadImageTask(internal var bmImage: ImageView) : AsyncTask<String, Int, Bitmap>() {

        override fun onPreExecute() {

        }

        override fun doInBackground(vararg urls: String): Bitmap? {
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

        override fun onPostExecute(result: Bitmap) {
            progressBar!!.visibility = View.GONE
            ImageViewAnimatedChange(applicationContext, bmImage, result)
        }

        fun ImageViewAnimatedChange(c: Context, v: ImageView, new_image: Bitmap) {
            val fadeFirstImageOut = AnimationUtils.loadAnimation(c, android.R.anim.fade_out)
            val fadeSecondImageIn = AnimationUtils.loadAnimation(c, android.R.anim.fade_in)
            fadeFirstImageOut.setAnimationListener(object : AnimationListener {
                override fun onAnimationStart(animation: Animation) {}

                override fun onAnimationRepeat(animation: Animation) {}

                override fun onAnimationEnd(animation: Animation) {
                    v.setImageBitmap(new_image)
                    fadeSecondImageIn.setAnimationListener(object : AnimationListener {
                        override fun onAnimationStart(animation: Animation) {}

                        override fun onAnimationRepeat(animation: Animation) {}

                        override fun onAnimationEnd(animation: Animation) {}
                    })
                    v.startAnimation(fadeSecondImageIn)
                }
            })
            v.startAnimation(fadeFirstImageOut)
        }
    }
}