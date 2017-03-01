package com.example.qinhe.gesture.Gesture;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.v4.util.ArrayMap;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Scroller;

import com.example.qinhe.gesture.DisplayUtils;
import com.example.qinhe.gesture.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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

    private int mTouchSlopSquare;
    private int mMinimumFlingVelocity;
    private int mMaximumFlingVelocity;

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

    private float leftMarginWidth;
    private float rightMarginWidth;
    private int[] mLeftIntRes;
    private int[] mRightIntRes;
    private List<Integer> mLeftViewWidthList;
    private List<Integer> mRightViewWidthList;
    private List<IEventListener> mLeftEventListeners;
    private List<IEventListener> mRightEventListeners;
    private IDragListener mDragListener;

    private VelocityTracker mVelocityTracker;

    @NonNull
    private Scroller mScroller;
    @NonNull
    private DisplayUtils displayUtils;

    public ItemTouchListener(RecyclerView recyclerView) {
        ViewConfiguration configuration = ViewConfiguration.get(recyclerView.getContext());
        int touchSlop = configuration.getScaledTouchSlop();
        mTouchSlopSquare = touchSlop * touchSlop;
        mMinimumFlingVelocity = configuration.getScaledMinimumFlingVelocity();
        mMaximumFlingVelocity = configuration.getScaledMaximumFlingVelocity();

        mHandler = new GestureHandler();
        isScrolling = false;
        displayUtils = new DisplayUtils(recyclerView.getContext());
        mScroller = new Scroller(recyclerView.getContext(), new AccelerateDecelerateInterpolator());
        mRecyclerView = recyclerView;

        mLeftViewWidthList = new ArrayList<>();
        mRightViewWidthList = new ArrayList<>();
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

        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(ev);

        switch (ev.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:

                mDownFocusX = focusX;
                mDownFocusY = focusY;
                isOnceEventFlow = true;
                if (mView != null) {
                    if (mDownFocusY < mView.getY() + mView.getHeight()
                            && mDownFocusY > mView.getY()) {
                        if (mLeftEventListeners != null) {
                            if (mCurrentX < 0
                                    && mLeftViewWidthList.size() == mLeftEventListeners.size()) {
                                int width = 0;
                                for (int i = 0; i < mLeftViewWidthList.size(); i++) {
                                    width += mLeftViewWidthList.get(i);
                                    if (mDownFocusX < width) {
                                        mLeftEventListeners.get(i)
                                                .onClickListen(mRecyclerView.getChildLayoutPosition(mView));
                                        break;
                                    }
                                }
                            }
                        }
                        if (mRightEventListeners != null) {
                            if (mCurrentX > 0
                                    && mRightViewWidthList.size() == mRightEventListeners.size()) {
                                int width = 0;
                                for (int i = mRightViewWidthList.size() - 1; i >= 0; i--) {
                                    width += mRightViewWidthList.get(i);
                                    if (mDownFocusX > mView.getMeasuredWidth() - width) {
                                        mRightEventListeners.get(i)
                                                .onClickListen(mRecyclerView.getChildLayoutPosition(mView));
                                        break;
                                    }
                                }
                            }
                        }
                    }

                    resetSlidslipView();
                    return false;
                }

                mView = rv.findChildViewUnder(x, y);
                if (mLeftIntRes != null) {
                    for (int i = 0; i < mLeftIntRes.length; i++) {
                        int width = mView.findViewById(mLeftIntRes[i]).getMeasuredWidth();
                        mLeftViewWidthList.add(width);
                        leftMarginWidth += width;
                    }
                }
                if (mRightIntRes != null) {
                    for (int i = 0; i < mRightIntRes.length; i++) {
                        int width = mView.findViewById(mRightIntRes[i]).getMeasuredWidth();
                        mRightViewWidthList.add(width);
                        rightMarginWidth += width;
                    }
                }

                if (longPress) {
                    mHandler.removeMessages(LONG_PRESS);
                    mHandler.sendEmptyMessageAtTime(LONG_PRESS, ev.getDownTime()
                            + TAP_TIMEOUT + LONGPRESS_TIMEOUT);
                }

                break;
            case MotionEvent.ACTION_MOVE:

                if (longPressDraging) {

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
                    mView.setTranslationY(0);
                    if (mDownFocusY < mView.getY() + mView.getHeight() && mDownFocusY > mView.getY()) {
                        mMarkFocusY = mDownFocusY - mView.getY();
                    }
                    float displacement = y - mView.getY() - mMarkFocusY;
                    mView.setTranslationY(displacement);
                    int dragPosition = rv.getChildLayoutPosition(mView);


                    if (mView.getY() > rv.getHeight() - mView.getMeasuredHeight()) {
                        rv.scrollBy(0, 20);
                    }
                    if (mView.getY() < mView.getMeasuredHeight()) {
                        rv.scrollBy(0, -20);
                    }

                    int targetPosition = chooseDropTarget(rv, dragPosition, y);
                    Log.d("dragPosition", "onTouchEvent: " + dragPosition + "====" + targetPosition);
                    if (mLastTargetView != null && mLastTargetView != mView) {
                        mLastTargetView.setBackground(null);
                        mLastTargetView.setPadding(0, 0, 0, 0);
                    }
                    mLastTargetView = rv.getLayoutManager().findViewByPosition(targetPosition);
                    if (mLastTargetView != null && dragPosition != targetPosition) {
                        mLastTargetView.setBackground(mLastTargetView.getContext().getResources().getDrawable(R.drawable.red_shape));

                        if (dragPosition != targetPosition && targetPosition != -1 && swap) {
                            if (Math.abs(dragPosition - targetPosition) == 2) {
                                if (dragPosition < targetPosition) {
                                    if (mDragListener != null) {
                                        mDragListener.onSwapListener(dragPosition, targetPosition - 1);
                                    }
                                    rv.getAdapter().notifyItemMoved(dragPosition
                                            , targetPosition - 1);
                                } else {
                                    if (mDragListener != null) {
                                        mDragListener.onSwapListener(dragPosition, targetPosition + 1);
                                    }
                                    rv.getAdapter().notifyItemMoved(dragPosition
                                            , targetPosition + 1);
                                }
                                if (dragPosition == 0) {
                                    rv.scrollToPosition(0);
                                }
                            } else {
                                if (targetPosition == 0 && y < 0) {
                                    if (mDragListener != null) {
                                        mDragListener.onSwapListener(dragPosition, 0);
                                    }
                                    rv.getAdapter().notifyItemMoved(dragPosition, 0);
                                    rv.scrollToPosition(0);
                                }
                            }
                        }
                    } else {
                        if (dragPosition == rv.getAdapter().getItemCount() - 2
                                && targetPosition == rv.getAdapter().getItemCount() && swap) {
                            if (mDragListener != null) {
                                mDragListener.onSwapListener(dragPosition, rv.getAdapter().getItemCount() - 1);
                            }
                            rv.getAdapter().notifyItemMoved(dragPosition, rv.getAdapter().getItemCount() - 1);
                        }
                    }
                    break;
                }

                if (!isScrolling && isOnceEventFlow) {

                    //手指向左移动，view左移动，触发右边事件
                    if (mDownFocusX - x > 0 && Math.abs(x - mDownFocusX) <= displayUtils.dip2px(rightMarginWidth)
                            && mView.getScrollX() != displayUtils.dip2px(rightMarginWidth) && rightSideslip) {
                        mView.scrollTo((int) (mDownFocusX - x), 0);
                        mCurrentX = (int) (mDownFocusX - x);
                    }
                    //手指向右移动，view右移动，触发左边事件
                    if (mDownFocusX - x < -displayUtils.dip2px(leftMarginWidth) / 2 &&
                            (mCurrentX == 0 || mCurrentX == displayUtils.dip2px(leftMarginWidth)) && leftSideslip && leftAutoClose) {
                        if (mCurrentX == displayUtils.dip2px(leftMarginWidth)) {
//                            ViewCompat.setTranslationZ(rightBtn, -1f);
                        }
                        mScroller.startScroll(mCurrentX, 0, -displayUtils.dip2px(leftMarginWidth), 0);
                        mCurrentX += -displayUtils.dip2px(leftMarginWidth);
                        mHandler.sendEmptyMessage(SIDESLIP);
                    }
                    if (x - mDownFocusX > 0 &&
                            Math.abs(x - mDownFocusX) <= displayUtils.dip2px(leftMarginWidth)
                            && mView.getScrollX() != 0 - displayUtils.dip2px(leftMarginWidth) && leftSideslip && !leftAutoClose) {
                        if (mCurrentX == displayUtils.dip2px(leftMarginWidth)) {
//                            ViewCompat.setTranslationZ(rightBtn, -1f);
                        }
                        mView.scrollTo((int) (mDownFocusX - x), 0);
                        mCurrentX = (int) (mDownFocusX - x);
                    }
                }

                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:

                final VelocityTracker velocityTracker = mVelocityTracker;
                final int pointerId = event.getPointerId(0);
                velocityTracker.computeCurrentVelocity(1000, mMaximumFlingVelocity);
                final float velocityY = velocityTracker.getYVelocity(pointerId);
                final float velocityX = velocityTracker.getXVelocity(pointerId);

                mHandler.removeMessages(SIDESLIP);
                if ((Math.abs(velocityY) > mMinimumFlingVelocity)
                        || (Math.abs(velocityX) > mMinimumFlingVelocity) && !longPressDraging) {
                    if (velocityX < -1000 || velocityX > 1000) {
                        int width = 0;
                        if (velocityX < 0 && rightSideslip) {
                            width = displayUtils.dip2px(rightMarginWidth);
                        } else {
                            if (velocityX > 0 && leftSideslip) {
                                width = -displayUtils.dip2px(leftMarginWidth);
                            }
                        }
                        mScroller.startScroll(mView.getScrollX(), 0
                                , -mView.getScrollX() + width, 0);
                        mCurrentX = width;

                        mHandler.sendEmptyMessage(SIDESLIP);
                    } else {
                        mScroller.startScroll(mView.getScrollX(), 0, -mView.getScrollX(), 0);
                        mCurrentX = 0;
                        mHandler.sendEmptyMessage(SIDESLIP);
                    }
                } else {
                    if (!longPressDraging) {
                        mScroller.startScroll(mView.getScrollX(), 0, -mView.getScrollX(), 0);
                        mCurrentX = 0;
                        mHandler.sendEmptyMessage(SIDESLIP);
                    }
                }

                if (longPressDraging) {
                    if (mDragListener != null) {
                        mDragListener.onChoseListener(rv.getChildLayoutPosition(mView), rv.getChildLayoutPosition(mLastTargetView));
                    }
                    resetLongPressView();
                }

                if (mVelocityTracker != null) {
                    // This may have been cleared when we called out to the
                    // application above.
                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
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
        leftMarginWidth = 0;
        rightMarginWidth = 0;
        mLeftViewWidthList.clear();
        mRightViewWidthList.clear();
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
                            if (mCurrentX == -displayUtils.dip2px(leftMarginWidth)) {
                                mScroller.startScroll(mCurrentX, 0, displayUtils.dip2px(leftMarginWidth), 0);
                                mHandler.sendEmptyMessage(SIDESLIP);
                                mCurrentX = 0;
                                isOnceEventFlow = false;
                                // execute event
                                for (int i = 0; i < mLeftEventListeners.size(); i++) {
                                    mLeftEventListeners.get(i)
                                            .onClickListen(mRecyclerView.getChildLayoutPosition(mView));
                                }
                            } else {
                                isScrolling = false;
                                if (mCurrentX == 0) {
                                    mView = null;
                                    leftMarginWidth = 0;
                                    rightMarginWidth = 0;
                                    mLeftViewWidthList.clear();
                                    mRightViewWidthList.clear();
                                }
                            }
                        } else {
                            isScrolling = false;
                            if (mCurrentX == 0) {
                                mView = null;
                                leftMarginWidth = 0;
                                rightMarginWidth = 0;
                                mLeftViewWidthList.clear();
                                mRightViewWidthList.clear();
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
                        final ObjectAnimator animator = ObjectAnimator.ofFloat(mView, "mview", 1f, 0.8f)
                                .setDuration(300);
                        animator.start();
                        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(ValueAnimator animation) {
                                float value = (Float) animation.getAnimatedValue();
                                if (mView == null) {
                                    animator.cancel();
                                } else {
                                    mView.setScaleX(value);
                                    mView.setScaleY(value);
                                }
                            }
                        });
                        longPressDraging = true;
                    } else {
                        mView = null;
                        leftMarginWidth = 0;
                        rightMarginWidth = 0;
                        mLeftViewWidthList.clear();
                        mRightViewWidthList.clear();
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
                mLeftIntRes = ((TurnLeftSideslip) value).value();
//                setLeftView(((TurnLeftSideslip) value).value());
            }
        }
        value = arrayMap.get("TurnRightSideslip");
        if (value != null) {
            if (value instanceof TurnRightSideslip) {
                rightSideslip = true;
                mRightIntRes = ((TurnRightSideslip) value).value();
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
//                swap = ((Drag) value).swap();
                swap = true;
            }
        }
    }

    public void setLeftClickListener(List<IEventListener> eventListeners) {
        mLeftEventListeners = eventListeners;
    }

    public void setRightClickListener(List<IEventListener> eventListeners) {
        mRightEventListeners = eventListeners;
    }

    public void setDragListener(IDragListener dragListener) {
        mDragListener = dragListener;
    }
}
