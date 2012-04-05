package csh.cryptonite;

import csh.cryptonite.storage.Storage;
import csh.cryptonite.storage.StorageManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class DropboxFragment extends Fragment {
    
    public TextView tv;
    public Button buttonAuth, buttonDecrypt, buttonBrowseDecrypted,
        buttonForgetDecryption, buttonCreate;
    
    private Cryptonite mAct;

    private View mView;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState)
    {
        super.onCreateView(inflater, container, savedInstanceState);
        
        mView = inflater.inflate(R.layout.db_tab, container, false);

        mAct = (Cryptonite)getActivity();
        
        tv = (TextView)mView.findViewById(R.id.tvVersionDb);

        /* Link with Dropbox */
        buttonAuth = (Button)mView.findViewById(R.id.btnAuthDb);
        buttonAuth.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    // If necessary, we create a new AuthSession 
                    // so that we can use the Dropbox API.
                    // Not done on program startup so that the user can
                    // decide between app folder and full access.
                    if (DBInterface.INSTANCE.getDBApi() == null) {
                        mAct.buildSession();
                    } else {
                        if (mAct.mLoggedIn) {
                            mAct.logOut();
                        } else {
                            mAct.triedLogin = true;
                            // Start the remote authentication
                            DBInterface.INSTANCE.getDBApi()
                                .getSession().startAuthentication(mAct);
                        }
                    }
                }});

        buttonAuth.setEnabled(true);
        
        /* Decrypt EncFS volume on Dropbox */
        buttonDecrypt = (Button)mView.findViewById(R.id.btnDecryptDb);
        buttonDecrypt.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    StorageManager.INSTANCE.initEncFSStorage(mAct, Storage.STOR_DROPBOX, mAct.mApp);
                    mAct.opMode = Cryptonite.SELECTDBENCFS_MODE;
                    mAct.currentDialogLabel = getString(R.string.select_enc);
                    mAct.currentDialogButtonLabel = getString(
                            R.string.select_enc_short);
                    mAct.currentDialogMode = SelectionMode.MODE_OPEN_DB;
                    mAct.currentDialogStartPath = mAct.getPrivateDir(CryptoniteApp.BROWSEPNT).getPath();
                    mAct.currentDialogRoot = mAct.currentDialogStartPath;
                    mAct.currentDialogRootName = getString(R.string.dropbox_root_name);
                    if (mAct.mLoggedIn) {
                        mAct.launchBuiltinFileBrowser();
                    }
                }});

        /* Browse decrypted volume */
        buttonBrowseDecrypted = (Button)mView.findViewById(R.id.btnBrowseDecryptedDb);
        buttonBrowseDecrypted.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    mAct.browseEncFS(mAct.mApp.getCurrentBrowsePath(), 
                            mAct.mApp.getCurrentBrowseStartPath());
                }});
        
        /* Clear decryption information */
        buttonForgetDecryption = (Button)mView.findViewById(R.id.btnForgetDecryptionDb);
        buttonForgetDecryption.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    mAct.mApp.cleanUpDecrypted();
                    updateDecryptButtons();
                }});
        
        /* Create EncFS volume on Dropbox */
        buttonCreate = (Button)mView.findViewById(R.id.btnCreateDb);
        buttonCreate.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    mAct.createEncFS(true);
                }});

        return mView;
    }

    @Override
    public void onActivityCreated (Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mAct.setDBFragment(this);
        tv.setText(mAct.textOut);
        updateDecryptButtons();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        updateDecryptButtons();
        if (tv != null) {
            tv.setText(mAct.textOut);
            tv.invalidate();
        }
    }

    public void updateDecryptButtons() {
        if (buttonDecrypt == null || buttonBrowseDecrypted == null ||
                buttonForgetDecryption == null || buttonCreate == null) 
        {
            return;
        }

        boolean volumeLoaded = (Cryptonite.jniVolumeLoaded() == Cryptonite.jniSuccess());

        buttonDecrypt.setEnabled(!volumeLoaded && mAct.mLoggedIn);
        buttonBrowseDecrypted.setEnabled(volumeLoaded && 
                StorageManager.INSTANCE.getEncFSStorageType() == Storage.STOR_DROPBOX);
        buttonForgetDecryption.setEnabled(volumeLoaded);
        buttonCreate.setEnabled(!volumeLoaded && mAct.mLoggedIn);
        
        updateLoginButtons();
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

