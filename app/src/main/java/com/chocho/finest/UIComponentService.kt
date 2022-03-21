package com.chocho.finest

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.*
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.graphics.Rect
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.RelativeLayout
import android.widget.Toast
import com.chocho.finest.features.FeatureDetectorManager
import com.chocho.finest.lbs.FirebaseUploader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import java.util.*
import kotlin.coroutines.CoroutineContext


class UIComponentService : AccessibilityService(), CoroutineScope {

    val TAG = "UICOMP"
    private val PREF_NAME = "feature-extraction"
    private var PRIVATE_MODE = 0

    var windowManager: WindowManager? = null
    lateinit var mScreenOffReceiver: ScreenOffReceiver

    data class PrevSourceCtx (
        var node: AccessibilityNodeInfo,
        val currPackage: String,
        var ovKey: Int
    )

    lateinit var context: Context

    override val coroutineContext: CoroutineContext =
        Dispatchers.Main + SupervisorJob()

    override fun onCreate() {
        Log.d(TAG, "onCreate")
        context = this

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val sharedPrefs = getSharedPreferences(PREF_NAME, PRIVATE_MODE)

        // Set up Firebase uploader that uploads zip files of raw data using username
        val username = sharedPrefs.getString("username", null)
        FirebaseUploader.getInstance().init(context, username)
        val zips = getLbsContext().lbsDir.listFiles { file -> file.length() > 0 && file.name.matches("^.*.zip$".toRegex())}
        FirebaseUploader.getInstance().enqueue(zips)
    }

    override fun onDestroy() {
        // Detach all receivers and jobs on destroy
        super.onDestroy()
        coroutineContext[Job]!!.cancel()
        unregisterReceiver(mScreenOffReceiver)
        FeatureDetectorManager.instance.onDestroy()
    }
    override fun onServiceConnected() {
        instance = this

        //Register screen off receiver to detect when the phone screen is off
        mScreenOffReceiver = ScreenOffReceiver()
        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_SCREEN_OFF)
        registerReceiver(mScreenOffReceiver, filter)

        // Reset all ESM condition checks to False every 3 hours
        // Because we restrict each condition to appear only once in 3hr window
        // Alarm receiver is used to count 3 hours
        val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val now = Calendar.getInstance()
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        while (cal.timeInMillis < now.timeInMillis) cal.add(Calendar.HOUR_OF_DAY, 3)

        val alarmReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                Log.i(TAG, "BroadcastReceiver::OnReceive()")
                val sharedPrefs = getSharedPreferences(PREF_NAME, PRIVATE_MODE)
                val editor = sharedPrefs.edit()
                editor.putBoolean("esmFacebook1", false)        // <= 30s
                editor.putBoolean("esmFacebook2", false)        // <= 5min
                editor.putBoolean("esmFacebook3", false)        // > 5min
                editor.putBoolean("esmInstagram1", false)        // <= 30s
                editor.putBoolean("esmInstagram2", false)        // <= 5min
                editor.putBoolean("esmInstagram3", false)        // > 5min
                editor.putBoolean("esmYouTube1", false)        // <= 30s
                editor.putBoolean("esmYouTube2", false)        // <= 5min
                editor.putBoolean("esmYouTube3", false)        // > 5min
                editor.putBoolean("esmKakaoTalk1", false)        // <= 30s
                editor.putBoolean("esmKakaoTalk2", false)        // <= 5min
                editor.putBoolean("esmKakaoTalk3", false)        // > 5min
                editor.putLong("lastUpdate", System.currentTimeMillis())
                editor.apply()
            }
        }

        registerReceiver(alarmReceiver, IntentFilter("com.alarm.example"))
        val pi = PendingIntent.getBroadcast(context, 333, Intent("com.alarm.example"), PendingIntent.FLAG_UPDATE_CURRENT)
        alarmMgr.setRepeating(AlarmManager.RTC_WAKEUP, cal.timeInMillis, AlarmManager.INTERVAL_HOUR * 3, pi)
    }
    override fun onInterrupt() {
//        200711 dhkim: Do nothing
//        TODO("Not yet implemented")
    }

    var nodes: MutableList<AccessibilityNodeInfo> = mutableListOf()
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // This function is called on every accessibility event
        val source = event.source
        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_SCROLLED,
            AccessibilityEvent.TYPE_VIEW_FOCUSED,
            AccessibilityEvent.TYPE_VIEW_CLICKED,
            AccessibilityEvent.TYPE_WINDOWS_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val root: AccessibilityNodeInfo = rootInActiveWindow ?: return
                if (event.packageName != null && event.packageName != "com.chocho.finest") {
                    // For the stated AccessibilityEvent types,
                    // if the package name is neither our own service nor null
                    // Call handleLayoutUpdate with the current package name and class information
                    var currPackage = event.packageName.toString()
                    var currClass = ""
                    if (event.className != null) currClass = event.className.toString()
                    handleLayoutUpdate(context, root, source, currPackage, currClass, event)
                }
            }
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        return super.onUnbind(intent)
    }

    companion object {
        private lateinit var instance: UIComponentService

        val mInstance: UIComponentService
            get() {
                if (instance == null) {
                    instance = UIComponentService()
                }
                return instance
            }
    }
}