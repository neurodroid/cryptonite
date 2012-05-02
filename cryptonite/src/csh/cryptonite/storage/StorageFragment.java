package csh.cryptonite.storage;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;

import csh.cryptonite.Cryptonite;
import csh.cryptonite.DirectorySettings;
import csh.cryptonite.R;

public abstract class StorageFragment extends SherlockFragment {

    public TextView tv;
    
    protected Cryptonite mAct;
    protected Button buttonDecrypt, buttonBrowseDecrypted,
        buttonForgetDecryption, buttonCreate, buttonSave, buttonLoad;
    protected int idLayout, idTvVersion, idBtnDecrypt, idBtnBrowseDecrypted,
        idBtnForgetDecryption, idBtnSave, idBtnLoad, idBtnCreate;
    protected int storageType;
    protected int opMode;
    protected int dialogMode;
    protected String storagePrefsKey;
    protected View mView;
    
    public StorageFragment() {
        super();
        
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState)
    {
        super.onCreateView(inflater, container, savedInstanceState);
        mView = inflater.inflate(idLayout, container, false);

        mAct = (Cryptonite)getActivity();
        
        tv = (TextView)mView.findViewById(idTvVersion);
        
        /* Decrypt EncFS volume */
        buttonDecrypt = (Button)mView.findViewById(idBtnDecrypt);
        buttonDecrypt.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    StorageManager.INSTANCE.initEncFSStorage(mAct, storageType);
                    mAct.opMode = opMode;
                    mAct.currentDialogLabel = getString(R.string.select_enc);
                    mAct.currentDialogButtonLabel = getString(
                            R.string.select_enc_short);
                    mAct.currentDialogMode = dialogMode;
                    openEncFSVolume();
                }});

        /* Browse decrypted volume */
        buttonBrowseDecrypted = (Button)mView.findViewById(idBtnBrowseDecrypted);
        buttonBrowseDecrypted.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    mAct.browseEncFS(DirectorySettings.INSTANCE.currentBrowsePath, 
                            DirectorySettings.INSTANCE.currentBrowseStartPath);
                }});
        
        /* Clear decryption information */
        buttonForgetDecryption = (Button)mView.findViewById(idBtnForgetDecryption);
        buttonForgetDecryption.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    mAct.cleanUpDecrypted();
                    updateDecryptButtons();
                }});
        
        /* Save as default */
        buttonSave = (Button)mView.findViewById(idBtnSave);
        buttonSave.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    SharedPreferences prefs = mAct.getBaseContext()
                            .getSharedPreferences(Cryptonite.ACCOUNT_PREFS_NAME, 0);
                    Editor prefEdit = prefs.edit();
                    prefEdit.putBoolean(storagePrefsKey, true);
                    prefEdit.commit();
                    updateDecryptButtons();
                }});

        /* Load default */
        buttonLoad = (Button)mView.findViewById(idBtnLoad);
        buttonLoad.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    
                }});

        /* Create EncFS volume */
        buttonCreate = (Button)mView.findViewById(idBtnCreate);
        buttonCreate.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    mAct.createEncFS(true);
                }});

        return mView;
    }
    
    @Override
    public void onActivityCreated (Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
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
    
    abstract protected void openEncFSVolume();

    public void updateDecryptButtons() {
        if (buttonDecrypt == null || buttonBrowseDecrypted == null ||
                buttonForgetDecryption == null || buttonCreate == null) 
        {
            return;
        }

        boolean volumeLoaded = (Cryptonite.jniVolumeLoaded() == Cryptonite.jniSuccess());

        buttonDecrypt.setEnabled(!volumeLoaded);
        buttonBrowseDecrypted.setEnabled(volumeLoaded &&
                StorageManager.INSTANCE.getEncFSStorageType() == storageType);
        buttonForgetDecryption.setEnabled(volumeLoaded);
        buttonSave.setEnabled(volumeLoaded &&
                StorageManager.INSTANCE.getEncFSStorageType() == storageType);
        buttonLoad.setEnabled(!volumeLoaded && mAct.hasStored(storagePrefsKey));
        buttonCreate.setEnabled(!volumeLoaded);
    }
    
}
