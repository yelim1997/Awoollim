package com.example.awoollim;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.AsyncTask;
import android.speech.RecognizerIntent;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

/*
    음성해석 화면 (음성인식을 통해 수화영상과 텍스트 제공)
 */

public class Voice extends AppCompatActivity {

    private static final int RESULT_SPEECH = 1; //REQUEST_CODE로 쓰임

    private Intent i;
    private Button audiobtn;
    private TextView textView;
    private Button videoDownBtn;
    private ImageView videoImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.voice);

        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setCustomView(R.layout.title_bar);

        audiobtn = (Button) findViewById(R.id.audiobtn);
        videoDownBtn = (Button) findViewById(R.id.videoDownBtn);
        textView = (TextView) findViewById(R.id.audiotext);
        videoImage = (ImageView) findViewById(R.id.videoImage);

        //음성인식 버튼 클릭시
        audiobtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                videoImage.setBackgroundResource(R.color.colorImage);
                Glide.with(videoImage).load("").into(videoImage);

                i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                i.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getBaseContext().getPackageName());
                i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR");
                i.putExtra(RecognizerIntent.EXTRA_PROMPT, "말해주세요");

                try {
                    startActivityForResult(i, RESULT_SPEECH);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(getApplicationContext(), "Speech To Text를 지원하지 않습니다.", Toast.LENGTH_SHORT).show();
                    ;
                    e.getStackTrace();
                }

            }
        });

        //영상보기 버튼 클릭시
        videoDownBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                String text = textView.getText().toString();

                Toast.makeText(getApplicationContext(), "잠시만 기다려주십시오, 처리중입니다...", Toast.LENGTH_LONG).show();

                //음성인식 버튼 수행 후 실시
                if (text.equals("이곳에 텍스트가 나옵니다.")) {
                    Toast.makeText(getBaseContext(), "음성인식을 먼저 실행해주세요", Toast.LENGTH_SHORT).show();

                } else {

                    //다의어 부분
                    if (text.equals("경찰")) {
                        text = "경찰관";
                    }
                    if (text.equals("남자친구")) {
                        text = "남자 친구";
                    }
                    if (text.equals("여자친구")) {
                        text = "여자 친구";
                    }
                    if (text.equals("와이프")) {
                        text = "아내";
                    }
                    if (text.equals("아버지")) {
                        text = "아빠";
                    }
                    if (text.equals("어머니")) {
                        text = "엄마";
                    }

                    StorageReference storageRef = FirebaseStorage.getInstance().getReference();
                    StorageReference spaceRef = storageRef.child("video/" + text + ".gif");

                    videoImage.setBackgroundResource(R.color.colorAccent);

                    Glide.with(videoImage).load(spaceRef).into(videoImage);


                    spaceRef.getMetadata().addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            videoImage.setBackgroundResource(R.color.colorImage);
                            Toast.makeText(getBaseContext(), "일치하는 영상이 없습니다.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && (requestCode == RESULT_SPEECH)) {

            //data.getString..() 호출로 음성인식 결과를 ArrayList로 받는다.
            ArrayList<String> sstResult = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

            //결과들 중 음성과 가장 유사한 단어부터 시작되는 0번째 문자열을 저장한다.
            String result_sst = sstResult.get(0);

            textView.setText("" + result_sst);

        }
    }
}