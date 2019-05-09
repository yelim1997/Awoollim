package com.example.awoollim;

import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

public class Explanation extends AppCompatActivity{

    Adapter adapter;
    ViewPager viewPager;

    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.explanation);

        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setCustomView(R.layout.title_bar);

        viewPager = (ViewPager)findViewById(R.id.view);
        adapter = new Adapter(this);
        viewPager.setAdapter(adapter);
    }
}