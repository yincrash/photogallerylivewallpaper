package org.mikeyin.livewallpaper;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Random;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Environment;
import android.os.Handler;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

public class GalleryWallpaper extends WallpaperService
{
  private final Handler handler = new Handler();

  /**
   * Filter for choosing only files with image extensions
   */
  private static final FilenameFilter IMAGE_FILTER = new FilenameFilter()
  {

    @Override
    public boolean accept(File dir, String filename)
    {
      filename = filename.toLowerCase();
      return filename.endsWith(".jpg") ||
             filename.endsWith(".png") ||
             filename.endsWith(".gif") ||
             filename.endsWith(".bmp");
    }
  };

  public static final String SHARED_PREFS_NAME = "org.mikeyin:gallerywallpaper";

  private final Random randGen = new Random();

  public GalleryWallpaper()
  {
    super();
  }

  @Override
  public void onCreate()
  {
    super.onCreate();
  }

  @Override
  public void onDestroy()
  {
      super.onDestroy();
  }

  @Override
  public Engine onCreateEngine()
  {
    return new GalleryEngine();
  }

  protected final class GalleryEngine extends Engine implements SharedPreferences.OnSharedPreferenceChangeListener
  {
    private final Runnable animate = new Runnable()
    {
      public void run()
      {
        showNewImage();
      }
    };
    private final Rect surfaceFrame = getSurfaceHolder().getSurfaceFrame();
    private final BitmapFactory.Options options = new BitmapFactory.Options();
    private final BitmapFactory.Options optionsScale = new BitmapFactory.Options();

    private Bitmap currentBitmap = null;
    private File currentFile = null;
    private long timer = 5000;
    private long timeStarted = 0;
    private File[] images;
    private final GestureDetector doubleTapDetector;

    Paint noImagesPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint imagePaint;
    private boolean isMounted;
    private int xPixelOffset;
    private float xOffset;
    private float xOffsetStep;
    private boolean isScrolling;
    private boolean isStretch;
    private boolean allowClickToChange;
    private int desiredMinimumWidth;
    private int transition;
    private int currentAlpha;
    private int desiredMinimumHeight;

    /**
     * Set up the shared preferences listener and initialize the default prefs
     */
    public GalleryEngine()
    {
      allowClickToChange = false;
      currentAlpha = 0;
      imagePaint = new Paint();
      imagePaint.setAlpha(255);
      options.inTempStorage = new byte[16*1024];
      optionsScale.inTempStorage = options.inTempStorage;
      optionsScale.inSampleSize = 3;
      noImagesPaint.setTextAlign(Paint.Align.CENTER);
      noImagesPaint.setColor(Color.GRAY);
      noImagesPaint.setTextSize(24);
      // register the listener to detect preference changes
      SharedPreferences prefs = GalleryWallpaper.this.getSharedPreferences(SHARED_PREFS_NAME, 0);
      prefs.registerOnSharedPreferenceChangeListener(this);
      // initialize the starting preferences
      onSharedPreferenceChanged(prefs, null);
      setTouchEventsEnabled(true);
      doubleTapDetector = new GestureDetector(GalleryWallpaper.this, new DoubleTapGestureListener(this));
    }

    /* (non-Javadoc)
     * @see android.service.wallpaper.WallpaperService.Engine#onSurfaceDestroyed(android.view.SurfaceHolder)
     */
    @Override
    public void onSurfaceDestroyed(SurfaceHolder holder)
    {
      super.onSurfaceDestroyed(holder);
      handler.removeCallbacks(fadeAnimate);
      handler.removeCallbacks(animate);
    }

    @Override
    public void onOffsetsChanged(float xOffset, float yOffset, float xOffsetStep, float yOffsetStep, int xPixelOffset, int yPixelOffset)
    {
      this.xPixelOffset = xPixelOffset * -1;
      this.xOffset = xOffset;
      this.xOffsetStep = xOffsetStep;
      drawBitmap(currentBitmap);
    }

    /* (non-Javadoc)
     * @see android.service.wallpaper.WallpaperService.Engine#onDestroy()
     */
    @Override
    public void onDestroy()
    {
      super.onDestroy();
      handler.removeCallbacks(fadeAnimate);
      handler.removeCallbacks(animate);
    }

    /* (non-Javadoc)
     * @see android.service.wallpaper.WallpaperService.Engine#onCreate(android.view.SurfaceHolder)
     */
    @Override
    public void onCreate(SurfaceHolder surfaceHolder)
    {
      super.onCreate(surfaceHolder);
      this.desiredMinimumWidth = getDesiredMinimumWidth();
      this.desiredMinimumHeight = getDesiredMinimumHeight();
    }

    /* (non-Javadoc)
     * @see android.service.wallpaper.WallpaperService.Engine#onVisibilityChanged(boolean)
     */
    @Override
    public void onVisibilityChanged(boolean visible)
    {
      super.onVisibilityChanged(visible);
      if (visible)
      {
        // if there is a bitmap with time left to keep around, redraw it
        if (currentBitmap != null && systemTime() - timeStarted + 100 < timer)
        {
          // for some reason, it's sometimes recycled!
          if (currentBitmap.isRecycled())
          {
            currentBitmap = BitmapFactory.decodeFile(currentFile.getAbsolutePath());
          }
          drawBitmap(currentBitmap);
          // left over timer
          handler.postDelayed(animate, timer - (systemTime() - timeStarted));
        }
        // otherwise draw a new one since it's time for a new one
        else
        {
          showNewImage();
        }
      }
      else
      {
        handler.removeCallbacks(fadeAnimate);
        handler.removeCallbacks(animate);
      }
    }

    /**
     * Doesn't appear to happen on any current phones.. but to future proof.
     */
    @Override
    public void onDesiredSizeChanged(int desiredWidth, int desiredHeight)
    {
      super.onDesiredSizeChanged(desiredWidth, desiredHeight);
      this.desiredMinimumHeight = desiredHeight;
      this.desiredMinimumWidth = desiredWidth;
    }

    /**
     * Draw the bitmap to the surface canvas
     * @param b the bitmap to draw
     */
    private void drawBitmap(Bitmap b)
    {
      final SurfaceHolder holder = getSurfaceHolder();

      Canvas c = null;
      try
      {
          c = holder.lockCanvas();
          if (c != null && b != null)
          {
            int virtualWidth = this.desiredMinimumWidth;
            int vertMargin = (b.getHeight() - surfaceFrame.bottom) / 2;
            Rect window;
            Rect dstWindow = surfaceFrame;
            if (!isScrolling)
            {
              virtualWidth = surfaceFrame.right; // virtual width becomes screen width
              xPixelOffset = 0;
            }
            if (b.getWidth() >= virtualWidth && b.getHeight() >= this.desiredMinimumHeight)
            {
              int pictureHorizOffset = xPixelOffset + (b.getWidth() - virtualWidth) / 2;
              window = new Rect(pictureHorizOffset, vertMargin, pictureHorizOffset + surfaceFrame.right, b.getHeight() - vertMargin);
            }
            else if (!isStretch)
            {
              int pictureHorizOffset = xPixelOffset + (b.getWidth() - virtualWidth) / 2;
              c.drawColor(Color.BLACK);
              window = null;
              dstWindow = new Rect(surfaceFrame);
              dstWindow.top = -vertMargin;
              dstWindow.bottom = b.getHeight() - vertMargin;
              dstWindow.left = -pictureHorizOffset;
              dstWindow.right = -pictureHorizOffset + b.getWidth();
            }
            else
            {
              int width = b.getWidth();
              // x is the left side of the frame.  offset times (1 screenful less of the width)
              float screenful = width / ((1 / xOffsetStep) + 1);
              float x = xOffset * (width - screenful);
              window = new Rect(((int) x), vertMargin, (int) (x + screenful), b.getHeight() - vertMargin);
            }
            c.drawBitmap(b, window, dstWindow, imagePaint);

          }
      }
      finally
      {
          if (c != null)
            holder.unlockCanvasAndPost(c);
      }
      if (currentBitmap != null && b != currentBitmap)
      {
        currentBitmap.recycle();
      }
      currentBitmap = b;
    }

    @Override
    public void onTouchEvent(MotionEvent event)
    {
      super.onTouchEvent(event);
      this.doubleTapDetector.onTouchEvent(event);
    }

    /**
     * Called whenever you want a new image.
     * This will also post the message for when the next frame should be drawn
     */
    protected void showNewImage()
    {
      if (images != null && images.length != 0)
      {
        File image = images[randGen.nextInt(images.length)];
        try
        {
          Bitmap b = BitmapFactory.decodeFile(image.getAbsolutePath(), options);
          currentFile = image;
          switch (transition)
          {
          case GalleryWallpaperSettings.FADE_TRANSITION:
            fadeTransition(b, 0);
            break;
          default:
            drawBitmap(b);
          }
        }
        catch (OutOfMemoryError e)
        {
          try
          {
            System.gc();
            Log.i("wallpaper", "Image too big, attempting to scale.");
            Bitmap b = BitmapFactory.decodeFile(image.getAbsolutePath(), optionsScale);
            drawBitmap(b);
            Log.i("wallpaper", "Scale successful.");
          }
          catch (OutOfMemoryError e2)
          {
            Log.e("wallpaper", "Scale failed: " + image.getAbsolutePath());
            System.gc();
            // skip to next image.
            showNewImage();
            return;
          }
        }
      }
      else if (!isMounted)
      {
        drawTextHelper(getString(R.string.sd_card_not_available));
      }
      else
      {
        drawTextHelper(getString(R.string.no_images_found));
      }

      /*
       * This is how it animates. After drawing a frame, ask it to draw another one.
       */
      handler.removeCallbacks(animate);
      if (isVisible())
      {
          handler.postDelayed(animate, timer);
          timeStarted = systemTime();
      }
    }

    private final Runnable fadeAnimate = new Runnable()
    {
      public void run()
      {
        fadeTransition(currentBitmap, currentAlpha);
      }
    };

    /**
     * Execute a fade transition.
     * Increments the current alpha
     * then draws at that alpha
     * then posts a message for it to be rerun
     * It should cycle from 0 to 255 in 13 frames.
     * @param b the bitmap to fade
     * @param alpha the alpha to start at, or the current alpha
     */
    private void fadeTransition(Bitmap b, int alpha)
    {
      currentAlpha = alpha;
      currentAlpha += 255 / 25;
      if (currentAlpha > 255)
      {
        currentAlpha = 255;
      }
      //Log.v("wallpaper", "alpha " + currentAlpha);
      imagePaint.setAlpha(currentAlpha);
      drawBitmap(b);

      /*
       * This is how it animates. After drawing a frame, ask it to draw another one.
       */
      handler.removeCallbacks(fadeAnimate);
      if (isVisible() && currentAlpha < 255) // stop when at full opacity
      {
          handler.post(fadeAnimate);
      }
    }

    private void drawTextHelper(String line)
    {
      final SurfaceHolder holder = getSurfaceHolder();

      Canvas c = null;
      try
      {
          c = holder.lockCanvas();
          if (c != null)
          {
            c.drawColor(Color.BLACK);
            c.drawText(line, surfaceFrame.right / 2, surfaceFrame.bottom / 2, noImagesPaint);
          }
      }
      finally
      {
          if (c != null)
            holder.unlockCanvasAndPost(c);
      }
    }

    /**
     * Convenience method to return a time that is suitable for measuring timeouts in milliseconds
     * @return the system time in milliseconds. could be negative.
     * @see System#nanoTime()
     */
    private long systemTime()
    {
      return System.nanoTime() / 1000000;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPrefs, String keyChanged)
    {
      timer = Integer.valueOf(sharedPrefs.getString(GalleryWallpaperSettings.TIMER_KEY, "5000"));
      isScrolling = sharedPrefs.getBoolean(GalleryWallpaperSettings.SCROLLING_KEY, true);
      isStretch = sharedPrefs.getBoolean(GalleryWallpaperSettings.STRETCHING_KEY, false);
      allowClickToChange = sharedPrefs.getBoolean(GalleryWallpaperSettings.CLICK_TO_CHANGE_KEY, false);
      String state = Environment.getExternalStorageState();
      File folder;
      if (Environment.MEDIA_MOUNTED.equals(state) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state))
      {
        folder = new File(sharedPrefs.getString(GalleryWallpaperSettings.FOLDER_KEY, "/"));
      }
      else
      {
        folder = new File("/");
      }
      images = folder.listFiles(GalleryWallpaper.IMAGE_FILTER);
      isMounted = sharedPrefs.getBoolean(GalleryWallpaperSettings.IS_MOUNTED, true);
      transition = Integer.valueOf(sharedPrefs.getString(GalleryWallpaperSettings.TRANSITION_KEY, "0"));
      if (transition != GalleryWallpaperSettings.FADE_TRANSITION)
      {
        imagePaint.setAlpha(255);
      }
    }

    public boolean allowClickToChange()
    {
      return allowClickToChange;
    }
  }
}
