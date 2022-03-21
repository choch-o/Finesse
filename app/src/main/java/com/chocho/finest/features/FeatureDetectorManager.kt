package com.chocho.finest.features

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.view.ContextThemeWrapper
import com.chocho.finest.*
import com.chocho.finest.lbs.Lbs.RootLayoutElement
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kr.ac.kaist.nmsl.xdroid.util.Singleton
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.random.Random.Default.nextBoolean


class FeatureDetectorManager private constructor() {
    private val TAG = "FEAT_MGR"

    private var mDetectorMap: HashMap<String, FeatureDetector> = HashMap()

    private var prevPackage: String? = null
    private var trackingPackage : String? = ""        // TODO: Save it as a SharedPrefs item?
    private var backButtonPressed: Boolean = false
    private var notificationPanelOpened: Boolean = false
    private var packagesArray: ArrayList<Pair<String?,Long>> = ArrayList()
    var lastTrackingPackageTime: Long = 0

    private var PRIVATE_MODE = 0
    private val PREF_NAME = "feature-extraction"
    private var sharedPrefs : SharedPreferences? = null

    val db = Firebase.firestore

    var lastUserInteraction = 0.toLong()

    init {
        registerFeatureDetector(INSTAGRAM, InstaFeatureDetector())
        registerFeatureDetector(FACEBOOK, FacebookFeatureDetector())
        registerFeatureDetector(KAKAOTALK, KakaoFeatureDetector())
        registerFeatureDetector(YOUTUBE, YoutubeFeatureDetector())

    }

    fun onDestroy() {
        if (sharedPrefs == null)
            return;

        val editor = sharedPrefs!!.edit()
        editor.putString("trackingPackage", "")
        editor.putString("prevPackage", "")
        editor.apply()
    }

    fun registerFeatureDetector(packageName: String?, detector: FeatureDetector?) {
        if (packageName == null || detector == null) return
        mDetectorMap[packageName] = detector
    }

    fun handleLayoutUpdate(context: Context, packageName: String?, root: RootLayoutElement?, source: AccessibilityNodeInfo?, event: AccessibilityEvent?) {
        if (sharedPrefs == null)
            sharedPrefs = context.getSharedPreferences(PREF_NAME, PRIVATE_MODE)

        val detector = mDetectorMap[packageName]

        var trackingPackage = sharedPrefs!!.getString("trackingPackage", "")

        // App transition detector. Check if a session ended.
        if (packageName == "screenoff" && source == null && event == null) {
            // Screen is off. App transition occurred.
            Log.d(TAG, "-----Screen Off-----")
            checkSessionEnd(context, trackingPackage)
        } else if (packageName.toString().contains("launcher")
            || packageName == "com.samsung.android.spay") {
            // User is in home screen (launcher or Samsung Pay). App transition occurred.
            Log.d(TAG, "-----To Launcher-----")
            checkSessionEnd(context, trackingPackage)
        } else if (packageName == "com.android.systemui") {
            // User opened notification panel.
            if (source?.viewIdResourceName == "com.android.systemui:id/notification_panel") {
                Log.d(TAG, "Notification Panel Opened2")
                notificationPanelOpened = true;
            }
        } else if (packageName == "com.samsung.android.app.cocktailbarservice") {
            // User opened notification panel.
            Log.d(TAG, "Notification Panel Opened1")
            notificationPanelOpened = true;
        } else if (notificationPanelOpened) {
            // User switched to another package through notification panel
            Log.d(TAG, "-----Thru Notification Panel-----")
            if (!packageName.equals(trackingPackage)) checkSessionEnd(context, trackingPackage)
        }

        // If the current package is one of target apps and not being tracked yet
        if (TARGET_APPS.contains(packageName) && trackingPackage != packageName) {
            // End if any undetected tracking session
            if (trackingPackage != "") checkSessionEnd(context, trackingPackage)

            // Start of a new tracking session
            Log.d(TAG, "Start of a tracking session")
            packagesArray = ArrayList()
            val sessionStartTime = System.currentTimeMillis()
            packagesArray.add(Pair(packageName, sessionStartTime))
            lastTrackingPackageTime = sessionStartTime

            // Start all feature detectors
            detector?.onSessionStart()
            // Save the current tracking package and start time in SharedPreferences
            val editor = sharedPrefs!!.edit()
            editor.putString("trackingPackage", packageName)
            editor.putLong("startTime", sessionStartTime)
            editor.apply()
        }

        trackingPackage = sharedPrefs!!.getString("trackingPackage", "")
        // if tracking is ongoing
        if (trackingPackage != "") {
            if (trackingPackage == packageName) {
                lastTrackingPackageTime = System.currentTimeMillis()
                detector?.detect(context, root)
                notificationPanelOpened = false
            } else {
                if (packagesArray.isEmpty() || packagesArray[packagesArray.size - 1].first != packageName)
                    if (packageName != null && packageName != "com.android.systemui")
                        packagesArray.add(Pair(packageName, System.currentTimeMillis()))
            }
        }
    }

    private fun checkSessionEnd(context: Context, trackingPackage: String) {
        if (sharedPrefs == null)
            sharedPrefs = context.getSharedPreferences(PREF_NAME, PRIVATE_MODE)
        if (!trackingPackage.equals("")) {
            Log.d("FINESSE", "End of a tracking session")
            val endDetector = mDetectorMap[trackingPackage]

            // Save the last feature
            var sessionEndTime = System.currentTimeMillis()
            if (lastTrackingPackageTime != 0.toLong()) sessionEndTime = lastTrackingPackageTime
            endDetector?.onSessionEnd(context, sessionEndTime)

            if (endDetector != null) {
                if (sharedPrefs!!.getBoolean("samplingOn", true)) {
                    val lastEsmTime = sharedPrefs!!.getLong("lastEsmTime", 0)
                    if (System.currentTimeMillis() - lastEsmTime > 60 * 1000) // > 1min
                        summarizeSession(context, trackingPackage, sessionEndTime)
                    else Log.d("FINESSE", "cool time")
                } else {
                    // Make the summarize call
                    summarizeSession(context, trackingPackage, sessionEndTime)
                }
            }

            val editor = sharedPrefs!!.edit()
            // Reset tracking package
            editor.putString("trackingPackage", "")
            editor.putString("sessionData", "")
            editor.putLong("startTime", 0)
            editor.apply()

            notificationPanelOpened = false;
        }
    }

    private fun summarizeSession(context: Context, packageName: String, sessionEndTime: Long) {
        val sessionData = sharedPrefs!!.getString("sessionData", "")
        if (sessionData == "") {
            Log.d("FINESSE", "no session data")
            return
        }
        val sessionStart = sharedPrefs!!.getLong("startTime", 0)
        val sessionDuration = sessionEndTime - sessionStart
        if (sessionDuration < 5000) {
            Log.d("FINESSE", "session duration < 5s")
            return
        } // < 5s

        var condition = ""
        when (packageName) {
            FACEBOOK -> condition = "esmFacebook"
            INSTAGRAM -> condition = "esmInstagram"
            KAKAOTALK -> condition = "esmKakaoTalk"
            YOUTUBE -> condition = "esmYouTube"
        }
        if (sessionDuration <= 30 * 1000) {     // <= 30s
            condition += "1"
        } else if (sessionDuration <= 5 * 60 * 1000) {  // <= 5 min
            condition += "2"
        } else {    // > 5 min
            condition += "3"
        }

        var esmStatus = ""
        esmStatus += "Facebook <= 30s: ${sharedPrefs!!.getBoolean("esmFacebook1", false)}\n"
        esmStatus += "Facebook <= 5min: ${sharedPrefs!!.getBoolean("esmFacebook2", false)}\n"
        esmStatus += "Facebook > 5min: ${sharedPrefs!!.getBoolean("esmFacebook3", false)}\n\n"

        esmStatus += "Instagram <= 30s: ${sharedPrefs!!.getBoolean("esmInstagram1", false)}\n"
        esmStatus += "Instagram <= 5min: ${sharedPrefs!!.getBoolean("esmInstagram2", false)}\n"
        esmStatus += "Instagram > 5min: ${sharedPrefs!!.getBoolean("esmInstagram3", false)}\n\n"

        esmStatus += "YouTube <= 30s: ${sharedPrefs!!.getBoolean("esmYouTube1", false)}\n"
        esmStatus += "YouTube <= 5min: ${sharedPrefs!!.getBoolean("esmYouTube2", false)}\n"
        esmStatus += "YouTube > 5min: ${sharedPrefs!!.getBoolean("esmYouTube3", false)}\n\n"

        esmStatus += "KakaoTalk <= 30s: ${sharedPrefs!!.getBoolean("esmKakaoTalk1", false)}\n"
        esmStatus += "KakaoTalk <= 5min: ${sharedPrefs!!.getBoolean("esmKakaoTalk2", false)}\n"
        esmStatus += "KakaoTalk > 5min: ${sharedPrefs!!.getBoolean("esmKakaoTalk3", false)}\n"

        val lastUpdateTime = Date(sharedPrefs!!.getLong("lastUpdate", System.currentTimeMillis()))
        val dateFormat = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.KOREA)
        val lastUpdate = dateFormat.format(lastUpdateTime)

        if (sharedPrefs!!.getBoolean("samplingOn", true)) {
            if (sharedPrefs!!.getBoolean(condition, false)) {
                Log.d("FINESSE", "Condition checked already")
                // Condition has been checked already
                // Just save the data without ESM responses
                val sessionObject = hashMapOf(
                    "sessionData" to sessionData,
                    "sessionStart" to sessionStart,
                    "sessionDuration" to sessionDuration,
                    "esmTime" to System.currentTimeMillis(),
                    "packageLog" to packagesArray.toString(),
                    "esmStatus" to esmStatus,
                    "lastSamplingUpdate" to lastUpdate
                )
                val user = sharedPrefs!!.getString("username", null)
                if (user == null) {
                    Toast.makeText(context, "Enter a username", Toast.LENGTH_SHORT)
                } else {
                    db.collection("/sessions/${user}/$packageName")
                        .add(sessionObject)
                        .addOnSuccessListener { documentReference ->
                            Log.d(
                                "FINESSE",
                                "FIRESTORE: DocumentSnapshot written with ID: ${documentReference.id}"
                            )
                        }
                        .addOnFailureListener { e ->
                            Log.w("FINESSE", "FIRESTORE: Error adding document", e)
                        }
                }
                return
            }
            if (nextBoolean()) {  // 50/50 chance
                Log.d("FINESSE", "50/50: don't po pup")
                // Just save the data without ESM responses
                val sessionObject = hashMapOf(
                    "sessionData" to sessionData,
                    "sessionStart" to sessionStart,
                    "sessionDuration" to sessionDuration,
                    "esmTime" to System.currentTimeMillis(),
                    "packageLog" to packagesArray.toString(),
                    "esmStatus" to esmStatus,
                    "lastSamplingUpdate" to lastUpdate
                )
                val user = sharedPrefs!!.getString("username", null)
                if (user == null) {
                    Toast.makeText(context, "Enter a username", Toast.LENGTH_SHORT)
                } else {
                    db.collection("/sessions/${user}/$packageName")
                        .add(sessionObject)
                        .addOnSuccessListener { documentReference ->
                            Log.d(
                                "FINESSE",
                                "FIRESTORE: DocumentSnapshot written with ID: ${documentReference.id}"
                            )
                        }
                        .addOnFailureListener { e ->
                            Log.w("FINESSE", "FIRESTORE: Error adding document", e)
                        }
                }
                return
            }
        }
        // Show ESM

        val builder = AlertDialog.Builder(context)
        builder.setTitle(context.getString(R.string.esm_header, convertPackageToAppName(packageName)))
        builder.setIcon(R.drawable.ic_finesse)
        builder.setCancelable(false)

        val overlayEsmView = LayoutInflater.from(ContextThemeWrapper(context, R.style.AppTheme))
            .inflate(R.layout.layout_esm, null)
        builder.setView(overlayEsmView)

        val alertDialog = builder.create()
        alertDialog.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        alertDialog.window?.decorView?.apply {
            systemUiVisibility = 0
        }
        alertDialog.window?.addFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)

        alertDialog.show()
        alertDialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)

        val btnSubmit = overlayEsmView.findViewById<MaterialButton>(R.id.btnSubmit)
        val btnSkip = overlayEsmView.findViewById<Button>(R.id.btnSkip)

        val webView = overlayEsmView.findViewById<WebView>(R.id.webview)
        val webSettings = webView.settings
        webView.addJavascriptInterface(WebAppInterface(context, sessionData!!, sessionStart, sessionDuration), "Android")
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.loadWithOverviewMode = true
        webSettings.useWideViewPort = true
        webSettings.setSupportZoom(false)
        webSettings.defaultTextEncodingName = "utf-8"
        webSettings.builtInZoomControls = false
        webSettings.displayZoomControls = false
        webView.requestFocusFromTouch()

        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                consoleMessage?.apply {
                    Log.d("FINESSE", "${message()} -- From line ${lineNumber()} of ${sourceId()}")
                }
                return true
            }
        }

        webView.loadUrl("file:///android_asset/highchart.html")
        webView.setOnClickListener { _ ->
            lastUserInteraction = System.currentTimeMillis()
        }

        checkPageFinished(webView)

        btnSkip.setOnClickListener { view ->
            // Just save the data without ESM responses
            val sessionObject = hashMapOf(
                "sessionData" to sessionData,
                "sessionStart" to sessionStart,
                "sessionDuration" to sessionDuration,
                "esmTime" to System.currentTimeMillis(),
                "packageLog" to packagesArray.toString(),
                "esmStatus" to esmStatus,
                "lastSamplingUpdate" to lastUpdate,
                "skipped" to true
            )
            val user = sharedPrefs!!.getString("username", null)
            if (user == null) {
                Toast.makeText(context, "Enter a username", Toast.LENGTH_SHORT)
            } else {
                db.collection("/sessions/${user}/$packageName")
                    .add(sessionObject)
                    .addOnSuccessListener { documentReference ->
                        Log.d(
                            "FINESSE",
                            "FIRESTORE: DocumentSnapshot written with ID: ${documentReference.id}"
                        )
                    }
                    .addOnFailureListener { e ->
                        Log.w("FINESSE", "FIRESTORE: Error adding document", e)
                    }
            }

            alertDialog.dismiss()
            val editor = sharedPrefs!!.edit()
            editor.putLong("lastEsmTime", System.currentTimeMillis())
            editor.apply()
        }
        btnSubmit.setOnClickListener { view ->
            // Save to Firestore
            webView.evaluateJavascript("javascript:getSelected();") { selectedItems ->
                Log.d("FINESSE", "getSelected(): $selectedItems")


                val sessionObject = hashMapOf(
                    "sessionData" to sessionData,
                    "sessionStart" to sessionStart,
                    "sessionDuration" to sessionDuration,
                    "esmTime" to System.currentTimeMillis(),
                    "packageLog" to packagesArray.toString(),
                    "esmStatus" to esmStatus,
                    "lastSamplingUpdate" to lastUpdate,
                    "regretfulItems" to selectedItems.replace("\\", "").dropLast(1)
                        .replaceFirst("\"", "")
//                    "regretfulItemsReason" to etA4.text.toString()
                )
                val user = sharedPrefs!!.getString("username", null)
                if (user == null) {
                    Toast.makeText(context, "Enter a username", Toast.LENGTH_SHORT)
                } else {
                    db.collection("/sessions/${user}/$packageName")
                        .add(sessionObject)
                        .addOnSuccessListener { documentReference ->
                            Log.d(
                                "FINESSE",
                                "DocumentSnapshot written with ID: ${documentReference.id}"
                            )
                            if (sharedPrefs!!.getBoolean("samplingOn", true)) {
                                val editor = sharedPrefs!!.edit()
                                editor.putBoolean(condition, true)
                                Log.d("FINESSE", "Condition set true: ${condition}")
                                editor.apply()
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.w("FINESSE", "Error adding document", e)
                        }
                }
            }

            // Close dialog
            alertDialog.dismiss()

            val editor = sharedPrefs!!.edit()
            editor.putLong("lastEsmTime", System.currentTimeMillis())
            editor.apply()
        }

        // Hide after some seconds
        val handler = Handler()
        val runnable = Runnable {
            if (alertDialog.isShowing) {
                alertDialog.dismiss()
            }
        }

        alertDialog.setOnDismissListener(DialogInterface.OnDismissListener {
            handler.removeCallbacks(
                runnable
            )
        })

        handler.postDelayed(runnable, 5 * 60 * 1000)    // 5 min
    }

    fun checkPageFinished(webView: WebView) {
        if (webView.contentHeight == 0) {
            //Run off main thread to control delay
            webView.postDelayed(
                { //Load url into the "WebView"
                    webView.loadUrl("file:///android_asset/highchart.html")
                }, //Set 1s delay to give the view a longer chance to load before
                // setting the view (or more likely to display blank)
                1000
            )

            webView.postDelayed(
                {
                    //If view is still blank:
                    if (webView.contentHeight == 0) {
                        //Loop until it works
                        checkPageFinished(webView)
                    }
                }, //Safely loop this function after 1.5s delay if page is not loaded
                1500
            )

        }
    }

    fun convertPackageToAppName (packageName: String) : String {
        var appName = ""
        when (packageName) {
            KAKAOTALK -> appName = "카카오톡"
            INSTAGRAM -> appName = "인스타그램"
            FACEBOOK -> appName = "페이스북"
            YOUTUBE -> appName = "유튜브"
        }
        return appName
    }

    companion object {
        private val singleton: Singleton<FeatureDetectorManager?> =
            object : Singleton<FeatureDetectorManager?>() {
                override fun create(): FeatureDetectorManager {
                    return FeatureDetectorManager()
                }
            }

        val instance: FeatureDetectorManager
            get() = singleton.get()!!
    }

    public class WebAppInterface internal constructor(
        val context: Context,
        val sessionData: String,
        val sessionStart: Long,
        val sessionDuration: Long
    ) {
        @JavascriptInterface
        fun getData(): String {
//            println("GET DATA")
//            println(sessionData)
            if (!sessionData.equals(""))
                return "[" + sessionData.substring(0, sessionData.length-1) + "]"
            else return sessionData
        }

        @JavascriptInterface
        fun getDuration(): Long {
            return sessionDuration
        }

        @JavascriptInterface
        fun getStart(): Long {
            return sessionStart
        }
    }
}