package com.angelrobotics.flutter.android_multiple_identifier

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.NonNull
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import java.util.*

/** AndroidMultipleIdentifierPlugin */
class AndroidMultipleIdentifierPlugin : FlutterPlugin, MethodCallHandler, ActivityAware {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private lateinit var channel: MethodChannel
    private lateinit var activityBinding: ActivityPluginBinding
    private val MY_PERMISSIONS_REQUEST_READ_PHONE_STATE = 0
    private var result: Result? = null

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "android_multiple_identifier")
        channel.setMethodCallHandler(this)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onMethodCall(call: MethodCall, res: Result) {
        if (call.method == "getPlatformVersion") {
            res.success("Android " + Build.VERSION.RELEASE)
            return
        }
        if (call.method == "getIMEI") {
            val imei = getIMEI(activityBinding.activity.baseContext)
            res.success(imei)
            return
        }
        if (call.method == "getSerial") {
            val serial = getSerial()
            res.success(serial)
            return
        }
        if (call.method == "getAndroidID") {
            val androidID = getAndroidID(activityBinding.activity.baseContext)
            res.success(androidID)
            return
        }
        if (call.method == "getIdMap") {
            val idMap: Map<*, *>? = getIdMap(activityBinding.activity.baseContext)
            res.success(idMap)
            return
        }
        if (call.method == "checkPermissionMap") {
            var response: MutableMap<String, Boolean>? = mutableMapOf()
            if (isAPI23Up()) {
                response = checkPermissionMap(activityBinding.activity)
            } else {
                response?.put("isGranted", true)
                response?.put("isRejected", false)
            }
            res.success(response)
            return
        }
        if (call.method == "checkPermission") {
            val response = if (isAPI23Up()) checkPermission(activityBinding.activity) else true
            res.success(response)
            return
        }
        if (call.method == "checkPermissionRationale") {
            val response = if (isAPI23Up()) checkPermissionRationale(activityBinding.activity) else false
            res.success(response)
            return
        }
        if (call.method == "requestPermission") {
            result = res
            if (isAPI23Up()) {
                requestPermission(activityBinding.activity)
            } else {
                val oldAPIStatusMap: MutableMap<String, Boolean> = HashMap()
                oldAPIStatusMap["status"] = true
                oldAPIStatusMap["neverAskAgain"] = false
                res.success(oldAPIStatusMap)
            }
            return
        }
        if (call.method == "openSettings") {

            // result.success(true);
            openSettings()
            return
        }
        res.notImplemented()
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getIMEI(c: Context): String? {
        Log.i(ContentValues.TAG, "ATTEMPTING TO getIMEI: ")
        val telephonyManager: TelephonyManager = c.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        var deviceId: String? = ""
        deviceId = if (telephonyManager.imei == null) {
            "returned null"
        } else {
            telephonyManager.imei
        }
        return deviceId
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getSerial(): String {
        Log.i(ContentValues.TAG, "ATTEMPTING TO getSerial: ")
        var serial: String? = ""
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            serial = Build.getSerial()
            if (serial == null) {
                serial = "returned null"
            }
        } else {
            serial = Build.getSerial()
            if (serial == null) {
                serial = "returned null"
            }
        }
        return serial
    }

    private fun getAndroidID(c: Context): String? {
        Log.i(ContentValues.TAG, "ATTEMPTING TO getAndroidID: ")
        var androidId: String? = ""
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CUPCAKE) {
            androidId = Settings.Secure.getString(c.contentResolver, Settings.Secure.ANDROID_ID)
            if (androidId == null) {
                androidId = "returned null"
            }
        }
        return androidId
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getIdMap(c: Context): Map<String, String?>? {
        val imei = getIMEI(c)
        val serial = getSerial()
        val androidId = getAndroidID(c)
        val idMap: MutableMap<String, String?> = HashMap()
        idMap["imei"] = imei
        idMap["serial"] = serial
        idMap["androidId"] = androidId
        return idMap
    }

    private fun checkPermission(thisActivity: Activity): Boolean {
        var res = false
        if (ContextCompat.checkSelfPermission(thisActivity,
                        Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            res = true
        }
        return res
    }

    private fun checkPermissionRationale(thisActivity: Activity): Boolean {
        var res = false
        if (ActivityCompat.shouldShowRequestPermissionRationale(thisActivity, Manifest.permission.READ_PHONE_STATE)) {
            res = true
        }
        return res
    }

    private fun checkPermissionMap(activity: Activity): MutableMap<String, Boolean>? {
        val resultMap: MutableMap<String, Boolean> = HashMap()
        resultMap["isGranted"] = checkPermission(activity)
        resultMap["isRejected"] = checkPermissionRationale(activity)
        return resultMap
    }

    private fun requestPermission(thisActivity: Activity) {
        Log.i(ContentValues.TAG, "requestPermission: REQUESTING")
        ActivityCompat.requestPermissions(thisActivity, arrayOf(Manifest.permission.READ_PHONE_STATE),
                MY_PERMISSIONS_REQUEST_READ_PHONE_STATE)
    }

    private fun isAPI23Up(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
    }

    private fun openSettings() {
        val activity: Activity = activityBinding.activity
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + activity.packageName))
        intent.addCategory(Intent.CATEGORY_DEFAULT)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        activity.startActivity(intent)
    }

    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray): Boolean {
        val statusMap: MutableMap<String, Boolean> = HashMap()
        statusMap["status"] = false
        statusMap["neverAskAgain"] = false
        val permission = permissions[0]
        Log.i(ContentValues.TAG, "requestResponse: INITIALIZED")
        if (requestCode == 0 && grantResults.size > 0) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activityBinding.activity, permission)) {
                Log.e("ResquestResponse", "DENIED: $permission") // allowed//denied
                statusMap["status"] = false
            } else {
                if (ActivityCompat.checkSelfPermission(activityBinding.activity.applicationContext, permission) == PackageManager.PERMISSION_GRANTED) {
                    Log.e("ResquestResponse", "ALLOWED: $permission") // allowed
                    statusMap["status"] = true
                } else {
                    // set to never ask again
                    Log.e("ResquestResponse", "set to never ask again$permission")
                    statusMap["neverAskAgain"] = true
                }
            }
        }
        val res: Result? = result
        this.result = null
        if (res != null) {
            try {
                Log.i(ContentValues.TAG, "onRequestPermissionsResult: Returning result")
                res.success(statusMap)
            } catch (e: IllegalStateException) {
                Log.i(ContentValues.TAG, "onRequestPermissionsResult: Illegal state, NOT Returning result")
                return false
            }
        } else {
            Log.i(ContentValues.TAG, "onRequestPermissionsResult: NOT Returning result")
            return false
        }
        return true
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activityBinding = binding;
    }

    override fun onDetachedFromActivityForConfigChanges() {

    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {

    }

    override fun onDetachedFromActivity() {
    }
}
