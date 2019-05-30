package com.example.awoollim;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/*
     수화해석 화면 (수화 영상을 녹화해 텍스트로 번역)
 */

public class SignLanguage extends AppCompatActivity implements SurfaceHolder.Callback{

    private static String INTERNAL_STORAGE_PATH = "";
    private static String RECORDED_FILE = "video_recorded";
    private static String filename = "";
    private Camera camera = null;
    TextView recordText;

    MediaPlayer player;
    MediaRecorder recorder;
    SurfaceHolder holder;
    Uri videoUri;

    File file = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);


        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setCustomView(R.layout.title_bar);


        Context context = getApplicationContext();

        //INTERNAL_STORAGE_PATH = Environment.getExternalStorageDirectory().getAbsolutePath();
        INTERNAL_STORAGE_PATH =  Environment.getDataDirectory().getAbsolutePath()+File.separator+"data"+File.separator+context.getPackageName();

        Toast.makeText(getApplicationContext(), INTERNAL_STORAGE_PATH, Toast.LENGTH_LONG).show();

        setContentView(R.layout.signlanguage);
        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.videoLayout);

        holder = surfaceView.getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        final Button recordBtn = (Button) findViewById(R.id.recordBtn);
        final Button recordStopBtn = (Button) findViewById(R.id.recordStopBtn);
        final Button recordSend = (Button) findViewById(R.id.recordSend);
        recordText = (TextView) findViewById(R.id.recordText);

        recordBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    if (recorder == null)
                    {
                        recorder = new MediaRecorder();
                    }

                    camera.unlock();
                    recorder.setCamera(camera);

                    recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                    recorder.setVideoSource(MediaRecorder.VideoSource.DEFAULT);
                    recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

                    CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
                    recorder.setVideoEncodingBitRate(profile.videoBitRate);
                    recorder.setVideoFrameRate(30);
                    recorder.setVideoSize(640,480);
                    recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                    recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
                    recorder.setOrientationHint(90);

                    filename = createFilename();
                    recorder.setOutputFile(filename);

                    recorder.setPreviewDisplay(holder.getSurface());
                    recorder.prepare();
                    recorder.start();

                } catch (Exception ex) {
                    recorder.release();
                    recorder = null;
                }
            }
        });

        recordStopBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (recorder == null)
                    return;

                recorder.stop();
                recorder.reset();
                recorder.release();
                recorder = null;

                ContentValues values = new ContentValues(10);

                values.put(MediaStore.MediaColumns.TITLE, "RecordedVideo");
                values.put(MediaStore.Audio.Media.ALBUM, "Video Album");
                values.put(MediaStore.Audio.Media.ARTIST, "YELIM");
                values.put(MediaStore.Audio.Media.DISPLAY_NAME, "Recorded Video");
                values.put(MediaStore.MediaColumns.DATE_ADDED, System.currentTimeMillis() / 1000);
                values.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
                values.put(MediaStore.Audio.Media.DATA, filename);

                videoUri = getContentResolver().insert(MediaStore.Video.Media.INTERNAL_CONTENT_URI, values);
                if (videoUri == null)
                {
                    return;
                }

                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, videoUri));
            }
        });

        recordSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(filename == "")
                {
                    Toast.makeText(getBaseContext(),"촬영된 수화영상이 없습니다.", Toast.LENGTH_LONG).show();
                }
                else
                {
                    Toast.makeText(getApplicationContext(), "잠시만 기다려주십시오, 처리중입니다...", Toast.LENGTH_LONG).show();
                    new JSONTask().execute("http://malgeul.ga/");

                }
            }
        });
    }


    public void surfaceCreated(SurfaceHolder holder)
    {
        camera = Camera.open();
        camera.setDisplayOrientation(90);

        try {
            if (camera == null)
            {
                camera.setPreviewDisplay(holder);
                camera.startPreview();
            }
        } catch (IOException e) { }
    }


    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {
        if (holder.getSurface() == null)
        {
            return;
        }
        try
        {
            camera.stopPreview();
        } catch (Exception e) { }

        Camera.Parameters parameters = camera.getParameters();
        List<String> focusModes = parameters.getSupportedFocusModes();
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO))
        {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        }
        camera.setParameters(parameters);
        try {
            camera.setPreviewDisplay(holder);
            camera.startPreview();
        } catch (Exception e) { }
    }


    public void surfaceDestroyed(SurfaceHolder holder)
    {
        if (camera != null)
        {
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }


    private String createFilename()
    {

        String newFilename = "";
        if (INTERNAL_STORAGE_PATH == null || INTERNAL_STORAGE_PATH.equals(""))
        {
            newFilename = RECORDED_FILE + ".mp4";
        }
        else
            {
            newFilename = INTERNAL_STORAGE_PATH + "/" + RECORDED_FILE + ".mp4";
            }

        return newFilename;
    }

    private void deleteVideo( ) {
        File file = new File(filename);
        if (file.exists()) {
            file.delete();
        }

        getContentResolver().delete( videoUri,
                MediaStore.MediaColumns.DATA + "=?", new String[]{ filename } );
    }


    protected void onPause()
    {
        if (camera != null)
        {
            camera.release();
            camera = null;
        }

        if (recorder != null)
        {
            recorder.release();
            recorder = null;
        }

        if (player != null)
        {
            player.release();
            player = null;
        }


        if(filename != "")
        {
            deleteVideo();
            filename = "";

        }

        super.onPause();
    }

    public class JSONTask extends AsyncTask<String, String, String>{

        @Override
        protected String doInBackground(String... strings) {
            try{
                JSONObject jsonObject = new JSONObject();
                jsonObject.accumulate("result", "androidTest");

                HttpURLConnection con = null;
                BufferedReader reader = null;

                try{
                    URL url = new URL(strings[0]);

                    con = (HttpURLConnection)url.openConnection();
                    con.connect();

                    InputStream stream = con.getInputStream();

                    reader = new BufferedReader(new InputStreamReader(stream));

                    StringBuffer buffer = new StringBuffer();

                    String line = "";

                    while((line = reader.readLine()) != null){
                        buffer.append(line);
                    }

                    return buffer.toString();
                }catch (MalformedURLException e){
                    e.printStackTrace();
                }catch (IOException e){
                    e.printStackTrace();
                }finally {
                    if(con != null){
                        con.disconnect();
                    }
                    try{
                        if(reader != null){
                            reader.close();
                        }
                    }catch (IOException e){
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            StringBuffer sb = new StringBuffer();
            try {
                JSONArray jsonArray = new JSONArray(s);
                for(int i = 0 ; i < jsonArray.length() ; i++){
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    String result = jsonObject.getString("result");

                    sb.append(result);
                }

                if(sb.toString().indexOf("park")!= -1){
                    recordText.setText("공원");
                }else if(sb.toString().indexOf("preschcool") != -1) {
                    recordText.setText("유치원");
                }else if(sb.toString().indexOf("policeman") != -1){
                    recordText.setText("경찰관");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}
