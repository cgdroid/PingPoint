package com.tmhnry.pingpoint.vision;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.view.Display;
import android.view.Surface;

import androidx.camera.core.ImageProxy;

import com.tmhnry.pingpoint.TransferLearningModelWrapper;

import java.nio.ByteBuffer;

public class CameraUtils {
    private static final int LOWER_BYTE_MASK = 0xFF;

    public static float[][][] prepareCameraImage(Bitmap bitmap, int rotationDegrees) {
        int modelImageSize = TransferLearningModelWrapper.IMAGE_SIZE;


        Bitmap paddedBitmap = padToSquare(bitmap);
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(
                paddedBitmap, modelImageSize, modelImageSize, true);

        Matrix rotationMatrix = new Matrix();
        rotationMatrix.postRotate(rotationDegrees);
        Bitmap rotatedBitmap = Bitmap.createBitmap(
                scaledBitmap, 0, 0, modelImageSize, modelImageSize, rotationMatrix, false);

        float[][][] normalizedRgb = new float[modelImageSize][modelImageSize][3];
        for (int y = 0; y < modelImageSize; y++) {
            for (int x = 0; x < modelImageSize; x++) {
                int rgb = rotatedBitmap.getPixel(x, y);

                float r = ((rgb >> 16) & LOWER_BYTE_MASK) * (1 / 255.f);
                float g = ((rgb >> 8) & LOWER_BYTE_MASK) * (1 / 255.f);
                float b = (rgb & LOWER_BYTE_MASK) * (1 / 255.f);

                normalizedRgb[y][x][0] = r;
                normalizedRgb[y][x][1] = g;
                normalizedRgb[y][x][2] = b;
            }
        }

        return normalizedRgb;
    }


    private static Bitmap padToSquare(Bitmap source) {
        int width = source.getWidth();
        int height = source.getHeight();

        int paddingX = width < height ? (height - width) / 2 : 0;
        int paddingY = height < width ? (width - height) / 2 : 0;
        Bitmap paddedBitmap = Bitmap.createBitmap(
                width + 2 * paddingX, height + 2 * paddingY, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(paddedBitmap);
        canvas.drawARGB(0xFF, 0xFF, 0xFF, 0xFF);
        canvas.drawBitmap(source, paddingX, paddingY, null);
        return paddedBitmap;
    }




    public static Integer getDisplaySurfaceRotation(Display display) {
        if (display == null) {
            return null;
        }

        switch (display.getRotation()) {
            case Surface.ROTATION_0:
                return 0;
            case Surface.ROTATION_90:
                return 90;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_270:
                return 270;
            default:
                return null;
        }
    }

    public static Bitmap yuvCameraImageToBitmap(ImageProxy imageProxy) {
        if (imageProxy.getFormat() != ImageFormat.YUV_420_888) {
            throw new IllegalArgumentException(
                    "Expected a YUV420 image, but got " + imageProxy.getFormat());
        }

        ImageProxy.PlaneProxy yPlane = imageProxy.getPlanes()[0];
        ImageProxy.PlaneProxy uPlane = imageProxy.getPlanes()[1];

        int width = imageProxy.getWidth();
        int height = imageProxy.getHeight();

        byte[][] yuvBytes = new byte[3][];
        int[] argbArray = new int[width * height];
        for (int i = 0; i < imageProxy.getPlanes().length; i++) {
            final ByteBuffer buffer = imageProxy.getPlanes()[i].getBuffer();
            yuvBytes[i] = new byte[buffer.capacity()];
            buffer.get(yuvBytes[i]);
        }

        ImageUtils.convertYUV420ToARGB8888(
                yuvBytes[0],
                yuvBytes[1],
                yuvBytes[2],
                width,
                height,
                yPlane.getRowStride(),
                uPlane.getRowStride(),
                uPlane.getPixelStride(),
                argbArray);

        return Bitmap.createBitmap(argbArray, width, height, Bitmap.Config.ARGB_8888);
    }

}
