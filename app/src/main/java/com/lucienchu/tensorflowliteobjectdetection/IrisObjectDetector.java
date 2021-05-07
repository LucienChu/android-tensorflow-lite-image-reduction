package com.lucienchu.tensorflowliteobjectdetection;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.util.Log;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class IrisObjectDetector {
    private static final String TAG = "IrisObjectDetector";
    static final int INPUT_SIZE = 300;
    static String MODEL_FILE_NAME = "detect.tflite";
    static String MODEL_LABEL_NAME = "labelmap.txt";
    static boolean isQuantized = true;
    final WeakReference<Context> contextWeakReference;

    private ObjectDetector objectDetector;
    public IrisObjectDetectorListener listener;

    private static final float minimumConfidence = 0.5f;

    public IrisObjectDetector(Context context, IrisObjectDetectorListener listener) {
        this.contextWeakReference = new WeakReference<>(context);

        long loadModelStartTime = System.currentTimeMillis();
        this.listener = listener;
        try {
            this.objectDetector = ObjectDetectUtil.create(contextWeakReference.get(), MODEL_FILE_NAME, MODEL_LABEL_NAME, INPUT_SIZE, isQuantized);
            objectDetector.setUseNNAPI(true);
            long loadModelEndTime = System.currentTimeMillis();
            Log.i(TAG, "IrisObjectDetector: loadModel took " + (loadModelEndTime - loadModelStartTime) / 1000f + " seconds");
        } catch (IOException e) {
            e.printStackTrace();
            if (this.listener != null) {
                listener.onError("Error in instantiating model files");
            }
        }

    }


//    /**
//     * detect cars and pedestrians on the image
//     */
//    public void detectCarsAndPedestrians(Bitmap image, String imagePath) {
//        if (this.objectDetector == null) {
//            return;
//        }
//
//        // make a copy of mutable image, otherwise, cannot change its pixels
//        Bitmap croppedBitmap = Bitmap.createScaledBitmap(image, INPUT_SIZE, INPUT_SIZE, false);
//
//        long recognitionStartTime = System.currentTimeMillis();
//        List<ObjectDetector.Recognition> results = this.objectDetector.recognizeImage(croppedBitmap);
//        long recognitionEndTime = System.currentTimeMillis();
//        Log.i(TAG, "analysisImage__recognition took " + (recognitionEndTime - recognitionStartTime) / 1000f + " seconds");
//
//        Bitmap maskedCroppedImage = null;
//        Bitmap maskedImage = null;
//
//
//        ArrayList<RectF> locations = new ArrayList<>();
//
//        for (ObjectDetector.Recognition result : results) {
//            RectF location = result.getLocation();
//            String title = result.getTitle().toLowerCase();
//            if (location != null && result.getConfidence() >= minimumConfidence && (title.equals("person") || title.equals("car"))) {
//                locations.add(location);
//            }
//        }
//
//        long maskingStartTime = System.currentTimeMillis();
    // if locations has values (object found), do reduction here
//        if (locations.size() > 0) {
//            maskedCroppedImage = ImageUtils.getMaskBitMap(croppedBitmap, locations);
//            maskedImage = ImageUtils.getMaskBitMap(image, locations);
//        }
//        long maskingEndTime = System.currentTimeMillis();
//        Log.i(TAG, "analysisImage__masking took " + (maskingEndTime - maskingStartTime) / 1000f + " seconds");
//
//        // save images for analysis
//        if (maskedCroppedImage != null) {
//            ImageUtils.saveBitmap(maskedCroppedImage, imagePath.split("\\.jpg")[0] + "BLUE.jpg");
//        }
//        if (maskedImage != null) {
//            ImageUtils.saveBitmap(maskedImage, imagePath.split("\\.jpg")[0] + INPUT_SIZE + "BLUE.jpg");
//        }
//    }


    /**
     * detect cars and pedestrians on the image
     */
    public void detectCarsAndPedestrians(Bitmap image, String imagePath) {
        if (this.objectDetector == null) {
            return;
        }

        // make a copy of mutable image, otherwise, cannot change its pixels
        Bitmap croppedBitmap = Bitmap.createScaledBitmap(image, INPUT_SIZE, INPUT_SIZE, false);

        long recognitionStartTime = System.currentTimeMillis();
        List<ObjectDetector.Recognition> results = this.objectDetector.recognizeImage(croppedBitmap);
        long recognitionEndTime = System.currentTimeMillis();
        Log.i(TAG, "analysisImage__recognition took " + (recognitionEndTime - recognitionStartTime) / 1000f + " seconds");

        Bitmap maskedCroppedImage = null;
        Bitmap maskedImage = null;

        ArrayList<RectF> locations = new ArrayList<>();
        for (ObjectDetector.Recognition result : results) {
            RectF location = result.getLocation();
            String title = result.getTitle().toLowerCase();
            if (location != null && result.getConfidence() >= minimumConfidence && (title.equals("person") || title.equals("car"))) {
                locations.add(location);
            }
        }

        long maskingStartTime = System.currentTimeMillis();

        // if locations has values (object found), do reduction here
        if (locations.size() > 0) {
            maskedCroppedImage = ImageUtils.blur(contextWeakReference.get(), croppedBitmap, locations);
            maskedImage = ImageUtils.blur(contextWeakReference.get(), image, locations);

        }
        long maskingEndTime = System.currentTimeMillis();
        Log.i(TAG, "analysisImage__masking took " + (maskingEndTime - maskingStartTime) / 1000f + " seconds");

        // save images for analysis
        if (maskedCroppedImage != null) {
            ImageUtils.saveBitmap(maskedCroppedImage, imagePath.split("\\.jpg")[0] + "BLUE.jpg");
        }
        if (maskedImage != null) {
            ImageUtils.saveBitmap(maskedImage, imagePath.split("\\.jpg")[0] + INPUT_SIZE + "BLUE.jpg");
        }
    }


    interface IrisObjectDetectorListener {
        void onError(String message);
    }
}
