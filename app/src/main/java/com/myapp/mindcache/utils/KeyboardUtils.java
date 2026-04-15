package com.myapp.mindcache.utils;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

public class KeyboardUtils {

    private KeyboardUtils() { }

    // Скрыть клавиатуру для определенного View
    public static void hideKeyboard(View view) {
        if (view == null) return;

        InputMethodManager imm = (InputMethodManager) view.getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    // Скрыть клавиатуру при касании вне EditText (для использования в dispatchTouchEvent)
    public static boolean dispatchTouchEvent(View focusView, MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN && focusView instanceof android.widget.EditText) {
            android.graphics.Rect outRect = new android.graphics.Rect();
            focusView.getGlobalVisibleRect(outRect);
            if (!outRect.contains((int) ev.getRawX(), (int) ev.getRawY())) {
                focusView.clearFocus();
                hideKeyboard(focusView);
            }
        }
        return false;
    }
}