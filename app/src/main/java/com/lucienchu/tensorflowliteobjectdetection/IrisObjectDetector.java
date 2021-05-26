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
    static final int INPUT_SIZE = (int) Constants.SIZE;
    static boolean isQuantized = true;
    private WeakReference<Context> contextWeakReference;

    private ObjectDetector objectDetector;
    public IrisObjectDetectorListener listener;

    private static final float minimumConfidence = 0f;

    public IrisObjectDetector(Context context, IrisObjectDetectorListener listener) {
        this.contextWeakReference = new WeakReference<>(context);

        long loadModelStartTime = System.currentTimeMillis();
        this.listener = listener;
        try {
            this.objectDetector = ObjectDetectUtil.create(contextWeakReference.get(), Constants.MODEL_FILE_NAME, Constants.MODEL_LABEL_NAME, INPUT_SIZE, isQuantized);
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
            Log.i(TAG, "detectCarsAndPedestrians: result.toString(): " + result.toString());
            RectF location = result.getLocation();
            String title = result.getTitle().toLowerCase();
            if (location != null && result.getConfidence() >= minimumConfidence && (title.equals("person") || title.equals("car"))) {
                locations.add(location);
//                Log.i(TAG, "detectCarsAndPedestrians: result.toString(): " + result.toString() );

//                Log.i(TAG, "detectCarsAndPedestrians: locations300: [" + location.top + ", " + location.left + ", " + location.bottom + ", " + location.bottom + " ]");
            }
        }

        long maskingStartTime = System.currentTimeMillis();


        // if locations has values (object found), do reduction here
        if (locations.size() > 0) {
            maskedCroppedImage = ImageUtils.blur(contextWeakReference.get(), croppedBitmap, locations);
            maskedImage = ImageUtils.blur(contextWeakReference.get(), image, locations);
//            Bitmap boxedImage = ImageUtils.drawBoundingBoxes(image, locations);
//            ImageUtils.saveBitmap(maskedImage, imagePath.split("\\.jpg")[0] + INPUT_SIZE + "BOXED.jpg");

//            maskedCroppedImage = ImageUtils.drawBoundingBoxes(croppedBitmap, locations);
//            maskedImage = ImageUtils.drawBoundingBoxes(image, locations);

        }
        long maskingEndTime = System.currentTimeMillis();
        Log.i(TAG, "analysisImage__masking took " + (maskingEndTime - maskingStartTime) / 1000f + " seconds");

        // save images for analysis
//        if (maskedCroppedImage != null) {
//            ImageUtils.saveBitmap(maskedCroppedImage, imagePath.split("\\.jpg")[0] + "BLUE.jpg");
//        }
//        if (maskedImage != null) {
//            ImageUtils.saveBitmap(maskedImage, imagePath.split("\\.jpg")[0] + INPUT_SIZE + "BLUE.jpg");
//        }
                if (maskedImage != null) {
            ImageUtils.saveBitmap(maskedImage, imagePath);
        }
    }



    /*===================================================================== reduce muti times START =====================================================================*/


//    /**
//     * detect cars and pedestrians on the image
//     */
//    public Bitmap reduceImage(Bitmap image, String imagePath, int index) {
//        if (this.objectDetector == null) {
//            return null;
//        }
//
//        long reductionStartTime = System.currentTimeMillis();
//
////        ImageUtils.saveBitmap(image, imagePath.split("\\.jpg")[0] + "_" +index+ "_before.jpg");
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
//        ArrayList<RectF> locations = new ArrayList<>();
//        Log.i(TAG, "reduceImage: result: START " + index + "=================================================");
//        for (ObjectDetector.Recognition result : results) {
//            Log.i(TAG, "reduceImage: result: individual details: " + result.toString() );
//            RectF location = result.getLocation();
//            String title = result.getTitle().toLowerCase();
//            if (location != null && result.getConfidence() >= minimumConfidence && (title.equals("person") || title.equals("car"))) {
//                locations.add(location);
//                Log.i(TAG, "reduceImage: result: valid details: " + result.toString() );
//                Log.i(TAG, "detectCarsAndPedestrians: locations300: [" + location.top + ", " + location.left + ", " + location.bottom + ", " + location.bottom + " ]");
//            }
//        }
//        Log.i(TAG, "reduceImage: result: END " + index + "=================================================");
//
//        long maskingStartTime = System.currentTimeMillis();
//
//
//        // if locations has values (object found), do reduction here
//        if (locations.size() > 0) {
//            maskedCroppedImage = ImageUtils.blur(contextWeakReference.get(), croppedBitmap, locations);
//            maskedImage = ImageUtils.blur(contextWeakReference.get(), image, locations);
////            Bitmap boxedImage = ImageUtils.drawBoundingBoxes(image, locations);
////            ImageUtils.saveBitmap(maskedImage, imagePath.split("\\.jpg")[0] + INPUT_SIZE + "BOXED.jpg");
//
////            maskedCroppedImage = ImageUtils.drawBoundingBoxes(croppedBitmap, locations);
////            maskedImage = ImageUtils.drawBoundingBoxes(image, locations);
//
//        }
//        long maskingEndTime = System.currentTimeMillis();
//        Log.i(TAG, "analysisImage__masking took " + (maskingEndTime - maskingStartTime) / 1000f + " seconds");
//
//        // save images for analysis
////        if (maskedCroppedImage != null) {
////            ImageUtils.saveBitmap(maskedCroppedImage, imagePath.split("\\.jpg")[0] + "BLUE.jpg");
////        }
////        if (maskedImage != null) {
////            ImageUtils.saveBitmap(maskedImage, imagePath.split("\\.jpg")[0] + INPUT_SIZE + "BLUE.jpg");
////        }
//
////        ImageUtils.saveBitmap(maskedImage, imagePath.split("\\.jpg")[0]+"_" +index+ "_after.jpg");
//        long reductionEndTime = System.currentTimeMillis();
//        Log.i(TAG, "reduction took " +index+ ": " + (reductionEndTime - reductionStartTime) / 1000f + " seconds" );
//        return maskedImage;
//    }


    /**
     * detect cars and pedestrians on the image
     */
//    public void detectCarsAndPedestrians(Bitmap image, String imagePath) {
//        if (this.objectDetector == null) {
//            return;
//        }
//
//        Bitmap maskedImage = null;
//        for (int i = 0; i < 10; i++) {
//            if(maskedImage == null) {
//                maskedImage = reduceImage(image, imagePath, i);
//            }else {
//                maskedImage = reduceImage(maskedImage, imagePath, i);
//            }
////            ImageUtils.saveBitmap(maskedImage, imagePath.split("\\.jpg")[0] + i + ".jpg");
//        }
//        if (maskedImage != null) {
//            ImageUtils.saveBitmap(maskedImage, imagePath);
//        }
//    }

    /*===================================================================== reduce muti times END =====================================================================*/



    public void close() {
        if (this.objectDetector != null) {
            objectDetector.close();
            Log.i(TAG, "close: objected detector is CLOSED");
        }
    }

    public void releaseContext() {
        this.contextWeakReference = null;
        this.listener = null;
    }


    interface IrisObjectDetectorListener {
        void onError(String message);
    }
}
