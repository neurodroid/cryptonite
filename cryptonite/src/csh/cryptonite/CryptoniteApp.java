package csh.cryptonite;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;

import android.app.Application;

public class CryptoniteApp extends Application {

    private DropboxAPI<AndroidAuthSession> mApi = null;
    
    public DropboxAPI<AndroidAuthSession> getDBApi() {
        return mApi;
    }
    
    public void setDBApi(DropboxAPI<AndroidAuthSession> api) {
        mApi = api;
    }
}
