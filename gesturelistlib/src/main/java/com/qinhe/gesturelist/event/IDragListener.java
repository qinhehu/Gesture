package com.qinhe.gesturelist.event;

/**
 * Created by QinHe on 2017/3/1.
 */

public interface IDragListener {
    void onSwapListener(int fromPosition, int toPosition);

    void onChoseListener(int fromPosition, int toPosition);
}
