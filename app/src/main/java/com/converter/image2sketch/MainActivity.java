package com.converter.image2sketch;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.tabs.TabLayout;

import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private ImageView target;
    private Bitmap bmOriginal;
    private SketchImage sketchImage;
    private int MAX_PROGRESS = 100;
    private int effectType = SketchImage.ORIGINAL_TO_GRAY;
    private ProgressBar pb;
    private Button downloadButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        target = findViewById(R.id.iv_target);
        pb = findViewById(R.id.pb);
        downloadButton = findViewById(R.id.btn_download);
        ImageView userImageView = findViewById(R.id.iv_user);

        if (!OpenCVLoader.initDebug()) {
            Log.e("OpenCV", "OpenCV initialization failed");
        } else {
            Log.d("OpenCV", "OpenCV initialized successfully");
        }

        String imageUriString = getIntent().getStringExtra("imageUri");
        if (imageUriString != null) {
            Uri imageUri = Uri.parse(imageUriString);
            loadUserImage(imageUri, userImageView);
            loadImage(imageUri);
        } else {
            Log.e(TAG, "No image URI provided.");
        }

        SeekBar seek = findViewById(R.id.simpleSeekBar);
        TextView tvPB = findViewById(R.id.tv_pb);

        tvPB.setText(String.format("%d %%", MAX_PROGRESS));
        seek.setMax(MAX_PROGRESS);
        seek.setProgress(MAX_PROGRESS);

        TabLayout tabLayout = findViewById(R.id.tabLayout);
        setupTabLayout(tabLayout, tvPB, seek);



        downloadButton.setOnClickListener(v -> {
            Bitmap processedBitmap = sketchImage.getImageAs(effectType, seek.getProgress());
            if (processedBitmap != null) {
                saveImageToGallery(processedBitmap);
            } else {
                Toast.makeText(MainActivity.this, "No image to save", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadImage(Uri imageUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            bmOriginal = BitmapFactory.decodeStream(inputStream);
            if (bmOriginal != null) {
                target.setImageBitmap(bmOriginal);
                sketchImage = new SketchImage.Builder(this, bmOriginal).build();
                target.setImageBitmap(sketchImage.getImageAs(effectType, MAX_PROGRESS));
            } else {
                Log.e(TAG, "Failed to decode bitmap from the image URI.");
            }
        } catch (IOException e) {
            Log.e(TAG, "Error loading image: " + e.getMessage());
        }
    }

    private void setupTabLayout(TabLayout tabLayout, TextView tvPB, SeekBar seek) {
        String[] effects = {
                "Original to Gray", "Original to Sketch", "Original to Colored Sketch",
                "Original to Soft Sketch", "Original to Soft Color Sketch", "Gray to Sketch",
                "Gray to Colored Sketch", "Gray to Soft Sketch", "Gray to Soft Color Sketch",
                "Sketch to Color Sketch"
        };

        for (String effect : effects) {
            tabLayout.addTab(tabLayout.newTab().setText(effect));
        }

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                effectType = tab.getPosition();
                tvPB.setText(String.format("%d %%", MAX_PROGRESS));
                seek.setProgress(MAX_PROGRESS);
                target.setImageBitmap(sketchImage.getImageAs(effectType, MAX_PROGRESS));
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvPB.setText(String.format("%d %%", progress));
                target.setImageBitmap(sketchImage.getImageAs(effectType, progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                pb.setVisibility(View.VISIBLE);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                pb.setVisibility(View.INVISIBLE);
                target.setImageBitmap(sketchImage.getImageAs(effectType, seekBar.getProgress()));
            }
        });
    }

    private void saveImageToGallery(Bitmap bitmap) {
        OutputStream fos;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentResolver resolver = getContentResolver();
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "sketch_image_" + System.currentTimeMillis() + ".jpg");
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);
                Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                if (imageUri != null) {
                    fos = resolver.openOutputStream(imageUri);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                    fos.flush();
                    fos.close();
                    Toast.makeText(this, "Image saved to gallery", Toast.LENGTH_SHORT).show();
                }
            } else {
                String imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
                File image = new File(imagesDir, "sketch_image_" + System.currentTimeMillis() + ".jpg");
                fos = new FileOutputStream(image);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                fos.flush();
                fos.close();
                Toast.makeText(this, "Image saved to gallery", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error saving image: " + e.getMessage());
            Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap decodeSampledBitmapFromResource(Resources res, int resId, int reqWidth, int reqHeight) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, resId, options);
    }

    private void loadUserImage(Uri imageUri, ImageView userImageView) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            Bitmap userBitmap = BitmapFactory.decodeStream(inputStream);
            if (userBitmap != null) {
                userImageView.setImageBitmap(userBitmap);
            } else {
                Log.e(TAG, "Failed to decode bitmap from the image URI for user image.");
            }
        } catch (IOException e) {
            Log.e(TAG, "Error loading user image: " + e.getMessage());
        }
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }
}
