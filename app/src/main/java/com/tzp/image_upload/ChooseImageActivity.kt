package com.tzp.image_upload

import android.Manifest
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_IMAGES
import android.Manifest.permission.READ_MEDIA_VIDEO
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat.getDrawable
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import coil.compose.rememberAsyncImagePainter
import com.google.gson.JsonObject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.sql.Timestamp

class ChooseImageActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                ImageUploadScreen(innerPadding)
            }
        }
    }

}

@Composable
fun ImageUploadScreen(innerPadding: PaddingValues) {
    val context = LocalContext.current
    val imageUri = remember { mutableStateOf<android.net.Uri?>(null) }
    val bitmap = remember { mutableStateOf<Bitmap?>(null) }



    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        imageUri.value = uri
        uri?.let {
            val bitmapTemp = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            bitmap.value = bitmapTemp
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        imageUri.value?.let { uri ->
            val bitmapTemp = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            bitmap.value = bitmapTemp
            val a=bitmap.value
            Image(
                bitmap = bitmapTemp.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.size(200.dp)
            )
        } ?: run {
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .padding(bottom = 16.dp)
            )
        }

        Button(onClick = {
            launcher.launch("image/*")
           /* if (multiplePermissionsState.allPermissionsGranted) {

            } else {
                multiplePermissionsState.launchMultiplePermissionRequest()
            }*/
        }) {
            Text(text = "Choose Your Photo", modifier = Modifier.padding(horizontal = 30.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            bitmap.value?.let { bitmap ->
                val resizedBitmap = getResizedBitmap(bitmap, 100)
                val stream = ByteArrayOutputStream()
                resizedBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                val byteArray = stream.toByteArray()
                val timestamp = Timestamp(System.currentTimeMillis())
                val imageName = "${timestamp.time}"

                try {
                    val file = File(context.cacheDir, imageName)
                    if (!file.exists()) {
                        file.createNewFile()
                    }
                    val fos = FileOutputStream(file)
                    fos.write(byteArray)
                    fos.flush()
                    fos.close()

                    val requestFile = RequestBody.create("image/*".toMediaTypeOrNull(), file)
                    val body = MultipartBody.Part.createFormData("image", file.name, requestFile)

                    RetrofitClient.getInstance()
                        .api
                        .uploadImage(body)
                        .enqueue(object : Callback<JsonObject>{
                            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                                Toast.makeText(context, "Upload successful", Toast.LENGTH_LONG).show()
                            }
                            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                                t.printStackTrace()
                                Toast.makeText(context, "Upload failed: ${t.message}", Toast.LENGTH_LONG).show()
                            }
                        })
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } ?: run {
                Toast.makeText(context, "Select Your Image", Toast.LENGTH_LONG).show()
            }
        }) {
            Text(text = "Upload", modifier = Modifier.padding(horizontal = 30.dp))
        }
    }


}

fun getResizedBitmap(image: Bitmap, maxSize: Int): Bitmap {
    val width = image.width
    val height = image.height

    val bitmapRatio: Float = width.toFloat() / height.toFloat()
    val newWidth: Int
    val newHeight: Int

    if (bitmapRatio > 1) {
        newWidth = maxSize
        newHeight = (newWidth / bitmapRatio).toInt()
    } else {
        newHeight = maxSize
        newWidth = (newHeight * bitmapRatio).toInt()
    }
    return Bitmap.createScaledBitmap(image, newWidth, newHeight, true)
}



