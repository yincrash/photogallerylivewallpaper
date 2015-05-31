/**
 * 
 */
package org.mikeyin.livewallpaper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

/**
 * This saves to SharedPreferences on media mounting which signals the wallpaper to check the media again.
 * @author yincrash
 */
public class ExternalStorageInfoReceiver extends BroadcastReceiver
{

  /* (non-Javadoc)
   * @see android.content.BroadcastReceiver#onReceive(android.content.Context, android.content.Intent)
   */
  @Override
  public void onReceive(Context context, Intent intent)
  {
    SharedPreferences prefs = context.getSharedPreferences(GalleryWallpaper.GalleryEngine.SHARED_PREFS_NAME, 0);
    SharedPreferences.Editor editor = prefs.edit();
    String action = intent.getAction();
    if (Intent.ACTION_MEDIA_UNMOUNTED.equals(action) ||
        Intent.ACTION_MEDIA_REMOVED.equals(action) ||
        Intent.ACTION_MEDIA_NOFS.equals(action) ||
        Intent.ACTION_MEDIA_EJECT.equals(action) ||
        Intent.ACTION_MEDIA_BAD_REMOVAL.equals(action))
    {
      editor.putBoolean(GalleryWallpaperSettings.IS_MOUNTED, false);
    }
    else if (Intent.ACTION_MEDIA_MOUNTED.equals(action))
    {
      editor.putBoolean(GalleryWallpaperSettings.IS_MOUNTED, true);
    }
    editor.commit();
  }

}
