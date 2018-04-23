package com.transitiontose.xkcdviewand

//import retrofit2.Retrofit;
import android.Manifest
import android.R.color.white
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.AssetFileDescriptor
import android.content.res.Configuration
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
import java.lang.ref.WeakReference
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

class XkcdActivity : Activity() {

    private lateinit var relativeLayout: RelativeLayout
    private lateinit var getSpecificComicButton: Button
    private lateinit var numberTextView: TextView
    private lateinit var dateTextView: TextView
    private lateinit var titleTextView: TextView
    private var comicImageView: ImageView? = null // still needs to be nullable in the case that an AsyncTask tries to update the view during configuration change
    private lateinit var comicNumTaker: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var shareIcon: ImageView
    private var maximumComicNumber = 1810
    private var counter = 1800
    private var urlToRequestDataFrom: String = "https://xkcd.com/info.0.json"
    private lateinit var json: JSONObject
    private var isFirstQuery = true
    private lateinit var player: MediaPlayer
    private var shouldPlaySound = true
    private val networkInfo: NetworkInfo
        get() {
            val connMgr = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            return connMgr.activeNetworkInfo
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme)
        //AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        Log.d("XkcdActivity", "onCreate")
        setContentView(R.layout.activity_main)

        relativeLayout = findViewById<View>(R.id.relativeLayout) as RelativeLayout
        relativeLayout.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                hideKeyboard()
            }
        }

        comicImageView = findViewById(R.id.comicImageView)
        player = MediaPlayer()
        json = JSONObject()

        if (isInPortraitMode()) {
            getSpecificComicButton = findViewById(R.id.getSpecificComic)
            getSpecificComicButton.isEnabled = false
            comicNumTaker = findViewById(R.id.comicNumTaker)
            comicNumTaker.isEnabled = false
            progressBar = findViewById(R.id.progressBar)
            progressBar.indeterminateDrawable?.setColorFilter(resources.getColor(white), android.graphics.PorterDuff.Mode.SRC_IN)
            comicNumTaker.background?.mutate()?.setColorFilter(resources.getColor(white), PorterDuff.Mode.SRC_ATOP)
            setEditTextOptions()
            shareIcon = findViewById(R.id.shareIcon)
            numberTextView = findViewById(R.id.numberTextView)
            dateTextView = findViewById(R.id.dateTextView)
            titleTextView = findViewById(R.id.titleTextView)
        }

        val initialURL = "https://xkcd.com/info.0.json"
        val networkInfo = networkInfo

        if (networkInfo != null && networkInfo.isConnected && savedInstanceState != null) {
            maximumComicNumber = savedInstanceState.getInt("oldMaximumComicNumber")
            counter = savedInstanceState.getInt("oldCounter")
            urlToRequestDataFrom = savedInstanceState.getString("oldURLtoRequestDataFrom")
            isFirstQuery = savedInstanceState.getBoolean("oldisFirstQuery")
            if (isInPortraitMode()) {
                getSpecificComicButton.isEnabled = true
                comicNumTaker.isEnabled = true
            }
            DownloadWebpageTask(WeakReference(this)).execute(urlToRequestDataFrom)
        } else if (networkInfo != null && networkInfo.isConnected && savedInstanceState == null) {
            DownloadWebpageTask(WeakReference(this)).execute(initialURL)
        } else if (networkInfo == null) {
            networkToast()
        }
    }

    fun isInPortraitMode() : Boolean {
        if(resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            return true
        }
        return false
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("XkcdActivity", "onDestroy")
        player.release()
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
        comicNumTaker.imeOptions = EditorInfo.IME_ACTION_DONE // collapse keyboard when done is pressed

        comicNumTaker.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                comicNumTaker.clearFocus()
                relativeLayout.requestFocus()
            }
            false
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("oldMaximumComicNumber", maximumComicNumber)
        outState.putInt("oldCounter", counter)
        outState.putString("oldURLtoRequestDataFrom", urlToRequestDataFrom)
        outState.putBoolean("oldisFirstQuery", isFirstQuery)
    }

    fun audioPressed(v: View) {
        shouldPlaySound = !shouldPlaySound
        Toast.makeText(this, "Audio toggled.", Toast.LENGTH_SHORT).show()
    }

    fun sharePressed(v: View?) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            val drawable = comicImageView?.drawable
            if (drawable != null) {
                val bitmap = (drawable as BitmapDrawable).bitmap
                if (bitmap != null) {
                    val path = MediaStore.Images.Media.insertImage(contentResolver, bitmap, "Image I want to share", null)
                    if (path != null) {
                        val uri = Uri.parse(path)
                        val shareIntent = Intent()
                        shareIntent.action = Intent.ACTION_SEND
                        shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
                        shareIntent.type = "image/*"
                        if (shareIntent.resolveActivity(packageManager) != null) {
                            startActivity(Intent.createChooser(shareIntent, "Share Image"))
                        }
                    }
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
            urlToRequestDataFrom = "https://xkcd.com/$counter/info.0.json"
            if (isFirstQuery) {
                urlToRequestDataFrom = "https://xkcd.com/info.0.json"
            }
            getData()
        } else {
            networkToast()
        }
    }

    fun randomInteger(min: Int, max: Int): Int {
        return Random().nextInt(max - min + 1) + min
    }

    fun leftPressed(v: View) {
        val networkInfo = networkInfo

        if (networkInfo != null && networkInfo.isConnected) {
            if (counter in 2..maximumComicNumber) {
                counter--
                if (shouldPlaySound) {
                    playSound()
                }
                urlToRequestDataFrom = "https://xkcd.com/$counter/info.0.json"
                if (isFirstQuery) {
                    urlToRequestDataFrom = "https://xkcd.com/info.0.json"
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
                urlToRequestDataFrom = "https://xkcd.com/$counter/info.0.json"
                if (isFirstQuery) {
                    urlToRequestDataFrom = "https://xkcd.com/info.0.json"
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
            val drawable = comicImageView?.drawable
            if (drawable != null) {
                val bitmap = (comicImageView?.drawable as BitmapDrawable).bitmap
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
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    val bitmap = (comicImageView?.drawable as BitmapDrawable).bitmap
                    saveImage(bitmap)
                    Toast.makeText(this, "Image saved to ~/xkcdview", Toast.LENGTH_SHORT).show()
                } else {
                    // permission denied, boo!
                    Toast.makeText(this, "This permission is required in order to save image to your device.", Toast.LENGTH_SHORT).show()
                }
            }

            14 -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
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
        val myDir = File("$root/xkcdview")
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
            comicToGet = Integer.parseInt(comicNumTaker.text.toString())
        } catch (e: IllegalArgumentException) {
            relativeLayout.requestFocus()
            invalidToast()
            return
        }

        val networkInfo = networkInfo

        if (networkInfo != null && networkInfo.isConnected) {
            if (comicToGet in 1..maximumComicNumber) {
                counter = comicToGet
                if (shouldPlaySound) {
                    playSound()
                }
                urlToRequestDataFrom = "https://xkcd.com/$counter/info.0.json" // update the URL
                if (isFirstQuery) {
                    urlToRequestDataFrom = "https://xkcd.com/info.0.json"
                }
                getData()
            } else {
                invalidToast()
            }
        } else {
            networkToast()
        }

        relativeLayout.requestFocus()
    }

    private fun hideKeyboard() {
        val view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    private fun getData() {
        DownloadWebpageTask(WeakReference(this)).execute(urlToRequestDataFrom)
    }

    private fun getComicImage(jsonArg: JSONObject) {
        var imageURLtoFetch = ""
        try {
            imageURLtoFetch = jsonArg.getString("img")
        } catch (j: JSONException) {
            j.printStackTrace()
        }

        DownloadImageTask(WeakReference(findViewById<View>(R.id.comicImageView) as ImageView), WeakReference(this)).execute(imageURLtoFetch)
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

        dateTextView.text = "$month/$day/$year"
    }

    private fun getComicTitle(jsonArg: JSONObject) {
        var title = ""

        try {
            title = jsonArg.getString("title")
        } catch (j: org.json.JSONException) {
            Log.d("getComicTitle", "Can't parse title.")
        }

        titleTextView.text = title
    }

    private fun getComicNumber(jsonArg: JSONObject) {
        var num = -1

        try {
            num = jsonArg.getInt("num")
        } catch (j: org.json.JSONException) {
            Log.d("getComicNumber", "Can't parse number.")
        }

        numberTextView.text = "comic #: $num"
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
        if (player.isPlaying) {
            player.stop()
        }

        try {
            player.reset()
            val afd: AssetFileDescriptor = assets.openFd("sound.wav")
            player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            player.prepare()
            player.start()
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    // task for downloading json from webpage
    private class DownloadWebpageTask(private val xkcdActivity: WeakReference<XkcdActivity>?) : AsyncTask<String, Void, String>() {

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

        private fun convertStreamToString(`is`: InputStream?): String {
            val scanner = Scanner(`is`!!, "UTF-8").useDelimiter("\\A")
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
            try {
                xkcdActivity?.get()?.json = JSONObject(result)
            } catch (j: org.json.JSONException) {
                Log.d("downloadUrl", "JSONObject creation failed.")
                Toast.makeText(xkcdActivity?.get()?.applicationContext, "Could not fetch comic.", Toast.LENGTH_SHORT).show()
                j.printStackTrace()
                return
            }

            if (xkcdActivity?.get()?.isFirstQuery == true) {
                val tempjson = xkcdActivity.get()?.json
                if (tempjson != null) {
                    xkcdActivity.get()?.firstQueryWork(tempjson)
                }
                //firstQueryWork(json)
                xkcdActivity.get()?.isFirstQuery = false
                if (xkcdActivity.get()?.isInPortraitMode() == true) {
                    xkcdActivity.get()?.getSpecificComicButton?.isEnabled = true
                    xkcdActivity.get()?.comicNumTaker?.isEnabled = true
                }
            }

            val tempjson = xkcdActivity?.get()?.json
            if (tempjson != null) {
                xkcdActivity?.get()?.getComicImage(tempjson)
                if (xkcdActivity?.get()?.isInPortraitMode() == true) {
                    xkcdActivity.get()?.getComicNumber(tempjson)
                    xkcdActivity.get()?.getComicTitle(tempjson)
                    xkcdActivity.get()?.getComicDate(tempjson)
                }
            }
        }
    }

    // task for downloading the comic image
    private class DownloadImageTask(private val bmImage: WeakReference<ImageView>?, private val xkcdActivity : WeakReference<XkcdActivity>?) : AsyncTask<String?, Int?, Bitmap?>() {

        override fun onPreExecute() {

        }

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
            fadeFirstImageOut.setAnimationListener(object : AnimationListener {
                override fun onAnimationStart(animation: Animation) {}
                override fun onAnimationRepeat(animation: Animation) {}
                override fun onAnimationEnd(animation: Animation) {
                    comicImageView?.get()?.setImageBitmap(newImage)
                    fadeSecondImageIn.setAnimationListener(object : AnimationListener {
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
}