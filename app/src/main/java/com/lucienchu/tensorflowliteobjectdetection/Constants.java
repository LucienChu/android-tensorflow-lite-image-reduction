package com.lucienchu.tensorflowliteobjectdetection;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;

import java.io.File;

public class Constants {
    static String MODEL_FILE_NAME = "detect.tflite";
    static String MODEL_LABEL_NAME = "labelmap.txt";
    public static final float SIZE = 300;
    public static final int NUM_DETECTIONS = 10;

    // long
//    static String MODEL_FILE_NAME = "lite_model_efficientdet_lite4_detection_metadata_2.tflite";
//    static String MODEL_LABEL_NAME = "labelmap.txt";
//    public static final float SIZE = 640;
//    public static final int NUM_DETECTIONS = 25;

    // short
//    static String MODEL_FILE_NAME = "detect.tflite";
//    static String MODEL_LABEL_NAME = "labelmap.txt";
//    public static final float SIZE = 300;
//    public static final int NUM_DETECTIONS = 10;

//    static String MODEL_FILE_NAME = "model.tflite";
//    static String MODEL_LABEL_NAME = "labelmap.txt";
//    public static final float SIZE = 640;
//    public static final int NUM_DETECTIONS = 20;

//    private void startReduction() {
//        File image = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) + "/0f74c705-4302-460d-a730-891c0ff26fe9.jpg");
//        IrisObjectDetector objectDetector = new IrisObjectDetector(this, this);
//        Bitmap temp = BitmapFactory.decodeFile(image.getPath());
//        IrisObjectDetector.ReductionResult result = objectDetector.reduceCarsAndPedestrians(temp, 10);
//        ImageUtils.saveBitmap(result.getReducedBitMap(), image.getPath().split(".jpg")[0] + "_temp.jpg");
//    }
}


