package org.mikeyin.livewallpaper;

import java.io.File;
import java.io.FilenameFilter;

import android.content.Context;
import android.service.wallpaper.WallpaperService;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

public class GalleryWallpaper extends WallpaperService implements Gallery{

    private GalleryEngine e;
    private DelegateEngine de;
    
    /**
     * Filter for choosing only files with image extensions
     */
    static final FilenameFilter IMAGE_FILTER = new FilenameFilter() {

        @Override
        public boolean accept(File dir, String filename) {
            filename = filename.toLowerCase();
            return filename.endsWith(".jpg") || filename.endsWith(".png")
                    || filename.endsWith(".gif") || filename.endsWith(".bmp");
        }
    };

    public static final String SHARED_PREFS_NAME = "org.mikeyin:gallerywallpaper";

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
        e = new GalleryEngine(this);
        de = new DelegateEngine();
        return de;
    }

    /**
     * {@link GalleryEngine} can't extend the nested {@link Engine} class
     * directly since it's not static, so we're going to delegate it.
     * @author yincrash
     *
     */
    class DelegateEngine extends Engine{
        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            e.onCreate(surfaceHolder);
        }
        
        @Override
        public void onDesiredSizeChanged(int desiredWidth, int desiredHeight) {
            super.onDesiredSizeChanged(desiredWidth, desiredHeight);
            e.onDesiredSizeChanged(desiredWidth, desiredHeight);
        }
        
        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            e.onSurfaceDestroyed(holder);
        }
        
        @Override
        public void onTouchEvent(MotionEvent event) {
            super.onTouchEvent(event);
            e.onTouchEvent(event);
        }
        
        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            e.onVisibilityChanged(visible);
        }
    }

    @Override
    public Context getContext() {
        return this;
    }

    @Override
    public int getDesiredMinimumWidth() {
        return de.getDesiredMinimumWidth();
    }

    @Override
    public int getDesiredMinimumHeight() {
        return de.getDesiredMinimumHeight();
    }
}
