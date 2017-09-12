package com.example.qinhe.gesture;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.Toast;

import com.qinhe.gesturelist.gesture.Drag;
import com.qinhe.gesturelist.GestureEngine;
import com.qinhe.gesturelist.event.IDragListener;
import com.qinhe.gesturelist.event.IEventListener;
import com.qinhe.gesturelist.gesture.LongTouch;
import com.qinhe.gesturelist.gesture.TurnLeftSideslip;
import com.qinhe.gesturelist.gesture.TurnRightSideslip;


public class MainActivity extends AppCompatActivity {

    @LongTouch
    @Drag
    @TurnRightSideslip(value = {R.id.image_read3, R.id.image_read4})
    @TurnLeftSideslip(autoClose = true, value = {R.id.image_read})
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

        GestureEngine gestureEngine = new GestureEngine(this, list);
        gestureEngine.addLeftClickListener(new IEventListener() {
            @Override
            public void onClickListen(int position) {
                Toast.makeText(getApplicationContext(), "left_green===" + position, Toast.LENGTH_SHORT).show();
            }
        });
        gestureEngine.addLeftClickListener(new IEventListener() {
            @Override
            public void onClickListen(int position) {
                Toast.makeText(getApplicationContext(), "left_red===" + position, Toast.LENGTH_SHORT).show();
            }
        });

        gestureEngine.addRightClickListener(new IEventListener() {
            @Override
            public void onClickListen(int position) {
                Toast.makeText(getApplicationContext(), "right_green===" + position, Toast.LENGTH_SHORT).show();
            }
        });
        gestureEngine.addRightClickListener(new IEventListener() {
            @Override
            public void onClickListen(int position) {
                Toast.makeText(getApplicationContext(), "right_red===" + position, Toast.LENGTH_SHORT).show();
            }
        });
        gestureEngine.setDragListener(new IDragListener() {
            @Override
            public void onSwapListener(int fromPosition, int toPosition) {
                Toast.makeText(getApplicationContext(), fromPosition+" swap " + toPosition, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onChoseListener(int fromPosition, int toPosition) {
                Toast.makeText(getApplicationContext(), fromPosition+" to " + toPosition, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
