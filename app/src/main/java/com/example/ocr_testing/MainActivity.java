package com.example.ocr_testing;

import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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

    private Button mCaptureButton, mExtractButton;
    private ImageView mPictureImageView, mMicrCodesImageView;
    private TextView mExtractedMicrCodesTextView;

    private int mHeight, mWidth, mPaddingRL, mPaddingTB;
    private String mDataPath = "";

    private Bitmap mPictureBitmap, mMicrCodesBitmap;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mHeight = getIntent().getIntExtra("Height", 0);
        mWidth = getIntent().getIntExtra("Width", 0);
        mPaddingRL = getIntent().getIntExtra("PaddingRL", 0);
        mPaddingTB = getIntent().getIntExtra("PaddingTB", 0);

        mDataPath = getFilesDir() + "/tesseract/";

        initView();
    }

    private void initView() {
        mPictureImageView = findViewById(R.id.pictureImageView);
        mCaptureButton = findViewById(R.id.imageCaptureButton);
        mMicrCodesImageView = findViewById(R.id.micrCodesImageView);
        mExtractedMicrCodesTextView = findViewById(R.id.extractedMicrCodesTextView);
        mExtractButton = findViewById(R.id.extractMicrCodesButton);
        loadCapturedPicture();
        mCaptureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), CameraActivity.class));
                finish();
            }
        });
        mExtractButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mMicrCodesBitmap != null) {
                    loadMicrCodesText();
                }
            }
        });
    }

    private void loadCapturedPicture() {
        File file = new File(getExternalFilesDir(null), "pic.jpg");
        mPictureBitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
        if (mPictureBitmap == null) {
            return;
        }
        mPictureImageView.setImageBitmap(mPictureBitmap);
    }

    private void loadMicrCodesText() {
        String micrCodesText = getMicrCodesText();
        String result = "";
        if (String.valueOf(micrCodesText.charAt(0)).equals("a")) {
            result = getRoutingNumber(micrCodesText) + "; " + getAccountNumber(micrCodesText);
        } else if (String.valueOf(micrCodesText.charAt(0)).equals("c")) {
            result = getTransitNumber(micrCodesText) + "; "
                    + getFinancialInstitutionNumber(micrCodesText) + "; "
                    + getCanadianAccountNumber(micrCodesText);
        }
        result = result.equals("") ? "Error trying to extract Micr Codes from the captured picture." : result;
        mExtractedMicrCodesTextView.setText(result);
    }

    private String getMicrCodesText() {
        checkFile(new File(mDataPath + "tessdata/"), "mcr");
        TessBaseAPI tess = new TessBaseAPI();
        tess.init(mDataPath, "mcr");
        Bitmap image = BitmapFactory.decodeResource(getResources(), R.drawable.micr2);
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
        mMicrCodesBitmap = getMicrCodesBitmap();
        if (mMicrCodesBitmap != null) {
            mMicrCodesImageView.setImageBitmap(mMicrCodesBitmap);
        }
        super.onResume();
    }

    @Nullable
    private Bitmap getMicrCodesBitmap() {
        if (mPictureBitmap == null || mWidth == 0 || mHeight == 0) {
            return null;
        }

        //Width and height of the layout
        Log.v("METRICS", " layoutWidth: " + mWidth + " layoutHeight: " + mHeight);

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
        double aux = (imgHeight * 0.8);
        int y = (int) aux;

        aux = imgHeight * 0.1;

        Log.v("METRICS", "x: " + x + " y: " + (int) aux);

        return Bitmap.createBitmap(mPictureBitmap, x, y, imgWidth, (int) aux);
    }
}


