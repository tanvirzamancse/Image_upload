package com.tzp.image_upload

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkInfo
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class Runtime_Permission {
    private  val REQUEST_ID_MULTIPLE_PERMISSIONS: Int = 111
    @Volatile
    private  var permission: Runtime_Permission? = null
    private  var rContext: Context? = null

    fun getInstance(context: Context?): Runtime_Permission? {

        if (permission == null) {
            rContext = context
            permission = Runtime_Permission()
        }
        return permission
    }

    fun isInternetConnected(): Boolean {
        var activeNetworkInfo: NetworkInfo? = null
        try {
            val connectivityManager =
                rContext!!.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            activeNetworkInfo = connectivityManager.activeNetworkInfo
        } catch (ignored: Exception) {
        }
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }


    fun checkRuntimePermission(activity: Activity?): Boolean {

        val storagePermision =
            ContextCompat.checkSelfPermission(rContext!!, Manifest.permission.READ_EXTERNAL_STORAGE)

        val listPermissionsNeeded: MutableList<String> = ArrayList()

        if (storagePermision != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(
                activity!!,
                listPermissionsNeeded.toTypedArray<String>(),
                REQUEST_ID_MULTIPLE_PERMISSIONS
            )
            return false
        }
        return true
    }

}