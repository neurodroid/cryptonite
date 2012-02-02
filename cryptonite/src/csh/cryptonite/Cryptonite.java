// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.

// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

// Copyright (c) 2012, Christoph Schmidt-Hieber

package csh.cryptonite;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import android.graphics.drawable.Drawable;

import android.net.Uri;

import android.os.Bundle;
import android.os.Environment;

import android.text.SpannableString;
import android.text.util.Linkify;
import android.text.method.ScrollingMovementMethod;

import android.util.Log;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;

import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.Session.AccessType;
import com.dropbox.client2.session.TokenPair;

public class Cryptonite extends Activity
{

    private static final int REQUEST_PREFS=0, REQUEST_CODE_PICK_FILE_OR_DIRECTORY=1;
    private static final int MOUNT_MODE=0, SELECTLOCALENCFS_MODE=1, SELECTDBENCFS_MODE=2,
        VIEWMOUNT_MODE=3, SELECTLOCALEXPORT_MODE=4, LOCALEXPORT_MODE=5, DROPBOX_AUTH_MODE=6,
        SELECTDBEXPORT_MODE=7, DBEXPORT_MODE=8;
    private static final int DIRPICK_MODE=0, FILEPICK_MODE=1;
    private static final int MY_PASSWORD_DIALOG_ID = 0;
    private static final int DIALOG_MARKETNOTFOUND=1, DIALOG_OI_UNAVAILABLE=2;
    private static final int MAX_JNI_SIZE = 512;
    public static final String MNTPNT = "/csh.cryptonite/mnt";
    public static final String BINDIR = "/data/data/csh.cryptonite";
    public static final String ENCFSBIN = BINDIR + "/encfs";
    public static final String ENCFSCTLBIN = BINDIR + "/encfsctl";
    public static final String TAG = "cryptonite";

    private static final String BROWSEPNT = "browse";
    private static final String OPENPNT = "open";
    private static final String DROPBOXPNT = "dropbox";
    private static final String READPNT = "read";

    private String currentDialogStartPath = "/";
    private String currentDialogLabel = "";
    private String currentDialogButtonLabel = "OK";
    private String currentDialogRoot = "/";
    private String currentDialogDBEncFS = "";
    private String currentDialogRootName = currentDialogRoot;
    private String currentReturnPath = "/";
    private String currentBrowsePath = "/";
    private String currentBrowseStartPath = "/";
    private String currentOpenPath = "/";
    private String currentUploadPath = "/";
    private String currentPassword = "\0";
    private String encfsBrowseRoot = "/";
    private String[] currentReturnPathList = {};
    private int currentDialogMode = SelectionMode.MODE_OPEN;

    private String mntDir = "/sdcard" + MNTPNT;
    private TextView tv;
    private TextView tvMountInfo;
    private String encfsversion, encfsoutput;
    private Button buttonDropbox, buttonDropboxDecrypt, buttonLocalDecrypt,
        buttonBrowseDecrypted, buttonForgetDecryption,
        buttonMount, buttonViewMount;
    private int opMode = -1;
    private int prevMode = -1;
    private boolean alert = false;
    private String alertMsg = "";
 
    private boolean mLoggedIn = false;
    private boolean mDropboxDecrypted = false;
    private boolean mLocalDecrypted = false;
    private boolean hasFuse = false;
    private boolean triedLogin = false;

    // If you'd like to change the access type to the full Dropbox instead of
    // an app folder, change this value.
    final static private AccessType ACCESS_TYPE = AccessType.DROPBOX;

    final static private String ACCOUNT_PREFS_NAME = "csh.cryptonite_preferences";
    final static private String ACCESS_KEY_NAME = "ACCESS_KEY";
    final static private String ACCESS_SECRET_NAME = "ACCESS_SECRET";
    
    /** Called when the activity is first created. */
    @Override public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        // We create a new AuthSession so that we can use the Dropbox API.
        AndroidAuthSession session = buildSession();
        ((CryptoniteApp) getApplication()).setDBApi(new DropboxAPI<AndroidAuthSession>(session));
        
        setContentView(R.layout.main);

        getResources();

        cleanUpDecrypted();
        
        encfsversion = "EncFS " + jniVersion();
        Log.v(TAG, encfsversion);
        tv = (TextView)findViewById(R.id.tvVersion);
        tv.setText(encfsversion);

        if (externalStorageIsWritable()) {
            mntDir = Environment.getExternalStorageDirectory().getPath() + MNTPNT;
            File mntDirF = new File(mntDir);
            if (!mntDirF.exists()) {
                mntDirF.mkdirs();
            }
        }

        tvMountInfo = (TextView)findViewById(R.id.tvMountInfo);
        if (!externalStorageIsWritable() || !ShellUtils.supportsFuse()) {
            tvMountInfo.setText(this.getString(R.string.mount_info_unsupported));
        }
        
        boolean volumeLoaded = (jniVolumeLoaded() == jniSuccess());
        
        /* Copy the encfs binaries to binDir and make executable. */
        if (!(new File(ENCFSBIN)).exists()) {
            final ProgressDialog pd = ProgressDialog.show(this,
                                                          this.getString(R.string.wait_msg),
                                                          this.getString(R.string.copying_bins), true);
            new Thread(new Runnable(){
                    public void run(){
                        cpEncFSBin();
                        runOnUiThread(new Runnable(){
                                public void run() {
                                    if (pd.isShowing())
                                        pd.dismiss();
                                }
                            });
                    }
                }).start();
        }
        
        /* Link with Dropbox */
        buttonDropbox = (Button)findViewById(R.id.btnDropbox);
        buttonDropbox.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    if (mLoggedIn) {
                        logOut();
                    } else {
                        triedLogin = true;
                        // Start the remote authentication
                        ((CryptoniteApp) getApplication()).getDBApi()
                            .getSession().startAuthentication(Cryptonite.this);
                    }                        
                }});

        buttonDropbox.setEnabled(true);
        
        /* Decrypt EncFS volume on Dropbox */
        buttonDropboxDecrypt = (Button)findViewById(R.id.btnDropboxDecrypt);
        buttonDropboxDecrypt.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    opMode = SELECTDBENCFS_MODE;
                    currentDialogLabel = Cryptonite.this.getString(R.string.select_enc);
                    currentDialogButtonLabel = Cryptonite.this.getString(
                            R.string.select_enc_short);
                    currentDialogMode = SelectionMode.MODE_OPEN_DB;
                    currentDialogStartPath = getPrivateDir(BROWSEPNT).getPath();
                    currentDialogRoot = currentDialogStartPath;
                    currentDialogRootName = getString(R.string.dropbox_root_name);
                    currentDialogDBEncFS = "";
                    if (mLoggedIn) {
                        launchBuiltinFileBrowser();
                    }                        
                }});

        buttonDropboxDecrypt.setEnabled(mLoggedIn && !volumeLoaded);
        
        /* Decrypt local encFS volume */
        buttonLocalDecrypt = (Button)findViewById(R.id.btnLocalDecrypt);
        buttonLocalDecrypt.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    opMode = SELECTLOCALENCFS_MODE;
                    currentDialogLabel = Cryptonite.this.getString(R.string.select_enc);
                    currentDialogButtonLabel = Cryptonite.this.getString(R.string.select_enc_short);
                    currentDialogMode = SelectionMode.MODE_OPEN;
                    if (externalStorageIsWritable()) {
                        currentDialogStartPath = Environment.getExternalStorageDirectory().getPath();
                    } else {
                        currentDialogStartPath = "/";
                    }
                    currentDialogRoot = "/";
                    currentDialogRootName = currentDialogRoot;
                    if (!externalStorageIsWritable()) {
                        showAlert(R.string.error, R.string.sdcard_not_writable);
                    } else {
                        launchBuiltinFileBrowser(); //DIRPICK_MODE);
                    }
                }});

        buttonLocalDecrypt.setEnabled(!volumeLoaded);

        /* Browse decrypted volume */
        buttonBrowseDecrypted = (Button)findViewById(R.id.btnBrowseDecrypted);
        buttonBrowseDecrypted.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    if (mLocalDecrypted) {
                        localBrowseEncFS(currentBrowsePath, currentBrowseStartPath);
                    } else if (mDropboxDecrypted) {
                        dbBrowseEncFS(currentBrowsePath, currentBrowseStartPath);
                    }
                    
                }});

        buttonBrowseDecrypted.setEnabled(volumeLoaded);
        
        /* Clear decryption information */
        buttonForgetDecryption = (Button)findViewById(R.id.btnForgetDecryption);
        buttonForgetDecryption.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    cleanUpDecrypted();
                    updateDecryptButtons();
                }});

        buttonForgetDecryption.setEnabled(volumeLoaded);
        
        /* Mount local EncFS volume */
        buttonMount = (Button)findViewById(R.id.btnMount);
        buttonMount.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (!ShellUtils.isMounted("fuse.encfs")) {
                    prevMode = opMode;
                    opMode = MOUNT_MODE;
                    currentDialogLabel = Cryptonite.this.getString(R.string.select_enc);
                    currentDialogButtonLabel = Cryptonite.this.getString(R.string.select_enc_short);
                    currentDialogMode = SelectionMode.MODE_OPEN;
                    if (externalStorageIsWritable()) {
                        currentDialogStartPath = Environment.getExternalStorageDirectory().getPath();
                    } else {
                        currentDialogStartPath = "/";
                    }
                    currentDialogRoot = "/";
                    currentDialogRootName = currentDialogRoot;
                    currentDialogDBEncFS = "";
                    
                    if (!externalStorageIsWritable()) {
                        showAlert(R.string.error, R.string.sdcard_not_writable);
                    } else {
                        launchBuiltinFileBrowser();//DIRPICK_MODE);
                    }
                    
                } else {
                    String[] umountlist = {"umount", mntDir};
                    ShellUtils.runBinary(umountlist, BINDIR, null, true);
                    updateMountButtons();                        
                }
            }});

        /* View mounted EncFS volume */
        buttonViewMount = (Button)findViewById(R.id.btnViewMount);
        buttonViewMount.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    prevMode = opMode;
                    opMode = VIEWMOUNT_MODE;
                    currentDialogStartPath = mntDir;
                    currentDialogRoot = "/";
                    currentDialogRootName = currentDialogRoot;
                    currentDialogDBEncFS = "";
                    currentDialogLabel = Cryptonite.this.getString(R.string.fb_name);
                    currentDialogButtonLabel = Cryptonite.this.getString(R.string.back);
                    currentDialogMode = SelectionMode.MODE_OPEN;
                    if (!externalStorageIsWritable()) {
                        showAlert(R.string.error, R.string.sdcard_not_writable);
                    } else {
                        launchFileBrowser(FILEPICK_MODE);
                    }
                    opMode = prevMode;
                }});

        hasFuse = ShellUtils.supportsFuse();
        updateMountButtons();
        updateDecryptButtons();
        
        VirtualFileSystem vfs = VirtualFileSystem.INSTANCE;
        vfs.init();
        
        /* Test local EncFS volume */
        /*
        currentDialogStartPath = Environment.getExternalStorageDirectory().getPath();
        currentDialogRoot = "/";
        currentReturnPath = "/mnt/sdcard/.AAEncfs3"; 
        localDecryptEncFS(currentReturnPath, "password");
        */
        showAlert(getString(R.string.disclaimer), getString(R.string.no_warranty),
                getString(R.string.understand));
    }

    private void cleanUpDecrypted() {
        jniResetVolume();
        
        /* Delete directories */
        deleteDir(getBaseContext().getFilesDir());
        deleteDir(getBaseContext().getDir(BROWSEPNT, Context.MODE_PRIVATE));
        deleteDir(getBaseContext().getDir(OPENPNT, Context.MODE_PRIVATE));
        deleteDir(getBaseContext().getDir(DROPBOXPNT, Context.MODE_PRIVATE));
        deleteDir(getBaseContext().getDir(READPNT, Context.MODE_WORLD_READABLE));
        
        /* Delete virtual file system */
        VirtualFileSystem.INSTANCE.clear();
    }

    private void updateDecryptButtons() {
        boolean volumeLoaded = (jniVolumeLoaded() == jniSuccess());

        buttonLocalDecrypt.setEnabled(!volumeLoaded);
        buttonDropboxDecrypt.setEnabled(!volumeLoaded && mLoggedIn);
        buttonBrowseDecrypted.setEnabled(volumeLoaded);
        buttonForgetDecryption.setEnabled(volumeLoaded);
        
        if (!volumeLoaded) {
            mDropboxDecrypted = false;
            mLocalDecrypted = false;
        }
    }

    private void updateMountButtons() {
        boolean ism = ShellUtils.isMounted("fuse.encfs");

        Log.v(TAG, "EncFS mount state: " + ism + "; FUSE support: " + hasFuse);
        buttonMount.setEnabled(hasFuse);
        
        if (ism) {
            buttonMount.setText(R.string.unmount);            
        } else {
            buttonMount.setText(R.string.mount);
        }
        
        buttonViewMount.setEnabled(ism && hasFuse);
    }

    /** Called upon exit from other activities */
    public synchronized void onActivityResult(final int requestCode,
                                              int resultCode, final Intent data) {

        switch (requestCode) {
        case SelectionMode.MODE_OPEN:
        case SelectionMode.MODE_OPEN_DB:
            /* file dialog */
            if (resultCode == Activity.RESULT_OK && data != null) {
                currentReturnPath = data.getStringExtra(FileDialog.RESULT_EXPORT_PATH);
                if (currentReturnPath != null ) {
                    switch (opMode) {
                    case MOUNT_MODE:
                    case SELECTLOCALENCFS_MODE:
                    case SELECTDBENCFS_MODE:
                        showDialog(MY_PASSWORD_DIALOG_ID);
                        break;
                    case LOCALEXPORT_MODE:
                    case DBEXPORT_MODE:
                        if (currentReturnPathList != null) {
                            final ProgressDialog pd = ProgressDialog.show(this,
                                    this.getString(R.string.wait_msg),
                                    this.getString(R.string.running_export), true);
                            new Thread(new Runnable(){
                                public void run(){
                                    String exportName = currentReturnPath + "/Cryptonite";
                                    Log.v(TAG, "Exporting to " + exportName);
                                    if (!new File(exportName).exists()) {
                                        new File(exportName).mkdirs();
                                    }
                                    if (!new File(exportName).exists()) {
                                        alert = true;
                                    } else {
                                        if (opMode == LOCALEXPORT_MODE) {
                                            alert = !localExport(currentReturnPathList, encfsBrowseRoot, 
                                                    currentReturnPath + "/Cryptonite", currentDialogDBEncFS);
                                            /* alert = (jniExport(currentReturnPathList, encfsBrowseRoot, 
                                                    currentReturnPath + "/Cryptonite") != jniSuccess()); */
                                        } else {
                                            alert = !dbExport(currentReturnPathList, encfsBrowseRoot, 
                                                    currentReturnPath + "/Cryptonite", currentDialogDBEncFS);
                                        }
                                    }
                                    runOnUiThread(new Runnable(){
                                        public void run() {
                                            if (pd.isShowing())
                                                pd.dismiss();
                                            if (alert) {
                                                showAlert(R.string.error, R.string.export_failed);
                                                alert = false;
                                            }
                                        }
                                    });
                                }
                            }).start();
                        }
                        break;
                    }
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                /* Log.d(TAG, "file not selected"); */
            }
            break;
        case SelectionMode.MODE_OPEN_MULTISELECT:
        case SelectionMode.MODE_OPEN_MULTISELECT_DB:
            /* file dialog */
            if (resultCode == Activity.RESULT_OK && data != null) {
                currentReturnPathList = data.getStringArrayExtra(FileDialog.RESULT_EXPORT_PATH);
                if (currentReturnPathList != null && currentReturnPathList.length > 0) {

                    if (currentReturnPathList.length > MAX_JNI_SIZE) {
                        showAlert(R.string.error, R.string.jni_arg_too_large);
                        break;
                    }

                    /* Select destination directory for exported files */
                    currentDialogLabel = Cryptonite.this.getString(R.string.select_exp);
                    currentDialogButtonLabel = Cryptonite.this.getString(R.string.select_exp_short);
                    currentDialogMode = SelectionMode.MODE_OPEN;
                    if (externalStorageIsWritable()) {
                        currentDialogStartPath = Environment
                                .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                                .getPath();
                    } else {
                        currentDialogStartPath = "/";
                    }
                    currentDialogRoot = "/";
                    currentDialogRootName = currentDialogRoot;
                    /* currentDialogDBEncFS = ""; Leave this untouched for dbExport */
                    switch (opMode) {
                    case SELECTLOCALEXPORT_MODE:
                        opMode = LOCALEXPORT_MODE;
                        break;
                    case SELECTDBEXPORT_MODE:
                        opMode = DBEXPORT_MODE;
                        break;
                    }
                    launchBuiltinFileBrowser();
                } else {
                    currentOpenPath = data.getStringExtra(FileDialog.RESULT_OPEN_PATH);
                    if (currentOpenPath != null && currentOpenPath.length() > 0) {
                        // Log.d(TAG, "Request to view " + currentOpenPath);
                        openEncFSFile(currentOpenPath, encfsBrowseRoot,
                                currentDialogDBEncFS, (opMode == SELECTDBEXPORT_MODE));
                    } else {
                        currentUploadPath = data.getStringExtra(FileDialog.RESULT_UPLOAD_PATH);
                        if (currentUploadPath != null && currentUploadPath.length() > 0) {
                            uploadEncFSFile(currentUploadPath, encfsBrowseRoot,
                                    currentDialogDBEncFS, (opMode == SELECTDBEXPORT_MODE));
                        }
                    }
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {

            }
            break;
        case REQUEST_PREFS:
            @SuppressWarnings("unused")
            SharedPreferences prefs = getBaseContext().getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
            break;
        case REQUEST_CODE_PICK_FILE_OR_DIRECTORY:
            /* from external OI file browser */
            if (resultCode == RESULT_OK && data != null) {
                // obtain the filename
                Uri fileUri = data.getData();
                if (fileUri != null) {
                    currentReturnPath = fileUri.getPath();
                }
            }
            break;
        default:
            Log.e(TAG, "Unknown request code");
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        AndroidAuthSession session = ((CryptoniteApp) getApplication()).getDBApi().getSession();

        // The next part must be inserted in the onResume() method of the
        // activity from which session.startAuthentication() was called, so
        // that Dropbox authentication completes properly.
        if (session.authenticationSuccessful()) {
            try {
                // Mandatory call to complete the auth
                session.finishAuthentication();

                // Store it locally in our app for later use
                TokenPair tokens = session.getAccessTokenPair();
                storeKeys(tokens.key, tokens.secret);
                
                ((CryptoniteApp) getApplication()).clearDBHashMap();
                
                setLoggedIn(true);
            } catch (IllegalStateException e) {
                Toast.makeText(this, 
                        getString(R.string.dropbox_auth_fail) + ": " + e.getLocalizedMessage(), 
                        Toast.LENGTH_LONG);
            }
        } else {
            if (triedLogin) {
                triedLogin = false;
                AlertDialog.Builder builder = new AlertDialog.Builder(Cryptonite.this);
                builder.setIcon(R.drawable.ic_launcher_cryptonite)
                    .setTitle(R.string.dropbox_enable)
                    .setMessage(R.string.dropbox_enable_email)
                    .setPositiveButton(R.string.send_email,
                            new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,
                                int which) {
                            final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
                            emailIntent.setType("plain/text");
                            emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL,
                                    new String[]{"christoph.schmidthieber@googlemail.com"});
                            emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
                                    getString(R.string.dropbox_enable_email_subject));
                            emailIntent.putExtra(android.content.Intent.EXTRA_TEXT,
                                    getString(R.string.dropbox_enable_email_content));
                            startActivity(Intent.createChooser(emailIntent, "Send mail..."));    
                        }
                    })
                    .setNegativeButton(R.string.cancel,
                            new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,
                                int which) {
        
                        }
                    });  
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        }
        
        updateMountButtons();
      
        updateDecryptButtons();
    }

    /** Copy encfs to binDir and make executable */
    public void cpEncFSBin() {
        cpEncFSBin("encfs");
    }
    
    /** Copy encfs to binDir and make executable */
    public void cpEncFSBin(String trunk) {
        String arch = "armeabi";
        /* if (withVfp) {
            arch += "-v7a";
            } */
            
        File binDir = new File(BINDIR);
        if (!binDir.exists()) {
            throw new RuntimeException("Couldn't find binary directory");
        }
        String binName = BINDIR + "/" + trunk;

        /* Catenate split files */
        Log.v(TAG, "Looking for assets in " + arch);
        try {
            String[] assetsFiles = getAssets().list(arch);
            File newf = new File(binName);
            FileOutputStream os = new FileOutputStream(newf);
            for (String assetsFile : assetsFiles) {
                if (assetsFile.substring(0, assetsFile.indexOf(".")).compareTo(trunk) == 0) {
                    Log.v(TAG, "Found EncFS binary part: " + assetsFile);
                    InputStream is = getAssets().open(arch + "/" + assetsFile);

                    byte[] buffer = new byte[is.available()]; 

                    is.read(buffer);

                    os.write(buffer);

                    is.close();
                }
            }
            os.close();

            ShellUtils.chmod(binName, "755");
            
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private void showAlert(int alert_id, int msg_id) {
        showAlert(getString(alert_id), getString(msg_id));
    }
    
    private void showAlert(int alert_id, String msg) {
        showAlert(getString(alert_id), msg);
    }
    
    private void showAlert(String alert, String msg) {
        showAlert(alert, msg, "OK");
    }
    
    private void showAlert(String alert, String msg, String btnLabel) {
        AlertDialog.Builder builder = new AlertDialog.Builder(Cryptonite.this);
        builder.setIcon(R.drawable.ic_launcher_cryptonite)
            .setTitle(alert)
            .setMessage(msg)
            .setPositiveButton(btnLabel,
                               new DialogInterface.OnClickListener() {
                                   public void onClick(DialogInterface dialog,
                                                       int which) {
                                       
                                   }
                               });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /** Deletes all files and subdirectories under dir.
     * Returns true if all deletions were successful.
     * If a deletion fails, the method stops attempting to delete and returns false.
     */
    public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i=0; i<children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }

        // The directory is now empty so delete it
        return dir.delete();
    }
    
    /** This will run the shipped encfs binary and spawn a daemon on rooted devices
     */
    private void mountEncFS(final String srcDir, String pwd) {
        tv.setText(encfsversion);
        tv.invalidate();

        if (jniIsValidEncFS(srcDir) != jniSuccess()) {
            showAlert(R.string.error, R.string.invalid_encfs);
            Log.v(TAG, "Invalid EncFS");
            return;
        }
        
        final ProgressDialog pd = ProgressDialog.show(this,
                                                      this.getString(R.string.wait_msg),
                                                      this.getString(R.string.running_encfs), true);
        Log.v(TAG, "Running encfs with " + srcDir + " " + mntDir);
        new Thread(new Runnable(){
                public void run(){
                    String[] cmdlist = {ENCFSBIN, "--public", "--stdinpass", srcDir, mntDir};
                    encfsoutput = ShellUtils.runBinary(cmdlist, BINDIR, currentPassword, true);
                    runOnUiThread(new Runnable(){
                            public void run() {
                                if (pd.isShowing())
                                    pd.dismiss();
                                if (encfsoutput.length() > 0) {
                                    tv.setText(encfsversion + "\n" + encfsoutput);
                                }
                                nullPassword();
                                updateMountButtons();
                            }
                        });
                }
            }).start();
            
    }
    
    /** Initialize a local EncFS volume. This will check
     * whether the EncFS volume is valid an initialize the EncFS
     * root information
     * 
     * @param srcDir Path to EncFS volume
     * @param pwd password
     */
     private void localDecryptEncFS(final String srcDir, final String pwd) {
        
        if (jniIsValidEncFS(srcDir) != jniSuccess()) {
            showAlert(R.string.error, R.string.invalid_encfs);
            Log.v(TAG, "Invalid EncFS");
            return;
        }
        alertMsg = "";

        final ProgressDialog pd = ProgressDialog.show(this,
                                                      this.getString(R.string.wait_msg),
                                                      this.getString(R.string.running_encfs), true);
        new Thread(new Runnable(){
                public void run(){
                    /* Order is important here: DB root has to store
                     * the previous state of the dialog root.
                     */
                    currentBrowsePath = currentReturnPath;
                    currentBrowseStartPath = currentDialogStartPath;
                    currentDialogDBEncFS = currentReturnPath.substring(currentDialogStartPath.length());
                    Log.i(TAG, "Dialog DB root is " + currentReturnPath);
                    if (jniInit(srcDir, pwd) != jniSuccess()) {
                        Log.v(TAG, getString(R.string.browse_failed));
                        alertMsg = getString(R.string.browse_failed);
                    } else {
                        Log.v(TAG, "Decoding succeeded");
                    }
                    runOnUiThread(new Runnable(){
                            public void run() {
                                if (pd.isShowing())
                                    pd.dismiss();
                                nullPassword();
                                updateDecryptButtons();
                                mLocalDecrypted = true;
                                if (alertMsg.length()!=0) {
                                    showAlert(R.string.error, alertMsg);
                                }
                            }
                        });
                }
            }).start();
            
    }

    /** Browse a local encFS directory using a virtual file system.
     * File names are queried on demand when a directory is opened.
     * @param browsePath EncFS path 
     * @param browseStartPath Root path
     */
     private void localBrowseEncFS(final String browsePath, final String browseStartPath) {
        final VirtualFile browseDirF = new VirtualFile(VirtualFile.VIRTUAL_TAG + "/" + BROWSEPNT);
        browseDirF.mkdirs();
        
        final ProgressDialog pd = ProgressDialog.show(this,
                                                      this.getString(R.string.wait_msg),
                                                      this.getString(R.string.running_encfs), true);
        new Thread(new Runnable(){
                public void run(){
                    currentDialogDBEncFS = browsePath.substring(browseStartPath.length());
                    Log.i(TAG, "Dialog DB root is " + browsePath);
                    currentDialogStartPath = browseDirF.getPath();
                    currentDialogLabel = getString(R.string.select_file_export);
                    currentDialogButtonLabel = getString(R.string.export);
                    currentDialogRoot = currentDialogStartPath;
                    encfsBrowseRoot = currentDialogRoot;
                    currentDialogRootName = getString(R.string.encfs_root);
                    currentDialogMode = SelectionMode.MODE_OPEN_MULTISELECT;
                    runOnUiThread(new Runnable(){
                            public void run() {
                                if (pd.isShowing())
                                    pd.dismiss();
                                opMode = SELECTLOCALEXPORT_MODE;
                                launchBuiltinFileBrowser();
                            }
                        });
                }
            }).start();
            
    }
    
    /** Initialize an EncFS volume on Dropbox. This will check
     * whether the EncFS volume is valid an initialize the EncFS
     * root information
     * 
     * @param srcDir Path to EncFS volume
     * @param pwd password
     */
    private void dbDecryptEncFS(final String srcDir, String pwd) {
        tv.setText(encfsversion);
        tv.invalidate();

        /* Download encfs*.xml from Dropbox 
         * to browse folder */
        String dbPath = srcDir.substring(currentDialogRoot.length());
        String encfsXmlPath = "";
        String encfsXmlRegex = "\\.encfs.\\.xml";
        
        try {
            Entry dbEntry = ((CryptoniteApp) getApplication()).getDBEntry(dbPath); 
            if (dbEntry != null) {
                if (dbEntry.isDir) {
                    if (dbEntry.contents != null) {
                        if (dbEntry.contents.size() > 0) {
                            for (Entry dbChild : dbEntry.contents) {
                                if (!dbChild.isDir) {
                                    if (dbChild.fileName().matches(encfsXmlRegex)) {
                                        encfsXmlPath = currentDialogRoot + dbChild.path;
                                        FileOutputStream fos = new FileOutputStream(encfsXmlPath);
                                        ((CryptoniteApp) getApplication()).getDBApi()
                                            .getFile(dbChild.path, null, fos, null);
                                        Log.i(TAG, "Downloaded " + dbChild.fileName() + " to " + encfsXmlPath);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (DropboxException e) {
            showAlert(R.string.error, getString(R.string.dropbox_read_fail) + e.toString());
            return;
        } catch (FileNotFoundException e) {
            showAlert(R.string.error, getString(R.string.dropbox_read_fail) + e.toString());
            return;
        }
        
        if (jniIsValidEncFS(srcDir) != jniSuccess()) {
            showAlert(R.string.error, getString(R.string.invalid_encfs));
            Log.v(TAG, "Invalid EncFS");
            return;
        }
        alertMsg = "";
        final ProgressDialog pd = ProgressDialog.show(this, 
                this.getString(R.string.wait_msg), 
                this.getString(R.string.running_encfs), true);
        new Thread(new Runnable(){
                public void run(){
                    /* Order is important here: DB root has to store
                     * the previous state of the dialog root.
                     */
                    currentBrowsePath = currentReturnPath;
                    currentBrowseStartPath = currentDialogStartPath;
                    currentDialogDBEncFS = currentReturnPath.substring(currentDialogStartPath.length());
                    Log.i(TAG, "Dialog DB root is " + currentReturnPath);
                    if (jniInit(srcDir, currentPassword) != jniSuccess()) {
                        Log.v(TAG, getString(R.string.browse_failed));
                        alertMsg = getString(R.string.browse_failed);
                    } else {
                        Log.v(TAG, "Decoding succeeded");
                    }
                    runOnUiThread(new Runnable(){
                            public void run() {
                                if (pd.isShowing())
                                    pd.dismiss();
                                nullPassword();
                                updateDecryptButtons();
                                mDropboxDecrypted = true;
                                if (alertMsg.length()!=0) {
                                    showAlert(R.string.error, alertMsg);
                                }
                            }
                        });
                }
            }).start();
            
    }
 
    /** Browse an encFS directory on Dropbox using a virtual file system.
     * File names are queried on demand when a directory is opened.
     * See https://www.dropbox.com/developers/reference/bestpractice
     * @param browsePath EncFS path 
     * @param browseStartPath Dropbox path
     */
    private void dbBrowseEncFS(final String browsePath, final String browseStartPath) {
 
        final VirtualFile browseDirF = new VirtualFile(VirtualFile.VIRTUAL_TAG + "/" + DROPBOXPNT);
        browseDirF.mkdirs();
        
        final ProgressDialog pd = ProgressDialog.show(this, 
                this.getString(R.string.wait_msg), 
                this.getString(R.string.running_encfs), true);
        new Thread(new Runnable(){
                public void run(){
                    /* Order is important here: DB root has to store
                     * the previous state of the dialog root.
                     */
                    currentDialogDBEncFS = browsePath.substring(browseStartPath.length());
                    Log.i(TAG, "Dialog DB root is " + browsePath);
                    currentDialogStartPath = browseDirF.getPath();
                    currentDialogLabel = getString(R.string.select_file_export);
                    currentDialogButtonLabel = getString(R.string.export);
                    currentDialogRoot = currentDialogStartPath;
                    encfsBrowseRoot = currentDialogRoot;
                    currentDialogRootName = getString(R.string.encfs_root);
                    currentDialogMode = SelectionMode.MODE_OPEN_MULTISELECT_DB;
                    runOnUiThread(new Runnable(){
                            public void run() {
                                if (pd.isShowing())
                                    pd.dismiss();
                                opMode = SELECTDBEXPORT_MODE;
                                launchBuiltinFileBrowser();
                            }
                        });
                }
            }).start();
            
    }
    
    public File getPrivateDir(String label) {
        return getPrivateDir(label, Context.MODE_PRIVATE);
    }
    
    public File getPrivateDir(String label, int mode) {
        /* Tear down and recreate the browse directory to make
         * sure we have appropriate permissions */
        File browseDirF = getBaseContext().getDir(label, mode);
        if (browseDirF.exists()) {
            if (!deleteDir(browseDirF)) {
                showAlert(R.string.error, R.string.target_dir_cleanup_failure);
                return null;
            }
        }
        browseDirF = getBaseContext().getDir(label, mode);
        return browseDirF;
    }
    
    private void nullPassword() {
        char[] fill = new char[currentPassword.length()];
        Arrays.fill(fill, '\0');
        currentPassword = new String(fill);
    }
    
    @Override protected Dialog onCreateDialog(int id) {
        switch (id) {
         case MY_PASSWORD_DIALOG_ID:
             LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

             final View layout = inflater.inflate(R.layout.password_dialog, (ViewGroup) findViewById(R.id.root));
             final EditText password = (EditText) layout.findViewById(R.id.EditText_Pwd);

             AlertDialog.Builder builder = new AlertDialog.Builder(this);
             builder.setTitle(R.string.title_password);
             builder.setView(layout);
             builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                     public void onClick(DialogInterface dialog, int whichButton) {
                         removeDialog(MY_PASSWORD_DIALOG_ID);
                     }
                 });
             builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                     public void onClick(DialogInterface dialog, int which) {
                         currentPassword = password.getText().toString();
                         removeDialog(MY_PASSWORD_DIALOG_ID);
                         if (currentPassword.length() > 0) {
                             switch (opMode) {
                              case MOUNT_MODE:
                                  mountEncFS(currentReturnPath, currentPassword);
                                  opMode = prevMode;
                                  break;
                              case SELECTLOCALENCFS_MODE:
                                  localDecryptEncFS(currentReturnPath, currentPassword);
                                  break;
                              case SELECTDBENCFS_MODE:
                                  dbDecryptEncFS(currentReturnPath, currentPassword);
                                  break;
                             }
                         } else {
                             showAlert(R.string.error, R.string.empty_password);
                         }
                     }
                 });
             return builder.create();
         case DIALOG_OI_UNAVAILABLE:
             return new AlertDialog.Builder(Cryptonite.this)
                 .setIcon(R.drawable.ic_launcher_folder)
                 .setTitle(R.string.app_oi_missing)
                 .setPositiveButton(R.string.app_oi_get, new DialogInterface.OnClickListener() {
                         public void onClick(DialogInterface dialog, int whichButton) {
                             Intent intent = new Intent(Intent.ACTION_VIEW,
                                                        Uri.parse("market://details?id=org.openintents.filemanager"));
                             try {
                                 startActivity(intent);
                             } catch (ActivityNotFoundException e) {
                                 showDialog(DIALOG_MARKETNOTFOUND);
                             }
                         }
                     })
                 .setNegativeButton(R.string.app_oi_builtin, new DialogInterface.OnClickListener() {
                         public void onClick(DialogInterface dialog, int whichButton) {
                             launchBuiltinFileBrowser();
                         }
                     })
                 .create();
         case DIALOG_MARKETNOTFOUND:
             return new AlertDialog.Builder(Cryptonite.this)
                 .setIcon(android.R.drawable.ic_dialog_alert)
                 .setTitle(R.string.market_missing)
                 .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                         public void onClick(DialogInterface dialog, int whichButton) {
                             /* Return silently */
                         }
                     })
                 .create();
        }

        return null;
    }

    /** Creates an options menu */
    @Override public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    /** Opens the options menu */
    @Override public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
         case R.id.preferences:
             Intent settingsActivity = new Intent(getBaseContext(),
                                                  Preferences.class);
             startActivityForResult(settingsActivity, REQUEST_PREFS);
             return true;
         case R.id.about:
             AlertDialog builder;
             try {
                 builder = AboutDialogBuilder.create(this);
                 builder.show();
                 return true;
             } catch (PackageManager.NameNotFoundException e) {
                 // TODO Auto-generated catch block
                 return false;
             }
         default:
             return super.onOptionsItemSelected(item);
        }
    }

    private static class AboutDialogBuilder {
        public static AlertDialog create(Context context) throws PackageManager.NameNotFoundException {
            PackageInfo pInfo = context.getPackageManager().
                getPackageInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            Drawable pIcon = context.getPackageManager().
                getApplicationIcon(context.getPackageName());
            String aboutTitle = String.format("%s %s", context.getString(R.string.app_name), pInfo.versionName);
            String aboutText = context.getString(R.string.about);

            final TextView message = new TextView(context);
            final SpannableString s = new SpannableString(aboutText);

            message.setPadding(5, 5, 5, 5);
            message.setMovementMethod(new ScrollingMovementMethod());
            message.setText(s);
            Linkify.addLinks(message, Linkify.ALL);

            return new AlertDialog.Builder(context).setTitle(aboutTitle).
                setIcon(pIcon).
                setCancelable(true).
                setPositiveButton(context.getString(android.R.string.ok), null).
                setView(message).create();
        }
    }

    private boolean externalStorageIsWritable() {
        /* Check sd card state */
        String state = Environment.getExternalStorageState();

        boolean extStorAvailable = false;
        boolean extStorWriteable = false;

        if (Environment.MEDIA_MOUNTED.equals(state)) {
            // We can read and write the media
            extStorAvailable = extStorWriteable = true;
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            // We can only read the media
            extStorAvailable = true;
            extStorWriteable = false;
        } else {
            // Something else is wrong. It may be one of many other states, but all we need
            //  to know is we can neither read nor write
            extStorAvailable = extStorWriteable = false;
        }

        return extStorAvailable && extStorWriteable;
    }

    private void launchFileBrowser(int mode) {
        SharedPreferences prefs = getBaseContext().getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
        boolean useBuiltin = prefs.getBoolean("cb_builtin", false);
        if (!useBuiltin) {
            // Note the different intent: PICK_DIRECTORY
            String oiIntent = "org.openintents.action.PICK_FILE";
            if (mode == DIRPICK_MODE) {
                oiIntent = "org.openintents.action.PICK_DIRECTORY";
            }
            Intent intent = new Intent(oiIntent);

            // Construct URI from file name.
            File file = new File(currentDialogStartPath);
            intent.setData(Uri.fromFile(file));

            intent.putExtra("org.openintents.extra.TITLE", currentDialogLabel);
            intent.putExtra("org.openintents.extra.BUTTON_TEXT", currentDialogButtonLabel);

            try {
                startActivityForResult(intent, REQUEST_CODE_PICK_FILE_OR_DIRECTORY);
            } catch (ActivityNotFoundException e) {
                showDialog(DIALOG_OI_UNAVAILABLE);
            }
        } else {
            launchBuiltinFileBrowser();
        }

    }
    
    private void launchBuiltinFileBrowser() {
        Intent intent = new Intent(getBaseContext(), FileDialog.class);
        intent.putExtra(FileDialog.CURRENT_ROOT, currentDialogRoot);
        intent.putExtra(FileDialog.CURRENT_DBROOT, currentDialogDBEncFS);
        intent.putExtra(FileDialog.CURRENT_ROOT_NAME, currentDialogRootName);
        intent.putExtra(FileDialog.BUTTON_LABEL, currentDialogButtonLabel);
        intent.putExtra(FileDialog.START_PATH, currentDialogStartPath);
        intent.putExtra(FileDialog.LABEL, currentDialogLabel);
        intent.putExtra(FileDialog.SELECTION_MODE, currentDialogMode);
        startActivityForResult(intent, currentDialogMode);
    }
    
    
    private void logOut() {
        // Remove credentials from the session
        ((CryptoniteApp) getApplication()).getDBApi().getSession().unlink();
        // Clear our stored keys
        clearKeys();
        
        ((CryptoniteApp) getApplication()).clearDBHashMap();
        
        // Change UI state to display logged out version
        setLoggedIn(false);
    }

    /**
     * Convenience function to change UI state based on being logged in
     */
    private void setLoggedIn(boolean loggedIn) {
        mLoggedIn = loggedIn;
        if (loggedIn) {
            buttonDropbox.setText(R.string.dropbox_unlink);
            buttonDropboxDecrypt.setEnabled(true);
        } else {
            buttonDropbox.setText(R.string.dropbox_link);
            buttonDropboxDecrypt.setEnabled(false);
            if (mDropboxDecrypted) {
                jniResetVolume();
                updateDecryptButtons();
            }
        }
    }

    private void clearKeys() {
        SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
        Editor edit = prefs.edit();
        edit.clear();
        edit.commit();
    }
    
    /**
     * Shows keeping the access keys returned from Trusted Authenticator in a local
     * store, rather than storing user name & password, and re-authenticating each
     * time (which is not to be done, ever).
     *
     * @return Array of [access_key, access_secret], or null if none stored
     */
    private String[] getKeys() {
        SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
        String key = prefs.getString(ACCESS_KEY_NAME, null);
        String secret = prefs.getString(ACCESS_SECRET_NAME, null);
        if (key != null && secret != null) {
            String[] ret = new String[2];
            ret[0] = key;
            ret[1] = secret;
            return ret;
        } else {
            return null;
        }
    }

    /**
     * Shows keeping the access keys returned from Trusted Authenticator in a local
     * store, rather than storing user name & password, and re-authenticating each
     * time (which is not to be done, ever).
     */
    private void storeKeys(String key, String secret) {
        // Save the access key for later
        SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
        Editor edit = prefs.edit();
        edit.putString(ACCESS_KEY_NAME, key);
        edit.putString(ACCESS_SECRET_NAME, secret);
        edit.commit();
    }

    private AndroidAuthSession buildSession() {
        AppKeyPair appKeyPair = new AppKeyPair(jniAppKey(), jniAppPw());
        AndroidAuthSession session;

        String[] stored = getKeys();
        if (stored != null) {
            AccessTokenPair accessToken = new AccessTokenPair(stored[0], stored[1]);
            session = new AndroidAuthSession(appKeyPair, ACCESS_TYPE, accessToken);
        } else {
            session = new AndroidAuthSession(appKeyPair, ACCESS_TYPE);
        }

        return session;
    }

    /** Create an empty file in a local destination directory
     * preserving relative file paths from Dropbox
     * @param dbEntry Dropbox entry
     * @param destRoot Destination target root path
     * @throws IOException
     */
    public static void dbTouch(Entry dbEntry, String destRoot) throws IOException {
        File file = new File(destRoot + dbEntry.path);
        if (dbEntry.isDir) {
            file.mkdirs();
        } else {
            file.createNewFile();
        }
    }
    
    /** Create an empty file with a decoded file name
     * 
     * @param encodedPath The full encoded source path
     * @param destRoot Root destination directory path
     * @param isDir true for directories
     * @throws IOException
     */
    public static void decode(String encodedPath, String destRoot, boolean isDir) throws IOException {

        Log.i(TAG, "encodedPath is " + encodedPath);
        
        /* Decoded name */
        String decodedPath = jniDecode(encodedPath);
        
        Log.i(TAG, "Creating new file" + destRoot + "/" + decodedPath);
        VirtualFile file = new VirtualFile(destRoot + "/" + decodedPath);
        if (isDir) {
            file.mkdirs();
        } else {
            file.createNewFile();
        }
        
    }
    
    /** Download an encrypted file from Dropbox and save a decoded
     * version to a local directory.
     * 
     * @param srcPath Full encoded source path
     * @param targetDir Target root directory
     * @param encFSDBRoot encFS path from Dropbox root
     * @param encFSLocalRoot encFS path from local root
     * @return true upon success
     * @throws IOException
     * @throws DropboxException
     */
    private boolean dbDownloadDecode(String srcPath, String targetDir, String encFSDBRoot, 
            String encFSLocalRoot)
            throws IOException, DropboxException
    {
        return dbDownloadDecode(srcPath, targetDir, encFSDBRoot, encFSLocalRoot, false);
    }
    
    /** Download an encrypted file from Dropbox and save a decoded
     * version to a local directory.
     * 
     * @param srcPath Full encoded source path
     * @param targetDir Target root directory
     * @param encFSDBRoot encFS path from Dropbox root
     * @param encFSLocalRoot encFS path from local root
     * @param forceReadable enforce readable file creation mode
     * @return true upon success
     * @throws IOException
     * @throws DropboxException
     */
    private boolean dbDownloadDecode(String srcPath, String targetDir, String encFSDBRoot, 
                                     String encFSLocalRoot, boolean forceReadable)
            throws IOException, DropboxException
    {
        String cachePath = encFSLocalRoot + srcPath;
        (new File(cachePath)).getParentFile().mkdirs();

        /* Download encoded file to cache dir */
        FileOutputStream fos = new FileOutputStream(cachePath);
        ((CryptoniteApp) getApplication()).getDBApi()
            .getFile(encFSDBRoot + srcPath, null, fos, null);
        fos.close();
    
        return (jniCopy(encFSLocalRoot + srcPath, targetDir, forceReadable) == jniSuccess());
    }

    /** Walks a Dropbox file tree, copying decrypted files to a local
     * directory (recursive part).
     * Recursion is only used within encrypted subfolders!
     * See https://www.dropbox.com/developers/reference/bestpractice
     * 
     * @param currentPath Decoded file path
     * @param exportRoot Root path up to encFS volume
     * @param destDir Destination directory
     * @param dbEncFSPath encFS path from Dropbox root
     * @throws IOException
     * @throws DropboxException
     */
    private void dbRecTree(String currentPath, String exportRoot, String destDir,
            String dbEncFSPath) throws IOException, DropboxException 
    {
        /* normalise path names */
        String bRoot = new File(exportRoot).getPath();
        String bPath = new File(currentPath).getPath();
        String stripstr = bPath.substring(bRoot.length());
        if (!stripstr.startsWith("/")) {
            stripstr = "/" + stripstr;
        }
        
        /* Convert current path to encoded file name */
        String encodedPath = jniEncode(stripstr);
        String destPath = destDir + stripstr;
        
        /* Remove local root directory to get Dropbox path */
        String encFSLocalRoot = Cryptonite.jniEncode("/");
        String dbLocalRoot = encFSLocalRoot.substring(0, 
                encFSLocalRoot.length()-dbEncFSPath.length());
        String encFSDBRoot = encFSLocalRoot.substring(dbLocalRoot.length());
        String dbPath = "/" + encodedPath.substring(dbLocalRoot.length()) ;
                
        /* Find file in Dropbox */
        Entry dbEntry = ((CryptoniteApp) getApplication()).getDBEntry(dbPath);
        
        if (dbEntry.isDir) {
            /* Create the decoded directory */
            (new File(destPath)).mkdirs();
            
            /* fullList.add(stripstr); */
            if (dbEntry.contents != null) {
                if (dbEntry.contents.size()>0) {
                    for (Entry dbChild : dbEntry.contents) {
                        if (dbChild.isDir) {
                            String decodedChildPath = dbLocalRoot +
                                    jniDecode(dbChild.path.substring(dbEncFSPath.length()));
                            dbRecTree(decodedChildPath, exportRoot, destDir, dbEncFSPath);
                        } else {
                            /* Download all children non-dirs right here
                             * so that we don't have to retrieve the metadata every time
                             * 
                             * Download and decode file
                             */
                            if (!dbDownloadDecode(dbChild.path.substring(dbEncFSPath.length()), destDir, encFSDBRoot,
                                    encFSLocalRoot))
                            {
                                Toast.makeText(getBaseContext(), 
                                        getString(R.string.file_not_found) + dbChild.path,
                                        Toast.LENGTH_LONG);
                            }
                        }
                    }
                }
            }

        } else {
            
            if (!dbDownloadDecode(dbEntry.path.substring(dbEncFSPath.length()), destDir, encFSDBRoot,
                    encFSLocalRoot))
            {
                Toast.makeText(getBaseContext(), 
                        getString(R.string.file_not_found) + dbEncFSPath,
                        Toast.LENGTH_LONG);
            }
        }
    }
    
    /** Walks a Dropbox file tree, copying decrypted files to a local
     * directory.
     * Recursion is only used within encrypted subfolders!
     * See https://www.dropbox.com/developers/reference/bestpractice
     * 
     * @param exportPaths Decoded file path array
     * @param exportRoot Root path up to encFS volume
     * @param destDir Destination directory
     * @param dbEncFSPath encFS path from Dropbox root
     * @return true upon success
     * @throws IOException
     * @throws DropboxException
     */
    private boolean dbExport(String[] exportPaths, String exportRoot, 
            String destDir, String dbEncFSPath) 
    {
        try {
            for (String path : exportPaths) {
                dbRecTree(path, exportRoot, destDir, dbEncFSPath);
            }
        } catch (IOException e) {
            Toast.makeText(getBaseContext(), getString(R.string.export_failed) + e.toString(),
                    Toast.LENGTH_LONG);
            return false;
        } catch (DropboxException e) {
            Toast.makeText(getBaseContext(), getString(R.string.export_failed) + e.toString(),
                    Toast.LENGTH_LONG);
            return false;
        }
        return true;
    }

    /** Walks a local file tree, copying decrypted files to a local
     * directory.
     * 
     * @param currentPath Decoded file path
     * @param errorList File paths that couldn't be copied
     * @param exportRoot Root path up to encFS volume
     * @param destDir Destination directory
     * @param localEncFSPath encFS path from local root
     */
    private void localRecTree(String currentPath, Set<String> errorList, String exportRoot,
            String destDir, String localEncFSPath)
    {
        if (new VirtualFile(currentPath).exists()) {

            /* normalise path names */
            String bRoot = new VirtualFile(exportRoot).getPath();
            String bPath = new VirtualFile(currentPath).getPath();
            String stripstr = bPath.substring(bRoot.length());

            if (new VirtualFile(bPath).isDirectory()) {

                for (VirtualFile f : new VirtualFile(bPath).listFiles()) {
                    localRecTree(f.getPath(), errorList, exportRoot, destDir, localEncFSPath);
                }

            } else {
                /* Set up dir for decrypted file */
                String finalPath = destDir + (new File(bPath)).getParent().substring(bRoot.length());
                File finalDir = new File(finalPath);
                if (!finalDir.exists()) {
                    finalDir.mkdirs();
                }
                /* Convert current path to encoded file name */
                String encodedPath = jniEncode(stripstr);

                if (jniCopy(encodedPath, destDir, false) != jniSuccess()) {
                    errorList.add(stripstr);
                    Log.e(TAG, "Couldn't copy " + encodedPath + " to " + destDir);
                }
            }
        }
    }
    
    /** Walks a local file tree, copying decrypted files to a local
     * directory.
     * 
     * @param exportPaths Decoded file path array
     * @param exportRoot Root path up to encFS volume
     * @param destDir Destination directory
     * @param localEncFSPath encFS path from local root
     * @return true upon success
     */
    private boolean localExport(String[] exportPaths, String exportRoot, 
            String destDir, String localEncFSPath) 
    {
        Set<String> errorList = new HashSet<String>();

        for (String path : exportPaths) {
            localRecTree(path, errorList, exportRoot, destDir, localEncFSPath);
        }

        return errorList.size() == 0;
    }
    
    private static String fileExt(String url) {
        String ext = url.substring(url.lastIndexOf(".") );
        if (ext.indexOf("?")>-1) {
            ext = ext.substring(0,ext.indexOf("?"));
        }
        if (ext.indexOf("%")>-1) {
            ext = ext.substring(0,ext.indexOf("%"));
        }
        return ext;
    }
    
    private boolean openEncFSFile(String encFSFilePath, String fileRoot, String dbEncFSPath, boolean isDB) {

        /* normalise path names */
        String bRoot = new File(fileRoot).getPath();
        String bPath = new File(encFSFilePath).getPath();
        String stripstr = bPath.substring(bRoot.length());
        if (!stripstr.startsWith("/")) {
            stripstr = "/" + stripstr;
        }
        
        /* Convert current path to encoded file name */
        String encodedPath = jniEncode(stripstr);

        /* Set up temp dir for decrypted file */
        File openDir = getPrivateDir(OPENPNT); /*, Context.MODE_WORLD_READABLE); */
        String destPath = openDir.getPath() + (new File(bPath)).getParent().substring(bRoot.length());

        (new File(destPath)).mkdirs();
       
        if (isDB) {
            /* Remove local root directory to get Dropbox path */
            String encFSLocalRoot = Cryptonite.jniEncode("/");
            String dbLocalRoot = encFSLocalRoot.substring(0, 
                    encFSLocalRoot.length()-dbEncFSPath.length());
            String encFSDBRoot = encFSLocalRoot.substring(dbLocalRoot.length());
            String dbPath = "/" + encodedPath.substring(dbLocalRoot.length()) ;
            
            try {
                if (!dbDownloadDecode(dbPath.substring(dbEncFSPath.length()),
                                      openDir.getPath(), encFSDBRoot, encFSLocalRoot, true))
                {
                    Log.e(TAG, "Error while attempting to copy " + encodedPath);
                    return false;
                }
            } catch (IOException e) {
                showAlert(R.string.error, 
                        getString(R.string.dropbox_read_fail) + " " + e.toString());
                Log.e(TAG, "Dropbox read fail: " + e.toString());
                return false;
            } catch (DropboxException e) {
                showAlert(R.string.error, 
                        getString(R.string.dropbox_read_fail) + " " + e.toString());
                Log.e(TAG, "Dropbox read fail: " + e.toString());
                return false;
            }
        } else {
            /* Copy decrypted file */
            if (jniCopy(encodedPath, openDir.getPath(), true) != jniSuccess()) {
                showAlert(R.string.error, R.string.local_read_fail);
                Log.e(TAG, "Error while attempting to copy " + encodedPath);
                return false;
            }
        }

        /* Copy the resulting file to a readable folder */
        String openFilePath = openDir.getPath() + stripstr;
        String readableName = (new File(encFSFilePath)).getName();
        File readableDir = getPrivateDir(READPNT, Context.MODE_WORLD_READABLE);
        String readablePath = readableDir.getPath() + "/" + readableName;
        try {
            FileOutputStream fos = new FileOutputStream(new File(readablePath));

            FileInputStream fis = new FileInputStream(new File(openFilePath));

            byte[] buffer = new byte[fis.available()]; 

            fis.read(buffer);

            fos.write(buffer);

            fis.close();
            fos.close();

            /* Make world readable */
            ShellUtils.chmod(readablePath, "644");
            
            /* Delete tmp directory */
            getPrivateDir(OPENPNT);
        } catch (IOException e) {
            Log.e(TAG, "Error while attempting to open " + readableName
                    + ": " + e.toString());
            return false;
        }
        
        return fileOpen(readablePath);

    }
    
    private boolean fileOpen(String filePath) {
        /* Guess MIME type */
        Uri data = Uri.fromFile(new File(filePath));

        MimeTypeMap myMime = MimeTypeMap.getSingleton();
        String contentType = myMime.getMimeTypeFromExtension(fileExt(filePath).substring(1));
               
        /* Attempt to guess file type from content; seemingly very unreliable */
        if (contentType == null) {
            try {
                FileInputStream fis = new FileInputStream(filePath);
                contentType = URLConnection.guessContentTypeFromStream(fis);
            } catch (IOException e) {
                Log.e(TAG, "Error while attempting to guess MIME type of " + filePath
                        + ": " + e.toString());
                contentType = null;
            }
        }

        if (contentType == null) {
            Log.e(TAG, "Couldn't find content type; resorting to text/plain");
            contentType = "text/plain";
        }
        
        Intent intent = new Intent();
        
        intent.setAction(android.content.Intent.ACTION_VIEW);
        intent.setDataAndType(data, contentType);

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            showAlert(getString(R.string.activity_not_found_title), 
                    getString(R.string.activity_not_found_msg));
            Log.e(TAG, "Couldn't find activity: " + e.toString());
            return false;
        }
        
        return true;
    }

    private boolean uploadEncFSFile(String encFSFilePath, String fileRoot, String dbEncFSPath, boolean isDB) {
        
        /* normalise path names */
        String bRoot = new File(fileRoot).getPath();
        String bPath = new File(encFSFilePath).getPath();
        String stripstr = bPath.substring(bRoot.length());
        if (!stripstr.startsWith("/")) {
            stripstr = "/" + stripstr;
        }
        
        /* Convert current path to encoded target directory
         * String encodedPath = jniEncode(stripstr); 
         * TODO: Implement this
         */
        
        return true;



    }
        
    /* Native methods are implemented by the
     * 'cryptonite' native library, which is packaged
     * with this application.
     */
    public native int     jniFailure();
    public native int     jniSuccess();
    public native int     jniIsValidEncFS(String srcDir);
    public native int     jniVolumeLoaded();
    public native int     jniResetVolume();
    public native int     jniBrowse(String srcDir, String destDir, String password);
    public native int     jniInit(String srcDir, String password);
    public native int     jniExport(String[] exportPaths, String exportRoot, String destDir);
    public native int     jniCopy(String encodedName, String destDir, boolean forceReadable);
    public static native String  jniDecode(String name);
    public static native String  jniEncode(String name);
    public native String  jniVersion();
    public native String  jniAppKey();
    public native String  jniAppPw();
    
    /* this is used to load the 'cryptonite' library on application
     * startup. The library has already been unpacked into
     * /data/data/csh.cryptonite/lib/libcryptonite.so at
     * installation time by the package manager.
     */
    static {
        System.loadLibrary("cryptonite");
    }

}
