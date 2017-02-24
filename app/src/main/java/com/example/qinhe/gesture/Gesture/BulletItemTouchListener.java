package com.example.qinhe.gesture.Gesture;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.HapticFeedbackConstants;
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

import java.util.ArrayList;
import java.util.List;

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
    private boolean isLongPressDrag;

    private boolean isNeedLongPress = true;
    private boolean isNeedSideslip = true;

    private ArrayList<RecyclerView.ViewHolder> mVisibleViewArray;

    private View mView;
    private View mLastTargetView;

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
                    resetSlidslipView();
                    return false;
                }
                mView = rv.findChildViewUnder(x, y);
                if (isNeedLongPress) {
                    mHandler.removeMessages(LONG_PRESS);
                    mHandler.sendEmptyMessageAtTime(LONG_PRESS, ev.getDownTime()
                            + TAP_TIMEOUT + LONGPRESS_TIMEOUT);
                }
                break;
            case MotionEvent.ACTION_MOVE:

                if (isLongPressDrag) {
//                    rv.setClipChildren(false);
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

                    if (isNeedSideslip) {
                        if (isSideslipMove(x, y)) {
                            return true;
                        }
                    }
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if (isLongPressDrag) {
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

                if (isLongPressDrag) {
                    rv.getLayoutManager().endAnimation(mView);
                    //使用scrollTo的话，可以通过findChildViewUnder找到targetview，但scroll的绘制无法超过自身view大小。
                    //用TranslationY的话，当motion移动至view上方时，无法通过findChildViewUnder找到targetview
//                    mView.scrollTo(0, (int) mDownFocusY - y);
                    float displacement = y - mDownFocusY;
                    mView.setTranslationY(displacement);
                    int dragPosition = rv.getChildLayoutPosition(mView);

                    if (mLastTargetView != null) {
                        mLastTargetView.setBackground(null);
                        mLastTargetView.setPadding(0, 0, 0, 0);
                    }

                    int targetPosition = chooseDropTarget(rv, dragPosition, y);

                    mLastTargetView = rv.getChildAt(targetPosition);
                    if (mLastTargetView != null) {
                        mLastTargetView.setBackground(mLastTargetView.getContext().getResources().getDrawable(R.drawable.red_shape));
                        if (dragPosition != targetPosition && targetPosition != -1 && Math.abs(dragPosition - targetPosition) == 2) {
                            if (dragPosition < targetPosition) {
                                rv.getAdapter().notifyItemMoved(dragPosition
                                        , targetPosition - 1);
                                mDownFocusY += mView.getHeight();
                            } else {

                                rv.getAdapter().notifyItemMoved(dragPosition
                                        , targetPosition + 1);
                                mDownFocusY -= mView.getHeight();
                            }

//                            final RecyclerView.LayoutManager layoutManager = rv.getLayoutManager();
//                            if (layoutManager instanceof ItemTouchHelper.ViewDropHandler) {
//                                ((ItemTouchHelper.ViewDropHandler) layoutManager).prepareForDrop(mView,
//                                        mLastTargetView, x, y);
//                            }
                        }
                    }

                }

                if (!isScrolling && isOnceEventFlow && !isLongPressDrag) {
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

                if (isLongPressDrag) {
                    resetLongPressView();
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

    private void reset() {

        resetLongPressView();
        resetSlidslipView();
    }

    private void resetLongPressView() {
        isLongPressDrag = false;
        mView.scrollTo(0, 0);
        mView.setTranslationY(0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mView.setTranslationZ(0);
        }
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
        if (mVisibleViewArray != null) {
            mVisibleViewArray.clear();
        }
    }

    private void resetSlidslipView() {
        mScroller.startScroll(mCurrentX, 0, -mCurrentX, 0);
        mCurrentX = 0;
        mHandler.sendEmptyMessage(SIDESLIP);
    }


    private int chooseDropTarget(RecyclerView rv, int dragPosition, int curY) {
        LinearLayoutManager layoutManager = ((LinearLayoutManager) rv.getLayoutManager());
        int firstVisiblePosition = layoutManager.findFirstVisibleItemPosition();
        int lastVisiblePosition = layoutManager.findLastVisibleItemPosition();
        if (mVisibleViewArray == null) {
            mVisibleViewArray = new ArrayList<>();
        }
        mVisibleViewArray.clear();
        //如果集合已有数据，则采用进补位的方式。如无则增加
        if (mVisibleViewArray.size() > 0) {
            RecyclerView.ViewHolder viewHolder = mVisibleViewArray.get(0);


            //假设向上滚动，原先第一个view退居第二，最后一个view被移除
            if (viewHolder.getAdapterPosition() > firstVisiblePosition) {
                mVisibleViewArray.add(0, rv.findViewHolderForLayoutPosition(firstVisiblePosition));
                mVisibleViewArray.remove(mVisibleViewArray.size() - 1);
            }
            //假设向下滚动，原先第二个view进军第一，第一个view被移除，同时，新增新的view至堆尾
            if (viewHolder.getAdapterPosition() < firstVisiblePosition) {
                mVisibleViewArray.remove(0);
                mVisibleViewArray.add(mVisibleViewArray.size(), rv.findViewHolderForLayoutPosition(firstVisiblePosition));
            }

        } else {
            int index = 0;
            for (int i = firstVisiblePosition; i < lastVisiblePosition; ++i) {
                mVisibleViewArray.add(index, rv.findViewHolderForLayoutPosition(i));
                index++;
            }
        }


        //处理好集合，开始根据坐标选择viewHolder
        int visibleViewCount = mVisibleViewArray.size();
        float referenceUp = mView.getY() - mView.getTranslationY();
        if ((referenceUp > curY)) {
            if (referenceUp - curY > mView.getHeight()) {
                return dragPosition - 2;
            }
            if (referenceUp - curY < mView.getHeight() && referenceUp - curY > 0) {
                return dragPosition - 1;
            }
        }
        float referenceDown = mView.getY() - mView.getTranslationY() + mView.getHeight();
        if ((referenceDown < curY)) {
            if (curY - referenceDown > mView.getHeight()) {
                return dragPosition + 2;
            }
            if (curY - referenceDown < mView.getHeight() && curY - referenceDown > 0) {
                return dragPosition + 1;
            }
        }

//        int half = visibleViewCount / 2;
//        if (mVisibleViewArray.get(half).itemView.getY() > curY) {
//            for (int i = 0; i < half - 1; ++i) {
//                if (dragPosition == i) {
//                    continue;
//                }
//                View item = mVisibleViewArray.get(i).itemView;
//                if (item.getTop() + item.getHeight() > curY) {
//                    return mVisibleViewArray.get(i).getLayoutPosition();
//                }
//            }
//        } else {
//            for (int i = half - 1; i < visibleViewCount - 1; ++i) {
//                if (dragPosition == i) {
//                    continue;
//                }
//                View item = mVisibleViewArray.get(i).itemView;
//                if (item.getTop() + item.getHeight() > curY) {
//                    return mVisibleViewArray.get(i).getLayoutPosition();
//                }
//            }
//        }
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
                            if (mCurrentX == 0) {
                                mView = null;
                            }
                        }
                    }
                    break;
                case LONG_PRESS:
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        mView.setTranslationZ(1000);
                    }

                    mView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
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
                    isLongPressDrag = true;
                    break;
            }
        }
    }
}
