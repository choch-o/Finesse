package com.chocho.finest;

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log


class ScreenOffReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_SCREEN_OFF) {
            Log.e(TAG, "Phone locked / Screen OFF")
            handleScreenOff(context)
        }
    }

    companion object {
        const val TAG = "ScreenOffReceiver"
    }
}
