package com.example.qinhe.gesture.Gesture;

import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Scroller;

import com.example.qinhe.gesture.DisplayUtils;
import com.example.qinhe.gesture.IOnListListener;

/**
 * Created by QinHe on 2017/2/21.
 */

public class GestureEngine {

    private RecyclerView mRecyclerView;



    public GestureEngine(RecyclerView recyclerView){
        if(recyclerView == null){
            return;
        }
        mRecyclerView = recyclerView;
        mRecyclerView.addOnItemTouchListener(new BulletItemTouchListener(mRecyclerView.getContext()));
    }


}
