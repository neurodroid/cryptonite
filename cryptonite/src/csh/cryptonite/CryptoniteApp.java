// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.

// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

// Copyright (c) 2012, Christoph Schmidt-Hieber

package csh.cryptonite;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import csh.cryptonite.storage.StorageManager;
import csh.cryptonite.storage.VirtualFileSystem;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

public class CryptoniteApp extends Application {

    public static final String OPENPNT = "open";
    public static final String BROWSEPNT = "browse";
    public static final String DROPBOXPNT = "dropbox";
    private static final String READPNT = "read";

    private File openDir, readDir;
    
    private boolean disclaimerShown;

    private String binDirPath;
    private String encfsBin;

    private String currentBrowsePath;
    private String currentBrowseStartPath;
    
    public CryptoniteApp() {
        super();
    }

    @Override
    public void onCreate() {
        disclaimerShown = false;
        cleanUpDecrypted();

        binDirPath = getFilesDir().getParentFile().getPath();
        encfsBin = binDirPath + "/encfs";
        
        VirtualFileSystem.INSTANCE.init();
        StorageManager.INSTANCE.init();
        DBInterface.INSTANCE.init();
    }
    
    public boolean needsEncFSBinary() {
        if (!(new File(encfsBin)).exists()) {
            return true;
        }
        
        PackageInfo pInfo;
        try {
            pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch (NameNotFoundException e) {
            return true;
        }
        String appVersion = pInfo.versionName;
        
        SharedPreferences prefs = getBaseContext().getSharedPreferences(Cryptonite.ACCOUNT_PREFS_NAME, 0);
        String binVersion = prefs.getString("binVersion", "");
        return !binVersion.equals(appVersion);
    }

    public void setEncFSBinaryVersion() {
        PackageInfo pInfo;
        try {
            pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch (NameNotFoundException e) {
            return;
        }
        String appVersion = pInfo.versionName;
    
        SharedPreferences prefs = getBaseContext().getSharedPreferences(Cryptonite.ACCOUNT_PREFS_NAME, 0);
        Editor prefEdit = prefs.edit();
        prefEdit.putString("binVersion", appVersion);
        prefEdit.commit();
    }
    
    public boolean hasBin() {
        return new File(encfsBin).exists();
    }

    public String getBinDirPath() {
        return binDirPath;
    }
    
    public String getEncFSBinPath() {
        return encfsBin;
    }

    public String getCurrentBrowsePath() {
        return currentBrowsePath;
    }
    
    public void setCurrentBrowsePath(String value) {
        currentBrowsePath = value;
    }

    public String getCurrentBrowseStartPath() {
        return currentBrowseStartPath;
    }
    
    public void setCurrentBrowseStartPath(String value) {
        currentBrowseStartPath = value;
    }

    /** Copy encfs to binDirPath and make executable */
    public void cpBin(String trunk) {
        String arch = "armeabi";
        /* if (withVfp) {
            arch += "-v7a";
            } */
            
        File binDir = new File(binDirPath);
        if (!binDir.exists()) {
            throw new RuntimeException("Couldn't find binary directory");
        }
        String binName = binDir + "/" + trunk;

        /* Catenate split files */
        Log.v(Cryptonite.TAG, "Looking for assets in " + arch);
        try {
            String[] assetsFiles = getAssets().list(arch);
            File newf = new File(binName);
            FileOutputStream os = new FileOutputStream(newf);
            for (String assetsFile : assetsFiles) {
                if (assetsFile.substring(0, assetsFile.indexOf(".")).compareTo(trunk) == 0) {
                    Log.v(Cryptonite.TAG, "Found " + trunk + " binary part: " + assetsFile);
                    InputStream is = getAssets().open(arch + "/" + assetsFile);

                    byte[] buffer = new byte[is.available()]; 

                    is.read(buffer);

                    os.write(buffer);

                    is.close();
                }
            }
            os.close();
            ShellUtils.chmod(binName, "755");
            
        }
        catch (IOException e) {
            Log.e(Cryptonite.TAG, "Problem while copying binary: " + e.toString());
        } catch (InterruptedException e) {
            Log.e(Cryptonite.TAG, "Problem while copying binary: " + e.toString());
        }

    }

    public void cleanUpDecrypted() {
        Cryptonite.jniResetVolume();
        
        /* Delete directories */
        Cryptonite.deleteDir(getBaseContext().getFilesDir());
        Cryptonite.deleteDir(getBaseContext().getDir(BROWSEPNT, Context.MODE_PRIVATE));
        Cryptonite.deleteDir(getBaseContext().getDir(DROPBOXPNT, Context.MODE_PRIVATE));
        Cryptonite.deleteDir(openDir);
        Cryptonite.deleteDir(readDir);
        
        /* Delete virtual file system */
        VirtualFileSystem.INSTANCE.clear();
    }
    
    public void setupReadDirs(boolean external) {
        if (openDir != null) {
            Cryptonite.deleteDir(openDir);
        }
        if (readDir != null) {
            Cryptonite.deleteDir(readDir);
        }   
        
        if (external && Cryptonite.externalStorageIsWritable()) {
            getExternalCacheDir().mkdirs();
            openDir = new File(getExternalCacheDir().getPath() + "/" + OPENPNT);
            readDir = new File(getExternalCacheDir().getPath() + "/" + READPNT);
            openDir.mkdirs();
            readDir.mkdirs();
        } else {
            openDir = getDir(OPENPNT, Context.MODE_PRIVATE);
            readDir = getDir(READPNT, Context.MODE_WORLD_READABLE);
        }
    }
    
    public File getOpenDir() {
        return openDir;
    }
    
    public File getReadDir() {
        return readDir;
    }
    
    public boolean getDisclaimerShown() {
        return disclaimerShown;
    }
    
    public void setDisclaimerShown(boolean value) {
        disclaimerShown = value;
    }
    
}
