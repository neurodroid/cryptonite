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
import java.util.HashMap;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.exception.DropboxServerException;

import csh.cryptonite.storage.DropboxStorage;
import csh.cryptonite.storage.LocalStorage;
import csh.cryptonite.storage.Storage;
import csh.cryptonite.storage.VirtualFileSystem;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

public class CryptoniteApp extends Application {

    private Storage mStorage;

    public static final String OPENPNT = "open";
    public static final String BROWSEPNT = "browse";
    public static final String DROPBOXPNT = "dropbox";
    private static final String READPNT = "read";

    private File openDir, readDir;
    private DropboxAPI<AndroidAuthSession> mApi;
    private HashMap<String, Entry> dbHashMap;
    
    private boolean disclaimerShown;

    private String binDirPath;
    private String encfsBin;
    private String currentTabTag;

    private String currentDBEncFS;
    private String currentBrowsePath;
    private String currentBrowseStartPath;
    
    public CryptoniteApp() {
        super();
        mApi = null;
        mStorage = null;
        currentTabTag = Cryptonite.DBTAB_TAG;
        dbHashMap = new HashMap<String, Entry>();
    }

    @Override
    public void onCreate() {
        disclaimerShown = false;
        cleanUpDecrypted();

        binDirPath = getFilesDir().getParentFile().getPath();
        encfsBin = binDirPath + "/encfs";
        
        VirtualFileSystem.INSTANCE.init();
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
    
    public boolean isDropbox() {
        return mStorage != null && mStorage.type == Storage.STOR_DROPBOX;
    }
    
    public boolean isLocal() {
        return mStorage != null && mStorage.type == Storage.STOR_LOCAL;
    }
    
    public void resetStorage() {
        mStorage = null;
    }
    
    public Storage getStorage() {
        return mStorage;
    }
    
    public void initLocalStorage(Context context) {
        mStorage = new LocalStorage(context, this);        
    }
    
    public void initDropboxStorage(Context context) {
        mStorage = new DropboxStorage(context, this);        
    }
    
    public String getCurrentTabTag() {
        return currentTabTag;
    }
    
    public void setCurrentTabTag(String value) {
        currentTabTag = value;
    }
    
    public String getCurrentDBEncFS() {
        return currentDBEncFS;
    }
    
    public void setCurrentDBEncFS(String value) {
        currentDBEncFS = value;
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
    
    public DropboxAPI<AndroidAuthSession> getDBApi() {
        return mApi;
    }
    
    public void setDBApi(DropboxAPI<AndroidAuthSession> api) {
        mApi = api;
    }
    
    public Entry getDBEntry(String dbPath) throws DropboxException {
        if (mApi == null) {
            /* This shouldn't happen really */
            throw new DropboxException("mApi == null: " + getString(R.string.dropbox_null));
        }
        if (dbPath == null) {
            throw new DropboxException("dbPath == null: " + getString(R.string.dropbox_null));
        }
        
        String hash = null;
        
        if (dbHashMap.containsKey(dbPath)) {
            Log.d(Cryptonite.TAG, "Found hash for " + dbPath);
            hash = dbHashMap.get(dbPath).hash;
        }
        try {
            Entry dbEntry = mApi.metadata(dbPath, 0, hash, true, null);
            if (hash == null) {
                dbHashMap.put(dbPath, dbEntry);
            }
            return dbEntry;
        } catch (DropboxServerException e) {
            if (e.error == DropboxServerException._304_NOT_MODIFIED) {
                return dbHashMap.get(dbPath);
            } else {
                throw e;
            }
        }
    }
    
    public void clearDBHashMap() {
        dbHashMap.clear();
    }
    
    public void removeDBHashMapEntry(String dbPath) {
        dbHashMap.remove(dbPath);
    }
    
    public boolean dbFileExists(String dbPath) throws DropboxException {
        Entry dbEntry;
        try {
            /* We need a new metadata call here without hash
             * to make sure we get 404 if the file
             * doesn't exist rather than 304 if we've
             * asked before
             */
            dbEntry = mApi.metadata(dbPath, 0, null, true, null);
            if (dbEntry.isDeleted) {
                return false;
            }
        } catch (DropboxServerException e) {
            if (e.error == DropboxServerException._404_NOT_FOUND) {
                return false;
            }
        } catch (DropboxException e) {
            throw e;
        }
        return true;
    }

}
