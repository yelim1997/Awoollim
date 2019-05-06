package com.example.awoollim;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

public class FIrstFragment extends Fragment {

    private String title;
    private int page;

    public static FIrstFragment newInstance(int page, String title){
        FIrstFragment fragment = new FIrstFragment();
        Bundle args = new Bundle();
        args.putInt("someInt",page);
        args.putString("someTitle",title);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        page = getArguments().getInt("someInt",0);
        title = getArguments().getString("someTitle");
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.fragment_first,container,false);
        EditText tvLabel = (EditText)view.findViewById(R.id.editText1);
        tvLabel.setText(page+" -- "+title);
        return view;
    }
}
