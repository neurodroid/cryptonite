package csh.cryptonite.storage;

import com.actionbarsherlock.app.SherlockFragmentActivity;

public enum StorageManager {
    INSTANCE;

    private Storage mEncFSStorage;
    private Storage mLocalStorage;

    public void init() {
        mEncFSStorage = null;
        mLocalStorage = null;
    }

    public int getEncFSStorageType() {
        if (mEncFSStorage == null) {
            return Storage.STOR_UNDEFINED;
        } else {
            return mEncFSStorage.type;
        }
    }
    
    public void resetEncFSStorage() {
        mEncFSStorage = null;
    }
    
    public Storage getEncFSStorage() {
        return mEncFSStorage;
    }
    
    public Storage getLocalStorage() {
        return mLocalStorage;
    }
    
    public void initLocalStorage(SherlockFragmentActivity activity) {
        if (mLocalStorage == null || mLocalStorage.type != Storage.STOR_LOCAL) {
            mLocalStorage = new LocalStorage(activity);
        }
    }
    
    public void initEncFSStorage(SherlockFragmentActivity activity, int storType) {
        if (mEncFSStorage == null || mEncFSStorage.type != storType) {
            switch (storType) {
            case Storage.STOR_DROPBOX:
                mEncFSStorage = new DropboxStorage(activity);
                break;
            case Storage.STOR_LOCAL:
                mEncFSStorage = new LocalStorage(activity);
                break;
            }
        }
    }
    

    public String getEncFSPath() {
        if (mEncFSStorage != null) {
            return mEncFSStorage.getEncFSPath();
        } else {
            return "";
        }
    }
    
    public void setEncFSPath(String value) {
        if (mEncFSStorage != null) {
            mEncFSStorage.setEncFSPath(value);
        }
    }
    

}
