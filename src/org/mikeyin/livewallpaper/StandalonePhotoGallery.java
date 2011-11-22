/**
 * 
 */
package org.mikeyin.livewallpaper;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * @author yincrash
 * 
 */
public class StandalonePhotoGallery extends Activity implements Gallery, SurfaceHolder.Callback {
    
    private GalleryEngine galleryEngine;
    
    public StandalonePhotoGallery() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.standalone);
        galleryEngine = new GalleryEngine(this);
        ((SurfaceView) findViewById(R.id.surfaceview)).getHolder().addCallback(this);
    }

    @Override
    public Context getContext() {
        return this;
    }

    @Override
    public int getDesiredMinimumWidth() {
        return 0;
    }

    @Override
    public int getDesiredMinimumHeight() {
        return 0;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
            int height) {
       Log.d("wallpaper", "surfaceChanged");
       galleryEngine.onDesiredSizeChanged(width, height);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d("wallpaper", "surfaceCreated");
        galleryEngine.onCreate(holder);
        galleryEngine.onVisibilityChanged(true);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d("wallpaper", "surfaceDestroyed");
        galleryEngine.onVisibilityChanged(false);
        galleryEngine.onSurfaceDestroyed(holder);
    }
    
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            Intent intent = new Intent(this, GalleryWallpaperSettings.class);
            startActivity(intent);
            return true;
        }
        else
            return super.onKeyUp(keyCode, event);
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        galleryEngine.onTouchEvent(event);
        return true;
    }
    
}
