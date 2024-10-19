package com.converter.image2sketch;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.Core;

import java.nio.IntBuffer;


public class SketchImage {
    private Bitmap originalBitmap;
    private Context context;


    public static final int ORIGINAL_TO_GRAY = 0;
    public static final int ORIGINAL_TO_SKETCH = 1;
    public static final int ORIGINAL_TO_COLORED_SKETCH = 2;
    public static final int ORIGINAL_TO_SOFT_SKETCH = 3;
    public static final int ORIGINAL_TO_SOFT_COLOR_SKETCH = 4;

    public static final int GRAY_TO_SKETCH = 5;
    public static final int GRAY_TO_COLORED_SKETCH = 6;
    public static final int GRAY_TO_SOFT_SKETCH = 7;
    public static final int GRAY_TO_SOFT_COLOR_SKETCH = 8;
    public static final int SKETCH_TO_COLORED_SKETCH = 9;


    public static class Builder {
        private Bitmap bitmap;
        private Context context;

        public Builder(Context context, Bitmap bitmap) {
            this.context = context;
            this.bitmap = bitmap;
        }

        public SketchImage build() {
            return new SketchImage(this);
        }
    }

    private SketchImage(Builder builder) {
        this.originalBitmap = builder.bitmap;
        this.context = builder.context;
    }

    public Bitmap getImageAs(int effectType, int thickness) {
        Log.d("SketchImage", "Effect type: " + effectType);
        Bitmap result = null;


        Bitmap downscaledBitmap = downscaleImage(originalBitmap, 800);

        switch (effectType) {
            case ORIGINAL_TO_GRAY:
                result = toGrayScale(downscaledBitmap);
                break;
            case ORIGINAL_TO_SKETCH:
                Bitmap sketchBitmap = toSketch(downscaledBitmap, thickness);
                result = toColoredSketch(sketchBitmap, thickness);
                break;
            case ORIGINAL_TO_COLORED_SKETCH:
                result = toColoredSketch(downscaledBitmap, thickness);
                break;
            case ORIGINAL_TO_SOFT_SKETCH:
                result = toSoftSketch(downscaledBitmap, thickness);
                break;
            case ORIGINAL_TO_SOFT_COLOR_SKETCH:
                result = toSoftColorSketch(downscaledBitmap, thickness);
                break;
            case GRAY_TO_SKETCH:
                result = toSketch(toGrayScale(downscaledBitmap), thickness);
                break;
            case GRAY_TO_COLORED_SKETCH:
                result = toColoredSketch(toGrayScale(downscaledBitmap), thickness);
                break;
            case GRAY_TO_SOFT_SKETCH:
                result = toSoftSketch(toGrayScale(downscaledBitmap), thickness);
                break;
            case GRAY_TO_SOFT_COLOR_SKETCH:
                result = toSoftColorSketch(toGrayScale(downscaledBitmap), thickness);
                break;
            case SKETCH_TO_COLORED_SKETCH:
                Bitmap sketchBitmap2 = toSketch(downscaledBitmap, thickness);
                result = toColoredSketch(sketchBitmap2, thickness);
                break;
            default:
                result = downscaledBitmap;
        }

        Log.d("SketchImage", "Result bitmap dimensions: " + (result != null ? result.getWidth() + "x" + result.getHeight() : "null"));
        return result;
    }

    private Bitmap downscaleImage(Bitmap bitmap, int maxSize) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (width > maxSize || height > maxSize) {
            float aspectRatio = (float) width / (float) height;
            if (width > height) {
                width = maxSize;
                height = Math.round(maxSize / aspectRatio);
            } else {
                height = maxSize;
                width = Math.round(maxSize * aspectRatio);
            }
            return Bitmap.createScaledBitmap(bitmap, width, height, true);
        }
        return bitmap;
    }

    private Bitmap toGrayScale(Bitmap bitmap) {
        Mat mat = new Mat();
        Utils.bitmapToMat(bitmap, mat);
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGB2GRAY);
        Bitmap grayBitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat, grayBitmap);
        return grayBitmap;
    }

    private Bitmap toSketch(Bitmap bitmap, int thickness) {
        Bitmap grayBitmap = toGrayScale(bitmap);

        Mat matGray = new Mat();
        Mat matInverted = new Mat();
        Utils.bitmapToMat(grayBitmap, matGray);
        Core.bitwise_not(matGray, matInverted);


        Mat matBlurred = new Mat();
        Size blurSize = new Size(thickness * 2 + 1, thickness * 2 + 1);
        Imgproc.GaussianBlur(matInverted, matBlurred, blurSize, 0);


        Bitmap blurredBitmap = Bitmap.createBitmap(matBlurred.cols(), matBlurred.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(matBlurred, blurredBitmap);


        Bitmap sketchBitmap = ColorDodgeBlend(grayBitmap, blurredBitmap);

        return sketchBitmap;
    }

    private int colordodge(int in1, int in2) {
        float image = (float) in2;
        float mask = (float) in1;
        return ((int) ((image == 255) ? image : Math.min(255, (((long) mask << 8) / (255 - image)))));
    }

    public Bitmap ColorDodgeBlend(Bitmap source, Bitmap layer) {
        Bitmap base = source.copy(Bitmap.Config.ARGB_8888, true);
        Bitmap blend = layer.copy(Bitmap.Config.ARGB_8888, false);

        IntBuffer buffBase = IntBuffer.allocate(base.getWidth() * base.getHeight());
        base.copyPixelsToBuffer(buffBase);
        buffBase.rewind();

        IntBuffer buffBlend = IntBuffer.allocate(blend.getWidth() * blend.getHeight());
        blend.copyPixelsToBuffer(buffBlend);
        buffBlend.rewind();

        IntBuffer buffOut = IntBuffer.allocate(base.getWidth() * base.getHeight());
        buffOut.rewind();

        while (buffOut.position() < buffOut.limit()) {
            int filterInt = buffBlend.get();
            int srcInt = buffBase.get();

            int redValueFilter = Color.red(filterInt);
            int greenValueFilter = Color.green(filterInt);
            int blueValueFilter = Color.blue(filterInt);

            int redValueSrc = Color.red(srcInt);
            int greenValueSrc = Color.green(srcInt);
            int blueValueSrc = Color.blue(srcInt);

            int redValueFinal = colordodge(redValueFilter, redValueSrc);
            int greenValueFinal = colordodge(greenValueFilter, greenValueSrc);
            int blueValueFinal = colordodge(blueValueFilter, blueValueSrc);

            int pixel = Color.argb(255, redValueFinal, greenValueFinal, blueValueFinal);
            buffOut.put(pixel);
        }

        buffOut.rewind();
        base.copyPixelsFromBuffer(buffOut);
        blend.recycle();

        return base;
    }



    private Bitmap toColoredSketch(Bitmap bitmap, int thickness) {
        Bitmap sketchBitmap = toSketch(bitmap, thickness);
        Mat matOriginal = new Mat();
        Mat matSketch = new Mat();
        Mat matColoredSketch = new Mat();

        Utils.bitmapToMat(bitmap, matOriginal);
        Utils.bitmapToMat(sketchBitmap, matSketch);

        Core.addWeighted(matOriginal, 0.5, matSketch, 0.5, 0, matColoredSketch);

        Bitmap coloredSketchBitmap = Bitmap.createBitmap(matColoredSketch.cols(), matColoredSketch.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(matColoredSketch, coloredSketchBitmap);

        return coloredSketchBitmap;
    }

    private Bitmap toSoftSketch(Bitmap bitmap, int thickness) {
        Mat matGray = new Mat();
        Mat matBlurred = new Mat();
        Mat matResult = new Mat();

        Utils.bitmapToMat(toGrayScale(bitmap), matGray);
        Size blurSize = new Size(thickness * 2 + 1, thickness * 2 + 1);
        Imgproc.GaussianBlur(matGray, matBlurred, blurSize, 0);
        Core.divide(matGray, matBlurred, matResult, 256);

        Bitmap softSketchBitmap = Bitmap.createBitmap(matResult.cols(), matResult.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(matResult, softSketchBitmap);

        return softSketchBitmap;
    }

    private Bitmap toSoftColorSketch(Bitmap bitmap, int thickness) {
        Bitmap softSketch = toSoftSketch(bitmap, thickness);
        Bitmap coloredSketch = toColoredSketch(bitmap, thickness);
        Mat matSoftSketch = new Mat();
        Mat matColoredSketch = new Mat();
        Mat matResult = new Mat();

        Utils.bitmapToMat(softSketch, matSoftSketch);
        Utils.bitmapToMat(coloredSketch, matColoredSketch);

        Core.addWeighted(matSoftSketch, 0.5, matColoredSketch, 0.5, 0, matResult);

        Bitmap softColorSketchBitmap = Bitmap.createBitmap(matResult.cols(), matResult.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(matResult, softColorSketchBitmap);

        return softColorSketchBitmap;
    }
}