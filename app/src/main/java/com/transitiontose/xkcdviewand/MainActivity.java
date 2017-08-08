package com.transitiontose.xkcdviewand;

import android.Manifest;
import android.app.*;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.*;
import java.io.*;
import java.net.*;

import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import android.content.*;
import android.view.*;
import android.net.*;
import org.json.*;
import java.util.*;
import android.util.Log;
import android.graphics.*;
import android.view.animation.*;
import android.view.animation.Animation.*;
import android.media.MediaPlayer;
import android.content.res.AssetFileDescriptor;
import android.graphics.drawable.BitmapDrawable;

import static android.R.color.white;

public class MainActivity extends Activity {

    private RelativeLayout relativeLayout;
    private Button getSpecificComicButton;
    private TextView numberTextView;
    private TextView dateTextView;
    private TextView titleTextView;
    private ImageView comicImageView;
    private EditText comicNumTaker;
    private ProgressBar progressBar;
    private ImageView shareIcon;
    private int maximumComicNumber = 1810;
    private int counter = 1800;
    private String URLtoRequestDataFrom = "https://xkcd.com/info.0.json";
    private JSONObject json;
    private Boolean isFirstQuery = true;
    private MediaPlayer player;
    private boolean shouldPlaySound = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        relativeLayout = (RelativeLayout) findViewById(R.id.relativeLayout);
        relativeLayout.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    hideKeyboard();
                }
            }
        });

        getSpecificComicButton = (Button) findViewById(R.id.getSpecificComic);
        getSpecificComicButton.setEnabled(false);
        comicImageView = (ImageView) findViewById(R.id.comicImageView);
        comicNumTaker = (EditText) findViewById(R.id.comicNumTaker);
        comicNumTaker.setEnabled(false);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.getIndeterminateDrawable().setColorFilter(getResources().getColor(white), android.graphics.PorterDuff.Mode.SRC_IN);
        setEditTextOptions();
        comicNumTaker.getBackground().mutate().setColorFilter(getResources().getColor(white), PorterDuff.Mode.SRC_ATOP);
        shareIcon = (ImageView) findViewById(R.id.shareIcon);
        numberTextView = (TextView) findViewById(R.id.numberTextView);
        dateTextView = (TextView) findViewById(R.id.dateTextView);
        titleTextView = (TextView) findViewById(R.id.titleTextView);
        player = new MediaPlayer();
        json = new JSONObject();

        String initialURL = "https://xkcd.com/info.0.json";
        NetworkInfo networkInfo = getNetworkInfo();

        if (networkInfo != null && networkInfo.isConnected() && savedInstanceState != null) {
            maximumComicNumber = savedInstanceState.getInt("oldMaximumComicNumber");
            counter = savedInstanceState.getInt("oldCounter");
            URLtoRequestDataFrom = savedInstanceState.getString("oldURLtoRequestDataFrom");
            isFirstQuery = savedInstanceState.getBoolean("oldisFirstQuery");
            getSpecificComicButton.setEnabled(true);
            comicNumTaker.setEnabled(true);
            new DownloadWebpageTask().execute(URLtoRequestDataFrom);
        } else if (networkInfo != null && networkInfo.isConnected() && savedInstanceState == null) {
            new DownloadWebpageTask().execute(initialURL);
        } else if (networkInfo == null) {
            networkToast();
        }
    }

    private void setEditTextOptions() {
        comicNumTaker.setImeOptions(EditorInfo.IME_ACTION_DONE); // collapse keyboard when done is pressed

        comicNumTaker.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(actionId==EditorInfo.IME_ACTION_DONE){
                    comicNumTaker.clearFocus();
                    relativeLayout.requestFocus();
                }
                return false;
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState (Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("oldMaximumComicNumber", maximumComicNumber);
        outState.putInt("oldCounter", counter);
        outState.putString("oldURLtoRequestDataFrom", URLtoRequestDataFrom);
        outState.putBoolean("oldisFirstQuery", isFirstQuery);
    }

    public void audioPressed(View v) {
        shouldPlaySound = !shouldPlaySound;
        Toast.makeText(this, "Audio toggled.", Toast.LENGTH_SHORT).show();
    }

    public void sharePressed(View v) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            Drawable drawable = comicImageView.getDrawable();
            if (drawable != null) {
                Bitmap bitmap = ((BitmapDrawable)drawable).getBitmap();
                String path = MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "Image I want to share", null);
                Uri uri = Uri.parse(path);
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                shareIntent.setType("image/*");
                startActivity(Intent.createChooser(shareIntent, "Share Image"));
            }
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 14);
        }
    }

    public void randomComic(View v) {
        NetworkInfo networkInfo = getNetworkInfo();

        if (networkInfo != null && networkInfo.isConnected()) {
            // fetch data
            if (shouldPlaySound) {
                playSound();
            }
            counter = randomInteger(1, maximumComicNumber);
            URLtoRequestDataFrom = "https://xkcd.com/" + counter + "/info.0.json";
            if (isFirstQuery) {
                URLtoRequestDataFrom = "https://xkcd.com/info.0.json";
            }
            getData();
        } else {
           networkToast();
        }
    }

    private static int randomInteger(int min, int max) {
        return new Random().nextInt((max - min) + 1) + min;
    }

    private NetworkInfo getNetworkInfo() {
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return connMgr.getActiveNetworkInfo();
    }

    public void leftPressed(View v) {
        NetworkInfo networkInfo = getNetworkInfo();

        if (networkInfo != null && networkInfo.isConnected()) {
            if(counter >= 2 && counter <= maximumComicNumber) {
                counter--;
                if (shouldPlaySound) {
                    playSound();
                }
                URLtoRequestDataFrom = "https://xkcd.com/" + counter + "/info.0.json";
                if (isFirstQuery) {
                    URLtoRequestDataFrom = "https://xkcd.com/info.0.json";
                }
                getData();
            } else {
                invalidToast();
            }
        } else {
            networkToast();
        }
    }

    public void rightPressed(View v) {
        NetworkInfo networkInfo = getNetworkInfo();

        if (networkInfo != null && networkInfo.isConnected()) {
            if(counter >= 1 && counter <= maximumComicNumber - 1) {
                counter++;
                if (shouldPlaySound) {
                    playSound();
                }
                URLtoRequestDataFrom = "https://xkcd.com/" + counter + "/info.0.json";
                if (isFirstQuery) {
                    URLtoRequestDataFrom = "https://xkcd.com/info.0.json";
                }
                getData();
            } else {
                invalidToast();
            }
        } else {
            networkToast();
        }
    }

    public void savePressed(View v) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            Bitmap bitmap = ((BitmapDrawable)comicImageView.getDrawable()).getBitmap();
            saveImage(bitmap);
            Toast.makeText(this, "Image saved to ~/xkcdview", Toast.LENGTH_SHORT).show();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 13);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 13: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Bitmap bitmap = ((BitmapDrawable)comicImageView.getDrawable()).getBitmap();
                    saveImage(bitmap);
                    Toast.makeText(this, "Image saved to ~/xkcdview", Toast.LENGTH_SHORT).show();
                } else {
                    // permission denied, boo!
                    Toast.makeText(this, "This permission is required in order to save image to your device.", Toast.LENGTH_SHORT).show();
                }
                break;
            }

            case 14: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    sharePressed(null);
                } else {
                    // permission denied, boo!
                    Toast.makeText(this, "This permission is required in order to share image.", Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }
    }

    private void saveImage(Bitmap finalBitmap) {
        String root = Environment.getExternalStorageDirectory().toString();
        File myDir = new File(root + "/xkcdview");
        myDir.mkdirs();
        String fname = "Image-"+ counter +".jpg";
        File file = new File(myDir, fname);
        if (file.exists()) {
            file.delete();
        }
        try {
            FileOutputStream out = new FileOutputStream(file);
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void getSpecificComic(View v) {
        int comicToGet;

        try {
            comicToGet = Integer.parseInt(comicNumTaker.getText().toString());
        } catch (IllegalArgumentException e) {
            relativeLayout.requestFocus();
            invalidToast();
            return;
        }

        NetworkInfo networkInfo = getNetworkInfo();

        if (networkInfo != null && networkInfo.isConnected()) {
            if (comicToGet >= 1 && comicToGet <= maximumComicNumber) {
                counter = comicToGet;
                if (shouldPlaySound) {
                    playSound();
                }
                URLtoRequestDataFrom = "https://xkcd.com/" + counter + "/info.0.json"; // update the URL
                if (isFirstQuery) {
                    URLtoRequestDataFrom = "https://xkcd.com/info.0.json";
                }
                getData();
            } else {
                invalidToast();
            }
        } else {
            networkToast();
        }

        relativeLayout.requestFocus();
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public void getData() {
        new DownloadWebpageTask().execute(URLtoRequestDataFrom);
    }

    private String convertStreamToString(InputStream is) {
        Scanner scanner = new Scanner(is, "UTF-8").useDelimiter("\\A");
        return scanner.hasNext() ? scanner.next() : "";
    }

    private void getComicImage(JSONObject jsonArg) {
        String imageURLtoFetch = "";
        try {
            imageURLtoFetch = jsonArg.getString("img");
        } catch (JSONException j) {
            j.printStackTrace();
        }

        new DownloadImageTask((ImageView) findViewById(R.id.comicImageView)).execute(imageURLtoFetch);
    }

    private void getComicDate(JSONObject jsonArg) {
        String day =""; String month = ""; String year ="";
        try {
            day = jsonArg.getString("day");
            month = jsonArg.getString("month");
            year = jsonArg.getString("year");
        } catch (org.json.JSONException j) {
            j.printStackTrace();
        }

        dateTextView.setText(month + "/" + day + "/" + year);
    }

    private void getComicTitle(JSONObject jsonArg) {
        String title = "";

        try {
            title = jsonArg.getString("title");
        } catch (org.json.JSONException j) {
            Log.d("getComicTitle", "Can't parse title.");
        }

        titleTextView.setText(title);
    }

    private void getComicNumber(JSONObject jsonArg) {
        int num = -1;

        try {
            num = jsonArg.getInt("num");
        } catch (org.json.JSONException j) {
            Log.d("getComicNumber", "Can't parse number.");
        }

        numberTextView.setText("comic #: " + num);
    }

    private void firstQueryWork(JSONObject jsonArg) {
        int max = -1;

        try {
            max = jsonArg.getInt("num");
        } catch (org.json.JSONException j) {
            Log.d("firstQueryWork", "Can't parse number.");
        }

        maximumComicNumber = max;
        counter = maximumComicNumber;
    }

    private void invalidToast() {
        Toast.makeText(this, "Invalid comic number.", Toast.LENGTH_SHORT).show();
    }

    private void networkToast() {
        Toast.makeText(this, "Network unavailable.", Toast.LENGTH_SHORT).show();
    }

    private void playSound() {
        if(player.isPlaying()) {
            player.stop();
        }

        try {
            player.reset();
            AssetFileDescriptor afd;
            afd = getAssets().openFd("sound.wav");
            player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            player.prepare();
            player.start();
        } catch (IllegalStateException|IOException e) {
            e.printStackTrace();
        }
    }

    // task for downloading json from webpage
    private class DownloadWebpageTask extends AsyncTask<String, Void, String> {

        private String downloadUrl(String myurl) throws IOException {
            InputStream stream = null;

            try {
                URL url = new URL(myurl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(10000 /* milliseconds */);
                conn.setConnectTimeout(15000 /* milliseconds */);
                conn.setRequestMethod("GET");
                conn.setDoInput(true);
                conn.connect();
                //int response = conn.getResponseCode();
                stream = conn.getInputStream();
                return convertStreamToString(stream);
            } finally {
                if (stream != null) {
                    stream.close();
                }
            }
        }

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(String... urls) {

            try {
                return downloadUrl(urls[0]);
            } catch (IOException e) {
                return "Unable to retrieve web page.";
            }
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                json = new JSONObject(result);
            } catch (org.json.JSONException j) {
                Log.d("downloadUrl", "JSONObject creation failed.");
                Toast.makeText(getApplicationContext(), "Could not fetch comic.", Toast.LENGTH_SHORT).show();
                j.printStackTrace();
                return;
            }

            if (isFirstQuery) {
                firstQueryWork(json);
                isFirstQuery = false;
                getSpecificComicButton.setEnabled(true);
                comicNumTaker.setEnabled(true);
            }

            getComicImage(json);
            getComicNumber(json);
            getComicTitle(json);
            getComicDate(json);
        }
    }

    // task for downloading the comic image
    private class DownloadImageTask extends AsyncTask<String, Integer, Bitmap> {
        ImageView bmImage;

        protected DownloadImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap image = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                image = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return image;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            progressBar.setVisibility(View.GONE);
            ImageViewAnimatedChange(getApplicationContext(), bmImage, result);
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            progressBar.setProgress(values[0]);
        }

        public void ImageViewAnimatedChange(Context c, final ImageView v, final Bitmap new_image) {
            final Animation fadeFirstImageOut = AnimationUtils.loadAnimation(c, android.R.anim.fade_out);
            final Animation fadeSecondImageIn = AnimationUtils.loadAnimation(c, android.R.anim.fade_in);
            fadeFirstImageOut.setAnimationListener(new AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    v.setImageBitmap(new_image);
                    fadeSecondImageIn.setAnimationListener(new AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {
                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                        }
                    });
                    v.startAnimation(fadeSecondImageIn);
                }
            });
            v.startAnimation(fadeFirstImageOut);
        }
    }
}