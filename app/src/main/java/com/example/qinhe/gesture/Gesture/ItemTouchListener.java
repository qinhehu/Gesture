package com.example.qinhe.gesture.Gesture;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.util.ArrayMap;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Scroller;

import com.example.qinhe.gesture.DisplayUtils;
import com.example.qinhe.gesture.R;

/**
 * Created by QinHe on 2017/2/21.
 */

public class ItemTouchListener implements RecyclerView.OnItemTouchListener {

    private static final int SIDESLIP = 0;
    private static final int LONG_PRESS_SCROLL = 1;
    private static final int LONG_PRESS = 2;

    private static final int LONGPRESS_TIMEOUT = ViewConfiguration.getLongPressTimeout();
    private static final int TAP_TIMEOUT = ViewConfiguration.getTapTimeout();

    private static final float VERTICAL_MOVE_OFFSET = 10;//手势上下浮动
    private static final float HORIZONTAL_MOVE_OFFSET = 10;//手势左右浮动

    private static final float VIEW_LAYOUT_WIDTH = 70;

    private int mTouchSlopSquare;
    private float mDownFocusX;
    private float mDownFocusY;
    private float mMarkFocusY;

    private Handler mHandler;

    private int mCurrentX;

    private boolean isScrolling;
    private boolean isOnceEventFlow;
    private boolean longPressDraging;

    private boolean leftSideslip = false;
    private boolean leftAutoClose = false;
    private boolean rightSideslip = false;
    private boolean longPress = false;
    private boolean drag = false;
    private boolean swap = false;

    private View mView;
    private View mLastTargetView;
    private RecyclerView mRecyclerView;


    @NonNull
    private Scroller mScroller;
    @NonNull
    private DisplayUtils displayUtils;

    public ItemTouchListener(Context context) {
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
                    resetSlidslipView();
                    return false;
                }
                mView = rv.findChildViewUnder(x, y);

                if (longPress) {
                    mHandler.removeMessages(LONG_PRESS);
                    mHandler.sendEmptyMessageAtTime(LONG_PRESS, ev.getDownTime()
                            + TAP_TIMEOUT + LONGPRESS_TIMEOUT);
                }

                break;
            case MotionEvent.ACTION_MOVE:

                if (longPressDraging) {
                    mRecyclerView = rv;
                    return true;
                }

                final int deltaX = (int) (focusX - mDownFocusX);
                final int deltaY = (int) (focusY - mDownFocusY);
                int distance = (deltaX * deltaX) + (deltaY * deltaY);
                if (distance > mTouchSlopSquare) {

                    mHandler.removeMessages(LONG_PRESS);

                    if (mView == null) {
                        return false;
                    }

                    if (leftSideslip || rightSideslip) {
                        if (isSideslipMove(x, y)) {
                            return true;
                        }
                    }
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if (longPressDraging) {
                    resetLongPressView();
                }
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

                if (longPressDraging) {
                    rv.getLayoutManager().endAnimation(mView);
                    //使用scrollTo的话，可以通过findChildViewUnder找到targetview，但scroll的绘制无法超过自身view大小。
                    //用TranslationY的话，当motion移动至view上方时，无法通过findChildViewUnder找到targetview
//                    mView.scrollTo(0, (int) mDownFocusY - y);
                    mView.setTranslationY(0);
                    if (mDownFocusY < mView.getY() + mView.getHeight() && mDownFocusY > mView.getY()) {
                        mMarkFocusY = mDownFocusY - mView.getY();
                    }
                    float displacement = y - mView.getY() - mMarkFocusY;
                    mView.setTranslationY(displacement);
                    int dragPosition = rv.getChildLayoutPosition(mView);

                    if (mLastTargetView != null && mLastTargetView != mView) {
                        mLastTargetView.setBackground(null);
                        mLastTargetView.setPadding(0, 0, 0, 0);
                    }

                    if (mView.getY() > rv.getHeight() - mView.getMeasuredHeight()) {
                        rv.scrollBy(0, 20);
                    }
                    if (mView.getY() < mView.getMeasuredHeight()) {
                        rv.scrollBy(0, -20);
                    }

                    int targetPosition = chooseDropTarget(rv, dragPosition, y);

                    if (swap) {
                        mLastTargetView = rv.getLayoutManager().findViewByPosition(targetPosition);
                        if (mLastTargetView != null && dragPosition != targetPosition) {
                            mLastTargetView.setBackground(mLastTargetView.getContext().getResources().getDrawable(R.drawable.red_shape));

                            if (dragPosition != targetPosition && targetPosition != -1) {
                                if (Math.abs(dragPosition - targetPosition) == 2) {
                                    if (dragPosition < targetPosition) {
                                        rv.getAdapter().notifyItemMoved(dragPosition
                                                , targetPosition - 1);
                                    } else {
                                        rv.getAdapter().notifyItemMoved(dragPosition
                                                , targetPosition + 1);
                                    }
                                    if (dragPosition == 0) {
                                        rv.scrollToPosition(0);
                                    }
                                } else {
                                    if (targetPosition == 0 && y < 0) {
                                        rv.getAdapter().notifyItemMoved(dragPosition, 0);
                                        rv.scrollToPosition(0);
                                    }
                                }
                            }
                        } else {
                            if (dragPosition == rv.getAdapter().getItemCount() - 2
                                    && targetPosition == rv.getAdapter().getItemCount()) {
                                rv.getAdapter().notifyItemMoved(dragPosition, rv.getAdapter().getItemCount() - 1);
                            }
                        }
                    }
                }

                if (!isScrolling && isOnceEventFlow && !longPressDraging) {
                    //手指向左移动，view左移动，触发右边事件
                    if (mDownFocusX - x > 0 && Math.abs(x - mDownFocusX) <= displayUtils.dip2px(VIEW_LAYOUT_WIDTH)
                            && mView.getScrollX() != displayUtils.dip2px(VIEW_LAYOUT_WIDTH) && rightSideslip) {
                        mView.scrollTo((int) (mDownFocusX - x), 0);
                        mCurrentX = (int) (mDownFocusX - x);
                    }
                    //手指向右移动，view右移动，触发左边事件
                    if (mDownFocusX - x < -displayUtils.dip2px(VIEW_LAYOUT_WIDTH) / 2 &&
                            mCurrentX == 0 || mCurrentX == displayUtils.dip2px(VIEW_LAYOUT_WIDTH) && leftSideslip && leftAutoClose) {
                        if (mCurrentX == displayUtils.dip2px(VIEW_LAYOUT_WIDTH)) {
//                            ViewCompat.setTranslationZ(rightBtn, -1f);
                        }
                        mScroller.startScroll(mCurrentX, 0, -displayUtils.dip2px(VIEW_LAYOUT_WIDTH), 0);
                        mCurrentX += -displayUtils.dip2px(VIEW_LAYOUT_WIDTH);
                        mHandler.sendEmptyMessage(SIDESLIP);
                    }
                    if (x - mDownFocusX > 0 &&
                            Math.abs(x - mDownFocusX) <= displayUtils.dip2px(VIEW_LAYOUT_WIDTH)
                            && mView.getScrollX() != 0 - displayUtils.dip2px(VIEW_LAYOUT_WIDTH) && leftSideslip && !leftAutoClose) {
                        if (mCurrentX == displayUtils.dip2px(VIEW_LAYOUT_WIDTH)) {
//                            ViewCompat.setTranslationZ(rightBtn, -1f);
                        }
                        mView.scrollTo((int) (mDownFocusX - x), 0);
                        mCurrentX = (int) (mDownFocusX - x);
                    }
                }

                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:

                if (mView.getScrollX() > 0) {
                    if (mCurrentX >= displayUtils.dip2px(VIEW_LAYOUT_WIDTH) / 2) {
                        mScroller.startScroll(mView.getScrollX(), 0
                                , -mView.getScrollX() + displayUtils.dip2px(VIEW_LAYOUT_WIDTH), 0);
                        mCurrentX = displayUtils.dip2px(VIEW_LAYOUT_WIDTH);
                        mHandler.sendEmptyMessage(SIDESLIP);
                    }

                    if (mCurrentX < displayUtils.dip2px(VIEW_LAYOUT_WIDTH) / 2) {
                        mScroller.startScroll(mView.getScrollX(), 0, -mView.getScrollX(), 0);
                        mCurrentX = 0;
                        mHandler.sendEmptyMessage(SIDESLIP);
                    }
                }

                if (longPressDraging) {
                    resetLongPressView();
                }
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

    private void reset() {

        resetLongPressView();
        resetSlidslipView();
    }

    private void resetLongPressView() {
        longPressDraging = false;
        mView.scrollTo(0, 0);
        mView.setTranslationY(0);
        ViewCompat.setTranslationZ(mView, 0);
        mView.setBackground(null);
        mView.setPadding(0, 0, 0, 0);
        if (mLastTargetView != null) {
            mLastTargetView.setBackground(null);
            mLastTargetView.setPadding(0, 0, 0, 0);
            mLastTargetView = null;
        }
        mView.setScaleX(1f);
        mView.setScaleY(1f);
        mView = null;
    }

    private void resetSlidslipView() {
        mScroller.startScroll(mCurrentX, 0, -mCurrentX, 0);
        mCurrentX = 0;
        mHandler.sendEmptyMessage(SIDESLIP);
    }

    private int chooseDropTarget(RecyclerView rv, int dragPosition, int curY) {
        LinearLayoutManager layoutManager = ((LinearLayoutManager) rv.getLayoutManager());
        int firstVisiblePosition = layoutManager.findFirstVisibleItemPosition();

        if (mView.getY() < 0 && firstVisiblePosition == 0) {
            return firstVisiblePosition;
        }

        float referenceUp = mView.getY() - mView.getTranslationY();
        if ((referenceUp > curY) && mView.getTranslationY() < 0) {
            if (referenceUp - curY > mView.getHeight()) {
                return dragPosition - 2;
            }
            if (referenceUp - curY < mView.getHeight() && referenceUp - curY > 0) {
                return dragPosition - 1;
            }
        }
        float referenceDown = mView.getY() - mView.getTranslationY() + mView.getHeight();
        if ((referenceDown < curY) && mView.getTranslationY() > 0) {
            if (curY - referenceDown > mView.getHeight()) {
                return dragPosition + 2;
            }
            if (curY - referenceDown < mView.getHeight() && curY - referenceDown > 0) {
                return dragPosition + 1;
            }
        }

        return -1;
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
                        if (leftAutoClose) {
                            if (mCurrentX == -displayUtils.dip2px(VIEW_LAYOUT_WIDTH)) {
                                mScroller.startScroll(mCurrentX, 0, displayUtils.dip2px(VIEW_LAYOUT_WIDTH), 0);
                                mHandler.sendEmptyMessage(SIDESLIP);
                                mCurrentX = 0;
                                isOnceEventFlow = false;
                                // execute event
                            } else {
                                isScrolling = false;
                                if (mCurrentX == 0) {
                                    mView = null;
                                }
                            }
                        } else {
                            isScrolling = false;
                            if (mCurrentX == 0) {
                                mView = null;
                            }
                        }
                    }
                    break;
                case LONG_PRESS_SCROLL:
                    if (mScroller.computeScrollOffset()) {
                        mRecyclerView.scrollBy(mScroller.getCurrX(), mScroller.getCurrY());
                        mRecyclerView.invalidate();
                        mHandler.sendEmptyMessage(LONG_PRESS_SCROLL);
                    }
                    break;
                case LONG_PRESS:
                    mView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                    if (drag) {
                        ViewCompat.setTranslationZ(mView, 1000);
                        mView.setBackground(mView.getContext().getResources().getDrawable(R.drawable.shape));
                        ObjectAnimator animator = ObjectAnimator.ofFloat(mView, "mview", 1f, 0.8f)
                                .setDuration(300);
                        animator.start();
                        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(ValueAnimator animation) {
                                float value = (Float) animation.getAnimatedValue();
                                mView.setScaleX(value);
                                mView.setScaleY(value);
                            }
                        });
                        longPressDraging = true;
                    } else {
                        mView = null;
                        Log.d("event", "handleMessage: 响应长按事件");
                    }

                    break;
            }
        }
    }

    public void setLimitMap(ArrayMap arrayMap) {
        Object value = arrayMap.get("TurnLeftSideslip");
        if (value != null) {
            if (value instanceof TurnLeftSideslip) {
                leftSideslip = true;
                leftAutoClose = ((TurnLeftSideslip) value).autoClose();
            }
        }
        value = arrayMap.get("TurnRightSideslip");
        if (value != null) {
            if (value instanceof TurnRightSideslip) {
                rightSideslip = true;
            }
        }
        value = arrayMap.get("LongTouch");
        if (value != null) {
            if (value instanceof LongTouch) {
                longPress = true;
            }
        }
        value = arrayMap.get("Drag");
        if (value != null) {
            if (value instanceof Drag) {
                drag = true;
            }
        }
        value = arrayMap.get("Drag");
        if (value != null) {
            if (value instanceof SWAP) {
                swap = true;
            }
        }
    }
}
