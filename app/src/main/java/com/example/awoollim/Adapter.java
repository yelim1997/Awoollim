package com.example.awoollim;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class Adapter extends PagerAdapter{

    private int[] images = {R.drawable.ex1, R.drawable.ex2};
    private LayoutInflater inflater;
    private Context context;

    public Adapter(Context context){
        this.context = context;
    }

    @Override
    public int getCount(){
        return images.length;
    }

    @Override
    public boolean isViewFromObject(View view, Object object){
        return view == ((LinearLayout)object);
    }

    public Object instantiateItem(ViewGroup container, int position){
        inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.slider,container,false);
        ImageView imageView = (ImageView)v.findViewById(R.id.fragImageView);
        TextView textView = (TextView)v.findViewById(R.id.fragTextView);

        imageView.setImageResource(images[position]);

        String text = (position+1)+"번째 이미지";
        textView.setText(text);
        container.addView(v);
        return v;
    }

    public void destroyItem(ViewGroup container, int position, Object object){
        container.invalidate();
    }
}