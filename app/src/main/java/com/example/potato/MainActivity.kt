package com.example.potato

import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.Toast
import com.example.potato.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    private var selectedImageUri: Uri? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.imageView.setOnClickListener{
            opeinImageChooser()
        }
        binding.buttonUpload.setOnClickListener {
            uploadImage()
        }
    }

    private fun uploadImage() {

        if (selectedImageUri == null) {
            binding.layoutRoot.snackbar("Select an Image First")
//            layout_root.snackbar("Select an Image First")
            return
        }

        val parcelFileDescriptor = contentResolver.openFileDescriptor(
            selectedImageUri!!, "r", null
        ) ?: return

        val inputStream = FileInputStream(parcelFileDescriptor.fileDescriptor)
        val file = File(cacheDir, contentResolver.getFileName(selectedImageUri!!))
        val outputStream = FileOutputStream(file)
        inputStream.copyTo(outputStream)
//        val file = File("PATH_TO_IMAGE_FILE")
        uploadImage(file)
//        Toast.makeText(this, response, Toast.LENGTH_SHORT).show()
//        val outputStream = FileOutputStream(file)
//        inputStream.copyTo(outputStream)
//        progress_bar.progress = 0
//        val body = UploadRequestBody(file,"image",this)
//
//        MyApi().uploadImage(MultipartBody.Part.createFormData(
//            "image",
//            file.name,
//            body
//        ),
//            RequestBody.create("multipart/form-data".toMediaTypeOrNull(),"json")
//        ).enqueue(object : Callback<UploadResponse>{
//            override fun onResponse(
//                call: Call<UploadResponse>,
//                response: Response<UploadResponse>
//            ) {
//                response.body()?.let {
//                    layout_root.snackbar(it.message)
//                    progress_bar.progress = 100
//                }
//            }
//
//            override fun onFailure(call: Call<UploadResponse>, t: Throwable) {
//                layout_root.snackbar(t.message!!)
//                progress_bar.progress = 0
//            }

//        })


    }
    companion object {
        const val REQUEST_CODE_IMAGE = 101
    }

    private fun opeinImageChooser() {

        Intent(Intent.ACTION_PICK).also {
            it.type = "image/*"
            val mimeTypes = arrayOf("image/jpeg", "image/png")
            it.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
            startActivityForResult(it, REQUEST_CODE_IMAGE)
        }
    }

    fun uploadImage(file: File) {
        Thread {
            val client = OkHttpClient()
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.name, file.asRequestBody("image/*".toMediaTypeOrNull()))
                .build()
            val request = Request.Builder()
                .url("http://3.109.183.98/predict")
                .post(requestBody)
                .build()
            val response = client.newCall(request).execute()
            runOnUiThread {
                val responseBody = response.body?.string()
                val json_contact:JSONObject = JSONObject(responseBody)
                val classs = json_contact.getString("class")
//                val confidence = json_contact.getString("confidence")
                if(classs=="HEALTHY")binding.quality.setTextColor(Color.parseColor("#FF51ED24"))
                else binding.quality.setTextColor(Color.parseColor("#FF0000"))
                binding.quality.text = classs
            }
        }.start()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_CODE_IMAGE -> {
                    selectedImageUri = data?.data
                    binding.imageView.setImageURI(selectedImageUri)
                    binding.quality.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun ContentResolver.getFileName(selectedImageUri: Uri): String {
        var name = ""
        val returnCursor = this.query(selectedImageUri, null, null, null, null)
        if (returnCursor != null) {
            val nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            returnCursor.moveToFirst()
            name = returnCursor.getString(nameIndex)
            returnCursor.close()
        }

        return name
    }

    private fun View.snackbar(message: String) {
        Snackbar.make(this, message, Snackbar.LENGTH_LONG).also { snackbar ->
            snackbar.setAction("OK") {
                snackbar.dismiss()
            }
        }.show()
    }
}