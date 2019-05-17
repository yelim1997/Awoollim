package com.example.awoollim;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
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
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.List;

/*
     수화해석 화면 (수화 영상을 녹화해 텍스트로 번역)
 */

public class SignLanguage extends AppCompatActivity implements SurfaceHolder.Callback{

    private static String EXTERNAL_STORAGE_PATH = "";
    private static String RECORDED_FILE = "video_recorded";
    private static String filename = "";
    private Camera camera = null;

    MediaPlayer player;
    MediaRecorder recorder;
    SurfaceHolder holder;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);


        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setCustomView(R.layout.title_bar);

        String state = Environment.getExternalStorageState();

        if (!state.equals(Environment.MEDIA_MOUNTED)) { }
        else
            {
            EXTERNAL_STORAGE_PATH = Environment.getExternalStorageDirectory().getAbsolutePath();
            }

        setContentView(R.layout.signlanguage);
        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.videoLayout);

        holder = surfaceView.getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        final Button recordBtn = (Button) findViewById(R.id.recordBtn);
        final Button recordStopBtn = (Button) findViewById(R.id.recordStopBtn);
        final Button recordSend = (Button) findViewById(R.id.recordSend);
        final TextView recordText = (TextView) findViewById(R.id.recordText);

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

                Uri videoUri = getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
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
                    recordText.setText(filename);
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
        if (EXTERNAL_STORAGE_PATH == null || EXTERNAL_STORAGE_PATH.equals(""))
        {
            newFilename = RECORDED_FILE + ".mp4";
        }
        else
            {
            newFilename = EXTERNAL_STORAGE_PATH + "/" + RECORDED_FILE + ".mp4";
            }

        return newFilename;
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
            File file = new File(filename);
            if (file.exists()) {
                file.delete();
            }
            filename = "";

        }

        super.onPause();
    }
}
