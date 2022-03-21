package com.chocho.finest

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.RelativeLayout

class OverlayLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr) {
    init {}

    override fun onTouchEvent(event: MotionEvent?): Boolean {
//        super.onTouchEvent(event)
        return false
    }
}