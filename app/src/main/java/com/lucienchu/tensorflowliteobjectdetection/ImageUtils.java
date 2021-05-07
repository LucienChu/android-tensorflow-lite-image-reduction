package com.lucienchu.tensorflowliteobjectdetection;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.media.ExifInterface;
import android.os.Environment;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Utility class for manipulating images.
 */
public class ImageUtils {
    private static final String TAG = "ImageUtils";
    public static final float SIZE = 300;
    // This value is 2 ^ 18 - 1, and is used to clamp the RGB values before their ranges
    // are normalized to eight bits.
    static final int kMaxChannelValue = 262143;

    // for render script to blur image (bitmap)
    private static final float BLUR_RADIUS = 25f;


    /**
     * Utility method to compute the allocated size in bytes of a YUV420SP image of the given
     * dimensions.
     */
    public static int getYUVByteSize(final int width, final int height) {
        // The luminance plane requires 1 byte per pixel.
        final int ySize = width * height;

        // The UV plane works on 2x2 blocks, so dimensions with odd size must be rounded up.
        // Each 2x2 block takes 2 bytes to encode, one each for U and V.
        final int uvSize = ((width + 1) / 2) * ((height + 1) / 2) * 2;

        return ySize + uvSize;
    }

    /**
     * Saves a Bitmap object to disk for analysis.
     *
     * @param bitmap The bitmap to save.
     */
    public static void saveBitmap(final Bitmap bitmap) {
        saveBitmap(bitmap, "/sdcard/tensorflow/preview.jpg");
    }


    /**
     * Saves a Bitmap object to disk for analysis.
     *
     * @param bitmap   The bitmap to save.
     * @param filename The location to save the bitmap to.
     */
    public static void saveBitmap(final Bitmap bitmap, final String filename) {
        final File myDir = new File(filename);
        try {
            final FileOutputStream out = new FileOutputStream(myDir);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 89, out);
            out.flush();
            out.close();
            int a = 80;
        } catch (final Exception e) {
            Log.i(TAG, "Exception: saveBitmap: " + e.getMessage());
        }
    }


    public static void convertYUV420SPToARGB8888(byte[] input, int width, int height, int[] output) {
        final int frameSize = width * height;
        for (int j = 0, yp = 0; j < height; j++) {
            int uvp = frameSize + (j >> 1) * width;
            int u = 0;
            int v = 0;

            for (int i = 0; i < width; i++, yp++) {
                int y = 0xff & input[yp];
                if ((i & 1) == 0) {
                    v = 0xff & input[uvp++];
                    u = 0xff & input[uvp++];
                }

                output[yp] = YUV2RGB(y, u, v);
            }
        }
    }

    private static int YUV2RGB(int y, int u, int v) {
        // Adjust and check YUV values
        y = (y - 16) < 0 ? 0 : (y - 16);
        u -= 128;
        v -= 128;

        // This is the floating point equivalent. We do the conversion in integer
        // because some Android devices do not have floating point in hardware.
        // nR = (int)(1.164 * nY + 2.018 * nU);
        // nG = (int)(1.164 * nY - 0.813 * nV - 0.391 * nU);
        // nB = (int)(1.164 * nY + 1.596 * nV);
        int y1192 = 1192 * y;
        int r = (y1192 + 1634 * v);
        int g = (y1192 - 833 * v - 400 * u);
        int b = (y1192 + 2066 * u);

        // Clipping RGB values to be inside boundaries [ 0 , kMaxChannelValue ]
        r = r > kMaxChannelValue ? kMaxChannelValue : (r < 0 ? 0 : r);
        g = g > kMaxChannelValue ? kMaxChannelValue : (g < 0 ? 0 : g);
        b = b > kMaxChannelValue ? kMaxChannelValue : (b < 0 ? 0 : b);

        return 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
    }

    public static void convertYUV420ToARGB8888(
            byte[] yData,
            byte[] uData,
            byte[] vData,
            int width,
            int height,
            int yRowStride,
            int uvRowStride,
            int uvPixelStride,
            int[] out) {
        int yp = 0;
        for (int j = 0; j < height; j++) {
            int pY = yRowStride * j;
            int pUV = uvRowStride * (j >> 1);

            for (int i = 0; i < width; i++) {
                int uv_offset = pUV + (i >> 1) * uvPixelStride;

                out[yp++] = YUV2RGB(0xff & yData[pY + i], 0xff & uData[uv_offset], 0xff & vData[uv_offset]);
            }
        }
    }

    /**
     * Returns a transformation matrix from one reference frame into another. Handles cropping (if
     * maintaining aspect ratio is desired) and rotation.
     *
     * @param srcWidth            Width of source frame.
     * @param srcHeight           Height of source frame.
     * @param dstWidth            Width of destination frame.
     * @param dstHeight           Height of destination frame.
     * @param applyRotation       Amount of rotation to apply from one frame to another. Must be a multiple
     *                            of 90.
     * @param maintainAspectRatio If true, will ensure that scaling in x and y remains constant,
     *                            cropping the image if necessary.
     * @return The transformation fulfilling the desired requirements.
     */
    public static Matrix getTransformationMatrix(
            final int srcWidth,
            final int srcHeight,
            final int dstWidth,
            final int dstHeight,
            final int applyRotation,
            final boolean maintainAspectRatio) {
        final Matrix matrix = new Matrix();

        if (applyRotation != 0) {

            // Translate so center of image is at origin.
            matrix.postTranslate(-srcWidth / 2.0f, -srcHeight / 2.0f);

            // Rotate around origin.
            matrix.postRotate(applyRotation);
        }

        // Account for the already applied rotation, if any, and then determine how
        // much scaling is needed for each axis.
        final boolean transpose = (Math.abs(applyRotation) + 90) % 180 == 0;

        final int inWidth = transpose ? srcHeight : srcWidth;
        final int inHeight = transpose ? srcWidth : srcHeight;

        // Apply scaling if necessary.
        if (inWidth != dstWidth || inHeight != dstHeight) {
            final float scaleFactorX = dstWidth / (float) inWidth;
            final float scaleFactorY = dstHeight / (float) inHeight;

            if (maintainAspectRatio) {
                // Scale by minimum factor so that dst is filled completely while
                // maintaining the aspect ratio. Some image may fall off the edge.
                final float scaleFactor = Math.max(scaleFactorX, scaleFactorY);
                matrix.postScale(scaleFactor, scaleFactor);
            } else {
                // Scale exactly to fill dst from src.
                matrix.postScale(scaleFactorX, scaleFactorY);
            }
        }

        if (applyRotation != 0) {
            // Translate back from origin centered reference to destination frame.
            matrix.postTranslate(dstWidth / 2.0f, dstHeight / 2.0f);
        }

        return matrix;
    }


//  private static final float BITMAP_SCALE = 0.25f;
//  private static final float BLUR_RADIUS = 25f;
//
//  public static Bitmap blur(Context context, Bitmap image) {
//
//
//
//    Bitmap inputBitmap = Bitmap.createBitmap(image);
//    Bitmap outputBitmap = Bitmap.createBitmap(image);
//    RenderScript rs = RenderScript.create(context);
//    ScriptIntrinsicBlur intrinsicBlur = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
//    Allocation tmpIn = Allocation.createFromBitmap(rs, inputBitmap);
//    Allocation tmpOut = Allocation.createFromBitmap(rs, outputBitmap);
//    intrinsicBlur.setRadius(BLUR_RADIUS);
//    intrinsicBlur.setInput(tmpIn);
//    intrinsicBlur.forEach(tmpOut);
//    tmpOut.copyTo(outputBitmap);
//    return outputBitmap;
//    }


    public static Bitmap blur(Context context, Bitmap image) {
        int width = Math.round(image.getWidth());
        int height = Math.round(image.getHeight());

        Bitmap inputBitmap = Bitmap.createScaledBitmap(image, width, height, false);
        Bitmap outputBitmap = Bitmap.createBitmap(inputBitmap);
        RenderScript rs = RenderScript.create(context);
        ScriptIntrinsicBlur intrinsicBlur = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
        Allocation tmpIn = Allocation.createFromBitmap(rs, inputBitmap);
        Allocation tmpOut = Allocation.createFromBitmap(rs, outputBitmap);
        intrinsicBlur.setRadius(BLUR_RADIUS);
        intrinsicBlur.setInput(tmpIn);
        intrinsicBlur.forEach(tmpOut);
        tmpOut.copyTo(outputBitmap);
        return outputBitmap;
    }

    // blur image by looping the render script so that the blur effect
    // is more obvious
    public static Bitmap deepBlur(Context context, Bitmap image) {
        int width = Math.round(image.getWidth());
        int height = Math.round(image.getHeight());
        Bitmap inputBitmap = Bitmap.createScaledBitmap(image, width, height, false);
        Bitmap outputBitmap = Bitmap.createBitmap(inputBitmap);
        RenderScript rs = RenderScript.create(context);

        long t = System.currentTimeMillis();
        for (int i = 0; i < 8; i++) {
            final Allocation input = Allocation.createFromBitmap(rs, outputBitmap); //use this constructor for best performance, because it uses USAGE_SHARED mode which reuses memory
            final Allocation output = Allocation.createTyped(rs, input.getType());
            final ScriptIntrinsicBlur script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));

            script.setRadius(25F);
            script.setInput(input);
            script.forEach(output);
            output.copyTo(outputBitmap);
        }

        Log.i("AAA", "blur: took: " + (System.currentTimeMillis() - t));

        return outputBitmap;
    }


    public static Bitmap blur(Context context, Bitmap image, ArrayList<RectF> locations) {

        Bitmap copy = image;
        if (!image.isMutable()) {
            copy = image.copy(Bitmap.Config.ARGB_8888, true);
        }
        for (RectF location : locations) {
            copy = blur(context, copy, location);
        }
        return copy;
    }

    public static Bitmap blur(Context context, Bitmap image, RectF location) {

        float widthFactor = image.getWidth() / SIZE;
        float heightFactor = image.getHeight() / SIZE;


        int x0 = (int) Math.floor((location.left) * widthFactor);
        int y0 = (int) Math.floor((location.top) * heightFactor);
        int x1 = (int) Math.floor(((location.right)) * widthFactor);
        int y1 = (int) Math.floor(((location.bottom)) * heightFactor);

        // bound coordinates between 0 and the width or height respective
        // the number would be out of bound due to rounding issues
        // encountered during testing
        if (x0 < 0 || x0 > image.getWidth()) {
            x0 = x0 < 0 ? 0 : image.getWidth();
        }
        if (x1 < 0 || x1 > image.getWidth()) {
            x1 = x1 < 0 ? 0 : image.getWidth();
        }

        if (y1 < 0 || y1 > image.getHeight()) {
            y1 = y1 < 0 ? 0 : image.getHeight();
        }

        if (y0 < 0 || y0 > image.getHeight()) {
            y0 = y0 < 0 ? 0 : image.getHeight();
        }

        int croppedWidth = x1 - x0;
        int croppedHeight = y1 - y0;

//        Bitmap inputBitmap = Bitmap.createScaledBitmap(image, width, height, false);
        try {
            Bitmap inputBitmap = Bitmap.createBitmap(image, x0, y0, croppedWidth, croppedHeight);
//            saveBitmap(inputBitmap, "/sdcard/Android/data/com.lucienchu.tensorflowliteobjectdetection/files/Documents/images/part_not_blur.jpg");

            Bitmap outputBitmap = Bitmap.createBitmap(inputBitmap);
            RenderScript rs = RenderScript.create(context);
            ScriptIntrinsicBlur intrinsicBlur = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
            Allocation tmpIn = Allocation.createFromBitmap(rs, inputBitmap);
            Allocation tmpOut = Allocation.createFromBitmap(rs, outputBitmap);
            intrinsicBlur.setRadius(BLUR_RADIUS);
            intrinsicBlur.setInput(tmpIn);
            intrinsicBlur.forEach(tmpOut);

            tmpOut.copyTo(outputBitmap);


//        return outputBitmap;

//            saveBitmap(image, "/sdcard/Android/data/com.lucienchu.tensorflowliteobjectdetection/files/Documents/images/origin_not_merge.jpg");

            return getOverlayBitMap(image, outputBitmap, x0, y0);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Bitmap getOverlayBitMap(Bitmap image, Bitmap part, int x0, int y0) {
//        saveBitmap(image, "/sdcard/Android/data/com.lucienchu.tensorflowliteobjectdetection/files/Documents/images/origin_not_merge.jpg");

        Bitmap copy = image;
        if (!image.isMutable()) {
            copy = image.copy(Bitmap.Config.ARGB_8888, true);
        }
//        saveBitmap(part, "/sdcard/Android/data/com.lucienchu.tensorflowliteobjectdetection/files/Documents/images/part_original_blur.jpg");

        int[] blurredPixels = new int[part.getHeight() * part.getWidth()];
        part.getPixels(blurredPixels, 0, part.getWidth(), 0, 0, part.getWidth(), part.getHeight());
//        Bitmap newBlur = Bitmap.createBitmap(blurredPixels, 0,part.getWidth(),part.getWidth(), part.getHeight(), Bitmap.Config.ARGB_8888);
//        saveBitmap(newBlur, "/sdcard/Android/data/com.lucienchu.tensorflowliteobjectdetection/files/Documents/images/part_check_from_pixels.jpg");


//        image.setPixels(pixels, 0, image.getWidth(), 0, 0, image.getWidth(), image.getHeight());
        copy.setPixels(blurredPixels, 0, part.getWidth(), x0, y0, part.getWidth(), part.getHeight());
//                saveBitmap(copy, "/sdcard/Android/data/com.lucienchu.tensorflowliteobjectdetection/files/Documents/images/origin_merged.jpg");

        return copy;
    }


    /**
     * give an image and a set of locations, mask out the pixels within the locations
     *
     * @param image     original image
     * @param locations locations that the pixels would be masked
     * @return a deep copy of the original image which as pixels within the locations masked by a color
     */
    public static Bitmap getMaskBitMap(Bitmap image, ArrayList<RectF> locations) {
        Bitmap copy = image.copy(Bitmap.Config.ARGB_8888, true);
        for (RectF location : locations) {
            copy = getMaskBitMap(copy, location);
        }
        return copy;
    }


    /**
     * give an image and a location, mask out the pixels within the location
     *
     * @param image
     * @param location
     * @return
     */
    public static Bitmap getMaskBitMap(Bitmap image, RectF location) {
        float widthFactor = image.getWidth() / SIZE;
        float heightFactor = image.getHeight() / SIZE;


        int x0 = (int) Math.floor((location.left) * widthFactor);
        int y0 = (int) Math.floor((location.top) * heightFactor);
        int x1 = (int) Math.floor(((location.right)) * widthFactor);
        int y1 = (int) Math.floor(((location.bottom)) * heightFactor);

        // bound coordinates between 0 and the width or height respective
        // the number would be out of bound due to rounding issues
        // encountered during testing
        if (x0 < 0 || x0 > image.getWidth()) {
            x0 = x0 < 0 ? 0 : image.getWidth();
        }
        if (x1 < 0 || x1 > image.getWidth()) {
            x1 = x1 < 0 ? 0 : image.getWidth();
        }

        if (y1 < 0 || y1 > image.getHeight()) {
            y1 = y1 < 0 ? 0 : image.getHeight();
        }

        if (y0 < 0 || y0 > image.getHeight()) {
            y0 = y0 < 0 ? 0 : image.getHeight();
        }


        for (int x = x0; x < x1; x++) {
            for (int y = y0; y < y1; y++) {
                try {
                    image.setPixel(x, y, Color.BLUE);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                }
            }
        }
        return image;
    }


    /**
     * get the number of the degree an image should rotate so that
     * the image would not be flipped (visually).
     *
     * @param exifOrientation orientation
     * @return number of degree for image to rotate
     */
    private static int exifToDegrees(int exifOrientation) {
        if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) {
            return 90;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {
            return 180;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {
            return 270;
        }
        return 0;
    }

    /**
     * based on the exif info of the target bitmap, retrieve from its file path, get the target's
     * orientation and return a copy of it which is rotated by 180 degrees
     *
     * @param origin   target bitmap to be rotate
     * @param filePath file path of the target bitmap
     * @return a copy of the target bitmap which has been rotated to normal orientation
     */
    public static Bitmap getRotatedBitmap(Bitmap origin, String filePath) {
        ExifInterface exif = null;
        try {
            exif = new ExifInterface(filePath);
        } catch (IOException e) {
            return null;
        }

        // flipped rotation = 3;
        // normal rotation = 1

        int rotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        if (rotation == 3) {
            int rotationInDegrees = exifToDegrees(rotation);
            Matrix matrix = new Matrix();
            matrix.preRotate(rotationInDegrees);

            return Bitmap.createBitmap(origin, 0, 0, origin.getWidth(), origin.getHeight(), matrix, false);
        }
        return origin;
    }
}
