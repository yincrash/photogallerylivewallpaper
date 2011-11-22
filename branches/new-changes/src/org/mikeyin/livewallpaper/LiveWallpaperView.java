/**
 * 
 */
package org.mikeyin.livewallpaper;

import android.app.WallpaperManager;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * @author yincrash
 *
 */
public class LiveWallpaperView extends View {
    
    private WallpaperManager wallpaperManager;

    public LiveWallpaperView(Context context) {
        super(context);
        wallpaperManager = WallpaperManager.getInstance(context);
    }

    public LiveWallpaperView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        wallpaperManager = WallpaperManager.getInstance(context);
    }

    public LiveWallpaperView(Context context, AttributeSet attrs) {
        super(context, attrs);
        wallpaperManager = WallpaperManager.getInstance(context);
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //wallpaperManager.sendWallpaperCommand(getWindowToken(), action, x, y, z, extras)
        return super.onTouchEvent(event);
    }
    

}
