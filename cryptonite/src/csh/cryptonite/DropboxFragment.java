package csh.cryptonite;

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
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState)
    {
        View v = inflater.inflate(R.layout.db_tab, container, false);

        mAct = (Cryptonite)getActivity();
        
        tv = (TextView)v.findViewById(R.id.tvVersionDb);
        tv.setText(mAct.textOut);

        /* Link with Dropbox */
        buttonAuth = (Button)v.findViewById(R.id.btnAuthDb);
        buttonAuth.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    // If necessary, we create a new AuthSession 
                    // so that we can use the Dropbox API.
                    // Not done on program startup so that the user can
                    // decide between app folder and full access.
                    if (mAct.mApp.getDBApi() == null) {
                        mAct.buildSession();
                    } else {
                        if (mAct.mLoggedIn) {
                            mAct.logOut();
                        } else {
                            mAct.triedLogin = true;
                            // Start the remote authentication
                            mAct.mApp.getDBApi()
                                .getSession().startAuthentication(mAct);
                        }
                    }
                }});

        buttonAuth.setEnabled(true);
        
        /* Decrypt EncFS volume on Dropbox */
        buttonDecrypt = (Button)v.findViewById(R.id.btnDecryptDb);
        buttonDecrypt.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
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

        buttonDecrypt.setEnabled(mAct.mLoggedIn && 
                !(Cryptonite.jniVolumeLoaded() == Cryptonite.jniSuccess()));

        /* Browse decrypted volume */
        buttonBrowseDecrypted = (Button)v.findViewById(R.id.btnBrowseDecryptedDb);
        buttonBrowseDecrypted.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    mAct.browseEncFS(mAct.mApp.getCurrentBrowsePath(), 
                            mAct.mApp.getCurrentBrowseStartPath());
                }});

        buttonBrowseDecrypted.setEnabled(
                Cryptonite.jniVolumeLoaded() == Cryptonite.jniSuccess() &&
                mAct.mApp.isDropbox());
        
        
        /* Clear decryption information */
        buttonForgetDecryption = (Button)v.findViewById(R.id.btnForgetDecryptionDb);
        buttonForgetDecryption.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    mAct.mApp.cleanUpDecrypted();
                    mAct.updateDecryptButtons();
                }});

        buttonForgetDecryption.setEnabled(
                Cryptonite.jniVolumeLoaded() == Cryptonite.jniSuccess());
        
        
        /* Create EncFS volume on Dropbox */
        buttonCreate = (Button)v.findViewById(R.id.btnCreateDb);
        buttonCreate.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    mAct.createEncFS(true);
                }});

        buttonCreate.setEnabled(mAct.mLoggedIn && 
                !(Cryptonite.jniVolumeLoaded() == Cryptonite.jniSuccess()));

        updateDecryptButtons();
        
        return v;
    }

    public void updateDecryptButtons() {
        if (buttonDecrypt == null || buttonBrowseDecrypted == null ||
                buttonForgetDecryption == null || buttonCreate == null) 
        {
            return;
        }

        boolean volumeLoaded = (Cryptonite.jniVolumeLoaded() == Cryptonite.jniSuccess());

        buttonDecrypt.setEnabled(!volumeLoaded && mAct.mLoggedIn);
        buttonBrowseDecrypted.setEnabled(volumeLoaded && mAct.mApp.isDropbox());
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
        buttonDecrypt.setEnabled(mAct.mLoggedIn);
        
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

}

