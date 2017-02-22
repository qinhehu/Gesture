package com.example.qinhe.gesture;

import android.content.Context;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Scroller;

public class CstSideslip extends FrameLayout {

    private static final float VERTICAL_MOVE_OFFSET = 20;//手势上下浮动
    private static final float HORIZONTAL_MOVE_OFFSET = 10;//手势左右浮动

    private static final float VIEW_LAYOUT_WIDTH = 70;

    private int mLastX;
    private int mLastY;
    private int mDownX;
    private int mDownY;

    private int mCurrentX;

    private boolean mIsLeftScroll;
    private boolean mIsRightScroll;
    private boolean isScrolling;

    private ImageButton leftBtn;
    private ImageButton rightBtn;
    private LinearLayout mLinearLayout;

    private IOnListListener mMobiListListener;

    private Scroller mScroller;
    private DisplayUtils displayUtils;

    public CstSideslip(Context context) {
        this(context, null);
    }

    public CstSideslip(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CstSideslip(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initFromAttributes(context, attrs, defStyleAttr);

        displayUtils = new DisplayUtils(getContext());

        LayoutParams leftLayoutParams = new LayoutParams(displayUtils.dip2px(VIEW_LAYOUT_WIDTH)
                , ViewGroup.LayoutParams.MATCH_PARENT);
        leftLayoutParams.gravity = Gravity.LEFT;
        leftBtn = new ImageButton(getContext());
        leftBtn.setImageDrawable(getResources().getDrawable(R.drawable.read));
        leftBtn.setBackgroundColor(getResources().getColor(R.color.title_or_content));
        leftBtn.setLayoutParams(leftLayoutParams);
        addView(leftBtn);

        LayoutParams rightLayoutParams = new LayoutParams(displayUtils.dip2px(VIEW_LAYOUT_WIDTH)
                , ViewGroup.LayoutParams.MATCH_PARENT);
        rightLayoutParams.gravity = Gravity.RIGHT;
        rightBtn = new ImageButton(getContext());
        rightBtn.setImageDrawable(getResources().getDrawable(R.drawable.delete));
        rightBtn.setBackgroundColor(getResources().getColor(R.color.red_f32d3b));
        rightBtn.setLayoutParams(rightLayoutParams);
        addView(rightBtn);
        rightBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMobiListListener != null) {
                    if (mCurrentX == displayUtils.dip2px(VIEW_LAYOUT_WIDTH)) {
                        reset();
                    }
                    rightBtn.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mMobiListListener.onDelete();
                        }
                    }, 300);
                }
            }
        });
    }

    private void initFromAttributes(Context context, AttributeSet attrs, int defStyleAttr) {
        //还没封装，没什么用

//        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.sideslip, defStyleAttr, 0);
//        final int N = a.getIndexCount();
//
//        for (int i = 0; i < N; ++i) {
//            int attr = a.getIndex(i);
//            switch (attr) {
//                case R.attr.isleft_scroll:
//                    mIsLeftScroll = a.getBoolean(attr, true);
//                    break;
//                case R.attr.isRight_scroll:
//                    mIsRightScroll = a.getBoolean(attr, true);
//                    break;
//            }
//        }
//        a.recycle();
    }

    @Override
    protected void onAttachedToWindow() {

        super.onAttachedToWindow();

        int count = getChildCount();
        if (count == 0) {
            return;
        } else {
            mLinearLayout = (LinearLayout) getChildAt(2);
            mScroller = new Scroller(mLinearLayout.getContext());
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        isScrolling = false;
        int x = (int) ev.getX();
        int y = (int) ev.getY();
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mDownX = x;
                mDownY = y;
                break;
            case MotionEvent.ACTION_MOVE:
                if (isSideslipMove(x, y)) {
                    return true;
                }
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int x = (int) event.getX();
        int y = (int) event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                break;
            case MotionEvent.ACTION_MOVE:

                if (!isScrolling) {
                    //手指向左移动，view左移动，触发右边事件
                    if (mDownX - x > 0 && Math.abs(x - mDownX) <= displayUtils.dip2px(VIEW_LAYOUT_WIDTH)
                            && mLinearLayout.getScrollX() != displayUtils.dip2px(VIEW_LAYOUT_WIDTH)) {
                        mLinearLayout.scrollTo((int) (mDownX - x), 0);
                        mCurrentX = (int) (mDownX - x);
                        invalidate();
//                        LogUtils.d("CstSideslip", "onTouchEvent: ACTION_MOVE right "+mCurrentX);
                    }
                    //手指向右移动，view右移动，触发左边事件
                    if (mDownX - x < -displayUtils.dip2px(VIEW_LAYOUT_WIDTH)/2 && mCurrentX == 0 || mCurrentX == displayUtils.dip2px(VIEW_LAYOUT_WIDTH)) {
                        if (mCurrentX == displayUtils.dip2px(VIEW_LAYOUT_WIDTH)) {
                            ViewCompat.setTranslationZ(rightBtn, -1f);
                        }
//                        LogUtils.d("CstSideslip", "onTouchEvent: ACTION_MOVE left "+mCurrentX);
                        mScroller.startScroll(mCurrentX, 0, -displayUtils.dip2px(VIEW_LAYOUT_WIDTH), 0);
                        mCurrentX += -displayUtils.dip2px(VIEW_LAYOUT_WIDTH);
                        invalidate();
//                        LogUtils.d("CstSideslip", "onTouchEvent: ACTION_MOVE left "+mCurrentX);
                    }
                }

                break;
            case MotionEvent.ACTION_UP:
//                LogUtils.d("CstSideslip", "onTouchEvent: ACTION_UP" + x + "----" + y);
            case MotionEvent.ACTION_CANCEL:
                isScrolling = false;

                if (mLinearLayout.getScrollX() > 0) {
                    if (mCurrentX >= displayUtils.dip2px(VIEW_LAYOUT_WIDTH) / 2) {
//                        LogUtils.d("CstSideslip", "onTouchEvent: ACTION_UP show" + mLinearLayout.getScrollX());
                        mScroller.startScroll(mLinearLayout.getScrollX(), 0
                                , -mLinearLayout.getScrollX() + displayUtils.dip2px(VIEW_LAYOUT_WIDTH), 0);
                        mCurrentX = displayUtils.dip2px(VIEW_LAYOUT_WIDTH);
                        invalidate();
                    }

                    if (mCurrentX < displayUtils.dip2px(VIEW_LAYOUT_WIDTH) / 2) {
//                        LogUtils.d("CstSideslip", "onTouchEvent: ACTION_UP hide" + mLinearLayout.getScrollX() + "===" + mCurrentX);
                        mScroller.startScroll(mLinearLayout.getScrollX(), 0, -mLinearLayout.getScrollX(), 0);
                        mCurrentX = 0;
                        invalidate();
                    }
                }

//                LogUtils.d("CstSideslip", "onTouchEvent: ACTION_CANCEL" + x + "----" + y);
                break;
        }
        mLastX = x;
        mLastY = y;
        return true;
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            isScrolling = true;
            mLinearLayout.scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
            invalidate();
        } else {
            if (mCurrentX == -displayUtils.dip2px(VIEW_LAYOUT_WIDTH)) {
                mScroller.startScroll(mCurrentX, 0, displayUtils.dip2px(VIEW_LAYOUT_WIDTH), 0);
                mCurrentX = 0;
                invalidate();
                if (mMobiListListener != null) {
                    mMobiListListener.onRead();
                }
            }
            if (mCurrentX == displayUtils.dip2px(VIEW_LAYOUT_WIDTH)) {
                ViewCompat.setTranslationZ(rightBtn, 1f);
            }
        }
    }

    //只有移动超过offset过后，才认为是个侧滑动作，进行事件拦截。
    private Boolean isSideslipMove(float x, float y) {
        if (
//                Math.abs(mDownY - mLastY) <= VERTICAL_MOVE_OFFSET &&
//                        Math.abs(mDownY - y) <= DisplayUtils.dip2px(VERTICAL_MOVE_OFFSET) &&
                Math.abs(mDownX - x) >= displayUtils.dip2px(HORIZONTAL_MOVE_OFFSET)) {
            return true;
        }
        return false;
    }

    public void setMobiListListener(IOnListListener listener) {
        mMobiListListener = listener;
    }

    public void reset() {
        mScroller.startScroll(mCurrentX, 0, -mCurrentX, 0);
        mCurrentX = 0;
        ViewCompat.setTranslationZ(rightBtn, -1f);
        invalidate();
    }

    public int getmCurrentX() {
        return mCurrentX;
    }
}
