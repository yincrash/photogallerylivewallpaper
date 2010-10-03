package org.mikeyin.livewallpaper;

import java.io.File;
import java.io.FilenameFilter;

final class ImageFilenameFilter implements FilenameFilter {
  @Override
  public boolean accept(File dir, String filename) {
    filename = filename.toLowerCase();
    return filename.endsWith(".jpg") || filename.endsWith(".jpeg") || filename.endsWith(".png")
        || filename.endsWith(".gif") || filename.endsWith(".bmp");
  }
}