package csh.cryptonite.storage;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;

import csh.cryptonite.Cryptonite;
import csh.cryptonite.DirectorySettings;
import csh.cryptonite.R;
import csh.cryptonite.database.Volume;

public abstract class StorageFragment extends SherlockFragment {

    public TextView tv;
    
    protected Cryptonite mAct;
    protected Button buttonDecrypt, buttonBrowseDecrypted,
        buttonForgetDecryption, buttonCreate, buttonSaveLoad;
    protected int idLayout, idTvVersion, idBtnDecrypt, idBtnBrowseDecrypted,
        idBtnForgetDecryption, idBtnSaveLoad, idBtnCreate;
    protected int storageType;
    protected int opMode;
    protected int dialogMode, dialogModeDefault;
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
        buttonSaveLoad = (Button)mView.findViewById(idBtnSaveLoad);
        buttonSaveLoad.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    if (Cryptonite.jniVolumeLoaded() == Cryptonite.jniSuccess()) {
                        mAct.saveDefault(storageType, Volume.VIRTUAL);
                    } else {
                        Volume volume = mAct.restoreDefault(storageType, Volume.VIRTUAL);
                        StorageManager.INSTANCE.initEncFSStorage(mAct, storageType);
                        if (!StorageManager.INSTANCE.getEncFSStorage().exists(volume.getSource())) {
                            Toast.makeText(mAct, 
                                    getString(R.string.default_missing) + " (" + volume.getSource() + ")", 
                                    Toast.LENGTH_LONG).show();
                            return;
                        }
                        mAct.opMode = opMode;
                        mAct.currentDialogLabel = getString(R.string.select_enc);
                        mAct.currentDialogButtonLabel = getString(
                                R.string.select_enc_short);
                        mAct.currentDialogMode = dialogModeDefault;
                        openEncFSVolumeDefault(volume);
                    }
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
    abstract protected void openEncFSVolumeDefault(Volume volume);

    public void updateDecryptButtons() {
        if (buttonDecrypt == null || buttonBrowseDecrypted == null ||
                buttonForgetDecryption == null || buttonCreate == null ||
                buttonSaveLoad == null) 
        {
            return;
        }

        boolean volumeLoaded = (Cryptonite.jniVolumeLoaded() == Cryptonite.jniSuccess());

        buttonDecrypt.setEnabled(!volumeLoaded);
        buttonBrowseDecrypted.setEnabled(volumeLoaded &&
                StorageManager.INSTANCE.getEncFSStorageType() == storageType);
        buttonForgetDecryption.setEnabled(volumeLoaded);
        if (volumeLoaded && StorageManager.INSTANCE.getEncFSStorageType() == storageType) {
            buttonSaveLoad.setText(R.string.default_save);
            buttonSaveLoad.setEnabled(true);
        } else if (!volumeLoaded) {
            buttonSaveLoad.setText(R.string.default_restore);
            /* Is there any saved volume at all? */
            buttonSaveLoad.setEnabled(mAct.hasDefault(storageType, Volume.VIRTUAL));
        } else {
            buttonSaveLoad.setEnabled(false);
        }
        buttonCreate.setEnabled(!volumeLoaded);
    }
    
}
