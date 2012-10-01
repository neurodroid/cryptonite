package csh.cryptonite.storage;

import java.io.File;
import java.io.IOException;

import csh.cryptonite.Cryptonite;
import csh.cryptonite.DirectorySettings;
import csh.cryptonite.R;
import csh.cryptonite.SelectionMode;
import csh.cryptonite.ShellUtils;
import csh.cryptonite.database.Volume;

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

public class LocalFragment extends StorageFragment {

    public TextView tvMountInfo;
    
    private Button buttonMount, buttonViewMount, buttonSaveLoadMount;
    
    public LocalFragment() {
        super();
        idLayout = R.layout.local_tab;
        idTvVersion = R.id.tvVersionLocal;
        idBtnDecrypt = R.id.btnDecryptLocal;
        idBtnBrowseDecrypted = R.id.btnBrowseDecryptedLocal;
        idBtnForgetDecryption = R.id.btnForgetDecryptionLocal;
        idBtnSaveLoad = R.id.btnSaveLoadLocal;
        idBtnCreate = R.id.btnCreateLocal;
        storageType = Storage.STOR_LOCAL;
        opMode = Cryptonite.SELECTLOCALENCFS_MODE;
        dialogMode = SelectionMode.MODE_OPEN_ENCFS;
        dialogModeDefault = SelectionMode.MODE_OPEN_DEFAULT;
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState)
    {
        super.onCreateView(inflater, container, savedInstanceState);
        
        tvMountInfo = (TextView)mView.findViewById(R.id.tvMountInfo);
        
        /* Mount local EncFS volume */
        buttonMount = (Button)mView.findViewById(R.id.btnMount);
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
                    
                    MountManager.INSTANCE.initEncFSStorage(mAct);
                    
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
        buttonViewMount = (Button)mView.findViewById(R.id.btnViewMount);
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

        /* Save as default */
        buttonSaveLoadMount = (Button)mView.findViewById(R.id.btnSaveLoadMount);
        buttonSaveLoadMount.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    if (ShellUtils.isMounted("fuse.encfs")) {
                        mAct.saveDefault(storageType, Volume.MOUNT);
                    } else {
                        final Volume volume = mAct.restoreDefault(storageType, Volume.MOUNT);
                        MountManager.INSTANCE.initEncFSStorage(mAct);
                        boolean defaultExists = new File(Environment.getExternalStorageDirectory().getPath() + volume.getSource()).exists();
                        if (defaultExists) {
                            mAct.opMode = opMode;
                            mAct.currentDialogLabel = getString(R.string.select_enc);
                            mAct.currentDialogButtonLabel = getString(
                                    R.string.select_enc_short);
                            mAct.currentDialogMode = SelectionMode.MODE_OPEN_MOUNT_DEFAULT;
                            openEncFSVolumeDefault(volume);
                        } else {
                            Toast.makeText(mAct, 
                                    getString(R.string.default_missing) + " (" + volume.getSource() + ")", 
                                    Toast.LENGTH_LONG).show();   
                        }
                    }
                }});

        return mView;
    }

    @Override
    public void onActivityCreated (Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mAct.setLocalFragment(this);
        tvMountInfo.setText(mAct.mountInfo);
    }
    
    @Override
    protected void openEncFSVolume() {
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
    }
     
    @Override
    protected void openEncFSVolumeDefault(Volume volume) {
        mAct.currentDialogStartPath = Environment.getExternalStorageDirectory().getPath() + volume.getSource();
        mAct.currentDialogRoot = "/";
        mAct.currentDialogRootName = mAct.currentDialogRoot;
        mAct.currentConfigFile = volume.getEncfsConfig();
        if (!Cryptonite.externalStorageIsWritable()) {
            mAct.showAlert(R.string.error, R.string.sdcard_not_writable);
        } else {
            mAct.launchBuiltinFileBrowser();
        }
    }
     
    public void updateMountButtons() {
        if (buttonMount == null || buttonViewMount == null) {
            return;
        }
        
        boolean ism = ShellUtils.isMounted("fuse.encfs");
        if (!ism) {
            MountManager.INSTANCE.resetEncFSStorage();
        }
        Log.v(Cryptonite.TAG, "EncFS mount state: " + ism + "; FUSE support: " + mAct.hasFuse);
        buttonMount.setEnabled(mAct.hasFuse && DirectorySettings.INSTANCE.hasBin());
        
        if (ism) {
            buttonMount.setText(R.string.unmount);
        } else {
            buttonMount.setText(R.string.mount);
        }
        
        buttonViewMount.setEnabled(ism && mAct.hasFuse && DirectorySettings.INSTANCE.hasBin());

        if (ism) {
            buttonSaveLoadMount.setText(R.string.default_save);
            buttonSaveLoadMount.setEnabled(true);
        } else {
            buttonSaveLoadMount.setText(R.string.default_restore);
            /* Is there any saved volume at all? */
            buttonSaveLoadMount.setEnabled(mAct.hasDefault(storageType, Volume.MOUNT) && 
                    mAct.hasFuse && DirectorySettings.INSTANCE.hasBin());
        }
    }

    @Override
    public void updateDecryptButtons() {
        super.updateDecryptButtons();
        updateMountButtons();
    }
}
