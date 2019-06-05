package com.example.awoollim;


import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Looper;
import android.provider.MediaStore;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/*
     수화해석 화면 (수화 영상을 녹화해 텍스트로 번역)
 */

public class SignLanguage extends AppCompatActivity  implements SurfaceHolder.Callback {

    private static String INTERNAL_STORAGE_PATH = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString()+File.separator+"Camera";;
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

                File file = new File(filename);
                if (file.exists()) {
                    file.delete();
                }

                if (videoUri == null) {
                    Toast.makeText(getApplicationContext(), "첫 촬영 시작", Toast.LENGTH_LONG).show();
                }
                else {
                    getContentResolver().delete(videoUri, MediaStore.MediaColumns.DATA + "=?", new String[]{ filename } );
                }

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
                if (recorder == null) {
                    Toast.makeText(getBaseContext(), "redorder == null", Toast.LENGTH_LONG).show();
                    return;
                }

                recorder.stop();
                recorder.reset();
                recorder.release();
                recorder = null;

                ContentValues values = new ContentValues(7);

                values.put(MediaStore.MediaColumns.TITLE, "RecordedVideo");
                values.put(MediaStore.Audio.Media.ALBUM, "Video Album");
                values.put(MediaStore.Audio.Media.ARTIST, "YELIM");
                values.put(MediaStore.Audio.Media.DISPLAY_NAME, "Recorded Video");
                values.put(MediaStore.MediaColumns.DATE_ADDED, System.currentTimeMillis() / 1000);
                values.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
                values.put(MediaStore.Audio.Media.DATA, filename);

                videoUri = getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
                if (videoUri == null)
                {
                    Toast.makeText(getBaseContext(), "videoUri == null", Toast.LENGTH_LONG).show();
                    return;
                }

                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, videoUri));

                // uri는 임시경로(?)를 가지고 있는데(실제로 존재 X), 이걸 절대 경로로 바꿔주는 부분임
                ///Cursor cursor = getContentResolver().query(videoUri, null, null, null);
                //((Cursor) cursor).moveToNext();
                //String filePath = cursor.getString(cursor.getColumnIndex("_data"));

                uploadVideo();
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

    public void uploadVideo () {
        Toast.makeText(getApplicationContext(), "촬영된 영상을 전송중입니다.", Toast.LENGTH_LONG).show();

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run(){
                try
                {
                    String filePath = "/storage/emulated/0/DCIM/Camera/video_recorded.mp4";
                    FileInputStream mFileInputStream = new FileInputStream(filePath);
                    URL connectUrl = new URL("http://malgeul.ga/api/photo");
                    String lineEnd = "\r\n";
                    String twoHyphens = "--";
                    String boundary = "*****";

                    HttpURLConnection conn = (HttpURLConnection) connectUrl.openConnection();
                    conn.setDoInput(true);//입력할수 있도록
                    conn.setDoOutput(true); //출력할수 있도록
                    conn.setUseCaches(false);  //캐쉬 사용하지 않음
                    //post 전송
                    conn.setRequestMethod("POST");
                    //파일 업로드 할수 있도록 설정하기.
                    conn.setRequestProperty("Connection", "Keep-Alive");
                    conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

                    //DataOutputStream 객체 생성하기.
                    DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
                    //전송할 데이터의 시작임을 알린다.
                    dos.writeBytes(twoHyphens + boundary + lineEnd);
                    dos.writeBytes("Content-Disposition: form-data; name=\"upfile\";filename=\"" + filePath +"\"" + lineEnd);
                    dos.writeBytes(lineEnd);
                    //한번에 읽어들일수있는 스트림의 크기를 얻어온다.
                    int bytesAvailable = mFileInputStream.available();
                    //byte단위로 읽어오기 위하여 byte 배열 객체를 준비한다.
                    byte[] buffer = new byte[bytesAvailable];
                    int bytesRead = 0;
                    // read image
                    while (bytesRead!=-1) {
                        //파일에서 바이트단위로 읽어온다.
                        bytesRead = mFileInputStream.read(buffer);
                        if(bytesRead==-1)break; //더이상 읽을 데이터가 없다면 빠저나온다.
                        Log.e("Test", "image byte is " + bytesRead);
                        //읽은만큼 출력한다.
                        dos.write(buffer, 0, bytesRead);
                        //출력한 데이터 밀어내기
                        dos.flush();
                    }
                    //전송할 데이터의 끝임을 알린다.
                    dos.writeBytes(lineEnd);
                    dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
                    //flush() 타이밍??
                    //dos.flush();
                    dos.close();//스트림 닫아주기
                    mFileInputStream.close();//스트림 닫아주기.
                    // get response
                    int ch;
                    //입력 스트림 객체를 얻어온다.
                    InputStream is = conn.getInputStream();
                    StringBuffer b =new StringBuffer();
                    while( ( ch = is.read() ) != -1 ){
                        b.append( (char)ch );
                    }
                    String s=b.toString();
                    Log.e("Test", "result = " + s);
                    deleteVideo();
                }
                catch(Exception e)
                {
                    Log.e("shkang", "exception : " + e.getCause() + " str : " + e.toString());
                }

            }
        });

        thread.start();
    }

    private void deleteVideo( ) {
        File file = new File(filename);
        if (file.exists()) {
            file.delete();
        }

        if (videoUri == null)
        {
            Toast.makeText(getApplicationContext(), "영상 URI가 없습니다.", Toast.LENGTH_LONG).show();
        }
        else
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

        /*
        if(filename != "")
        {
            deleteVideo();
            filename = "";

        }
        */

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
                }else{
                    recordText.setText("인지 불가");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}
