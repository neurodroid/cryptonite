package csh.cryptonite;

import java.io.IOException;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class LocalFragment extends Fragment {
    
    public TextView tv;
    public TextView tvMountInfo;
    
    public Button buttonDecrypt, buttonBrowseDecrypted, buttonForgetDecryption,
        buttonCreate, buttonMount, buttonViewMount;
    
    private Cryptonite mAct;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState)
    {
        View v = inflater.inflate(R.layout.local_tab, container, false);

        mAct = (Cryptonite)getActivity();
        
        tv = (TextView)v.findViewById(R.id.tvVersionLocal);
        tv.setText(mAct.textOut);

        tvMountInfo = (TextView)v.findViewById(R.id.tvMountInfo);
        tvMountInfo.setText(mAct.mountInfo);
        
        /* Decrypt local encFS volume */
        buttonDecrypt = (Button)v.findViewById(R.id.btnDecryptLocal);
        buttonDecrypt.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    mAct.opMode = Cryptonite.SELECTLOCALENCFS_MODE;
                    mAct.currentDialogLabel = getString(R.string.select_enc);
                    mAct.currentDialogButtonLabel = getString(R.string.select_enc_short);
                    mAct.currentDialogMode = SelectionMode.MODE_OPEN;
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

        buttonDecrypt.setEnabled(!(Cryptonite.jniVolumeLoaded() == Cryptonite.jniSuccess()));

        /* Browse decrypted volume */
        buttonBrowseDecrypted = (Button)v.findViewById(R.id.btnBrowseDecryptedLocal);
        buttonBrowseDecrypted.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    mAct.browseEncFS(mAct.mApp.getCurrentBrowsePath(),
                            mAct.mApp.getCurrentBrowseStartPath());
                }});

        buttonBrowseDecrypted.setEnabled(
                Cryptonite.jniVolumeLoaded() == Cryptonite.jniSuccess() && 
                mAct.mApp.isLocal());

        /* Clear decryption information */
        buttonForgetDecryption = (Button)v.findViewById(R.id.btnForgetDecryptionLocal);
        buttonForgetDecryption.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    mAct.mApp.cleanUpDecrypted();
                    mAct.updateDecryptButtons();
                }});

        buttonForgetDecryption.setEnabled(
                Cryptonite.jniVolumeLoaded() == Cryptonite.jniSuccess());
        

        /* Create local EncFS volume */
        buttonCreate = (Button)v.findViewById(R.id.btnCreateLocal);
        buttonCreate.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    mAct.createEncFS(false);
                }});

        buttonCreate.setEnabled(
                !(Cryptonite.jniVolumeLoaded() == Cryptonite.jniSuccess()));

        
        /* Mount local EncFS volume */
        buttonMount = (Button)v.findViewById(R.id.btnMount);
        buttonMount.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (!ShellUtils.isMounted("fuse.encfs")) {
                    mAct.prevMode = mAct.opMode;
                    mAct.opMode = Cryptonite.MOUNT_MODE;
                    mAct.currentDialogLabel = getString(R.string.select_enc);
                    mAct.currentDialogButtonLabel = getString(R.string.select_enc_short);
                    mAct.currentDialogMode = SelectionMode.MODE_OPEN;
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
                    String[] umountlist = {"umount", mAct.mntDir};
                    try {
                        ShellUtils.runBinary(umountlist, mAct.mApp.getBinDirPath(), null, true);
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
                                    ShellUtils.runBinary(killlist, mAct.mApp.getBinDirPath(), null, true);
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
                    mAct.currentDialogStartPath = mAct.mntDir;
                    mAct.currentDialogRoot = "/";
                    mAct.currentDialogRootName = mAct.currentDialogRoot;
                    mAct.currentDialogLabel = getString(R.string.fb_name);
                    mAct.currentDialogButtonLabel = getString(R.string.back);
                    mAct.currentDialogMode = SelectionMode.MODE_OPEN;
                    if (!Cryptonite.externalStorageIsWritable()) {
                        mAct.showAlert(R.string.error, R.string.sdcard_not_writable);
                    } else {
                        mAct.launchFileBrowser(Cryptonite.FILEPICK_MODE);
                    }
                    mAct.opMode = mAct.prevMode;
                }});

        updateMountButtons();
        updateDecryptButtons();
        
        return v;
    }

    public void updateMountButtons() {
        if (buttonMount == null || buttonViewMount == null) {
            return;
        }
        
        boolean ism = ShellUtils.isMounted("fuse.encfs");
        
        Log.v(Cryptonite.TAG, "EncFS mount state: " + ism + "; FUSE support: " + mAct.hasFuse);
        buttonMount.setEnabled(mAct.hasFuse && mAct.mApp.hasBin());
        
        if (ism) {
            buttonMount.setText(R.string.unmount);
        } else {
            buttonMount.setText(R.string.mount);
        }
        
        buttonViewMount.setEnabled(ism && mAct.hasFuse && mAct.mApp.hasBin());
    }

    public void updateDecryptButtons() {
        if (buttonDecrypt == null || buttonBrowseDecrypted == null ||
                buttonForgetDecryption == null || buttonCreate == null) 
        {
            return;
        }

        boolean volumeLoaded = (Cryptonite.jniVolumeLoaded() == Cryptonite.jniSuccess());

        buttonDecrypt.setEnabled(!volumeLoaded);
        buttonBrowseDecrypted.setEnabled(volumeLoaded && mAct.mApp.isLocal());
        buttonForgetDecryption.setEnabled(volumeLoaded);
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

