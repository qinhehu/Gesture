package com.example.qinhe.gesture.Gesture;


import android.support.annotation.IdRes;
import android.view.View;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.HashMap;
import java.util.Map;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Created by QinHe on 2017/2/21.
 */

@Documented
@Target(FIELD)
@Retention(RUNTIME)
public @interface TurnLeftSideslip {
    boolean autoClose() default false;
    @IdRes int[] value();
}
