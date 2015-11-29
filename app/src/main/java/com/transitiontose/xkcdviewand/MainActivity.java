package com.transitiontose.xkcdviewand;

import android.app.*;
import android.os.*;
import java.io.*;
import java.net.*;
import android.widget.*;
import android.content.*;
import android.view.View.*;
import android.view.*;
import android.net.*;
import org.json.*;
import java.util.*;
import android.util.Log;
import android.graphics.*;
import android.provider.*;
import android.view.animation.*;
import android.view.animation.Animation.*;
import android.media.MediaPlayer;;
import android.content.res.AssetFileDescriptor;
import android.graphics.drawable.BitmapDrawable;

// Terry Schmidt, CSC472, Final Project, Fall 2015

public class MainActivity extends Activity {

    ButtonClickListener btnClick;
    TextView saveToPhotos;
    TextView numberTextView;
    TextView dateTextView;
    TextView titleTextView;
    ImageView leftArrow;
    ImageView rightArrow;
    ImageView audioIcon;
    public ImageView comicImageView;
    Button randomComicButton;
    Button getSpecificComic;
    EditText comicNumTaker;
    int maximumComicNumber = 1600;
    int counter = 2;
    String URLtoRequestDataFrom = "http://xkcd.com/info.0.json";
    JSONObject json;
    Boolean isFirstQuery = true;
    MediaPlayer player;
    boolean shouldPlaySound = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnClick = new ButtonClickListener();
        leftArrow = (ImageView) findViewById(R.id.leftArrow);
        rightArrow = (ImageView) findViewById(R.id.rightArrow);
        randomComicButton = (Button) findViewById(R.id.randomComicButton);
        saveToPhotos = (TextView) findViewById(R.id.saveToPhotos);
        getSpecificComic = (Button) findViewById(R.id.getSpecificComic);
        comicImageView = (ImageView) findViewById(R.id.comicImageView);
        comicNumTaker = (EditText) findViewById(R.id.comicNumTaker);
        numberTextView = (TextView) findViewById(R.id.numberTextView);
        dateTextView = (TextView) findViewById(R.id.dateTextView);
        titleTextView = (TextView) findViewById(R.id.titleTextView);
        audioIcon = (ImageView) findViewById(R.id.audio);
        player = new MediaPlayer();

        int[] idList = {R.id.leftArrow, R.id.rightArrow, R.id.randomComicButton, R.id.saveToPhotos,
                        R.id.getSpecificComic, R.id.audio};

        for (int id: idList) {
            View v = (View) findViewById(id);
            v.setOnClickListener(btnClick);
        }

        String initialURL = "http://xkcd.com/info.0.json";
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        if (networkInfo != null && networkInfo.isConnected() && savedInstanceState != null) {
            maximumComicNumber = savedInstanceState.getInt("oldMaximumComicNumber");
            counter = savedInstanceState.getInt("oldCounter");
            URLtoRequestDataFrom = savedInstanceState.getString("oldURLtoRequestDataFrom");
            isFirstQuery = savedInstanceState.getBoolean("oldisFirstQuery");
            new DownloadWebpageTask().execute(URLtoRequestDataFrom);
        } else if (networkInfo != null && networkInfo.isConnected() && savedInstanceState == null) {
            new DownloadWebpageTask().execute(initialURL);
        } else if (networkInfo == null) {
            System.exit(0);
        }
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

    private class ButtonClickListener implements OnClickListener {
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.leftArrow: leftPressed(); break;
                case R.id.rightArrow: rightPressed(); break;
                case R.id.randomComicButton: randomComic(); break;
                case R.id.saveToPhotos: savePressed(); break;
                case R.id.getSpecificComic: getSpecificComic(); break;
                case R.id.audio: audioPressed(); break;
            }
        }
    }

    void audioPressed() {
        shouldPlaySound = !shouldPlaySound;
        Toast.makeText(this, "Audio toggled.", Toast.LENGTH_SHORT).show();
    }

    void randomComic() {
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            // fetch data
            if (shouldPlaySound == true) {
                playSound();
            }
            System.out.println("Random pressed");
            int random = randomInteger(1, maximumComicNumber);
            counter = random;
            System.out.println(counter);
            URLtoRequestDataFrom = "http://xkcd.com/" + counter + "/info.0.json";
            getData();
        } else {
           networkToast();
        }
    }

    public static int randomInteger(int min, int max) {
        Random rng = new Random();
        int randomNum = rng.nextInt((max - min) + 1) + min;
        return randomNum;
    }

    void leftPressed() {
        System.out.println("Left pressed");
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        if (networkInfo != null && networkInfo.isConnected()) {
            if(counter >= 2 && counter <= maximumComicNumber) {
                counter--;
                if (shouldPlaySound == true) {
                    playSound();
                }
                System.out.println(counter);
                URLtoRequestDataFrom = "http://xkcd.com/" + counter + "/info.0.json";
                getData();
            } else {
                invalidToast();
            }
        } else {
            networkToast();
        }
    }

    void rightPressed() {
        System.out.println("Right pressed");

        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        if (networkInfo != null && networkInfo.isConnected()) {
            if(counter >= 1 && counter <= maximumComicNumber - 1) {
                counter++;
                if (shouldPlaySound == true) {
                    playSound();
                }
                System.out.println(counter);
                URLtoRequestDataFrom = "http://xkcd.com/" + counter + "/info.0.json";
                getData();
            } else {
                invalidToast();
            }
        } else {
            networkToast();
        }
    }

    void savePressed() {
        Bitmap bitmap = ((BitmapDrawable)comicImageView.getDrawable()).getBitmap();
        MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "2015" , "2015");
        Toast.makeText(this, "Image saved.", Toast.LENGTH_SHORT).show();
    }

    void getSpecificComic() {
        System.out.println("Get specific comic pressed");
        int comicToGet = -1;

        try {
            comicToGet = Integer.parseInt(comicNumTaker.getText().toString());
        } catch (IllegalArgumentException e) {
            invalidToast();
            return;
        }

        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        if (networkInfo != null && networkInfo.isConnected()) {
            if (comicToGet >= 1 && comicToGet <= maximumComicNumber) {
                counter = comicToGet;
                if (shouldPlaySound == true) {
                    playSound();
                }
                System.out.println(counter);
                URLtoRequestDataFrom = "http://xkcd.com/" + counter + "/info.0.json"; // update the URL
                getData();
            } else {
                invalidToast();
            }
        } else {
            networkToast();
        }
    }

    void getData() {
        new DownloadWebpageTask().execute(URLtoRequestDataFrom);
    }

    private String downloadUrl(String myurl) throws IOException {
        InputStream is = null;

        try {
            URL url = new URL(myurl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000 /* milliseconds */);
            conn.setConnectTimeout(15000 /* milliseconds */);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            // Starts the query
            System.out.println(URLtoRequestDataFrom);
            conn.connect();
            //int response = conn.getResponseCode();
            //System.out.println("The response is: " + response);
            is = conn.getInputStream();

            // Convert the InputStream into a string

            String contentAsString = convertStreamToString(is);

            System.out.println(contentAsString);

            try {
                json = new JSONObject(contentAsString);
            } catch (org.json.JSONException j) {
                System.out.println("JSONObject creation failed.");
                j.printStackTrace();
            }

            return contentAsString;
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    String convertStreamToString(InputStream is) {
        Scanner scanner = new Scanner(is, "UTF-8").useDelimiter("\\A");
        return scanner.hasNext() ? scanner.next() : "";
    }

    void getComicImage(JSONObject jsonArg) {
        String imageURLtoFetch = "";
        try {
            imageURLtoFetch = jsonArg.getString("img");
        } catch (JSONException j) {
            j.printStackTrace();
        }

        new DownloadImageTask((ImageView) findViewById(R.id.comicImageView)).execute(imageURLtoFetch);
    }

    void getComicDate(JSONObject jsonArg) {
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

    void getComicTitle(JSONObject jsonArg) {
        String title = "";

        try {
            title = jsonArg.getString("title");
        } catch (org.json.JSONException j) {

        }

        titleTextView.setText(title);
    }

    void getComicNumber(JSONObject jsonArg) {
        int num = -1;

        try {
            num = jsonArg.getInt("num");
        } catch (org.json.JSONException j) {

        }

        numberTextView.setText("comic #: " + num);
    }

    void firstQueryWork(JSONObject jsonArg) {
        int max = -1;

        try {
            max = jsonArg.getInt("num");
        } catch (org.json.JSONException j) {

        }

        maximumComicNumber = max;
        counter = maximumComicNumber;
    }

    void invalidToast() {
        Toast.makeText(this, "Invalid comic number.", Toast.LENGTH_SHORT).show();
    }

    void networkToast() {
        Toast.makeText(this, "Network unavailable.", Toast.LENGTH_SHORT).show();
    }

    void playSound() {
        if(player.isPlaying()) {
            player.stop();
        }

        try {
            player.reset();
            AssetFileDescriptor afd;
            afd = getAssets().openFd("button-31.wav");
            player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            player.prepare();
            player.start();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class DownloadWebpageTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {

            // params comes from the execute() call: params[0] is the url.
            try {
                return downloadUrl(urls[0]);
            } catch (IOException e) {
                return "Unable to retrieve web page.";
            }
        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            //textView.setText(result);
            if (isFirstQuery == true) {
                firstQueryWork(json);
                isFirstQuery = false;
            }

            getComicNumber(json);
            getComicTitle(json);
            getComicImage(json);
            getComicDate(json);
        }
    }

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        public DownloadImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }

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

        protected void onPostExecute(Bitmap result) {
            //bmImage.setImageBitmap(result);
            Context c = getApplicationContext();
            ImageViewAnimatedChange(c, bmImage, result);
        }
    }

    public static void ImageViewAnimatedChange(Context c, final ImageView v, final Bitmap new_image) {
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