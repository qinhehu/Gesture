package com.qinhe.gesturelist;

import android.support.annotation.NonNull;
import android.support.v4.util.ArrayMap;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import com.qinhe.gesturelist.event.IDragListener;
import com.qinhe.gesturelist.event.IEventListener;
import com.qinhe.gesturelist.gesture.Drag;
import com.qinhe.gesturelist.gesture.LongTouch;
import com.qinhe.gesturelist.gesture.TurnLeftSideslip;
import com.qinhe.gesturelist.gesture.TurnRightSideslip;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by QinHe on 2017/2/21.
 */

public class GestureEngine {

    private RecyclerView mRecyclerView;
    private ItemTouchListener itemListener;
    private List<IEventListener> mLeftEventListeners;
    private List<IEventListener> mRightEventListeners;

    public GestureEngine(@NonNull Object target, @NonNull RecyclerView recyclerView) {
        if (recyclerView == null) {
            return;
        }
        mRecyclerView = recyclerView;
        itemListener = new ItemTouchListener(mRecyclerView);
        mRecyclerView.addOnItemTouchListener(itemListener);
        parseAnnotation(target);
    }

    private void parseAnnotation(Object target) {
        ArrayMap map = new ArrayMap();
        Class<?> targetClass = target.getClass();
        Field[] fields = targetClass.getDeclaredFields();
        for (Field field : fields) {
            Annotation[] annotations = field.getAnnotations();
            for (Annotation annotation : annotations) {
                if (annotation instanceof TurnLeftSideslip) {
                    map.put("TurnLeftSideslip", annotation);
                } else if (annotation instanceof TurnRightSideslip) {
                    map.put("TurnRightSideslip", annotation);
                } else if (annotation instanceof LongTouch) {
                    map.put("LongTouch", annotation);
                } else if (annotation instanceof Drag) {
                    map.put("Drag", annotation);
                }
            }
        }
        itemListener.setLimitMap(map);
    }

    public void addLeftClickListener(IEventListener eventListener) {
        if (mLeftEventListeners == null) {
            mLeftEventListeners = new ArrayList<>();
        }
        mLeftEventListeners.add(eventListener);
        itemListener.setLeftClickListener(mLeftEventListeners);
    }

    public void addRightClickListener(IEventListener eventListener) {
        if (mRightEventListeners == null) {
            mRightEventListeners = new ArrayList<>();
        }
        mRightEventListeners.add(eventListener);
        itemListener.setRightClickListener(mRightEventListeners);
    }

    public void setDragListener(IDragListener dragListener){
        itemListener.setDragListener(dragListener);
    }
}
