package csh.cryptonite;

import java.util.HashMap;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.exception.DropboxServerException;

import android.app.Application;
import android.util.Log;

public class CryptoniteApp extends Application {

    private DropboxAPI<AndroidAuthSession> mApi = null;
    private HashMap<String, Entry> dbHashMap = new HashMap<String, Entry>();
    
    public DropboxAPI<AndroidAuthSession> getDBApi() {
        return mApi;
    }
    
    public void setDBApi(DropboxAPI<AndroidAuthSession> api) {
        mApi = api;
    }
    
    public Entry getDBEntry(String dbPath) throws DropboxException {
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
    
    public boolean dbFileExists(String dbPath) throws DropboxException {
        try {
            getDBEntry(dbPath);
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
