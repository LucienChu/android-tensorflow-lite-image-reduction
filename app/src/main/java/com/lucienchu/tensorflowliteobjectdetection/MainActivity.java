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
        long reductionStartTime = System.currentTimeMillis();

        for (File f : fs) {
            Bitmap origin = BitmapFactory.decodeFile(f.getPath());
            Bitmap flippedImage = ImageUtils.getRotatedBitmap(origin, f.getPath());

            long individualReductionStartTime = System.currentTimeMillis();


//                this.irisObjectDetector.oneShotBlur(flippedImage, f.toString());
//                this.irisObjectDetector.reduceInBlueAndKeptCropped(flippedImage, f.toString());
                this.irisObjectDetector.loopReductionAndBlue(flippedImage, f.toString(), 1);
            long individualReductionEndTime = System.currentTimeMillis();
            Log.i(TAG, "individualReduction took " + (individualReductionEndTime - individualReductionStartTime) / 1000f + " seconds" );
        }
        long reductionEndTime = System.currentTimeMillis();
        Log.i(TAG, "reduction took " + (reductionEndTime - reductionStartTime) / 1000f + " seconds" );
//        this.irisObjectDetector.close();
//        this.irisObjectDetector.releaseContext();
//        this.irisObjectDetector = null;
    }

    @Override
    public void onError(String message) {
        Log.i(TAG, "onError: cannot find file");
    }
}