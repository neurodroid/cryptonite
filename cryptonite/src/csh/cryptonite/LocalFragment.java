package csh.cryptonite;

import java.io.IOException;

import com.actionbarsherlock.app.SherlockFragment;

import csh.cryptonite.storage.Storage;
import csh.cryptonite.storage.StorageManager;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class LocalFragment extends SherlockFragment {
    
    public TextView tv;
    public TextView tvMountInfo;
    
    private Button buttonDecrypt, buttonBrowseDecrypted, buttonForgetDecryption,
        buttonCreate, buttonMount, buttonViewMount, buttonSave, buttonLoad;
    
    private Cryptonite mAct;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState)
    {
        super.onCreateView(inflater, container, savedInstanceState);
        
        View v = inflater.inflate(R.layout.local_tab, container, false);

        mAct = (Cryptonite)getActivity();
        
        tv = (TextView)v.findViewById(R.id.tvVersionLocal);
        tvMountInfo = (TextView)v.findViewById(R.id.tvMountInfo);
        
        /* Decrypt local encFS volume */
        buttonDecrypt = (Button)v.findViewById(R.id.btnDecryptLocal);
        buttonDecrypt.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    StorageManager.INSTANCE.initEncFSStorage(mAct, Storage.STOR_LOCAL);
                    mAct.opMode = Cryptonite.SELECTLOCALENCFS_MODE;
                    mAct.currentDialogLabel = getString(R.string.select_enc);
                    mAct.currentDialogButtonLabel = getString(R.string.select_enc_short);
                    mAct.currentDialogMode = SelectionMode.MODE_OPEN_ENCFS;
                    if (Cryptonite.externalStorageIsWritable()) {
                        mAct.currentDialogStartPath = Environment.getExternalStorageDirectory().getPath();
                    } else {
                        mAct.currentDialogStartPath = "/";
                    }
                    mAct.currentDialogRoot = "/";
                    mAct.currentDialogRootName = mAct.currentDialogRoot;
                    if (!Cryptonite.externalStorageIsWritable()) {
                        mAct.showAlert(R.string.error, R.string.sdcard_not_writable);
                    } else {
                        mAct.launchBuiltinFileBrowser(); //DIRPICK_MODE);
                    }
                }});

        /* Browse decrypted volume */
        buttonBrowseDecrypted = (Button)v.findViewById(R.id.btnBrowseDecryptedLocal);
        buttonBrowseDecrypted.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    mAct.browseEncFS(DirectorySettings.INSTANCE.currentBrowsePath,
                            DirectorySettings.INSTANCE.currentBrowseStartPath);
                }});

        /* Clear decryption information */
        buttonForgetDecryption = (Button)v.findViewById(R.id.btnForgetDecryptionLocal);
        buttonForgetDecryption.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    mAct.cleanUpDecrypted();
                    updateDecryptButtons();
                }});

        /* Save as default */
        buttonSave = (Button)v.findViewById(R.id.btnSaveLocal);
        buttonSave.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    mAct.updateDecryptButtons();
                }});

        /* Load default */
        buttonLoad = (Button)v.findViewById(R.id.btnLoadLocal);
        buttonLoad.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    
                }});
        
        /* Create local EncFS volume */
        buttonCreate = (Button)v.findViewById(R.id.btnCreateLocal);
        buttonCreate.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    mAct.createEncFS(false);
                }});

        /* Mount local EncFS volume */
        buttonMount = (Button)v.findViewById(R.id.btnMount);
        buttonMount.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (!ShellUtils.isMounted("fuse.encfs")) {
                    mAct.prevMode = mAct.opMode;
                    mAct.opMode = Cryptonite.MOUNT_MODE;
                    mAct.currentDialogLabel = getString(R.string.select_enc);
                    mAct.currentDialogButtonLabel = getString(R.string.select_enc_short);
                    mAct.currentDialogMode = SelectionMode.MODE_OPEN_ENCFS_MOUNT;
                    if (Cryptonite.externalStorageIsWritable()) {
                        mAct.currentDialogStartPath = Environment.getExternalStorageDirectory().getPath();
                    } else {
                        mAct.currentDialogStartPath = "/";
                    }
                    mAct.currentDialogRoot = "/";
                    mAct.currentDialogRootName = mAct.currentDialogRoot;
                    
                    if (!Cryptonite.externalStorageIsWritable()) {
                        mAct.showAlert(R.string.error, R.string.sdcard_not_writable);
                    } else {
                        mAct.launchBuiltinFileBrowser();//DIRPICK_MODE);
                    }
                    
                } else {
                    String[] umountlist = {"umount", DirectorySettings.INSTANCE.mntDir};
                    try {
                        ShellUtils.runBinary(umountlist,
                                DirectorySettings.INSTANCE.binDirPath, null, true);
                    } catch (IOException e) {
                        Toast.makeText(mAct, 
                                getString(R.string.umount_fail) + ": " + e.getMessage(), 
                                Toast.LENGTH_LONG).show();
                    } catch (InterruptedException e) {
                        Toast.makeText(mAct, 
                                getString(R.string.umount_fail) + ": " + e.getMessage(), 
                                Toast.LENGTH_LONG).show();
                    }
                    
                    /* Still mounted? Offer to kill encfs */
                    if (ShellUtils.isMounted("fuse.encfs")) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(mAct);
                        builder.setIcon(R.drawable.ic_launcher_cryptonite)
                        .setTitle(R.string.warning)
                        .setMessage(R.string.umount_still_mounted)
                        .setPositiveButton(R.string.umount_all,
                                new DialogInterface.OnClickListener()
                        {
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                String[] killlist = {"killall", "encfs"};
                                try {
                                    ShellUtils.runBinary(killlist, 
                                            DirectorySettings.INSTANCE.binDirPath, null, true);
                                } catch (IOException e) {
                                    Toast.makeText(mAct, 
                                            getString(R.string.umount_fail) + ": " + e.getMessage(), 
                                            Toast.LENGTH_LONG).show();
                                } catch (InterruptedException e) {
                                    Toast.makeText(mAct, 
                                            getString(R.string.umount_fail) + ": " + e.getMessage(), 
                                            Toast.LENGTH_LONG).show();
                                }
                                updateMountButtons();
                            }
                        })
                        .setNegativeButton(R.string.cancel,
                                new DialogInterface.OnClickListener()
                        {
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                
                            }
                        });
                        AlertDialog dialog = builder.create();
                        dialog.show();
                    }
                    updateMountButtons();
                }
            }});

        /* View mounted EncFS volume */
        buttonViewMount = (Button)v.findViewById(R.id.btnViewMount);
        buttonViewMount.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    mAct.prevMode = mAct.opMode;
                    mAct.opMode = Cryptonite.VIEWMOUNT_MODE;
                    mAct.currentDialogStartPath = DirectorySettings.INSTANCE.mntDir;
                    mAct.currentDialogRoot = "/";
                    mAct.currentDialogRootName = mAct.currentDialogRoot;
                    mAct.currentDialogLabel = getString(R.string.fb_name);
                    mAct.currentDialogButtonLabel = getString(R.string.back);
                    mAct.currentDialogMode = SelectionMode.MODE_OPEN_ENCFS;
                    if (!Cryptonite.externalStorageIsWritable()) {
                        mAct.showAlert(R.string.error, R.string.sdcard_not_writable);
                    } else {
                        mAct.launchFileBrowser(Cryptonite.FILEPICK_MODE);
                    }
                    mAct.opMode = mAct.prevMode;
                }});

        return v;
    }

    @Override
    public void onActivityCreated (Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mAct.setLocalFragment(this);
        tv.setText(mAct.textOut);
        tvMountInfo.setText(mAct.mountInfo);
        updateMountButtons();
        updateDecryptButtons();
    }
    
    public void updateMountButtons() {
        if (buttonMount == null || buttonViewMount == null) {
            return;
        }
        
        boolean ism = ShellUtils.isMounted("fuse.encfs");
        
        Log.v(Cryptonite.TAG, "EncFS mount state: " + ism + "; FUSE support: " + mAct.hasFuse);
        buttonMount.setEnabled(mAct.hasFuse && DirectorySettings.INSTANCE.hasBin());
        
        if (ism) {
            buttonMount.setText(R.string.unmount);
        } else {
            buttonMount.setText(R.string.mount);
        }
        
        buttonViewMount.setEnabled(ism && mAct.hasFuse && DirectorySettings.INSTANCE.hasBin());
    }

    public void updateDecryptButtons() {
        if (buttonDecrypt == null || buttonBrowseDecrypted == null ||
                buttonForgetDecryption == null || buttonCreate == null) 
        {
            return;
        }

        boolean volumeLoaded = (Cryptonite.jniVolumeLoaded() == Cryptonite.jniSuccess());

        buttonDecrypt.setEnabled(!volumeLoaded);
        buttonBrowseDecrypted.setEnabled(volumeLoaded &&
                StorageManager.INSTANCE.getEncFSStorageType() == Storage.STOR_LOCAL);
        buttonForgetDecryption.setEnabled(volumeLoaded);
        buttonSave.setEnabled(volumeLoaded &&
                StorageManager.INSTANCE.getEncFSStorageType() == Storage.STOR_LOCAL);
        buttonLoad.setEnabled(!volumeLoaded && mAct.hasStoredLocal());
        buttonCreate.setEnabled(!volumeLoaded);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        updateMountButtons();
        updateDecryptButtons();
        if (tv != null) {
            tv.setText(mAct.textOut);
            tv.invalidate();
        }
    }
}

