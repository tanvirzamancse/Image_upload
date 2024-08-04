package com.tzp.image_upload

import android.Manifest
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_IMAGES
import android.Manifest.permission.READ_MEDIA_VIDEO
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresExtension
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import com.google.gson.JsonObject
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.http.Url
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.sql.Timestamp


class ImageUploadActivity : AppCompatActivity() {
    private val REQUEST_ID_MULTIPLE_PERMISSIONS: Int = 111
    private var mLocationPermissionGranted = false
    private val TAG = "ImageUploadActivity"
    private lateinit var imageView: AppCompatImageView
    private lateinit var button: AppCompatButton
    private lateinit var upload: AppCompatButton
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var  url:Url
    private var width: Int = 0
    private var height: Int = 0


    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private val multiplePermissions = arrayOf(
        android.Manifest.permission.READ_MEDIA_IMAGES,
        android.Manifest.permission.READ_EXTERNAL_STORAGE,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
    )

    @RequiresExtension(extension = Build.VERSION_CODES.R, version = 2)
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_upload)

        imageView = findViewById(R.id.imageView)
        button = findViewById(R.id.button)
        upload = findViewById(R.id.upload)

        myPermissions()
        registration()

        button.setOnClickListener {
            val intent = Intent(MediaStore.ACTION_PICK_IMAGES)
            activityResultLauncher.launch(intent)
        }
        //application/json
        upload.setOnClickListener {
           val image=imageView.drawable
            if (image!=null){
                val a= Bitmap.createBitmap(image.getIntrinsicWidth(), image.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                val bitmap= getResizedBitmap(a,100)

                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                val byteArray = stream.toByteArray()

                val timestamp = Timestamp(System.currentTimeMillis())
                val imageName = timestamp.time.toString() + ""

                var body: MultipartBody.Part? =null
                try {
                    val f: File = File(applicationContext.cacheDir, imageName)
                    if (!f.exists()) {
                        f.createNewFile()
                    }
                    val fos = FileOutputStream(f)
                    fos.write(byteArray)
                    fos.flush()
                    fos.close()

                    val requestFile = RequestBody.create("image/*".toMediaType(), f)
                    body = MultipartBody.Part.createFormData("image", f.name, requestFile)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                RetrofitClient
                    .getInstance()
                    .api
                    .uploadImage(body)
                    .enqueue(object : Callback<JsonObject> {
                        override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                            Log.d("image", "onResponse: ${response.message()}")
                        }
                        override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                            t.printStackTrace()
                            Log.d("image", "onFailure: $t")
                        }
                    })
            }else{
                Toast.makeText(applicationContext,"Select Your Image",Toast.LENGTH_LONG).show()
            }

        }
    }

    private fun registration() {
        activityResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult ->
            val url = result.data?.data
            imageView.setImageURI(url)
        }
    }


    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permission: Map<String, Boolean?> ->
            var allGranted = true
            for (isGranted in permission.values) {
                if (!isGranted!!) {
                    allGranted = false
                    break
                }
            }
            if (allGranted) {
                // All is granted
            } else {
                // All is not granted
            }
        }

    fun getResizedBitmap(image: Bitmap, maxSize: Int): Bitmap {
        width = image.width
        height = image.height

        val bitmapRatio: Float = width.toFloat() / height.toFloat()
        if (bitmapRatio > 1) {
            width = maxSize
            height = (width / bitmapRatio).toInt()
        } else {
            height = maxSize
            width = (height * bitmapRatio).toInt()
        }
        return Bitmap.createScaledBitmap(image, width, height, true)
    }
    private fun myPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissions = arrayOf(
                READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_AUDIO,
                READ_MEDIA_VIDEO,
            )

            val permissionsTORequest: MutableList<String> = ArrayList()
            for (permission in permissions) {
                if (ContextCompat.checkSelfPermission(
                        this,
                        permission
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    permissionsTORequest.add(permission)
                }
            }

            if (permissionsTORequest.isEmpty()) {
                // All permissions are already granted
                Toast.makeText(this, "All permissions are already granted", Toast.LENGTH_SHORT)
                    .show()
            } else {
                val permissionsArray = permissionsTORequest.toTypedArray<String>()
                var shouldShowRationale = false

                for (permission in permissionsArray) {
                    if (shouldShowRequestPermissionRationale(permission)) {
                        shouldShowRationale = true
                        break
                    }
                }

                if (shouldShowRationale) {
                    AlertDialog.Builder(this)
                        .setMessage("Please allow all permissions")
                        .setCancelable(false)
                        .setPositiveButton(
                            "YES"
                        ) { _, _ ->
                            requestPermissionLauncher.launch(
                                permissionsArray
                            )
                        }
                        .setNegativeButton(
                            "NO"
                        ) { dialogInterface, i -> dialogInterface.dismiss() }

                        .show()
                } else {
                    requestPermissionLauncher.launch(permissionsArray)
                }
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val permissions = arrayOf(
                READ_EXTERNAL_STORAGE,
            )


            val permissionsTORequest: MutableList<String> = ArrayList()
            for (permission in permissions) {
                if (ContextCompat.checkSelfPermission(
                        this,
                        permission
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    permissionsTORequest.add(permission)
                }
            }

            if (permissionsTORequest.isEmpty()) {
                // All permissions are already granted
                Toast.makeText(this, "All permissions are already granted", Toast.LENGTH_SHORT)
                    .show()
            } else {
                val permissionsArray = permissionsTORequest.toTypedArray<String>()
                var shouldShowRationale = false

                for (permission in permissionsArray) {
                    if (shouldShowRequestPermissionRationale(permission)) {
                        shouldShowRationale = true
                        break
                    }
                }

                if (shouldShowRationale) {
                    AlertDialog.Builder(this)
                        .setMessage("Please allow all permissions")
                        .setCancelable(false)
                        .setPositiveButton("YES",
                            DialogInterface.OnClickListener { dialogInterface, i ->
                                requestPermissionLauncher.launch(
                                    permissionsArray
                                )
                            })
                        .setNegativeButton(
                            "NO"
                        ) { dialogInterface, i -> dialogInterface.dismiss() }
                        .show()
                } else {
                    requestPermissionLauncher.launch(permissionsArray)
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ID_MULTIPLE_PERMISSIONS) {
            val image = data?.data
            imageView.setImageURI(image)
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        mLocationPermissionGranted = false
        when (requestCode) {
            REQUEST_ID_MULTIPLE_PERMISSIONS -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.size > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    mLocationPermissionGranted = true
                }
            }

        }

    }
}
