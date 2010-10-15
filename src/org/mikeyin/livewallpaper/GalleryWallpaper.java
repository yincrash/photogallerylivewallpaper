package org.mikeyin.livewallpaper;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Random;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Environment;
import android.os.Handler;
import android.service.wallpaper.WallpaperService;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.WindowManager;

public class GalleryWallpaper extends WallpaperService {

  public GalleryWallpaper() {
    super();
  }

  @Override
  public void onCreate() {
    super.onCreate();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
  }

  @Override
  public Engine onCreateEngine() {
    return new GalleryEngine();
  }

  public class GalleryEngine extends Engine implements OnSharedPreferenceChangeListener {

    private final Handler               handler           = new Handler();

    /**
     * Filter for choosing only files with image extensions
     */
    private final FilenameFilter        IMAGE_FILTER      = new ImageFilenameFilter();

    public static final String          SHARED_PREFS_NAME = "org.mikeyin:gallerywallpaper";

    private final Random                randGen           = new Random();

    private final Runnable              animate           = new Runnable() {
                                                            public void run() {
                                                              showNewImage();
                                                            }
                                                          };

    private Rect                        surfaceFrame      = getSurfaceHolder().getSurfaceFrame();
    private final BitmapFactory.Options options           = new BitmapFactory.Options();
    private final BitmapFactory.Options optionsScale      = new BitmapFactory.Options();

    private static final String         TAG               = "GalleryEngine";

    private Bitmap                      currentBitmap     = null;
    private File                        currentFile       = null;
    private long                        timer             = 5000;
    private long                        timeStarted       = 0;
    private File[]                      images;
    private final GestureDetector       doubleTapDetector;

    private final Paint                 noImagesPaint     = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint                 imagePaint;

    private boolean                     isMounted;

    private int                         xPixelOffset;
    private float                       xOffset;
    private float                       xOffsetStep;

    private boolean                     isRotate;
    private boolean                     isScrolling;
    private boolean                     isStretch;
    private boolean                     isTrim;

    private boolean                     allowClickToChange;

    private int                         desiredMinimumWidth;
    private int                         screenHeight;
    private int                         screenWidth;

    private int                         transition;
    private int                         currentAlpha;

    private boolean                     imageIsSetup      = false;

    /**
     * Set up the shared preferences listener and initialize the default prefs
     */
    public GalleryEngine() {
      allowClickToChange = false;
      currentAlpha = 0;
      imagePaint = new Paint();
      imagePaint.setAlpha(255);
      options.inTempStorage = new byte[16 * 1024];

      optionsScale.inTempStorage = options.inTempStorage;
      optionsScale.inSampleSize = 4;

      noImagesPaint.setTextAlign(Paint.Align.CENTER);
      noImagesPaint.setColor(Color.GRAY);
      noImagesPaint.setTextSize(24);
      // register the listener to detect preference changes
      SharedPreferences prefs = getSharedPreferences(SHARED_PREFS_NAME, 0);
      prefs.registerOnSharedPreferenceChangeListener(this);
      // initialize the starting preferences
      onSharedPreferenceChanged(prefs, null);
      setTouchEventsEnabled(true);
      doubleTapDetector = new GestureDetector(new DoubleTapGestureListener(this));

      getScreenSize();

      // surfaceFrame = new Rect(0, 0, metrics.widthPixels,
      // metrics.heightPixels);
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.service.wallpaper.WallpaperService.Engine#onSurfaceDestroyed
     * (android.view.SurfaceHolder)
     */
    @Override
    public void onSurfaceDestroyed(SurfaceHolder holder) {
      super.onSurfaceDestroyed(holder);
      handler.removeCallbacks(fadeAnimate);
      handler.removeCallbacks(animate);
    }

    @Override
    public void onOffsetsChanged(float xOffset, float yOffset, float xOffsetStep,
        float yOffsetStep, int xPixelOffset, int yPixelOffset) {
      // an extra check -- this function seems to be called often for some
      // reason
      if (this.xPixelOffset != xPixelOffset * -1 || this.xOffset != xOffset
          || this.xOffsetStep != xOffsetStep) {
        this.xPixelOffset = xPixelOffset * -1;
        this.xOffset = xOffset;
        this.xOffsetStep = xOffsetStep;
        drawBitmap(currentBitmap);
      }
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.service.wallpaper.WallpaperService.Engine#onDestroy()
     */
    @Override
    public void onDestroy() {
      super.onDestroy();
      handler.removeCallbacks(fadeAnimate);
      handler.removeCallbacks(animate);
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.service.wallpaper.WallpaperService.Engine#onCreate(android
     * .view.SurfaceHolder)
     */
    @Override
    public void onCreate(SurfaceHolder surfaceHolder) {
      super.onCreate(surfaceHolder);
      this.desiredMinimumWidth = getDesiredMinimumWidth();
      // this.desiredMinimumHeight = getDesiredMinimumHeight();
      showNewImage();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * android.service.wallpaper.WallpaperService.Engine#onVisibilityChanged
     * (boolean)
     */
    @Override
    public void onVisibilityChanged(boolean visible) {
      super.onVisibilityChanged(visible);
      getScreenSize(); // onVisibilityChanged can be called on screen rotation.
      if (visible) {
        // if there is a bitmap with time left to keep around, redraw it
        if (currentFile != null && systemTime() - timeStarted + 100 < timer) {
          // for some reason, it's sometimes recycled!
          if (currentBitmap.isRecycled()) {
            currentBitmap = BitmapFactory.decodeFile(currentFile.getAbsolutePath());
            imageIsSetup = false;
          }
          drawBitmap(currentBitmap);
          // left over timer
          handler.postDelayed(animate, timer - (systemTime() - timeStarted));
        }
        // otherwise draw a new one since it's time for a new one
        else {
          showNewImage();
        }
      } else {
        handler.removeCallbacks(fadeAnimate);
        handler.removeCallbacks(animate);
      }
    }

    /**
     * This happens on launcher rotation
     */
    @Override
    public void onDesiredSizeChanged(int desiredWidth, int desiredHeight) {
      super.onDesiredSizeChanged(desiredWidth, desiredHeight);
      Log.v(TAG, "onDesiredSizeChanged");
      getScreenSize();
      this.desiredMinimumWidth = desiredWidth;
      
      drawBitmap(currentBitmap);
    }

    /**
     * 
     */
    private void getScreenSize()
    {
      DisplayMetrics metrics = new DisplayMetrics();
      Display display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
      display.getMetrics(metrics);

      if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
        this.screenWidth = metrics.heightPixels;
        this.screenHeight = metrics.widthPixels;
      }
      this.screenHeight = metrics.heightPixels;
      this.screenWidth = metrics.widthPixels;
    }

    /**
     * Draw the bitmap to the surface canvas
     * 
     * @param b
     *          the bitmap to draw
     */
    private void drawBitmap(Bitmap b) {
      if (b == null) {
        Log.d(TAG, "b == null!");
        /*
         * { try { throw new RuntimeException(); } catch (Exception e) {
         * e.printStackTrace(); } }
         */
        return;
      }

      final SurfaceHolder holder = getSurfaceHolder();

      int virtualWidth = this.desiredMinimumWidth;

      // shouldn't happen
      if (surfaceFrame == null) {
        Log.d(TAG, "surfaceFrame == null!");
      }

      Rect window;
      Rect dstWindow = surfaceFrame;
      if (!isScrolling) {
        // virtual width becomes screen width
        virtualWidth = screenWidth;
        xPixelOffset = 0;
      }

      // not sure why but it happens
      if (virtualWidth == 0) {
        Log.d(TAG, "virtualWidth == 0 !!");
        return;
      }

      int virtualHeight = screenHeight;
      Log.d(TAG, "width = " + virtualWidth + " height = " + virtualHeight);

      // we only need to convert the image once, then we can reuse it
      if (b == currentBitmap && !imageIsSetup) {
        System.gc();

        float screenRatio = (float) virtualWidth / (float) virtualHeight;
        float bitmapRatio = (float) b.getWidth() / (float) b.getHeight();

        if (isTrim) {

          // downscale the bitmap to make the trim detection faster
          Matrix dmatrix = new Matrix();
          float dscale = (float) 50 / b.getWidth();
          dmatrix.postScale(dscale, dscale * bitmapRatio);
          Bitmap c = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), dmatrix, true);
          int trimLeft = (int) (BitmapHelper.sizeTrimmableLeft(c) * b.getWidth());
          int trimRight = (int) (BitmapHelper.sizeTrimmableRight(c) * b.getWidth());
          int trimTop = (int) (BitmapHelper.sizeTrimmableTop(c) * b.getHeight());
          int trimBottom = (int) (BitmapHelper.sizeTrimmableBottom(c) * b.getHeight());
          c.recycle();

          // b = BitmapHelper.doTrim(b, trimLeft, trimRight, trimTop,
          // trimBottom);

          // we calculate the aspect ratio of the important part of the picture
          bitmapRatio = (float) (b.getWidth() - trimLeft - trimRight)
              / (float) (b.getHeight() - trimTop - trimBottom);
        }

        // rotate the bitmap if rotation is enabled and it would fit the screen
        // better
        if (isRotate
            && ((screenRatio > 1 && bitmapRatio < 1) || (screenRatio < 1 && bitmapRatio > 1))) {
          b = BitmapHelper.doRotate(b, 90);
          // get the ratio again
          bitmapRatio = (float) b.getWidth() / (float) b.getHeight();
        }

        if (isTrim) {
          // TODO remove this redundancy
          Matrix dmatrix = new Matrix();

          int downScaleRes = 100;

          float dscale = (float) downScaleRes / b.getWidth();
          dmatrix.postScale(dscale, dscale * bitmapRatio);
          Bitmap c = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), dmatrix, true);

          final int extraBorder = 10;

          int trimLeft = (int) (BitmapHelper.sizeTrimmableLeft(c) * b.getWidth()) - extraBorder;
          int trimRight = (int) (BitmapHelper.sizeTrimmableRight(c) * b.getWidth()) - extraBorder;
          int trimTop = (int) (BitmapHelper.sizeTrimmableTop(c) * b.getHeight()) - extraBorder;
          int trimBottom = (int) (BitmapHelper.sizeTrimmableBottom(c) * b.getHeight())
              - extraBorder;
          c.recycle();

          if (trimLeft < 0)
            trimLeft = 0;
          if (trimRight < 0)
            trimRight = 0;
          if (trimTop < 0)
            trimTop = 0;
          if (trimBottom < 0)
            trimBottom = 0;

          // TODO replace this crap with actual math
          while (true) {
            bitmapRatio = (float) (b.getWidth() - trimLeft - trimRight)
                / (float) (b.getHeight() - trimTop - trimBottom);

            if (trimLeft == 0 && trimRight == 0 && trimTop == 0 && trimBottom == 0
                || Math.abs(screenRatio - bitmapRatio) < 0.01)
              break;

            if (bitmapRatio < screenRatio) {
              // horizontal
              if (trimLeft == 0 && trimRight == 0)
                break;
              if (trimLeft != 0)
                trimLeft--;
              if (trimRight != 0)
                trimRight--;
            } else {
              // vertical
              if (trimTop == 0 && trimBottom == 0)
                break;
              if (trimTop != 0)
                trimTop--;
              if (trimBottom != 0)
                trimBottom--;
            }
          }
          b = BitmapHelper.doTrim(b, trimLeft, trimRight, trimTop, trimBottom);

        }

        // scale the bitmap without distortion or cropping to fit the screen
        // as well as possible
        if (isStretch) {
          Log.d(TAG, "pic dimensions: " + b.getWidth() + "x" + b.getHeight());
          float scale = 0;
          if (virtualHeight - b.getHeight() < virtualWidth - b.getWidth()) {
            // vertically
            scale = (float) virtualHeight / b.getHeight();
            Log.d(TAG, "vertical scale: " + scale);
          } else {
            // horizontally
            scale = (float) virtualWidth / b.getWidth();
            Log.d(TAG, "horizontal scale: " + scale);
          }

          b = BitmapHelper.doScale(b, scale);
        }

        imageIsSetup = true;
        currentBitmap = b;
        System.gc();
      }

      int vertMargin = (b.getHeight() - virtualHeight) / 2;

      if (b.getWidth() >= virtualWidth && b.getHeight() >= virtualHeight) {
        int pictureHorizOffset = xPixelOffset + (b.getWidth() - virtualWidth) / 2;
        window = new Rect(pictureHorizOffset, vertMargin, pictureHorizOffset + surfaceFrame.right,
            b.getHeight() - vertMargin);
      } else {
        int pictureHorizOffset = xPixelOffset + (b.getWidth() - virtualWidth) / 2;

        window = null;
        dstWindow = new Rect(surfaceFrame);
        dstWindow.top = -vertMargin;
        dstWindow.bottom = b.getHeight() - vertMargin;
        dstWindow.left = -pictureHorizOffset;
        dstWindow.right = -pictureHorizOffset + b.getWidth();
      }

      Canvas c = null;
      try {
        c = holder.lockCanvas();
        if (c != null && b != null) {
          c.drawColor(Color.BLACK);
          c.drawBitmap(b, window, dstWindow, imagePaint);
        }
      } finally {
        if (c != null)
          holder.unlockCanvasAndPost(c);
      }

    }

    @Override
    public void onTouchEvent(MotionEvent event) {
      super.onTouchEvent(event);
      this.doubleTapDetector.onTouchEvent(event);
    }

    /**
     * Called whenever you want a new image. This will also post the message for
     * when the next frame should be drawn
     */
    protected void showNewImage() {
      Log.d(TAG, "showNewImage");

      if (images != null && images.length != 0) {
        File image = images[randGen.nextInt(images.length)];

        if (currentBitmap != null)
          currentBitmap.recycle();
        currentBitmap = null;
        System.gc();

        try {
          currentFile = image;
          currentBitmap = BitmapFactory.decodeFile(image.getAbsolutePath(), options);
          imageIsSetup = false;

          switch (transition) {
            case GalleryWallpaperSettings.FADE_TRANSITION:
              fadeTransition(currentBitmap, 0);
              break;
            default:
              drawBitmap(currentBitmap);
              break;
          }
        } catch (OutOfMemoryError e) {
          try {
            System.gc();
            Log.i(TAG, "Image too big, attempting to scale.");
            currentBitmap = BitmapFactory.decodeFile(image.getAbsolutePath(), optionsScale);
            drawBitmap(currentBitmap);
            Log.i(TAG, "Scale successful.");
          } catch (OutOfMemoryError e2) {
            Log.e(TAG, "Scale failed: " + image.getAbsolutePath());
            // skip to next image.
            showNewImage();
            return;
          }
        }
      } else if (!isMounted) {
        drawTextHelper(getString(R.string.sd_card_not_available));
      } else {
        drawTextHelper(getString(R.string.no_images_found));
      }

      /*
       * This is how it animates. After drawing a frame, ask it to draw another
       * one.
       */
      handler.removeCallbacks(animate);
      if (isVisible()) {
        handler.postDelayed(animate, timer);
        timeStarted = systemTime();
      }
    }

    private final Runnable fadeAnimate = new Runnable() {
                                         public void run() {
                                           fadeTransition(currentBitmap, currentAlpha);
                                         }
                                       };

    /**
     * Execute a fade transition. Increments the current alpha then draws at
     * that alpha then posts a message for it to be rerun It should cycle from 0
     * to 255 in 13 frames.
     * 
     * @param b
     *          the bitmap to fade
     * @param alpha
     *          the alpha to start at, or the current alpha
     */
    private void fadeTransition(Bitmap b, int alpha) {
      currentAlpha = alpha;
      currentAlpha += 255 / 25;
      if (currentAlpha > 255) {
        currentAlpha = 255;
      }
      // Log.v(TAG, "alpha " + currentAlpha);
      imagePaint.setAlpha(currentAlpha);
      drawBitmap(b);

      /*
       * This is how it animates. After drawing a frame, ask it to draw another
       * one.
       */
      handler.removeCallbacks(fadeAnimate);
      if (isVisible() && currentAlpha < 255) // stop when at full opacity
      {
        handler.post(fadeAnimate);
      }
    }

    private void drawTextHelper(String line) {
      final SurfaceHolder holder = getSurfaceHolder();

      Canvas c = null;
      try {
        c = holder.lockCanvas();
        if (c != null) {
          c.drawColor(Color.BLACK);
          c.drawText(line, surfaceFrame.right / 2, surfaceFrame.bottom / 2, noImagesPaint);
        }
      } finally {
        if (c != null)
          holder.unlockCanvasAndPost(c);
      }
    }

    /**
     * Convenience method to return a time that is suitable for measuring
     * timeouts in milliseconds
     * 
     * @return the system time in milliseconds. could be negative.
     * @see System#nanoTime()
     */
    private long systemTime() {
      return System.nanoTime() / 1000000;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPrefs, String keyChanged) {
      timer = Integer.valueOf(sharedPrefs.getString(GalleryWallpaperSettings.TIMER_KEY, "5000"));
      isRotate = sharedPrefs.getBoolean(GalleryWallpaperSettings.ROTATE_KEY, true);
      isScrolling = sharedPrefs.getBoolean(GalleryWallpaperSettings.SCROLLING_KEY, true);
      isStretch = sharedPrefs.getBoolean(GalleryWallpaperSettings.STRETCHING_KEY, false);
      isTrim = sharedPrefs.getBoolean(GalleryWallpaperSettings.TRIM_KEY, true);
      allowClickToChange = sharedPrefs.getBoolean(GalleryWallpaperSettings.CLICK_TO_CHANGE_KEY,
          false);
      String state = Environment.getExternalStorageState();
      File folder;
      if (Environment.MEDIA_MOUNTED.equals(state)
          || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
        folder = new File(sharedPrefs.getString(GalleryWallpaperSettings.FOLDER_KEY, "/"));
      } else {
        folder = new File("/");
      }
      images = folder.listFiles(IMAGE_FILTER);
      isMounted = sharedPrefs.getBoolean(GalleryWallpaperSettings.IS_MOUNTED, true);
      transition = Integer.valueOf(sharedPrefs.getString(GalleryWallpaperSettings.TRANSITION_KEY,
          "0"));
      if (transition != GalleryWallpaperSettings.FADE_TRANSITION) {
        imagePaint.setAlpha(255);
      }
    }

    public boolean allowClickToChange() {
      return allowClickToChange;
    }
  }

}
