package csh.cryptonite.storage;

import csh.cryptonite.Cryptonite;
import csh.cryptonite.DirectorySettings;
import csh.cryptonite.R;
import csh.cryptonite.SelectionMode;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;

public class DropboxFragment extends StorageFragment {
    
    private Button buttonAuth;
    
    public DropboxFragment() {
        super();
        idLayout = R.layout.db_tab;
        idTvVersion = R.id.tvVersionDb;
        idBtnDecrypt = R.id.btnDecryptDb;
        idBtnBrowseDecrypted = R.id.btnBrowseDecryptedDb;
        idBtnForgetDecryption = R.id.btnForgetDecryptionDb;
        idBtnSave = R.id.btnSaveDb;
        idBtnLoad = R.id.btnLoadDb;
        idBtnCreate = R.id.btnCreateDb;
        storageType = Storage.STOR_DROPBOX;
        opMode = Cryptonite.SELECTDBENCFS_MODE;
        dialogMode = SelectionMode.MODE_OPEN_ENCFS_DB;
        storagePrefsKey = "cb_storagedb";
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState)
    {
        super.onCreateView(inflater, container, savedInstanceState);
        
        /* Link with Dropbox */
        buttonAuth = (Button)mView.findViewById(R.id.btnAuthDb);
        buttonAuth.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    // If necessary, we create a new AuthSession 
                    // so that we can use the Dropbox API.
                    // Not done on program startup so that the user can
                    // decide between app folder and full access.
                    if (DropboxInterface.INSTANCE.getDBApi() == null) {
                        mAct.buildSession();
                    } else {
                        if (mAct.mLoggedIn) {
                            mAct.logOut();
                            mAct.updateDecryptButtons();
                        } else {
                            mAct.dbAuthenticate();
                        }
                    }
                }});

        buttonAuth.setEnabled(true);
        
        return mView;
    }

    @Override
    public void onActivityCreated (Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mAct.setDBFragment(this);
    }

    @Override
    public void updateDecryptButtons() {
        super.updateDecryptButtons();
        updateLoginButtons();
    }
    
    @Override
    protected void openEncFSVolume() {
        mAct.currentDialogStartPath = mAct.getPrivateDir(DirectorySettings.BROWSEPNT).getPath();
        mAct.currentDialogRoot = mAct.currentDialogStartPath;
        mAct.currentDialogRootName = getString(R.string.dropbox_root_name);
        if (mAct.mLoggedIn) {
            mAct.launchBuiltinFileBrowser();
        }
    }
    
    public void updateLoginButtons() {
        if (buttonAuth == null || buttonDecrypt == null) {
            return;
        }
        
        if (mAct.mLoggedIn) {
            buttonAuth.setText(R.string.dropbox_unlink);
        } else {
            buttonAuth.setText(R.string.dropbox_link);
        }
    }
    
}

