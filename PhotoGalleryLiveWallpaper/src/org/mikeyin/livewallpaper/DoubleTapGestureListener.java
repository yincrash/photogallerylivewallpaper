/**
 *
 */
package org.mikeyin.livewallpaper;

import org.mikeyin.livewallpaper.GalleryWallpaper.GalleryEngine;

import android.view.MotionEvent;
import android.view.GestureDetector.SimpleOnGestureListener;

/**
 * @author yincrash
 *
 */
public class DoubleTapGestureListener extends SimpleOnGestureListener
{

  private final GalleryEngine galleryEngine;

  public DoubleTapGestureListener(GalleryEngine galleryEngine)
  {
    this.galleryEngine = galleryEngine;
  }

  /* (non-Javadoc)
   * @see android.view.GestureDetector.SimpleOnGestureListener#onDoubleTap(android.view.MotionEvent)
   */
  @Override
  public boolean onDoubleTap(MotionEvent e)
  {
    if (this.galleryEngine.allowClickToChange())
    {
      galleryEngine.showNewImage();
    }
    return true;
  }


}
