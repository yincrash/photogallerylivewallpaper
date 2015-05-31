/**
 * 
 */
package org.mikeyin.livewallpaper;

import java.io.File;
import java.io.FileFilter;

import android.app.AlertDialog;
import android.app.WallpaperManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.DialogPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.util.AttributeSet;

/**
 * The settings activity.
 * 
 * @author yincrash
 */
public class GalleryWallpaperSettings extends PreferenceActivity implements
    SharedPreferences.OnSharedPreferenceChangeListener {
  public static final String ROTATE_KEY             = "rotate";
  public static final String FOLDER_KEY             = "folder";
  public static final String TIMER_KEY              = "timer";
  public static final String SCROLLING_KEY          = "scrolling";
  public static final String IS_MOUNTED             = "isMounted";
  public static final String STRETCHING_KEY         = "stretching";
  public static final String TRIM_KEY               = "trim";
  public static final String FOLDER_OI_KEY          = "folder_oi";
  public static final String CLICK_TO_CHANGE_KEY    = "clickToChange";
  private static final int   FOLDER_OI_REQUEST_CODE = 1;
  public static final int    GALLERY_REQUEST_CODE   = 2;
  public static final String TRANSITION_KEY         = "transition";
  public static final int    FADE_TRANSITION        = 1;
  public static final int    NO_TRANSITION          = 0;

  private ListPreference     folders;

  @Override
  protected void onCreate(Bundle icicle) {
    super.onCreate(icicle);

    // this is the best way I've found to disable the stupid transparent
    // background
    getListView().setBackgroundColor(Color.WHITE);
    getListView().setCacheColorHint(Color.WHITE);

    getPreferenceManager().setSharedPreferencesName(
        GalleryWallpaper.GalleryEngine.SHARED_PREFS_NAME);
    addPreferencesFromResource(R.xml.wallpaper_settings);

    folders = new ListPreference(this);
    folders.setKey(FOLDER_KEY);
    folders.setTitle(R.string.external_storage_folder);
    folders.setSummary(R.string.external_storage_folder_summary);
    getPreferenceScreen().addPreference(folders);

    getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));

    Preference oiFolderPicker = new OiFolderPickerPreference(this);
    oiFolderPicker.setKey(FOLDER_OI_KEY);
    oiFolderPicker.setTitle(R.string.folder_oi);
    oiFolderPicker.setSummary(R.string.folder_oi_summary);
    getPreferenceScreen().addPreference(oiFolderPicker);

    DialogPreference dialogPreference = new BasicDialogPreference(this, null);
    dialogPreference.setTitle(R.string.help);
    dialogPreference.setSummary(R.string.help_summary);
    dialogPreference.setDialogMessage(getHelpMessage());
    getPreferenceScreen().addPreference(dialogPreference);

    getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
  }

  private CharSequence getHelpMessage() {
    WallpaperManager wallpaperManager = WallpaperManager.getInstance(this);
    return new StringBuilder()
        .append("Virtual Screen Width : ")
        .append(wallpaperManager.getDesiredMinimumWidth())
        .append('\n')
        .append("Virtual Screen Height: ")
        .append(wallpaperManager.getDesiredMinimumHeight())
        .append('\n')
        .append("Hopefully most everything is self-explanatory. \n")
        .append(
            "Scrolling enables the picture to move when switching home screen like a normal wallpaper.\n")
        .append("The ideal width for that picture is the virtual screen width. \n").append(
            "Some big pictures are forcibly scaled to prevent out of memory errors. \n").append(
            "Thanks for downloading! A rating and a nice comment is always appreciated! \n")
        .append("For more information, see the blog post linked in the market page, or email me.")
        .toString();
  }

  /**
   * Only want directories
   */
  private static final FileFilter DIRECTORY_FILTER = new FileFilter() {
                                                     @Override
                                                     public boolean accept(File pathname) {
                                                       return pathname.isDirectory();
                                                     }

                                                   };

  /**
   * Get the folders in the external storage. Just the root level. TODO: should
   * we list all folders?
   * 
   * @return the absolute paths of all the folders in the external storage
   *         directory
   */
  private CharSequence[] getFolders() {
    String state = Environment.getExternalStorageState();
    if (Environment.MEDIA_MOUNTED.equals(state)
        || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
      File sdRoot = Environment.getExternalStorageDirectory();
      File[] directories = sdRoot.listFiles(DIRECTORY_FILTER);
      int length = directories.length;
      String[] paths = new String[length];
      for (int i = 0; i < length; i++) {
        paths[i] = directories[i].getAbsolutePath();
      }
      return paths;
    }
    return null;
  }

  private static final CharSequence[] errorArrayValues = { "/" };

  @Override
  protected void onResume() {
    super.onResume();
    CharSequence[] folderArray = getFolders();
    if (folderArray != null) {
      folders.setEntries(folderArray);
      folders.setEntryValues(folderArray);
    } else {
      CharSequence[] errorArray = { this.getString(R.string.sd_card_not_available) };
      folders.setEntries(errorArray);
      folders.setEntryValues(errorArrayValues);
    }
  }

  @Override
  protected void onDestroy() {
    getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    super.onDestroy();
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
  }

  /**
   * Sadly. tightly coupled with the parent. uses the the activity to call the
   * OI Directory Picker, then that will go back to the activities result call.
   * TODO: implement a better picker.
   * 
   * @author yincrash
   * 
   */
  private class OiFolderPickerPreference extends Preference {
    public OiFolderPickerPreference(Context context) {
      super(context);
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.preference.Preference#onClick()
     */
    @Override
    protected void onClick() {
      // is it better to check the package manager? than catch an exception.
      // maybe.
      super.onClick();
      Intent intent = new Intent("org.openintents.action.PICK_DIRECTORY");
      intent.setData(Uri.parse("file://" + Environment.getExternalStorageDirectory()));
      intent.putExtra("org.openintents.extra.TITLE", "Please select a folder");
      intent.putExtra("org.openintents.extra.BUTTON_TEXT", "Use this folder");
      try {
        GalleryWallpaperSettings.this.startActivityForResult(intent, FOLDER_OI_REQUEST_CODE);
      } catch (ActivityNotFoundException e) {
        // ask to install
        new AlertDialog.Builder(this.getContext()).setMessage(R.string.no_oi).setPositiveButton(
            R.string.no_oi_please_install, new HandleOIInstallDialog()).setNegativeButton(
            R.string.no_oi_dont_install, null).show();
      }
      /*
       * Intent galleryIntent = new Intent(Intent.ACTION_PICK,
       * MediaStore.Images.Media.INTERNAL_CONTENT_URI);
       * GalleryWallpaperSettings.this.startActivityForResult(galleryIntent,
       * GALLERY_REQUEST_CODE);
       */
    }
  }

  private class HandleOIInstallDialog implements OnClickListener {
    @Override
    public void onClick(DialogInterface dialog, int which) {
      Uri marketUri = Uri.parse("market://details?id=org.openintents.filemanager");
      try {
        GalleryWallpaperSettings.this.startActivity(new Intent(Intent.ACTION_VIEW)
            .setData(marketUri));
      } catch (ActivityNotFoundException e) {
        // do nothing if market isn't there!!
      }
    }
  };

  /**
   * Handle the OI Folder result
   */
  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == FOLDER_OI_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
      String filename = data.getDataString();
      if (filename != null) {
        // Get rid of URI prefix:
        if (filename.startsWith("file://")) {
          filename = filename.substring(7);
        }
        // Log.v("wallpaper", "Saving folder preference using OI File Manager");
        getPreferenceManager().getSharedPreferences().edit().putString(FOLDER_KEY, filename)
            .commit();
      }
    }
    super.onActivityResult(requestCode, resultCode, data);
  }

  private class BasicDialogPreference extends DialogPreference {
    public BasicDialogPreference(Context context, AttributeSet attrs, int defStyle) {
      super(context, attrs, defStyle);
    }

    public BasicDialogPreference(Context context, AttributeSet attrs) {
      super(context, attrs);
    }
  }
}
