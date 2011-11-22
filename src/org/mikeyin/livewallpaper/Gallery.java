/**
 * 
 */
package org.mikeyin.livewallpaper;

import android.content.Context;
import android.content.SharedPreferences;
/**
 * @author yincrash
 *
 */
public interface Gallery {

    SharedPreferences getSharedPreferences(String name, int mode);

    Context getContext();

    int getDesiredMinimumWidth();

    int getDesiredMinimumHeight();
    
}
