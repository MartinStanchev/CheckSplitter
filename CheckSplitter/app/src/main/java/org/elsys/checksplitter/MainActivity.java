package org.elsys.checksplitter;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.util.Linkify;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.elsys.checksplitter.tools.RequestPermissionsTool;
import org.elsys.checksplitter.tools.RequestPermissionsToolImpl;
import com.googlecode.tesseract.android.TessBaseAPI;

import org.elsys.checksplitter.tools.RequestPermissionsToolImpl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity implements ActivityCompat.OnRequestPermissionsResultCallback  {

    private static final String TAG = MainActivity.class.getSimpleName();
    static final int PHOTO_REQUEST_CODE = 1;
    private static final int ACTIVITY_SELECT_IMAGE = 2;
    private TessBaseAPI tessBaseApi;
    TextView textView;
    Uri outputFileUri;
    private static String lang = "bul";
    private RequestPermissionsTool requestTool; //for API >=23 only
    private static final String DATA_PATH = Environment.getExternalStorageDirectory().toString() + "/TesseractSample/";
    private static final String TESSDATA = "tessdata";
    private int fromDirectory = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button captureImg = (Button) findViewById(R.id.action_btn);
        if (captureImg != null) {
            captureImg.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startCameraActivity();
                }
            });
        }
        Button pickImg = (Button) findViewById(R.id.gallery_image);
        if(pickImg != null) {
            pickImg.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startGalleryActivity();
                }
            });
        }

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions();
        }

        final Button changeLang = (Button) findViewById(R.id.change_lang);
        changeLang.setTag(1);
        changeLang.setText("BUL");
        changeLang.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final int status = (Integer) v.getTag();
                if(status == 1) {
                    changeLang.setText("ENG");
                    lang = "eng";
                    v.setTag(0);
                }
                else if(status == 0) {
                    changeLang.setText("BUL");
                    lang = "bul";
                    v.setTag(1);
                }
            }
        });
    }


    private void startGalleryActivity() {
        Intent pickImg = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(pickImg, ACTIVITY_SELECT_IMAGE);
    }

    /**
     * to get high resolution image from camera
     */
    private void startCameraActivity() {
        try {
            String IMGS_PATH = Environment.getExternalStorageDirectory().toString() + "/TesseractSample/imgs";
            prepareDirectory(IMGS_PATH);

            String img_path = IMGS_PATH + "/ocr.jpg";

            outputFileUri = Uri.fromFile(new File(img_path));

            final Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);

            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(takePictureIntent, PHOTO_REQUEST_CODE);
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent data) {
        //making photo
        if (requestCode == PHOTO_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            doOCR();

        } else if (requestCode == ACTIVITY_SELECT_IMAGE && resultCode == Activity.RESULT_OK) {
            outputFileUri = data.getData();
            fromDirectory = 1;
            doOCR();
        } else {
            Toast.makeText(this, "ERROR: Image was not obtained.", Toast.LENGTH_SHORT).show();
        }
    }

    private void doOCR() {
        prepareTesseract();
        startOCR(outputFileUri);
    }

    private void prepareDirectory(String path) {

        File dir = new File(path);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                Log.e(TAG, "ERROR: Creation of directory " + path + " failed");
            }
        } else {
            Log.i(TAG, "Created directory " + path);
        }
    }


    private void prepareTesseract() {
        try {
            prepareDirectory(DATA_PATH + TESSDATA);
        } catch (Exception e) {
            e.printStackTrace();
        }

        copyTessDataFiles(TESSDATA);
    }

    /**
     * Copy tessdata files (located on assets/tessdata) to destination directory
     *
     * @param path - name of directory with .traineddata files
     */
    private void copyTessDataFiles(String path) {
        try {
            String fileList[] = getAssets().list(path);

            for (String fileName : fileList) {

                // open file within the assets folder
                // if it is not already there copy it to the sdcard
                String pathToDataFile = DATA_PATH + path + "/" + fileName;
                if (!(new File(pathToDataFile)).exists()) {

                    InputStream in = getAssets().open(path + "/" + fileName);

                    OutputStream out = new FileOutputStream(pathToDataFile);

                    // Transfer bytes from in to out
                    byte[] buf = new byte[1024];
                    int len;

                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                    in.close();
                    out.close();

                    Log.d(TAG, "Copied " + fileName + "to tessdata");
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Unable to copy files to tessdata " + e.toString());
        }
    }


    private void startOCR(Uri imgUri) {
            try {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 4;
                System.out.println(imgUri);
                Bitmap bitmap = null;
                String result = null;
                if (fromDirectory == 1) {
                    InputStream is = getContentResolver().openInputStream(imgUri);
                    bitmap = BitmapFactory.decodeStream(is);
                } else {
                    bitmap = BitmapFactory.decodeFile(imgUri.getPath(), options);
                }

                result = extractText(bitmap);
                choosePaidProducts(result);


            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
    }

    public void choosePaidProducts(String text) {
        Intent startNewActivityIntent = new Intent(this, ChooseProductsActivity.class);
        startNewActivityIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startNewActivityIntent.putExtra("result", text);
        startActivity(startNewActivityIntent);
    }

    private String extractText(Bitmap bitmap) {
        try {
            tessBaseApi = new TessBaseAPI();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            if (tessBaseApi == null) {
                Log.e(TAG, "TessBaseAPI is null. TessFactory not returning tess object.");
            }
        }

        tessBaseApi.init(DATA_PATH, lang);

        Log.d(TAG, "Training file loaded");
        tessBaseApi.setImage(bitmap);
        String extractedText = null;
        try {
            extractedText = tessBaseApi.getUTF8Text();
        } catch (Exception e) {
            Log.e(TAG, "Error in recognizing text.");
        }
        tessBaseApi.end();

        return extractedText.replaceAll(" [^,. a-zA-Z0-9]+ ", "").replaceAll(",", ".");
    }


    private void requestPermissions() {
        String[] permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
        requestTool = new RequestPermissionsToolImpl();
        requestTool.requestPermissions(this, permissions);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        boolean grantedAllPermissions = true;
        for (int grantResult : grantResults) {
            if (grantResult != PackageManager.PERMISSION_GRANTED) {
                grantedAllPermissions = false;
            }
        }

        if (grantResults.length != permissions.length || (!grantedAllPermissions)) {

            requestTool.onPermissionDenied();
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }

    }
}


