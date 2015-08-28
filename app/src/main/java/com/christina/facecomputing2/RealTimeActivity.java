package com.christina.facecomputing2;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;


import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;


public class RealTimeActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener{

    public CascadeClassifier cascadeClassifier;
    public CascadeClassifier eyeClassifier;
    private JavaCameraView openCVCamera;
    public Mat mRgba;
    public Mat mGray;
    private Size sSize;
    private MatOfRect faces;
    private Scalar colorBlue;
    private Rect rect;
    public Mat mFace;
    public ImageView photoEmoticonView;
    public Boolean faceDetected = false;
    public String successResponse = "works";
    public File pictureFile;
    private static final int MEDIA_TYPE_IMAGE = 1;
    public String happyResponse, sadResponse, surpriseResponse, fearResponse, neutralResponse,
            disgustResponse, angryResponse;
    public String mainEmotion;
    public String emotions[] = new String[7];
    public Double values[] = new Double[7];
    public TextView photoTextResult;

    private MediaPlayer myDisgustPlayer;
    private MediaPlayer myHappyPlayer;
    private MediaPlayer mySadPlayer;
    private MediaPlayer mySurprisedPlayer;
    private MediaPlayer myNeutralPlayer;
    private MediaPlayer myAngryPlayer;
    private MediaPlayer myFearPlayer;
    private MediaPlayer myFacePlayer;

    public ToggleButton toggle;
    public Button resultButton;

    public Boolean realtime = false;
    int iMaxFaceHeight, iMaxFaceHeightIndex;

    public Bitmap bitmap;
    public Drawable temp;

    private Timer timer;
    private TimerTask timerTask;

    public Vibrator mVibrator;
    //public Bitmap bitmap;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    initializeOpenCVDependencies();
                    openCVCamera.enableView();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    private void initializeOpenCVDependencies() {


        try {
            // Copy the resource into a temp file so OpenCV can load it
            InputStream is = getResources().openRawResource(R.raw.haarcascade_frontalface_default);
            File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
            File mCascadeFile = new File(cascadeDir, /*"lbpcascade_frontalface.xml"*/ "haarcascade_frontalface_default.xml");
            FileOutputStream os = new FileOutputStream(mCascadeFile);


            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();

            // Load the cascade classifier
            cascadeClassifier = new CascadeClassifier(mCascadeFile.getAbsolutePath());
        } catch (Exception e) {
            Log.e("Debug", "Error loading cascade", e);
        }




    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_realtime);

        if (!OpenCVLoader.initDebug()) {
            Log.e("Debug", "error with initialization");
        }

        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        openCVCamera = (JavaCameraView) findViewById(R.id.cameraView);
        openCVCamera.setVisibility(SurfaceView.VISIBLE);
        openCVCamera.setCvCameraViewListener(this);
        openCVCamera.setMinimumWidth(640);
        openCVCamera.setMinimumHeight(480);

        photoEmoticonView = (ImageView) findViewById(R.id.bitmap);
        photoTextResult = (TextView) findViewById(R.id.textview);

        myDisgustPlayer = MediaPlayer.create(this,R.raw.explosion);
        myHappyPlayer = MediaPlayer.create(this,R.raw.bell);
        mySadPlayer = MediaPlayer.create(this,R.raw.down_well);
        mySurprisedPlayer = MediaPlayer.create(this,R.raw.comic);
        myAngryPlayer = MediaPlayer.create(this, R.raw.scream);
        myFearPlayer =  MediaPlayer.create(this, R.raw.heartbeat);
        myNeutralPlayer =  MediaPlayer.create(this, R.raw.river3);
        myFacePlayer = MediaPlayer.create(this, R.raw.facedetected);

        resultButton = (Button) findViewById(R.id.resultbutton);
        resultButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showResult();
            }
        });

        toggle = (ToggleButton) findViewById(R.id.togglebutton);
        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    photoTextResult.setText("");
                    Drawable temp = getResources().getDrawable(R.drawable.whiteimage);
                    photoEmoticonView.setImageDrawable(temp);
                    realtime = true;
                    resultButton.setVisibility(View.INVISIBLE);
                } else {
                    realtime = false;
                    resultButton.setVisibility(View.VISIBLE);
                    photoTextResult.setText("");
                    Drawable temp = getResources().getDrawable(R.drawable.whiteimage);
                    photoEmoticonView.setImageDrawable(temp);
                }
            }
        });


        initializeTimer();

    }

    private void initializeTimer(){
        timer = new Timer();
        timerTask = new TimerTask(){

            @Override
            public void run() {
                if(faceDetected  && mFace != null ) {

                   // if(eyesDetected && mEyes != null)
                        mVibrator.vibrate(100);
                        new StartSending().execute();
                }

                else{
                    new NoFace().execute();
                }

            }
        };

        timer.schedule(timerTask, 1000, 1000);
    }

    @Override
    public void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this, mLoaderCallback);
    }


    @Override
    public void onCameraViewStarted(int width, int height) {
        Log.e("Debug","show height: " + height);
        mRgba = new Mat(/*height, width,*/ 480, 640,CvType.CV_8UC4);
        mGray = new Mat(/*height, width,*/ 480, 640, CvType.CV_8UC4);
        colorBlue = new Scalar(247,12,189,255);
        sSize = new Size();
        faces = new MatOfRect();
        rect = new Rect();
        mFace = new Mat();


    }

    public void releaseMats(){
        mRgba.release();
        mGray.release();
        mFace.release();

    }

    @Override
    public void onCameraViewStopped() {
        releaseMats();

        if(openCVCamera != null)
            openCVCamera.disableView();

    }

    public void onDestroy() {
        super.onDestroy();
        if(openCVCamera != null)
            openCVCamera.disableView();
    }

    @Override
    public Mat onCameraFrame(Mat inputFrame) {
        inputFrame.copyTo(mRgba);

        Imgproc.cvtColor(mRgba, mGray, Imgproc.COLOR_RGBA2GRAY);

        iMaxFaceHeight = 0;
        iMaxFaceHeightIndex = -1;
        //int i=0;

        if (cascadeClassifier != null) {
            int height = mGray.rows();
            double faceSize = (double) height * 0.25;

            Log.e("Debug", "Height: " + faceSize);

            sSize.width = faceSize;
            sSize.height = faceSize;

            cascadeClassifier.detectMultiScale(mGray, faces, 1.1, 4, 2, sSize, new Size());


            Rect[] facesArray = faces.toArray();
            Log.e("Debug", "FacesArray" + facesArray.length);

            for (int i = 0; i < facesArray.length; i++) {

                // draw the rectangle itself
                Imgproc.rectangle(mRgba, facesArray[i].tl(), facesArray[i].br(), colorBlue, 3);
                //Core.rectangle(mRgba, facesArray[i].tl(), facesArray[i].br(), colorRed, 3);

                if (iMaxFaceHeight < facesArray[i].height) {
                    iMaxFaceHeight = facesArray[i].height;
                    iMaxFaceHeightIndex = i;
                }

            }



            if(facesArray.length == 0)
                faceDetected = false;


            if (iMaxFaceHeight > 0){
                //myFacePlayer.start();
                rect = facesArray[iMaxFaceHeightIndex];
                mFace = mRgba.submat(rect);

                faceDetected = true;

            }

        }
        return mRgba;
    }

    private class NoFace extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            temp = getResources().getDrawable(R.drawable.whiteimage);

            return successResponse;

        }

        protected void onPostExecute(String string){
            photoTextResult.setText("");
            photoEmoticonView.setImageDrawable(temp);

        }
    }

    private class StartSending extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {

            if(mFace == null)
                Log.e("Debug","face null");
            if(mFace != null)
                Log.e("Debug","face not null");
            bitmap = Bitmap.createBitmap(mFace.cols(), mFace.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(mFace, bitmap);
           // photoEmoticonView.setImageBitmap(bitmap);
            try {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
                byte[] bitmapdata = bos.toByteArray();
                pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(bitmapdata);
                fos.flush();
                fos.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }

           // uploadToServer();

            bitmap.recycle();
            return successResponse;

            //imageview.setImageBitmap(bitmap);
        }

        protected void onPostExecute(String string) {
            //photoEmoticonView.setImageBitmap(bitmap);
            uploadToServer();
        }
    }


    private static File getOutputMediaFile(int type) {

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "MyNewCameraApp");

        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_" + timeStamp + ".jpg");
        } else {
            return null;
        }

        return mediaFile;
    }

    public void uploadToServer() {
        SendHttpPicRequest sendPic = new SendHttpPicRequest();
        sendPic.execute();
    }

    private class SendHttpPicRequest extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            if (pictureFile != null)
                Log.e("Debug", "file not null");

            HttpClient client = new DefaultHttpClient();
            HttpPost post = new HttpPost("http://emo.vistawearables.com/bookmarks");

            String boundary = "-------------" + System.currentTimeMillis();

            HttpEntity httpEntity = MultipartEntityBuilder.create()
                    .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                    .setBoundary(boundary)
                    .addBinaryBody("bookmark[photo]", pictureFile, ContentType.create("image/jpeg"), pictureFile.getName())
                    .build();

            post.setEntity(httpEntity);

            try {
                client.execute(post);
                Log.e("Debug", "image posted");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return successResponse;
        }

        protected void onPostExecute(String successResponse) {
            Log.e("Debug", "posted");
            getResponse();

        }
    }

    public void getResponse(){
        new HttpResponseAsyncTask().execute("http://emo.vistawearables.com/bookmarks.json");
    }

    private class HttpResponseAsyncTask extends AsyncTask<String, Void, String> {
        String emotions[] = new String[7];

        @Override
        protected String doInBackground(String... urls) {
            return GET(urls[0]);
        }

        protected void onPostExecute(String result) {
            Log.e("Debug", "Server Response received!");

            // Parse response
            try {
                JSONObject json = new JSONObject(result);

                happyResponse = json.getString("happy");
                emotions[0] = happyResponse;
                sadResponse = json.getString("sad");
                emotions[1] = sadResponse;
                surpriseResponse = json.getString("surprise");
                emotions[2] = surpriseResponse;
                fearResponse = json.getString("fear");
                emotions[3] = fearResponse;
                neutralResponse = json.getString("neutral");
                emotions[4] = neutralResponse;
                disgustResponse = json.getString("disgust");
                emotions[5] = disgustResponse;
                angryResponse = json.getString("angry");
                emotions[6] = angryResponse;

            } catch (JSONException e) {
                e.printStackTrace();
            }

            for (int i = 0; i <= 6; i++) {
                values[i] = (Double.parseDouble(emotions[i]));
            }

            Double largestEmotion = compareEmotions();

            for(int i = 0; i<=6; i++){
                if(values[i] == largestEmotion)
                    mainEmotion = emotions[i];
            }
            if(realtime)
                showResult();
            Log.e("Debug", "Result showed " + mainEmotion);

        }
    }

    public Double compareEmotions(){
        Double largest = Double.MIN_VALUE;
        for(int i = 0; i <= 6; i++){
            if(values[i] > largest)
                largest = values[i];
        }
        return largest;
    }

    public static String GET(String url){
        InputStream inputStream = null;
        String result = "";
        try {

            HttpClient httpclient = new DefaultHttpClient();
            HttpResponse httpResponse = httpclient.execute(new HttpGet(url));
            inputStream = httpResponse.getEntity().getContent();
            if(inputStream != null)
                result = convertInputStreamToString(inputStream);
            else
                result = "Did not work!";

        } catch (Exception e) {
            Log.d("InputStream", e.getLocalizedMessage());
        }

        return result;
    }

    private static String convertInputStreamToString(InputStream inputStream) throws IOException {
        BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(inputStream));
        String line = "";
        String result = "";
        while((line = bufferedReader.readLine()) != null)
            result += line;

        inputStream.close();
        return result;

    }

    public void showResult(){

        if(mainEmotion == happyResponse) {
            Drawable temp = getResources().getDrawable(R.drawable.happy);
            photoEmoticonView.setImageDrawable(temp);
            photoTextResult.setText("Happy");
            myHappyPlayer.start();
        }

        if(mainEmotion == sadResponse){
            Drawable temp = getResources().getDrawable(R.drawable.sad);
            photoEmoticonView.setImageDrawable(temp);
            photoTextResult.setText("Sad");
            mySadPlayer.start();
        }

        if(mainEmotion == disgustResponse){
            Drawable temp = getResources().getDrawable(R.drawable.disgust);
            photoEmoticonView.setImageDrawable(temp);
            photoTextResult.setText("Disgust");
            myDisgustPlayer.start();
        }

        if(mainEmotion == surpriseResponse){
            Drawable temp = getResources().getDrawable(R.drawable.surprised);
            photoEmoticonView.setImageDrawable(temp);
            photoTextResult.setText("Surprise");
            mySurprisedPlayer.start();
        }

        if(mainEmotion == fearResponse){
            Drawable temp = getResources().getDrawable(R.drawable.fear);
            photoTextResult.setText("fear");
            photoEmoticonView.setImageDrawable(temp);
            myFearPlayer.start();
        }

        if(mainEmotion == neutralResponse){
            Drawable temp = getResources().getDrawable(R.drawable.neutral);
            photoTextResult.setText("Neutral");
            photoEmoticonView.setImageDrawable(temp);
            myNeutralPlayer.start();

        }

        if(mainEmotion == angryResponse){
            Drawable temp = getResources().getDrawable(R.drawable.angry);
            photoTextResult.setText("Angry");
            photoEmoticonView.setImageDrawable(temp);
            myAngryPlayer.start();

        }
    }
}
