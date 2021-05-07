package com.lucienchu.tensorflowliteobjectdetection;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import java.io.File;

public class MainActivity extends AppCompatActivity implements IrisObjectDetector.IrisObjectDetectorListener {
    private static final String TAG = "MainActivity";
    private IrisObjectDetector irisObjectDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        irisObjectDetector = new IrisObjectDetector(this, this);

        reduceImages();
    }

    public void reduceImages() {
        File[] fs = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) + "/images").listFiles();
        Log.i(TAG, "reduceImages: ls: " + fs.length);
        for (File f : fs) {
            Bitmap origin = BitmapFactory.decodeFile(f.getPath());
            Bitmap flippedImage = ImageUtils.getRotatedBitmap(origin, f.getPath());



//            ImageUtils.saveBitmap(croppedBitmap, f.getParent() + "/300X300/");

            this.irisObjectDetector.detectCarsAndPedestrians(flippedImage, f.toString());


        }
    }

    @Override
    public void onError(String message) {
        Log.i(TAG, "onError: cannot find file");
    }
}