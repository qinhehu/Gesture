package com.example.qinhe.gesture;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;

import com.example.qinhe.gesture.Gesture.GestureEngine;

public class MainActivity extends AppCompatActivity {

    RecyclerView list;

    ListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        list = (RecyclerView)findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(list.getContext()));
        list.setItemAnimator(new DefaultItemAnimator());
        adapter = new ListAdapter();
        list.setAdapter(adapter);

        new GestureEngine(list);
    }
}
