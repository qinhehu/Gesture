package com.example.qinhe.gesture;

import android.content.Context;

public class DisplayUtils {

    private Context mContext;

    public DisplayUtils(Context context) {
        mContext = context;
    }

    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    public int dip2px(float dpValue) {
        if (null == mContext) return (int) dpValue;
        final float scale = mContext.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    /**
     * 根据手机的分辨率从 px(像素) 的单位 转成为 dp
     */
    public int px2dip(float pxValue) {
        if (null == mContext) return (int) pxValue;
        final float scale = mContext.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

    /**
     * sp转px（正常字体下，1sp=1dp）
     *
     * @param spValue
     * @return
     */
    public int sp2px(float spValue) {
        if (null == mContext) return (int) spValue;
        final float fontScale = mContext.getResources().getDisplayMetrics().scaledDensity;
        return (int) (spValue * fontScale + 0.5f);
    }

    /**
     * sp转dp
     *
     * @param spValue
     * @return
     */
    public int sp2dp(float spValue) {
        int sp2Px = sp2px(spValue);
        return px2dip(sp2Px);
    }

    /**
     * 将px值转换为sp值，保证文字大小不变
     */
    public int px2sp(float pxValue) {
        if (null == mContext) return (int) pxValue;
        final float fontScale = mContext.getResources().getDisplayMetrics().scaledDensity;
        return (int) (pxValue / fontScale + 0.5f);
    }
}
