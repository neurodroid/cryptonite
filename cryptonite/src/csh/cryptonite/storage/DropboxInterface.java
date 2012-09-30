package csh.cryptonite.storage;

import java.util.HashMap;

import android.util.Log;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.exception.DropboxServerException;

import csh.cryptonite.Cryptonite;

public enum DropboxInterface {
    INSTANCE;
    private DropboxAPI<AndroidAuthSession> mApi;
    private HashMap<String, Entry> dbHashMap;
    
    public void init() {
        mApi = null;
        dbHashMap = new HashMap<String, Entry>();
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
            throw new DropboxException("mApi == null");
        }
        if (dbPath == null) {
            throw new DropboxException("dbPath == null");
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
