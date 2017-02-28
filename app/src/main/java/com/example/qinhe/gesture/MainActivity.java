package com.example.qinhe.gesture;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;
import android.widget.ListView;

import com.example.qinhe.gesture.Gesture.Drag;
import com.example.qinhe.gesture.Gesture.GestureEngine;
import com.example.qinhe.gesture.Gesture.LongTouch;
import com.example.qinhe.gesture.Gesture.TurnLeftSideslip;
import com.example.qinhe.gesture.Gesture.TurnRightSideslip;

public class MainActivity extends AppCompatActivity {

    @LongTouch
    @Drag
//    @TurnRightSideslip
//    @TurnLeftSideslip(autoClose = false)
    RecyclerView list;

    ListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        list = (RecyclerView) findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(list.getContext()));
        list.setItemAnimator(new DefaultItemAnimator());
        adapter = new ListAdapter();
        list.setAdapter(adapter);

        new GestureEngine(this, list);
    }
}
