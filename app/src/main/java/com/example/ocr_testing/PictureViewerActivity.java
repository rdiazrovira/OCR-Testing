package com.example.ocr_testing;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;


import java.io.File;

public class PictureViewerActivity extends AppCompatActivity {

    private ImageView mPictureImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picture_viewer);

        mPictureImageView = findViewById(R.id.pictureImageView);
        loadFullPicture();
    }

    private void loadFullPicture() {

        mPictureImageView.post(new Runnable() {
            @Override
            public void run() {
                // Get the path of the image
                File file = new File(getExternalFilesDir(null), "pic.jpg");
                String path = file.getAbsolutePath();

                // Get the dimensions of the View
                int targetW = mPictureImageView.getWidth();
                int targetH = mPictureImageView.getHeight();

                // Get the dimensions of the bitmap
                BitmapFactory.Options bmOptions = new BitmapFactory.Options();
                bmOptions.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(path, bmOptions);
                int photoW = bmOptions.outWidth;
                int photoH = bmOptions.outHeight;

                // Determine how much to scale down the image
                int scaleFactor = Math.min(photoW / targetW, photoH / targetH);

                // Decode the image file into a Bitmap sized to fill the View
                bmOptions.inJustDecodeBounds = false;
                bmOptions.inSampleSize = scaleFactor;
                bmOptions.inPurgeable = true;

                Bitmap bitmap = BitmapFactory.decodeFile(path, bmOptions);
                if (bitmap != null) {
                    mPictureImageView.setImageBitmap(bitmap);
                }
            }
        });
    }

}
