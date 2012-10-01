package csh.cryptonite.storage;

import com.actionbarsherlock.app.SherlockFragmentActivity;

public enum MountManager {
    INSTANCE;

    private Storage mEncFSStorage;

    public void init() {
        mEncFSStorage = null;
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
    
    public void initEncFSStorage(SherlockFragmentActivity activity) {
        if (mEncFSStorage == null) {
            mEncFSStorage = new LocalStorage(activity);
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
    
    public String getEncFSConfigPath() {
        if (mEncFSStorage != null) {
            return mEncFSStorage.getEncFSConfigPath();
        } else {
            return "";
        }
    }
    
    public void setEncFSConfigPath(String value) {
        if (mEncFSStorage != null) {
            mEncFSStorage.setEncFSConfigPath(value);
        }
    }

}
