/*****************************************************************************
 * VLCApplication.java
 *****************************************************************************
 * Copyright © 2010-2013 VLC authors and VideoLAN
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 *****************************************************************************/
package org.videolan.vlc;

import java.io.File;
import java.util.Locale;

import org.videolan.vlc.gui.audio.AudioUtil;

import com.softwarrior.libtorrent.LibTorrent;

import dp.ws.time4popcorn.StorageHelper;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.util.Log;

public class VLCApplication extends Application {
    public final static String TAG = "VLC/VLCApplication";
    private static VLCApplication instance;

    public final static String SLEEP_INTENT = "org.videolan.vlc.SleepIntent";
    
    // TRIBLER
 	private LibTorrent libTorrent = null;
 	private static int LISTEN_PORT = 0;
 	private static int LIMIT_UL = 0;
 	private static int LIMIT_DL = 0;
 	private static boolean ENCRYPTION = false;
 	
 	public File tempFolder;

 	public LibTorrent getLibTorrent() {
 		if (this.libTorrent == null) {
 			Log.d(TAG,"created new libTorrent from no settings!");
 			this.libTorrent = new LibTorrent();
 			this.libTorrent.SetSession(LISTEN_PORT, LIMIT_UL, LIMIT_DL, ENCRYPTION);
 			this.libTorrent.ResumeSession();
 		}
 		return this.libTorrent;
 	}
 	
 	public LibTorrent getLibTorrent(int listenPort, int uploadLimit, int downloadLimit, boolean encryption) {
 		if (this.libTorrent == null) {
 			this.libTorrent = new LibTorrent();
 			LISTEN_PORT = listenPort;
 			LIMIT_UL = uploadLimit;
 			LIMIT_DL = downloadLimit;
 			ENCRYPTION = encryption;
 			this.libTorrent.SetSession(listenPort, uploadLimit, downloadLimit, encryption);
 			this.libTorrent.ResumeSession();
 		}
 		return this.libTorrent;
 	}
 	
 	public void deleteLibTorrent() {
		this.libTorrent = null;
 	}

 	// \TRIBLER
    @Override
    public void onCreate() {
        super.onCreate();
        
        tempFolder = StorageHelper.getTempFolder(this);
        StorageHelper.clearFolder(tempFolder);

        // Are we using advanced debugging - locale?
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        String p = pref.getString("set_locale", "");
        if (p != null && !p.equals("")) {
            Locale locale;
            // workaround due to region code
            if(p.equals("zh-TW")) {
                locale = Locale.TRADITIONAL_CHINESE;
            } else if(p.startsWith("zh")) {
                locale = Locale.CHINA;
            } else if(p.equals("pt-BR")) {
                locale = new Locale("pt", "BR");
            } else if(p.equals("bn-IN") || p.startsWith("bn")) {
                locale = new Locale("bn", "IN");
            } else {
                /**
                 * Avoid a crash of
                 * java.lang.AssertionError: couldn't initialize LocaleData for locale
                 * if the user enters nonsensical region codes.
                 */
                if(p.contains("-"))
                    p = p.substring(0, p.indexOf('-'));
                locale = new Locale(p);
            }
            Locale.setDefault(locale);
            Configuration config = new Configuration();
            config.locale = locale;
            getBaseContext().getResources().updateConfiguration(config,
                    getBaseContext().getResources().getDisplayMetrics());
        }

        instance = this;

        // Initialize the database soon enough to avoid any race condition and crash
        MediaDatabase.getInstance(this);
        // Prepare cache folder constants
        AudioUtil.prepareCacheFolder(this);
    }

    /**
     * Called when the overall system is running low on memory
     */
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Log.w(TAG, "System is running low on memory");

        BitmapCache.getInstance().clear();
    }

    /**
     * @return the main context of the Application
     */
    public static Context getAppContext()
    {
        return instance;
    }

    /**
     * @return the main resources from the Application
     */
    public static Resources getAppResources()
    {
        if(instance == null) return null;
        return instance.getResources();
    }
}