package com.example.qinhe.gesture.Gesture;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.FrameLayout;
import android.widget.Scroller;

import com.example.qinhe.gesture.DisplayUtils;
import com.example.qinhe.gesture.IOnListListener;
import com.example.qinhe.gesture.R;

/**
 * Created by QinHe on 2017/2/21.
 */

public class BulletItemTouchListener implements RecyclerView.OnItemTouchListener {

    private static final int SIDESLIP = 0;
    private static final int LONG_PRESS = 2;

    private static final int LONGPRESS_TIMEOUT = ViewConfiguration.getLongPressTimeout();
    private static final int TAP_TIMEOUT = ViewConfiguration.getTapTimeout();

    private static final float VERTICAL_MOVE_OFFSET = 10;//手势上下浮动
    private static final float HORIZONTAL_MOVE_OFFSET = 10;//手势左右浮动

    private static final float VIEW_LAYOUT_WIDTH = 70;

    private int mTouchSlopSquare;
    private float mDownFocusX;
    private float mDownFocusY;
    private Handler mHandler;

    private int mCurrentX;

    private boolean isScrolling;
    private boolean isOnceEventFlow;

    private boolean isNeedLongPress = true;
    private boolean isNeedSideslip = true;

    private View mView;
    @Nullable
    private IOnListListener mMobiListListener;

    @NonNull
    private Scroller mScroller;
    @NonNull
    private DisplayUtils displayUtils;

    public BulletItemTouchListener(Context context) {
        ViewConfiguration configuration = ViewConfiguration.get(context);
        int touchSlop = configuration.getScaledTouchSlop();
        mTouchSlopSquare = touchSlop * touchSlop;

        mHandler = new GestureHandler();
        isScrolling = false;
        displayUtils = new DisplayUtils(context);
        mScroller = new Scroller(context, new AccelerateDecelerateInterpolator());
    }

    @Override
    public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent ev) {



        if (isScrolling) {
            return false;
        }

        int x = (int) ev.getX();
        int y = (int) ev.getY();

        final int action = ev.getAction();
        final boolean pointerUp =
                (action & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_POINTER_UP;
        final int skipIndex = pointerUp ? ev.getActionIndex() : -1;
        // Determine focal point
        float sumX = 0, sumY = 0;
        final int count = ev.getPointerCount();
        for (int i = 0; i < count; i++) {
            if (skipIndex == i) continue;
            sumX += ev.getX(i);
            sumY += ev.getY(i);
        }
        final int div = pointerUp ? count - 1 : count;
        final float focusX = sumX / div;
        final float focusY = sumY / div;

        switch (ev.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                mDownFocusX = focusX;
                mDownFocusY = focusY;
                isOnceEventFlow = true;
                if (mView != null) {
                    reset();
                }
                mView = rv.findChildViewUnder(x, y);
                if (isNeedLongPress) {
                    mHandler.removeMessages(LONG_PRESS);
                    mHandler.sendEmptyMessageAtTime(LONG_PRESS, ev.getDownTime()
                            + TAP_TIMEOUT + LONGPRESS_TIMEOUT);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                final int deltaX = (int) (focusX - mDownFocusX);
                final int deltaY = (int) (focusY - mDownFocusY);
                int distance = (deltaX * deltaX) + (deltaY * deltaY);
                if(distance > mTouchSlopSquare){

                    mHandler.removeMessages(LONG_PRESS);

                    if (isNeedSideslip) {
                        if (isSideslipMove(x, y)) {
                            return true;
                        }
                    }
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                Log.d("LONG_PRESS", "ACTION_UP: ");
                mHandler.removeMessages(LONG_PRESS);
                break;
        }
        return false;
    }

    @Override
    public void onTouchEvent(RecyclerView rv, MotionEvent event) {

        if (mView == null) {
            return;
        }

        int x = (int) event.getX();
        int y = (int) event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                break;
            case MotionEvent.ACTION_MOVE:
                Log.d("onTouchEvent", "onTouchEvent: " + isScrolling);
                if (!isScrolling && isOnceEventFlow) {
                    //手指向左移动，view左移动，触发右边事件
                    if (mDownFocusX - x > 0 && Math.abs(x - mDownFocusX) <= displayUtils.dip2px(VIEW_LAYOUT_WIDTH)
                            && mView.getScrollX() != displayUtils.dip2px(VIEW_LAYOUT_WIDTH)) {
                        mView.scrollTo((int) (mDownFocusX - x), 0);
                        mCurrentX = (int) (mDownFocusX - x);
//                        LogUtils.d("CstSideslip", "onTouchEvent: ACTION_MOVE right "+mCurrentX);
                    }
                    //手指向右移动，view右移动，触发左边事件
                    if (mDownFocusX - x < -displayUtils.dip2px(VIEW_LAYOUT_WIDTH) / 2 &&
                            mCurrentX == 0 || mCurrentX == displayUtils.dip2px(VIEW_LAYOUT_WIDTH)) {
                        if (mCurrentX == displayUtils.dip2px(VIEW_LAYOUT_WIDTH)) {
//                            ViewCompat.setTranslationZ(rightBtn, -1f);
                        }
//                        LogUtils.d("CstSideslip", "onTouchEvent: ACTION_MOVE left "+mCurrentX);
                        mScroller.startScroll(mCurrentX, 0, -displayUtils.dip2px(VIEW_LAYOUT_WIDTH), 0);
                        mCurrentX += -displayUtils.dip2px(VIEW_LAYOUT_WIDTH);
                        mHandler.sendEmptyMessage(SIDESLIP);
//                        LogUtils.d("CstSideslip", "onTouchEvent: ACTION_MOVE left "+mCurrentX);
                    }
                }

                break;
            case MotionEvent.ACTION_UP:
//                LogUtils.d("CstSideslip", "onTouchEvent: ACTION_UP" + x + "----" + y);
            case MotionEvent.ACTION_CANCEL:
                if (mView.getScrollX() > 0) {
                    if (mCurrentX >= displayUtils.dip2px(VIEW_LAYOUT_WIDTH) / 2) {
//                        LogUtils.d("CstSideslip", "onTouchEvent: ACTION_UP show" + mLinearLayout.getScrollX());
                        mScroller.startScroll(mView.getScrollX(), 0
                                , -mView.getScrollX() + displayUtils.dip2px(VIEW_LAYOUT_WIDTH), 0);
                        mCurrentX = displayUtils.dip2px(VIEW_LAYOUT_WIDTH);
                        mHandler.sendEmptyMessage(SIDESLIP);
                    }

                    if (mCurrentX < displayUtils.dip2px(VIEW_LAYOUT_WIDTH) / 2) {
//                        LogUtils.d("CstSideslip", "onTouchEvent: ACTION_UP hide" + mLinearLayout.getScrollX() + "===" + mCurrentX);
                        mScroller.startScroll(mView.getScrollX(), 0, -mView.getScrollX(), 0);
                        mCurrentX = 0;
                        mHandler.sendEmptyMessage(SIDESLIP);
                    }
                }



//                LogUtils.d("CstSideslip", "onTouchEvent: ACTION_CANCEL" + x + "----" + y);
                break;
        }
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

    }

    //只有移动超过offset过后，才认为是个侧滑动作，进行事件拦截。
    private Boolean isSideslipMove(float x, float y) {
        if (
                Math.abs(mDownFocusY - y) <= displayUtils.dip2px(VERTICAL_MOVE_OFFSET) &&
                        Math.abs(mDownFocusX - x) >= displayUtils.dip2px(HORIZONTAL_MOVE_OFFSET)) {
            return true;
        }
        return false;
    }

    public void reset() {
        mScroller.startScroll(mCurrentX, 0, -mCurrentX, 0);
//        mView.scrollTo(0, 0);
        mCurrentX = 0;
        mHandler.sendEmptyMessage(SIDESLIP);

    }

    private class GestureHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SIDESLIP:
                    if (mScroller.computeScrollOffset()) {
                        isScrolling = true;
                        mView.scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
                        mView.invalidate();
                        mHandler.sendEmptyMessage(SIDESLIP);
                    } else {
                        if (mCurrentX == -displayUtils.dip2px(VIEW_LAYOUT_WIDTH)) {
                            mScroller.startScroll(mCurrentX, 0, displayUtils.dip2px(VIEW_LAYOUT_WIDTH), 0);
                            mHandler.sendEmptyMessage(SIDESLIP);
                            mCurrentX = 0;
                            isOnceEventFlow = false;
                            if (mMobiListListener != null) {
                                mMobiListListener.onRead();
                            }
                        } else {
                            isScrolling = false;
                        }
                    }
                    break;
                case LONG_PRESS:
                    Log.d("LONG_PRESS", "handleMessage: ");

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        View view = mView;

//                        Transition transition = TransitionInflater.from(
//                                view.getContext()).inflateTransition(R.transition.full_view_transition);
//                        TransitionManager.beginDelayedTransition((ViewGroup) view, transition);
//                        RecyclerView.LayoutParams layoutParams =
//                                new RecyclerView.LayoutParams((int)(view.getMeasuredWidth()*0.8)
//                                , (int)(view.getMeasuredHeight()*0.8));
//                        view.setLayoutParams(layoutParams);
                        mView.setVisibility(View.INVISIBLE);
                    }
                    break;
            }
        }
    }
}