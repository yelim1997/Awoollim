package com.example.awoollim;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class Main3Activity extends AppCompatActivity {

    private static final int RESULT_SPEECH = 1; //REQUEST_CODE로 쓰임

    private Intent i;
    private Button button;
    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main3);

        button = (Button)findViewById(R.id.audiobtn);
        textView = (TextView)findViewById(R.id.audiotext);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                i.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,getBaseContext().getPackageName());
                i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR");
                i.putExtra(RecognizerIntent.EXTRA_PROMPT,"말해주세요");

                Toast.makeText(Main3Activity.this,"start speak",Toast.LENGTH_SHORT).show();

                try {
                    startActivityForResult(i,RESULT_SPEECH);
                }catch (ActivityNotFoundException e){
                    Toast.makeText(getApplicationContext(),"Speech To Text를 지원하지 않습니다.",Toast.LENGTH_SHORT).show();;
                    e.getStackTrace();
                }

            }
        });
    }

    protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode, data);
        if(resultCode==RESULT_OK&&(requestCode==RESULT_SPEECH)){

            //data.getString..() 호출로 음성인식 결과를 ArrayList로 받는다.
            ArrayList<String> sstResult = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

            //결과들 중 음성과 가장 유사한 단어부터 시작되는 0번째 문자열을 저장한다.
            String result_sst = sstResult.get(0);

            textView.setText(""+result_sst);

            Toast.makeText(Main3Activity.this,result_sst,Toast.LENGTH_SHORT).show();;
        }
    }
}
