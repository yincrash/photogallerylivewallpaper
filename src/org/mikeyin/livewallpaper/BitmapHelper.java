package org.mikeyin.livewallpaper;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.util.Log;

public final class BitmapHelper {

  private static final String TAG = "BitmapHelper";

  private static final double colorDistance(int color1, int color2) {
    int redDistance = Color.red(color1) - Color.red(color2);
    int greenDistance = Color.green(color1) - Color.green(color2);
    int blueDistance = Color.blue(color1) - Color.blue(color2);
    double distance = Math.sqrt(redDistance * redDistance + greenDistance * greenDistance
        + blueDistance * blueDistance);
    return distance / 256.;
  }

  // maximum accepted similarity between pixels
  private static final double FUZZ = 0.1;

  public static final float sizeTrimmableLeft(Bitmap b) {
    for (int x = 0; x < b.getWidth() - 1; x++) {
      for (int y = 0; y < b.getHeight() - 1; y++)
        if (colorDistance(b.getPixel(x, y), b.getPixel(x, y + 1)) > FUZZ)
          return (float) x / b.getWidth();
      if (colorDistance(b.getPixel(x, 0), b.getPixel(x + 1, 0)) > FUZZ)
        return (float) x / b.getWidth();
    }
    return 0;
  }

  public static final float sizeTrimmableRight(Bitmap b) {
    for (int x = 1; x < b.getWidth(); x++) {
      for (int y = 0; y < b.getHeight() - 1; y++)
        if (colorDistance(b.getPixel(b.getWidth() - x, y), b.getPixel(b.getWidth() - x, y + 1)) > FUZZ)
          return (float) (x - 1) / b.getWidth();
      if (colorDistance(b.getPixel(b.getWidth() - x, 0), b.getPixel(b.getWidth() - x - 1, 0)) > FUZZ)
        return (float) (x - 1) / b.getWidth();
    }
    return 0;
  }

  public static final float sizeTrimmableTop(Bitmap b) {
    for (int y = 0; y < b.getHeight() - 1; y++) {
      for (int x = 0; x < b.getWidth() - 1; x++)
        if (colorDistance(b.getPixel(x, y), b.getPixel(x + 1, y)) > FUZZ)
          return (float) y / b.getHeight();
      if (colorDistance(b.getPixel(0, y), b.getPixel(0, y + 1)) > FUZZ)
        return (float) y / b.getHeight();
    }
    return 0;
  }

  public static final float sizeTrimmableBottom(Bitmap b) {
    for (int y = 1; y < b.getHeight(); y++) {
      for (int x = 0; x < b.getWidth() - 1; x++)
        if (colorDistance(b.getPixel(x, b.getHeight() - y), b.getPixel(x + 1, b.getHeight() - y)) > FUZZ)
          return (float) (y - 1) / b.getHeight();
      if (colorDistance(b.getPixel(0, b.getHeight() - y), b.getPixel(0, b.getHeight() - y - 1)) > FUZZ)
        return (float) (y - 1) / b.getHeight();
    }
    return 0;
  }

  public static Bitmap doTrim(Bitmap b, int trimLeft, int trimRight, int trimTop, int trimBottom) {
    Log.d(TAG, "doTrim " + trimLeft + " " + trimRight + " " + trimTop + " " + trimBottom);

    if (trimLeft < 0)
      trimLeft = 0;
    if (trimRight < 0)
      trimRight = 0;
    if (trimTop < 0)
      trimTop = 0;
    if (trimBottom < 0)
      trimBottom = 0;

    // this shouldn't really happen unless the whole picture is the same color
    if (trimLeft + trimRight >= b.getWidth() || trimTop + trimBottom >= b.getHeight())
      return b;

    b = Bitmap.createBitmap(b, trimLeft, trimTop, b.getWidth() - trimRight - trimLeft, b
        .getHeight()
        - trimBottom - trimTop, null, true);

    return b;
  }

  public static Bitmap doRotate(Bitmap b, float angle) {
    Matrix matrix = new Matrix();
    matrix.postRotate(angle);
    b = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), matrix, true);
    return b;
  }

  public static Bitmap doScale(Bitmap b, float scale) {
    Matrix matrix = new Matrix();
    matrix.postScale(scale, scale);
    b = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), matrix, true);
    return b;
  }

}
