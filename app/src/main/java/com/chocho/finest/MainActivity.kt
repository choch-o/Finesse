package com.chocho.finest

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.opengl.Visibility
import android.os.Bundle
import android.provider.Settings
import android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION
import android.text.TextUtils
import android.util.Log
import android.view.View.GONE
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.chocho.finest.lbs.FirebaseUploader
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.auth.AuthResult
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {
    private val TAG = "FINEST"
    val REQUEST_CODE = 100
    val RC_SIGN_IN = 999
    lateinit var context: Context
    private val tvEnterUsername: TextView by bind(R.id.tvEnterUsername)
    private val etUsername: EditText by bind(R.id.etUsername)
    private val btnSave: MaterialButton by bind(R.id.btnSave)
    private val tvUsername: TextView by bind(R.id.tvUsername)
    private val switchSampling: SwitchMaterial by bind(R.id.switchSampling)
    private val tvEsmStatus: TextView by bind(R.id.tvEsmStatus)
    private val tvLastUpdate: TextView by bind(R.id.tvLastUpdate)

    lateinit var widgetIntent: Intent

    private var PRIVATE_MODE = 0
    private val PREF_NAME = "feature-extraction"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        context = this

        /* Check and grant draw overlay permission */
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + applicationContext.packageName))
            startActivityForResult(intent, REQUEST_CODE)
        }

        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            == PackageManager.PERMISSION_GRANTED) {
//            Toast.makeText(context, "Permission granted", Toast.LENGTH_SHORT).show()
        } else {
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_CODE)
        }

//        if (FirebaseUploader.getInstance().fireAuthUser == null) {
//            val signInIntent = FirebaseUploader.getInstance().getLoginIntent(context)
//            startActivityForResult(signInIntent, RC_SIGN_IN)
//        }
//        // Choose authentication providers
//        val providers = arrayListOf(
//            AuthUI.IdpConfig.EmailBuilder().build()
//        )
//
//        // Create and launch sign-in intent
//        startActivityForResult(
//            AuthUI.getInstance()
//                .createSignInIntentBuilder()
//                .setAvailableProviders(providers)
//                .build(),
//            RC_SIGN_IN)

        val sharedPrefs = getSharedPreferences(PREF_NAME, PRIVATE_MODE)

        var username = sharedPrefs.getString("username", null)
        if (username != null) {
            tvEnterUsername.visibility = GONE
            etUsername.visibility = GONE
            btnSave.visibility = GONE

            tvUsername.text = username
        } else {
            btnSave.setOnClickListener { _ ->
                if (etUsername.text.toString() != "") {
                    username = etUsername.text.toString()
                    val editor = sharedPrefs.edit()
                    editor.putString("username", username)
                    editor.apply()

                    tvEnterUsername.visibility = GONE
                    etUsername.visibility = GONE
                    btnSave.visibility = GONE

                    tvUsername.text = username
                } else Toast.makeText(context, "Username can't be empty", Toast.LENGTH_SHORT)
            }
        }

        switchSampling.isChecked = sharedPrefs.getBoolean("samplingOn", true)
        switchSampling.setOnCheckedChangeListener { buttonView, isChecked ->
            val editor = sharedPrefs.edit()
            editor.putBoolean("samplingOn", isChecked)
            editor.apply()
        }
        
        updateEsmStatus()
        
        val editor = sharedPrefs.edit()
        editor.putString("trackingPackage", "")
        editor.putString("prevPackage", "")
        editor.apply()
    }

    override fun onResume() {
        super.onResume()
        if (!isAccessibilitySettingOn(applicationContext)) {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        updateEsmStatus()
    }

    fun updateEsmStatus() {
        val sharedPrefs = getSharedPreferences(PREF_NAME, PRIVATE_MODE)
        var esmStatus = ""
        esmStatus += "Facebook <= 30s: ${sharedPrefs.getBoolean("esmFacebook1", false)}\n"
        esmStatus += "Facebook <= 5min: ${sharedPrefs.getBoolean("esmFacebook2", false)}\n"
        esmStatus += "Facebook > 5min: ${sharedPrefs.getBoolean("esmFacebook3", false)}\n\n"

        esmStatus += "Instagram <= 30s: ${sharedPrefs.getBoolean("esmInstagram1", false)}\n"
        esmStatus += "Instagram <= 5min: ${sharedPrefs.getBoolean("esmInstagram2", false)}\n"
        esmStatus += "Instagram > 5min: ${sharedPrefs.getBoolean("esmInstagram3", false)}\n\n"

        esmStatus += "YouTube <= 30s: ${sharedPrefs.getBoolean("esmYouTube1", false)}\n"
        esmStatus += "YouTube <= 5min: ${sharedPrefs.getBoolean("esmYouTube2", false)}\n"
        esmStatus += "YouTube > 5min: ${sharedPrefs.getBoolean("esmYouTube3", false)}\n\n"

        esmStatus += "KakaoTalk <= 30s: ${sharedPrefs.getBoolean("esmKakaoTalk1", false)}\n"
        esmStatus += "KakaoTalk <= 5min: ${sharedPrefs.getBoolean("esmKakaoTalk2", false)}\n"
        esmStatus += "KakaoTalk > 5min: ${sharedPrefs.getBoolean("esmKakaoTalk3", false)}\n"

//        tvEsmStatus.setText(esmStatus)
        val lastUpdateTime = Date(sharedPrefs.getLong("lastUpdate", System.currentTimeMillis()))
        val dateFormat = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.KOREA)
        tvLastUpdate.setText(dateFormat.format(lastUpdateTime))
    }

    fun isAccessibilitySettingOn(context: Context) : Boolean {
        var accessibilityEnabled = 0
        val service = packageName + "/" + UIComponentService::class.java.canonicalName

        accessibilityEnabled = Settings.Secure.getInt(context.applicationContext.contentResolver,
            android.provider.Settings.Secure.ACCESSIBILITY_ENABLED)

        val mStringColonSplitter = TextUtils.SimpleStringSplitter(':')
        if (accessibilityEnabled == 1) {
            Log.d(TAG, "ACCESSIBILITY IS ENABLED")
            val settingValue = Settings.Secure.getString(
                context.applicationContext.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            if (settingValue != null) {
                mStringColonSplitter.setString(settingValue)
                while (mStringColonSplitter.hasNext()) {
                    val accessibilityService = mStringColonSplitter.next()

                    Log.v(TAG,
                        "-------------- > accessibilityService :: $accessibilityService $service"
                    );
                    if (accessibilityService.equals(service, true)) {
                        Log.v(TAG, "We've found the correct setting - accessibility is switched on!");
                        return true
                    }
                }
            }
        } else {
            Log.v(TAG, "ACCESSIBILITY IS DISABLED")
        }
        return false
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE) {
            if (!isAccessibilitySettingOn(applicationContext)) {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }
    }


}
