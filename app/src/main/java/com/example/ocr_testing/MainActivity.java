package com.example.ocr_testing;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    private ImageButton mPictureImageButton;
    private TextView mEmptyTextView;
    private ImageView mMicrAreaImageView;
    private EditText mAccountNumber, mRoutingNumber, mFinancialInstitutionNumber, mTransitNumber;
    private int mHeight, mWidth, mPaddingRL, mPaddingTB;

    private Bitmap mPictureBitmap, mMicrCodesBitmap;
    private String mDataPath = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences pref = getSharedPreferences("CAMERA_PREVIEW_DIMENSIONS", MODE_PRIVATE);
        mHeight = pref.getInt("Height", 0);
        mWidth = pref.getInt("Width", 0);
        mPaddingRL = pref.getInt("PaddingRL", 0);
        mPaddingTB = pref.getInt("PaddingTB", 0);

        mDataPath = getFilesDir() + "/tesseract/";

        initView();
    }

    private void initView() {
        mPictureImageButton = findViewById(R.id.pictureImageButton);
        mMicrAreaImageView = findViewById(R.id.micrCodeImageView);
        mEmptyTextView = findViewById(R.id.emptyTextView);

        mAccountNumber = findViewById(R.id.accountNumber);
        mRoutingNumber = findViewById(R.id.routingNumber);
        mFinancialInstitutionNumber = findViewById(R.id.financialInstitution);
        mTransitNumber = findViewById(R.id.transitBranch);

        mPictureBitmap = getPictureBitmap();
        if (mPictureBitmap != null) {
            mPictureImageButton.setImageBitmap(mPictureBitmap);
        }

        mMicrCodesBitmap = getMicrCodesBitmap();
        if (mMicrCodesBitmap != null) {
            mEmptyTextView.setVisibility(View.GONE);
            mMicrAreaImageView.setImageBitmap(mMicrCodesBitmap);
            mMicrAreaImageView.setVisibility(View.VISIBLE);
            loadMicrCodesText();
        }

        mPictureImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), CameraActivity.class));
                finish();
            }
        });
    }

    private Bitmap getPictureBitmap() {
        // Get the path of the image
        File file = new File(getExternalFilesDir(null), "pic.jpg");
        String path = file.getAbsolutePath();

        return BitmapFactory.decodeFile(path);
    }

    private void loadCapturedPicture() {

        // Get the path of the image
        File file = new File(getExternalFilesDir(null), "pic.jpg");
        String path = file.getAbsolutePath();

        mPictureBitmap = BitmapFactory.decodeFile(path);
        if (mPictureBitmap != null) {
            mPictureImageButton.setImageBitmap(mPictureBitmap);
        }

        mMicrCodesBitmap = getMicrCodesBitmap();
        if (mMicrCodesBitmap != null) {

        }


       /* mPictureImageButton.post(new Runnable() {
            @Override
            public void run() {
                // Get the path of the image
                File file = new File(getExternalFilesDir(null), "pic.jpg");
                String path = file.getAbsolutePath();

                // Get the dimensions of the View
                int targetW = mPictureImageButton.getWidth();
                int targetH = mPictureImageButton.getHeight();

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
                    mPictureImageButton.setImageBitmap(bitmap);
                }

                mPictureBitmap = BitmapFactory.decodeFile(path);
                if (mPictureBitmap != null) {
                    mMicrCodesBitmap = getMicrCodesBitmap();
                    mMicrAreaImageView.setImageBitmap(mMicrCodesBitmap);
                    mEmptyTextView.setVisibility(View.GONE);
                    mMicrAreaImageView.setVisibility(View.VISIBLE);
                    loadMicrCodesText();
                }
            }
        });*/
    }

    private void loadMicrCodesText() {
        String micrCodesText = getMicrCodesText();
        if (micrCodesText.length() == 0 || micrCodesText == null) {
            return;
        }
        if (String.valueOf(micrCodesText.charAt(0)).equals("a")) {
            mAccountNumber.setText(getAccountNumber(micrCodesText));
            mRoutingNumber.setText(getRoutingNumber(micrCodesText));
            return;
        }
        if (String.valueOf(micrCodesText.charAt(0)).equals("c")) {
            mTransitNumber.setText(getTransitNumber(micrCodesText));
            mFinancialInstitutionNumber.setText(getFinancialInstitutionNumber(micrCodesText));
            mTransitNumber.setText(getCanadianAccountNumber(micrCodesText));
            return;
        }
        Log.v("RESULT", "Error trying to extract Micr Codes from the captured picture.");
    }

    private String getMicrCodesText() {
        checkFile(new File(mDataPath + "tessdata/"), "mcr");
        TessBaseAPI tess = new TessBaseAPI();
        tess.init(mDataPath, "mcr");
        tess.setImage(mMicrCodesBitmap);
        String result = tess.getUTF8Text();
        result = result.replace(" ", "");
        result = result.replace("\n", "");
        result = result.trim();
        Log.v("Result", result);
        return result;
    }

    private void checkFile(File dir, String name) {

        if (!dir.exists() && dir.mkdirs()) {
            copyFiles(name);
        }

        if (dir.exists()) {
            String datafilepath = mDataPath + "/tessdata/" + name + ".traineddata";
            File datafile = new File(datafilepath);
            if (!datafile.exists()) {
                copyFiles(name);
            }
        }
    }

    private void copyFiles(String name) {
        try {
            String fileName = "tessdata/" + name + ".traineddata";

            //Location of language data files
            String filepath = mDataPath + "/" + fileName;

            AssetManager assetManager = getAssets();

            //Open byte streams for reading/writing
            InputStream instream = assetManager.open(fileName);
            OutputStream outstream = new FileOutputStream(filepath);

            //Copy the file to the location specified by filepath
            byte[] buffer = new byte[1024];
            int read;
            while ((read = instream.read(buffer)) != -1) {
                outstream.write(buffer, 0, read);
            }
            outstream.flush();
            outstream.close();
            instream.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private String getRoutingNumber(String fullNumber) {
        char[] characters = fullNumber.toCharArray();
        String routingNumber = "";
        int count_a = 0;
        for (int index = 0; index < characters.length; index++) {
            String character = String.valueOf(characters[index]);
            if (character.equals("a")) {
                count_a++;
            }
            if (count_a < 2) {
                routingNumber += character;
            }
        }
        routingNumber = routingNumber.replace("a", "");
        Log.v("ROUTING", routingNumber);
        return routingNumber;
    }

    private String getAccountNumber(String fullNumber) {
        char[] characters = fullNumber.toCharArray();
        String accountNumber = "";
        int count_a = 0, count_c = 0;
        for (int index = 0; index < characters.length; index++) {
            String character = String.valueOf(characters[index]);
            if (character.equals("a")) {
                count_a++;
            }
            if (character.equals("c")) {
                count_c++;
            }
            if (count_a == 2 && count_c < 1) {
                accountNumber += character;
            }
        }
        accountNumber = accountNumber.replace("a", "");
        Log.v("ACCOUNT", accountNumber);
        return accountNumber;
    }

    private String getTransitNumber(String fullNumber) {
        char[] characters = fullNumber.toCharArray();
        String transitNumber = "";
        int count_a = 0, count_d = 0;
        for (int index = 0; index < characters.length; index++) {
            String character = String.valueOf(characters[index]);
            if (character.equals("a")) {
                count_a++;
            }
            if (character.equals("d")) {
                count_d++;
            }
            if (count_a == 1 && count_d < 1) {
                transitNumber += character;
            }
        }
        transitNumber = transitNumber.replace("a", "");
        Log.v("TRANSIT", transitNumber);
        return transitNumber;
    }

    private String getFinancialInstitutionNumber(String fullNumber) {
        char[] characters = fullNumber.toCharArray();
        String financialInstitution = "";
        int count_a = 0, count_d = 0;
        for (int index = 0; index < characters.length; index++) {
            String character = String.valueOf(characters[index]);
            if (character.equals("a")) {
                count_a++;
            }
            if (character.equals("d")) {
                count_d++;
            }
            if (count_a < 2 && count_d == 1) {
                financialInstitution += character;
            }
        }
        financialInstitution = financialInstitution.replace("d", "");
        financialInstitution = financialInstitution.replace("c", "");
        Log.v("FINANCIAL INSTITUTION", financialInstitution);
        return financialInstitution;
    }

    private String getCanadianAccountNumber(String fullNumber) {
        char[] characters = fullNumber.toCharArray();
        String accountNumber = "";
        int count_a = 0, count_c = 0;
        for (int index = 0; index < characters.length; index++) {
            String character = String.valueOf(characters[index]);
            if (character.equals("a")) {
                count_a++;
            }
            if (character.equals("c")) {
                count_c++;
            }
            if (count_a == 2 && count_c < 3) {
                accountNumber += character;
            }
        }
        accountNumber = accountNumber.replace("a", "");
        accountNumber = accountNumber.replace("d", "");
        Log.v("ACCOUNT", accountNumber);
        return accountNumber;
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Nullable
    private Bitmap getMicrCodesBitmap() {
        if (mPictureBitmap == null || mWidth == 0 || mHeight == 0) {
            return null;
        }

        //Width and height of the layout
        Log.v("METRICS", " layoutWidth: " + mWidth + " layoutHeight: " + mHeight + " layoutPaddingRL: " + mPaddingRL + " layoutPaddingTB: " + mPaddingTB);

        //Width and height of the full image
        int fullImgWidth = mPictureBitmap.getWidth();
        int fullImgHeight = mPictureBitmap.getHeight();

        Log.v("METRICS", " w: " + fullImgWidth + " h: " + fullImgHeight);

        //Image padding
        int paddingRL = (fullImgWidth * mPaddingRL) / mWidth;
        int paddingTB = (fullImgHeight * mPaddingTB) / mHeight;

        Log.v("METRICS", "paddingRL: " + paddingRL + " paddingTB: " + paddingTB);

        //Width and height of the image
        int imgWidth = fullImgWidth - (paddingRL * 2);
        int imgHeight = fullImgHeight - (paddingTB * 2);

        Log.v("METRICS", "imgWidth: " + imgWidth + " imgHeight: " + imgHeight);

        int x = paddingRL;
        double aux = (imgHeight * 0.75) + paddingTB;

        int y = (int) aux;

        aux = imgHeight * 0.2;

        Log.v("METRICS", "x: " + x + " y: " + y);
        Log.v("METRICS", "Aux: " + aux);

        return Bitmap.createBitmap(mPictureBitmap, x, y, imgWidth, (int) aux);
    }
}


