package com.converter.image2sketch;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class HomeActivity extends AppCompatActivity {

    private static final int PICK_IMAGE = 1;
    private static final int REQUEST_IMAGE_CAPTURE = 2;
    private static final int STORAGE_REQUEST_CODE = 101;
    private Uri photoUri;
    private static final String TAG = "HomeActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Button btnUpload = findViewById(R.id.btn_upload);
        Button btnCamera = findViewById(R.id.btn_camera);

        btnUpload.setOnClickListener(v -> openGallery());

        btnCamera.setOnClickListener(v -> requestStorageAndCameraPermission());
    }


    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE);
    }


    private void requestStorageAndCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_REQUEST_CODE);
        } else {
            new OpenCameraTask().execute();
        }
    }


    private class OpenCameraTask extends AsyncTask<Void, Void, File> {
        @Override
        protected File doInBackground(Void... voids) {
            try {
                return createImageFile();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred while creating the file", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(File photoFile) {
            if (photoFile != null) {
                Log.d(TAG, "Photo file created: " + photoFile.getAbsolutePath());
                photoUri = FileProvider.getUriForFile(HomeActivity.this, "com.converter.image2sketch.fileprovider", photoFile);
                Log.d(TAG, "Photo URI: " + photoUri.toString());

                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            } else {
                Toast.makeText(HomeActivity.this, "Error occurred while creating the file", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(null);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri selectedImageUri = data.getData();
            if (selectedImageUri != null) {
                Log.d(TAG, "Gallery image selected: " + selectedImageUri);
                processImage(selectedImageUri);
            } else {
                Log.e(TAG, "Error selecting image from gallery");
                Toast.makeText(this, "Error selecting image", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            if (photoUri != null) {
                Log.d(TAG, "Camera image captured: " + photoUri.toString());
                processImage(photoUri);
            } else {
                Log.e(TAG, "Error capturing image from camera");
                Toast.makeText(this, "Error capturing image", Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.d(TAG, "Operation canceled");
            Toast.makeText(this, "Operation canceled", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Storage and camera permissions granted.");
                new OpenCameraTask().execute();
            } else {
                Log.e(TAG, "Storage permission denied.");
                Toast.makeText(this, "Storage permission is required to capture an image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void processImage(Uri imageUri) {
        try {
            Intent intent = new Intent(HomeActivity.this, MainActivity.class);
            intent.putExtra("imageUri", imageUri.toString());
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error processing image", e);
            Toast.makeText(HomeActivity.this, "Error occurred while processing the image", Toast.LENGTH_SHORT).show();
        }
    }
}
