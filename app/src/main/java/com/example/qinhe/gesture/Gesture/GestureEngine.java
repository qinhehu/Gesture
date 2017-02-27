package com.example.qinhe.gesture.Gesture;

import android.support.v7.widget.RecyclerView;

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
        mRecyclerView.addItemDecoration(new ItemDecoration());
    }


}
