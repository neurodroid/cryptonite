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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLConnection;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.ActivityManager.RunningServiceInfo;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
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
import android.provider.Settings;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;

import android.text.SpannableString;
import android.text.util.Linkify;
import android.text.method.ScrollingMovementMethod;

import android.util.Base64;
import android.util.Log;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.Session.AccessType;
import com.dropbox.client2.session.TokenPair;

import csh.cryptonite.storage.Storage;
import csh.cryptonite.storage.StorageManager;
import csh.cryptonite.storage.VirtualFile;
import csh.cryptonite.storage.VirtualFileSystem;

public class Cryptonite extends SherlockFragmentActivity
{

    private static final int REQUEST_PREFS=0, REQUEST_CODE_PICK_FILE_OR_DIRECTORY=1;
    public static final int MOUNT_MODE=0, SELECTLOCALENCFS_MODE=1, SELECTDBENCFS_MODE=2,
        VIEWMOUNT_MODE=3, SELECTLOCALEXPORT_MODE=4, LOCALEXPORT_MODE=5, DROPBOX_AUTH_MODE=6,
        SELECTDBEXPORT_MODE=7, DBEXPORT_MODE=8, SELECTLOCALUPLOAD_MODE=9, SELECTDBUPLOAD_MODE=10;
    private static final int DIRPICK_MODE=0;
    public static final int FILEPICK_MODE=1;
    protected static final int MSG_SHOW_TOAST = 0;
    public static final int MY_PASSWORD_CONFIRM_DIALOG_ID = 4;
    private static final int DIALOG_JNI_FAIL=3;
    private static final int MAX_JNI_SIZE = 512;
    public static final int TERM_UNAVAILABLE=0, TERM_OUTDATED=1, TERM_AVAILABLE=2;
    public static final String MNTPNT = "/csh.cryptonite/mnt";
    public static final String TAG = "cryptonite";
    public static final String DBTAB_TAG = "tab_db", LOCALTAB_TAG="tab_local", EXPERTTAB_TAG="tab_expert";

    private String encfsVersion;
    private String opensslVersion;
    public String mountInfo;
    public String textOut;
    
    private static boolean hasJni = false;
    
    public String currentDialogStartPath = "/";
    public String currentDialogLabel = "";
    public String currentDialogButtonLabel = "OK";
    public String currentDialogRoot = "/";
    public String currentDialogRootName = currentDialogRoot;
    private String currentReturnPath = "/";
    private String currentOpenPath = "/";
    private String currentPreviewPath = "/";
    private String currentUploadPath = "/";
    private String currentPassword = "\0";
    private String encfsBrowseRoot = "/";
    private String[] currentReturnPathList = {};
    public int currentDialogMode = SelectionMode.MODE_OPEN;

    public String mntDir = "/sdcard" + MNTPNT;
    public int opMode = -1;
    public int prevMode = -1;
    private boolean alert = false;
    private String alertMsg = "";
 
    public boolean mLoggedIn = false;
    public boolean hasFuse = false;
    public boolean triedLogin = false;
    private boolean mInstrumentation = false;
    private boolean mUseAppFolder;
    private boolean showPassword = false;
    
    private TabHost mTabHost;
    private ViewPager  mViewPager;
    private TabsAdapter mTabsAdapter;
    
    private DropboxFragment dbFragment;
    private LocalFragment localFragment;
    
    public CryptoniteApp mApp;
    
    // If you'd like to change the access type to the full Dropbox instead of
    // an app folder, change this value.
    final static private AccessType ACCESS_TYPE = AccessType.DROPBOX;

    final static public String ACCOUNT_PREFS_NAME = "csh.cryptonite_preferences";
    final static public String ACCOUNT_DB_PREFS_NAME = "csh.cryptonite_db_preferences";
    final static private String ACCESS_KEY_NAME = "ACCESS_KEY";
    final static private String ACCESS_SECRET_NAME = "ACCESS_SECRET";
    
    /** Called when the activity is first created. */
    @Override public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.main);

        mApp = (CryptoniteApp) getApplication();
        getResources();

        if (!hasJni) {
            jniFail();
            return;
        }
        
        encfsVersion = "EncFS " + jniEncFSVersion();
        opensslVersion = jniOpenSSLVersion();
        textOut = encfsVersion + "\n" + opensslVersion;
        Log.v(TAG, encfsVersion + " " + opensslVersion);

        SharedPreferences prefs = getBaseContext().getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
        mApp.setupReadDirs(prefs.getBoolean("cb_extcache", false));
        StorageManager.INSTANCE.initLocalStorage(this, mApp);

        if (!externalStorageIsWritable() || !ShellUtils.supportsFuse()) {
            mountInfo = getString(R.string.mount_info_unsupported);
        } else {
            mountInfo = getString(R.string.mount_info);
            mntDir = prefs.getString("txt_mntpoint", defaultMntDir());
            File mntDirF = new File(mntDir);
            if (!mntDirF.exists()) {
                mntDirF.mkdirs();
            }
        }

        hasFuse = ShellUtils.supportsFuse();

        /* Running from Instrumentation? */
        if (getIntent() != null) {
            mInstrumentation = getIntent().getBooleanExtra("csh.cryptonite.instrumentation", false);
        } else {
            mInstrumentation = false;
        }
        
        if (mApp.needsEncFSBinary()) {
            final ProgressDialog pd = ProgressDialog.show(this,
                    this.getString(R.string.wait_msg),
                    this.getString(R.string.copying_bins), true);
            new Thread(new Runnable(){
                public void run(){
                    mApp.cpBin("encfs");
                    mApp.cpBin("truecrypt");
                    runOnUiThread(new Runnable(){
                        public void run() {
                            if (pd.isShowing())
                                pd.dismiss();
                            mApp.setEncFSBinaryVersion();
                        }
                    });
                }
            }).start();
        }
        
        if (!mInstrumentation) {
            if (!prefs.getBoolean("cb_norris", false) && !mApp.getDisclaimerShown()) {
                AlertDialog.Builder builder = new AlertDialog.Builder(Cryptonite.this);
                builder.setIcon(R.drawable.ic_launcher_cryptonite)
                    .setTitle(R.string.disclaimer)
                    .setMessage(R.string.no_warranty)
                    .setPositiveButton(R.string.understand,
                                       new DialogInterface.OnClickListener() {
                                           public void onClick(DialogInterface dialog,
                                                               int which) {
                                               mApp.setDisclaimerShown(true);
                                           }
                                       });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        }

        showPassword = false;
        
        mTabHost = (TabHost)findViewById(android.R.id.tabhost);
        mTabHost.setup();

        mViewPager = (ViewPager)findViewById(R.id.pager);
        mTabsAdapter = new TabsAdapter(this, mTabHost, mViewPager);
        
        mTabsAdapter.addTab(mTabHost.newTabSpec(DBTAB_TAG)
                .setIndicator(getString(R.string.dropbox_tabtitle)),
                DropboxFragment.class, null);
        mTabsAdapter.addTab(mTabHost.newTabSpec(LOCALTAB_TAG)
                .setIndicator(getString(R.string.local_tabtitle)),
                LocalFragment.class, null);
        mTabsAdapter.addTab(mTabHost.newTabSpec(EXPERTTAB_TAG)
                .setIndicator(getString(R.string.expert_tabtitle)),
                ExpertFragment.class, null);

        if (savedInstanceState != null) {
            mTabHost.setCurrentTabByTag(savedInstanceState.getString("tab"));
        }

    }

    public static boolean isValidMntDir(Context context, File newMntDir) {
        return isValidMntDir(context, newMntDir, true);
    }
    
    public static boolean isValidMntDir(Context context, File newMntDir, boolean showToast) {
        if (!newMntDir.exists()) {
            if (showToast) {
                Toast.makeText(context, R.string.txt_mntpoint_nexists, Toast.LENGTH_LONG).show();
            }
            return false;
        }
        if (!newMntDir.isDirectory()) {
            if (showToast) {
                Toast.makeText(context, R.string.txt_mntpoint_nisdir, Toast.LENGTH_LONG).show();
            }
            return false;
        }
        if (!newMntDir.canWrite()) {
            if (showToast) {
                Toast.makeText(context, R.string.txt_mntpoint_ncanwrite, Toast.LENGTH_LONG).show();
            }
            return false;
        }
        if (newMntDir.list().length != 0) {
            if (showToast) {
                Toast.makeText(context, R.string.txt_mntpoint_nempty, Toast.LENGTH_LONG).show();
            }
            return false;
        }
        return true;
    }

    public static String defaultMntDir() {
        return Environment.getExternalStorageDirectory().getPath() + MNTPNT;
    }

    /** Called upon exit from other activities */
    public synchronized void onActivityResult(final int requestCode,
            int resultCode, final Intent data) 
    {

        switch (requestCode) {
        case SelectionMode.MODE_OPEN:
        case SelectionMode.MODE_OPEN_DB:
        case SelectionMode.MODE_OPEN_UPLOAD_SOURCE:
        case SelectionMode.MODE_OPEN_CREATE:
            /* file dialog */
            if (resultCode == Activity.RESULT_OK && data != null) {
                currentReturnPath = data.getStringExtra(FileDialog.RESULT_EXPORT_PATHS);
                if (currentReturnPath != null ) {
                    switch (opMode) {
                    case MOUNT_MODE:
                    case SELECTLOCALENCFS_MODE:
                    case SELECTDBENCFS_MODE:
                        showPassword = true; // will be shown in onResume()
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
                                        alert = !StorageManager.INSTANCE.getEncFSStorage().exportEncFSFiles(currentReturnPathList, encfsBrowseRoot, 
                                                    currentReturnPath + "/Cryptonite");
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
                    case SELECTDBUPLOAD_MODE:
                    case SELECTLOCALUPLOAD_MODE:
                        final String srcPath = data.getStringExtra(FileDialog.RESULT_SELECTED_FILE);
                        /* Does the file exist? */
                        uploadEncFSFile(srcPath);
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
                currentReturnPathList = data.getStringArrayExtra(FileDialog.RESULT_EXPORT_PATHS);
                if (currentReturnPathList != null && currentReturnPathList.length > 0) {

                    if (currentReturnPathList.length > MAX_JNI_SIZE) {
                        showAlert(R.string.error, R.string.jni_arg_too_large);
                        break;
                    }

                    /* Select destination directory for exported files */
                    currentDialogLabel = Cryptonite.this.getString(R.string.select_exp);
                    currentDialogButtonLabel = Cryptonite.this.getString(R.string.select_exp_short);
                    currentDialogMode = SelectionMode.MODE_OPEN_CREATE;
                    if (externalStorageIsWritable()) {
                        currentDialogStartPath = 
                                getDownloadDir().getPath();
                        File downloadDir = new File(currentDialogStartPath);
                        if (!downloadDir.exists()) {
                            downloadDir.mkdir();
                        }
                        if (!downloadDir.exists()) {
                            currentDialogStartPath = "/";
                        }
                    } else {
                        currentDialogStartPath = "/";
                    }
                    currentDialogRoot = "/";
                    currentDialogRootName = currentDialogRoot;
                    /* mApp.setCurrentDBEncFS(""); Leave this untouched for dbExport */
                    if (StorageManager.INSTANCE.getEncFSStorage() != null) {
                        opMode = StorageManager.INSTANCE.getEncFSStorage().exportMode;
                    } else {
                        return;
                    }
                    launchBuiltinFileBrowser();
                } else {
                    currentOpenPath = data.getStringExtra(FileDialog.RESULT_OPEN_PATH);
                    if (currentOpenPath != null && currentOpenPath.length() > 0) {
                        openEncFSFile(currentOpenPath, encfsBrowseRoot);
                    } else {
                        currentPreviewPath = data.getStringExtra(FileDialog.RESULT_PREVIEW_PATH);
                        if (currentPreviewPath != null && currentPreviewPath.length() > 0) {
                            previewEncFSFile(currentPreviewPath, encfsBrowseRoot);
                        } else {
                            currentUploadPath = data.getStringExtra(FileDialog.RESULT_UPLOAD_PATH);
                            if (currentUploadPath != null && currentUploadPath.length() > 0) {
                                /* select file to upload */
                                currentDialogLabel = Cryptonite.this.getString(R.string.select_upload);
                                currentDialogButtonLabel = Cryptonite.this.getString(R.string.select_upload_short);
                                currentDialogMode = SelectionMode.MODE_OPEN_UPLOAD_SOURCE;
                                if (externalStorageIsWritable()) {
                                    currentDialogStartPath = Environment
                                            .getExternalStorageDirectory()
                                            .getPath();
                                } else {
                                    currentDialogStartPath = "/";
                                }
                                currentDialogRoot = "/";
                                currentDialogRootName = currentDialogRoot;
                                if (StorageManager.INSTANCE.getEncFSStorage() != null) {
                                    opMode = StorageManager.INSTANCE.getEncFSStorage().uploadMode;
                                } else {
                                    return;
                                }
                                launchBuiltinFileBrowser();
                            }
                        }
                    }
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {

            }
            break;
        case REQUEST_PREFS:
            SharedPreferences prefs = getBaseContext().getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
            Editor prefEdit = prefs.edit();
            
            mApp.setupReadDirs(prefs.getBoolean("cb_extcache", false));
            
            mntDir = prefs.getString("txt_mntpoint", defaultMntDir());

            /* If app folder settings have changed, we'll have to log out the user
             * from his Dropbox and restart the authentication from scratch during
             * the next login:
             */
            if (prefs.getBoolean("cb_appfolder", false) != mUseAppFolder) {
                prefEdit.putBoolean("dbDecided", true);
                prefEdit.commit();
                /* enforce re-authentication */
                DBInterface.INSTANCE.setDBApi(null);
                if (mLoggedIn) {
                    Toast.makeText(Cryptonite.this,
                            R.string.dropbox_forced_logout,
                            Toast.LENGTH_LONG).show();
                    logOut();
                }
            }
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
        case CreateEncFS.CREATE_DB:
        case CreateEncFS.CREATE_LOCAL:
            break;
        case TextPreview.REQUEST_PREVIEW:
            break;
        default:
            Log.e(TAG, "Unknown request code");
        }
    }

    private void uploadEncFSFile(final String srcPath) {
        final String stripstr = StorageManager.INSTANCE.getEncFSStorage().stripStr(currentUploadPath, encfsBrowseRoot, srcPath);
        
        /* Run in separate thread in case this involves a network operation */
        new Thread(new Runnable(){
            public void run(){
                final String nextFilePath = StorageManager.INSTANCE.getEncFSStorage().encodedExists(stripstr);
                runOnUiThread(new Runnable(){
                    public void run() {
                        if (!nextFilePath.equals(stripstr)) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(Cryptonite.this);
                            builder.setIcon(R.drawable.ic_launcher_cryptonite)
                            .setTitle(R.string.file_exists)
                            .setMessage(R.string.file_exists_options)
                            .setPositiveButton(R.string.overwrite,
                                    new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                        int which) {
                                    uploadEncFSFileExec(stripstr, srcPath);
                                }
                            })
                            .setNeutralButton(R.string.rename, 
                                    new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                        int which) {
                                    uploadEncFSFileExec(nextFilePath, srcPath);
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
                        }else {
                            uploadEncFSFileExec(stripstr, srcPath);
                        }
                    }
                });
            }
        }).start();
    }

    private void uploadEncFSFileExec(final String targetPath, final String srcPath) {
        final ProgressDialog pd = ProgressDialog.show(this,
                this.getString(R.string.wait_msg),
                this.getString(R.string.encrypting), true);
        alertMsg = "";
        new Thread(new Runnable(){
            public void run(){
                if (!StorageManager.INSTANCE.getEncFSStorage().uploadEncFSFile(targetPath, srcPath)) {
                    alertMsg = getString(R.string.upload_failure);
                }        
                runOnUiThread(new Runnable(){
                    public void run() {
                        if (pd.isShowing())
                            pd.dismiss();
                        updateDecryptButtons();
                        if (!alertMsg.equals("")) {
                            showAlert(R.string.error, alertMsg);
                        }
                    }
                });
            }
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!hasJni) {
            return;
        }
        
        if (showPassword) {
            showPassword = false;
            showPassword();
        }
        
        if (DBInterface.INSTANCE.getDBApi() != null && 
                DBInterface.INSTANCE.getDBApi().getSession() != null) {
            AndroidAuthSession session = DBInterface.INSTANCE.getDBApi().getSession();
    
            // The next part must be inserted in the onResume() method of the
            // activity from which session.startAuthentication() was called, so
            // that Dropbox authentication completes properly.
            // Make sure we're returning from an authentication attempt at all.
            if (session.authenticationSuccessful()) {
                triedLogin = false;
                try {
                    // Mandatory call to complete the auth
                    session.finishAuthentication();
    
                    // Store it locally in our app for later use
                    TokenPair tokens = session.getAccessTokenPair();
                    storeKeys(tokens.key, tokens.secret);
                    
                    DBInterface.INSTANCE.clearDBHashMap();
                    
                    setLoggedIn(true);
                } catch (IllegalStateException e) {
                    Toast.makeText(Cryptonite.this, 
                            getString(R.string.dropbox_auth_fail) + ": " + e.getLocalizedMessage(), 
                            Toast.LENGTH_LONG).show();
                }
            } else {
                if (triedLogin) {
                    triedLogin = false;
                    logOut();
                }
            }
        } else {
            setLoggedIn(false);
        }
    }
    
    private void showPassword() {
        // DialogFragment.show() will take care of adding the fragment
        // in a transaction.  We also want to remove any currently showing
        // dialog, so make our own transaction and take care of that here.
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment prev = getSupportFragmentManager().findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
        SherlockDialogFragment newFragment = PasswordDialogFragment.newInstance();
        newFragment.show(ft, "dialog");
    }

    public void showAlert(int alert_id, int msg_id) {
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
        if (dir == null) {
            return false;
        }
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
    private void mountEncFS(final String srcDir) {
        textOut = encfsVersion + "\n" + opensslVersion;

        if (jniIsValidEncFS(srcDir) != jniSuccess()) {
            showAlert(R.string.error, R.string.invalid_encfs);
            Log.v(TAG, "Invalid EncFS");
            return;
        }
        
        if (!isValidMntDir(this, new File(mntDir), true)) {
            showAlert(R.string.error, R.string.mount_point_invalid);
            return;
        }
        final ProgressDialog pd = ProgressDialog.show(this,
                this.getString(R.string.wait_msg),
                this.getString(R.string.running_encfs), true);
        Log.v(TAG, "Running encfs with " + srcDir + " " + mntDir);
        alertMsg = "";
        new Thread(new Runnable(){
                public void run(){
                    String encfsoutput = "";
                    String[] cmdlist = {mApp.getEncFSBinPath(), "--public", "--stdinpass",
                            "\"" + srcDir + "\"", "\"" + mntDir + "\""};
                    try {
                        encfsoutput = ShellUtils.runBinary(cmdlist, mApp.getBinDirPath(), currentPassword, true);
                    } catch (IOException e) {
                        alertMsg = getString(R.string.mount_fail) + ": " + e.getMessage();
                    } catch (InterruptedException e) {
                        alertMsg = getString(R.string.mount_fail) + ": " + e.getMessage();
                    }
                    final String fEncfsOutput = encfsoutput;
                    runOnUiThread(new Runnable(){
                            public void run() {
                                if (pd.isShowing())
                                    pd.dismiss();
                                if (localFragment != null) {
                                    localFragment.updateMountButtons();
                                }
                                if (fEncfsOutput != null && fEncfsOutput.length() > 0) {
                                    textOut = encfsVersion + "\n" + fEncfsOutput;
                                }
                                if (!alertMsg.equals("")) {
                                    showAlert(R.string.error, alertMsg);
                                }
                                nullPassword();
                            }
                        });
                }
            }).start();
            
    }
    
    /** Initialize an EncFS volume. This will check
     * whether the EncFS volume is valid an initialize the EncFS
     * root information
     * 
     * @param srcDir Path to EncFS volume
     * @param pwd password
     */
   private void initEncFS(final String srcDir) {
       alertMsg = "";
       
       final ProgressDialog pd = ProgressDialog.show(this, 
               this.getString(R.string.wait_msg), 
               this.getString(R.string.running_encfs), true);
       new Thread(new Runnable(){
               public void run(){
                   if (!StorageManager.INSTANCE.getEncFSStorage().initEncFS(srcDir, currentDialogRoot)) {
                       alertMsg = getString(R.string.invalid_encfs);
                       StorageManager.INSTANCE.resetEncFSStorage();
                   } else {
                       mApp.setCurrentBrowsePath(currentReturnPath);
                       mApp.setCurrentBrowseStartPath(currentDialogStartPath);
                       StorageManager.INSTANCE.setEncFSPath(currentReturnPath.substring(currentDialogStartPath.length()));
                       Log.i(TAG, "Dialog root is " + currentReturnPath);
                       if (jniInit(srcDir, currentPassword) != jniSuccess()) {
                           Log.v(TAG, getString(R.string.browse_failed));
                           alertMsg = getString(R.string.browse_failed);
                           StorageManager.INSTANCE.resetEncFSStorage();
                       } else {
                           Log.v(TAG, "Decoding succeeded");
                       }
                   }
                   runOnUiThread(new Runnable(){
                           public void run() {
                               if (pd.isShowing())
                                   pd.dismiss();
                               nullPassword();
                               updateDecryptButtons();
                               if (alertMsg.length()!=0) {
                                   showAlert(R.string.error, alertMsg);
                               }
                           }
                       });
               }
           }).start();
   }

   public void updateDecryptButtons() {
       if (dbFragment != null) {
           dbFragment.updateDecryptButtons();
       }
       if (localFragment != null) {
           localFragment.updateDecryptButtons();
       }
   }

   /** Browse an EncFS volume using a virtual file system.
    * File names are queried on demand when a directory is opened.
    * @param browsePath EncFS path 
    * @param browseStartPath Root path
    */
   public void browseEncFS(final String browsePath, final String browseStartPath) {
       final VirtualFile browseDirF = new VirtualFile(VirtualFile.VIRTUAL_TAG + "/" + StorageManager.INSTANCE.getEncFSStorage().browsePnt);
       browseDirF.mkdirs();

       final ProgressDialog pd = ProgressDialog.show(this,
               this.getString(R.string.wait_msg),
               this.getString(R.string.running_encfs), true);
       new Thread(new Runnable(){
               public void run(){
                   Log.i(TAG, "Dialog root is " + browsePath);
                   currentDialogStartPath = browseDirF.getPath();
                   currentDialogLabel = getString(R.string.select_file_export);
                   currentDialogButtonLabel = getString(R.string.export);
                   currentDialogRoot = currentDialogStartPath;
                   encfsBrowseRoot = currentDialogRoot;
                   currentDialogRootName = getString(R.string.encfs_root);
                   currentDialogMode = StorageManager.INSTANCE.getEncFSStorage().fdSelectionMode;
                   runOnUiThread(new Runnable(){
                           public void run() {
                               if (pd.isShowing())
                                   pd.dismiss();
                               opMode = StorageManager.INSTANCE.getEncFSStorage().selectExportMode;
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

    /** Creates an options menu */
    @Override public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSherlock().getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /** Opens the options menu */
    @Override public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
         case R.id.preferences:
             SharedPreferences prefs = getBaseContext().getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
             /* Store current app folder choice */
             mUseAppFolder = prefs.getBoolean("cb_appfolder", false);
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
                 return false;
             }
        }
        return super.onOptionsItemSelected(item);
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

    public static boolean externalStorageIsWritable() {
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

    public void launchFileBrowser(int mode) {
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
                OIUnavailableDialogFragment newFragment = OIUnavailableDialogFragment.newInstance();
                newFragment.show(getSupportFragmentManager(), "dialog");
            }
        } else {
            launchBuiltinFileBrowser();
        }

    }
    
    public void launchBuiltinFileBrowser() {
        Intent intent = new Intent(getBaseContext(), FileDialog.class);
        intent.putExtra(FileDialog.CURRENT_ROOT, currentDialogRoot);
        intent.putExtra(FileDialog.CURRENT_ROOT_NAME, currentDialogRootName);
        intent.putExtra(FileDialog.BUTTON_LABEL, currentDialogButtonLabel);
        intent.putExtra(FileDialog.START_PATH, currentDialogStartPath);
        intent.putExtra(FileDialog.LABEL, currentDialogLabel);
        intent.putExtra(FileDialog.SELECTION_MODE, currentDialogMode);
        startActivityForResult(intent, currentDialogMode);
    }
    
    public void createEncFS(final boolean isDB) {
        SharedPreferences prefs = getBaseContext().getSharedPreferences(Cryptonite.ACCOUNT_PREFS_NAME, 0);
        if (!prefs.getBoolean("cb_norris", false)) {

            AlertDialog.Builder builder = new AlertDialog.Builder(Cryptonite.this);
            builder.setIcon(R.drawable.ic_launcher_cryptonite)
            .setTitle(R.string.warning)
            .setMessage(R.string.create_warning)
            .setPositiveButton(R.string.create_short,
                    new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog,
                        int which) {
                    Intent intent = new Intent(getBaseContext(), CreateEncFS.class);
                    int createMode = CreateEncFS.CREATE_LOCAL;
                    if (isDB) {
                        createMode = CreateEncFS.CREATE_DB;
                    }
                    intent.putExtra(CreateEncFS.START_MODE, createMode);
                    startActivityForResult(intent, createMode);
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
        } else {
            Intent intent = new Intent(getBaseContext(), CreateEncFS.class);
            int createMode = CreateEncFS.CREATE_LOCAL;
            if (isDB) {
                createMode = CreateEncFS.CREATE_DB;
            }
            intent.putExtra(CreateEncFS.START_MODE, createMode);
            startActivityForResult(intent, createMode);
        }
        
        
    }
    
    public void logOut() {
        // Remove credentials from the session
        if (DBInterface.INSTANCE.getDBApi() != null) {
            DBInterface.INSTANCE.getDBApi().getSession().unlink();
            // Clear our stored keys
            clearKeys();
        
            DBInterface.INSTANCE.clearDBHashMap();
        }
        
        // Change UI state to display logged out version
        setLoggedIn(false);
    }

    /**
     * Convenience function to change UI state based on being logged in
     */
    private void setLoggedIn(boolean loggedIn) {
        mLoggedIn = loggedIn;
        if (!loggedIn) {
            if (StorageManager.INSTANCE.getEncFSStorageType() == Storage.STOR_DROPBOX) {
                jniResetVolume();
                StorageManager.INSTANCE.resetEncFSStorage();
                VirtualFileSystem.INSTANCE.clear();
            }
        }
    }

    private void clearKeys() {
        SharedPreferences prefs = getSharedPreferences(ACCOUNT_DB_PREFS_NAME, 0);
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
        SharedPreferences prefs = getSharedPreferences(ACCOUNT_DB_PREFS_NAME, 0);
        String key = "";
        String secret = "";
        try {
            key = decrypt(prefs.getString(ACCESS_KEY_NAME, null), getBaseContext());
            secret = decrypt(prefs.getString(ACCESS_SECRET_NAME, null), getBaseContext());
        } catch (RuntimeException e) {
            Log.e(Cryptonite.TAG, "Couldn't decrypt DB access keys");
            return null;
        }
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
        SharedPreferences prefs = getSharedPreferences(ACCOUNT_DB_PREFS_NAME, 0);
        Editor edit = prefs.edit();
        try {
            edit.putString(ACCESS_KEY_NAME, encrypt(key, getBaseContext()));
            edit.putString(ACCESS_SECRET_NAME, encrypt(secret, getBaseContext()));
        } catch (RuntimeException e) {
            Log.e(Cryptonite.TAG, "Couldn't encrypt DB access keys");
        }
        edit.commit();
    }

    public void buildSession() {
        // Has the user already decided whether to use an app folder?
        SharedPreferences prefs = 
                getBaseContext().getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
        if (!prefs.getBoolean("dbDecided", false)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(Cryptonite.this);
            builder.setIcon(R.drawable.ic_launcher_cryptonite)
            .setTitle(R.string.dropbox_access_title)
            .setMessage(R.string.dropbox_access_msg)
            .setPositiveButton(R.string.dropbox_full,
                    new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog,
                        int which) {
                    setSession(false);
                }   
            })  
            .setNegativeButton(R.string.dropbox_folder,
                    new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog,
                        int which) {
                    setSession(true);
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        } else {
            setSession(prefs.getBoolean("cb_appfolder", false));
        }
    }
    
    private void setSession(final boolean useAppFolder) {
        final ProgressDialog pd = ProgressDialog.show(this,
                this.getString(R.string.wait_msg),
                this.getString(R.string.dropbox_connecting), true);
        new Thread(new Runnable(){
            public void run(){
                SharedPreferences prefs = 
                        getBaseContext().getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
                Editor edit = prefs.edit();
                edit.putBoolean("dbDecided", true);
                if (!edit.commit()) {
                    Log.e(Cryptonite.TAG, "Couldn't write preferences");
                }
                edit.putBoolean("cb_appfolder", useAppFolder);
                if (!edit.commit()) {
                    Log.e(Cryptonite.TAG, "Couldn't write preferences");
                }
                
                AndroidAuthSession session;
                AppKeyPair appKeyPair;
                AccessType accessType;
                
                String[] stored = getKeys();
                
                if (useAppFolder) {
                    appKeyPair = new AppKeyPair(jniFolderKey(), jniFolderPw());
                    accessType = AccessType.APP_FOLDER;
                } else {
                    appKeyPair = new AppKeyPair(jniFullKey(), jniFullPw());
                    accessType = AccessType.DROPBOX;
                }
                if (stored != null && stored.length >= 2) {
                    AccessTokenPair accessToken = new AccessTokenPair(stored[0], stored[1]);
                    session = new AndroidAuthSession(appKeyPair, accessType, accessToken);
                } else {
                    session = new AndroidAuthSession(appKeyPair, accessType);
                }
                DBInterface.INSTANCE.setDBApi(new DropboxAPI<AndroidAuthSession>(session));

                triedLogin = true;
                // Start the remote authentication
                DBInterface.INSTANCE.getDBApi()
                    .getSession().startAuthentication(Cryptonite.this);
                runOnUiThread(new Runnable(){
                    public void run() {
                        if (pd.isShowing())
                            pd.dismiss();
                    }
                });
            }
        }).start();

    }

    public static File getExternalCacheDir(final Context context) {
        /* Api >= 8 */
        return context.getExternalCacheDir();

        /* Api < 8
        // e.g. "<sdcard>/Android/data/<package_name>/cache/"
        final File extCacheDir = new File(Environment.getExternalStorageDirectory(),
                                          "/Android/data/" + context.getApplicationInfo().packageName + "/cache/");
        extCacheDir.mkdirs();
        return extCacheDir;*/
    }
    
    private static File getDownloadDir() {
        /* Api >= 8 */
        return Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS);
        /* Api < 8 
        File downloadDir = new File(Environment.getExternalStorageDirectory(), "/download");
        if (!downloadDir.exists()) {
            File downloadDirD = new File(Environment.getExternalStorageDirectory(), "/Download");
            if (!downloadDirD.exists()) {
                // Make "download" dir
                downloadDir.mkdirs();
                return downloadDir;
            } else {
                return downloadDirD;
            }
        } else {
            return downloadDir;
        }*/
    }
    
    private boolean openEncFSFile(final String encFSFilePath, String fileRoot) {

        SharedPreferences prefs = getBaseContext().getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
        mApp.setupReadDirs(prefs.getBoolean("cb_extcache", false));

        /* normalise path names */
        String bRoot = new File(fileRoot).getPath();
        String bPath = new File(encFSFilePath).getPath();
        String stripstrtmp = bPath.substring(bRoot.length());
        if (!stripstrtmp.startsWith("/")) {
            stripstrtmp = "/" + stripstrtmp;
        }

        final String stripstr = stripstrtmp;
        
        /* Convert current path to encoded file name */
        final String encodedPath = jniEncode(stripstr);

        /* Set up temp dir for decrypted file */
        String destPath = mApp.getOpenDir().getPath() + (new File(bPath)).getParent().substring(bRoot.length());

        (new File(destPath)).mkdirs();
        alertMsg = "";
        final ProgressDialog pd = ProgressDialog.show(this,
                this.getString(R.string.wait_msg),
                this.getString(R.string.decrypting), true);
            new Thread(new Runnable(){
                public void run(){
                    if (!StorageManager.INSTANCE.getEncFSStorage().decryptEncFSFile(encodedPath, mApp.getOpenDir().getPath())) {
                        alertMsg = getString(R.string.decrypt_failure);
                        Log.e(TAG, "Error while attempting to copy " + encodedPath);
                    }
                    runOnUiThread(new Runnable(){
                        public void run() {
                            if (pd.isShowing())
                                pd.dismiss();
                            if (!alertMsg.equals("")) {
                                showAlert(R.string.error, alertMsg);
                                return;
                            }
                            /* Copy the resulting file to a readable folder */
                            String openFilePath = mApp.getOpenDir().getPath() + stripstr;
                            String readableName = (new File(encFSFilePath)).getName();
                            String readablePath = mApp.getReadDir().getPath() + "/" + readableName;
                            File readableFile = new File(readablePath);
                            
                            /* Make sure the readable Path exists */
                            readableFile.getParentFile().mkdirs();
                            
                            try {
                                FileOutputStream fos = new FileOutputStream(readableFile);

                                FileInputStream fis = new FileInputStream(new File(openFilePath));

                                byte[] buffer = new byte[fis.available()]; 

                                fis.read(buffer);

                                fos.write(buffer);

                                fis.close();
                                fos.close();

                                /* Make world readable */
                                try {
                                    ShellUtils.chmod(readablePath, "644");
                                } catch (InterruptedException e) {
                                    Log.e(Cryptonite.TAG, e.toString());
                                }
                                
                                /* Delete tmp directory */
                                deleteDir(mApp.getOpenDir());
                            } catch (IOException e) {
                                Toast.makeText(Cryptonite.this, "Error while attempting to open " + readableName
                                        + ": " + e.toString(), Toast.LENGTH_LONG).show();
                                return;
                            }
                            
                            fileOpen(readablePath);
                            
                        }
                    });
                }
            }).start();
        return true;        
    }
    
    private String getMimeTypeFromExtension(String filePath) {
        /* Guess MIME type */
        MimeTypeMap myMime = MimeTypeMap.getSingleton();
        String extension = Storage.fileExt(filePath);
        String contentType;
        if (extension.length() == 0) {
            contentType = null;
        } else {
            contentType = myMime.getMimeTypeFromExtension(extension.substring(1));
        }
        
        return contentType;
    }
    
    private boolean fileOpen(String filePath) {
        String contentType = getMimeTypeFromExtension(filePath);
        
        Uri data = Uri.fromFile(new File(filePath));

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

    private boolean previewEncFSFile(final String encFSFilePath, String fileRoot) {

        SharedPreferences prefs = getBaseContext().getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
        mApp.setupReadDirs(prefs.getBoolean("cb_extcache", false));

        /* normalise path names */
        String bRoot = new File(fileRoot).getPath();
        String bPath = new File(encFSFilePath).getPath();
        String stripstrtmp = bPath.substring(bRoot.length());
        if (!stripstrtmp.startsWith("/")) {
            stripstrtmp = "/" + stripstrtmp;
        }

        final String stripstr = stripstrtmp;

        /* Convert current path to encoded file name */
        final String encodedPath = jniEncode(stripstr);

        final ProgressDialog pd = ProgressDialog.show(this,
                this.getString(R.string.wait_msg),
                this.getString(R.string.decrypting), true);
            new Thread(new Runnable(){
                public void run() {
                    final DecodedBuffer buf = StorageManager.INSTANCE.getEncFSStorage().decryptEncFSFileToBuffer(encodedPath);
                    if (buf == null) {
                        alertMsg = getString(R.string.decrypt_failure);
                        Log.e(TAG, "Error while attempting to decrypt" + encodedPath);
                    }
                    runOnUiThread(new Runnable(){
                        public void run() {
                            if (pd.isShowing())
                                pd.dismiss();
                            if (!alertMsg.equals("")) {
                                showAlert(R.string.error, alertMsg);
                                return;
                            }
                            bufferPreview(buf);
                        }
                    });
                }
            }).start();
        return true;
    }
 
    public static class DecodedBuffer {
        public final String fileName;
        public final byte[] contents;
        
        public DecodedBuffer(String fn, byte[] buf) {
            fileName = fn;
            contents = buf;
        }
    }
    
    private void bufferPreview(final DecodedBuffer buf) {
        /* Attempt to guess file type from extension */
        String contentType = getMimeTypeFromExtension(buf.fileName);
        
        if (contentType != "text/plain" && Storage.fileExt(buf.fileName).length() > 0) {
            new AlertDialog.Builder(Cryptonite.this)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setTitle(R.string.warning)
            .setMessage(R.string.not_plain_text)
            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    textPreview(buf);
                }
            })
            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    return;
                }
            })
            .create().show();
        } else {
            textPreview(buf);
        }
    }
    
    private void textPreview(DecodedBuffer buf) {
        String str = "";
        try {
            str = new String(buf.contents, "utf-8");
        } catch (UnsupportedEncodingException e) {
            showAlert(R.string.error, R.string.unsupported_encoding);
            return;
        }
        String truncated = "";
        /* Truncate at 100KB; couldn't find any documentation
         * what the limit really is.
         */
        if (str.length() > 1e5) {
            str = str.substring(0, (int)1e5);
            truncated = " (truncated)";
        }
        Intent intent = new Intent(getBaseContext(), TextPreview.class);
        intent.putExtra(TextPreview.PREVIEW_TITLE, buf.fileName + truncated);
        intent.putExtra(TextPreview.PREVIEW_BODY, str);
        startActivityForResult(intent, TextPreview.REQUEST_PREVIEW);
    }
    
    public static int hasExtterm(Context context) {
        ComponentName termComp = new ComponentName("jackpal.androidterm", "jackpal.androidterm.Term");
        try {
            PackageInfo pinfo = context.getPackageManager().getPackageInfo(termComp.getPackageName(), 0);
            int patchCode = pinfo.versionCode;

            if (patchCode < 32) {
                return TERM_OUTDATED;
            } else {
                return TERM_AVAILABLE;
            }
        } catch (PackageManager.NameNotFoundException e) {
            return TERM_UNAVAILABLE;
        }
    }

    public void launchTerm() {
        launchTerm(false);
    }
    
    public void launchTerm(boolean root) {
        /* Is a reminal emulator running? */
        
            /* If Terminal Emulator is not installed or outdated,
             * offer to download
             */
            if (hasExtterm(getBaseContext())!=TERM_AVAILABLE) {
                TermUnavailableDialogFragment newFragment = TermUnavailableDialogFragment.newInstance();
                newFragment.show(getSupportFragmentManager(), "dialog");
            } else {
                ComponentName termComp = new ComponentName("jackpal.androidterm", "jackpal.androidterm.Term");
                try {
                    PackageInfo pinfo = getBaseContext().getPackageManager().getPackageInfo(termComp.getPackageName(), 0);
                    String patchVersion = pinfo.versionName;
                    Log.v(TAG, "Terminal Emulator version: " + patchVersion);
                    int patchCode = pinfo.versionCode;

                    if (patchCode < 32) {
                        showAlert(R.string.error, R.string.app_terminal_outdated);
                    } else {
                        Intent intent = new Intent(Intent.ACTION_MAIN);
                        intent.setComponent(termComp);
                        runTerm(intent, extTermRunning(), root);
                    }

                } catch (PackageManager.NameNotFoundException e) {
                    Toast.makeText(Cryptonite.this, R.string.app_terminal_missing, Toast.LENGTH_LONG).show();
                }
            }
    }

    private void runTerm(Intent intent, boolean running, boolean root) {
        /* If the terminal is running, abort */
        if (running) {
            new AlertDialog.Builder(Cryptonite.this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.warning)
                .setMessage(R.string.term_service_running)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        ;
                    }
                })
                .create().show();
            return;
        }
        
        String initCmd = "export PATH=" + mApp.getBinDirPath() + ":${PATH};";
        if (root) {
            initCmd += " su;";
        }
        intent.putExtra("jackpal.androidterm.iInitialCommand", initCmd);
        startActivity(intent);
    }

    private boolean extTermRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
             if ("jackpal.androidterm.TermService".equals(service.service.getClassName())) {
                 return true;
             }
        }
        return false;
    }
    
    public void setDBFragment(DropboxFragment fragment) {
        dbFragment = fragment;
    }
    
    public void setLocalFragment(LocalFragment fragment) {
        localFragment = fragment;
    }
    
    /* With some modifications from:
     * http://stackoverflow.com/questions/785973
     * Michael Burton (http://stackoverflow.com/users/82156/emmby)
     */
    public static String encrypt(String value, Context context) throws RuntimeException {

        try {
            final byte[] bytes = value!=null ? value.getBytes("utf-8") : new byte[0];
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
            SecretKey key = keyFactory.generateSecret(new PBEKeySpec(jniFullPw().toCharArray()));
            Cipher pbeCipher = Cipher.getInstance("PBEWithMD5AndDES");
            pbeCipher.init(Cipher.ENCRYPT_MODE, key,
                    new PBEParameterSpec(Settings.Secure.getString(context.getContentResolver(),
                            Settings.Secure.ANDROID_ID).getBytes("utf-8"), 20));
            return new String(Base64.encode(pbeCipher.doFinal(bytes), Base64.NO_WRAP), "utf-8");

        } catch( Exception e ) {
            throw new RuntimeException(e);
        }
    }

    public static String decrypt(String value, Context context) throws RuntimeException {
        try {
            final byte[] bytes = value!=null ? Base64.decode(value, Base64.DEFAULT) : new byte[0];
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
            SecretKey key = keyFactory.generateSecret(new PBEKeySpec(jniFullPw().toCharArray()));
            Cipher pbeCipher = Cipher.getInstance("PBEWithMD5AndDES");
            pbeCipher.init(Cipher.DECRYPT_MODE, key,
                    new PBEParameterSpec(Settings.Secure.getString(context.getContentResolver(),
                            Settings.Secure.ANDROID_ID).getBytes("utf-8"), 20));
            return new String(pbeCipher.doFinal(bytes), "utf-8");

        } catch( Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public static class PasswordDialogFragment extends SherlockDialogFragment {

        public static PasswordDialogFragment newInstance() {
            PasswordDialogFragment frag = new PasswordDialogFragment();
            return frag;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            LayoutInflater inflater = (LayoutInflater) getActivity()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View layout = inflater.inflate(R.layout.password_dialog, 
                    (ViewGroup) getActivity().findViewById(R.id.root));
            final EditText password = (EditText) layout.findViewById(R.id.EditText_Pwd);
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.title_password);
            builder.setView(layout);
            builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        
                    }
                });
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        ((Cryptonite)getActivity()).currentPassword = password.getText().toString();
                        if (((Cryptonite)getActivity()).currentPassword.length() > 0) {
                            switch (((Cryptonite)getActivity()).opMode) {
                             case MOUNT_MODE:
                                 ((Cryptonite)getActivity()).mountEncFS(
                                         ((Cryptonite)getActivity()).currentReturnPath);
                                 ((Cryptonite)getActivity()).opMode = (
                                         (Cryptonite)getActivity()).prevMode;
                                 break;
                             case SELECTLOCALENCFS_MODE:
                             case SELECTDBENCFS_MODE:
                                 ((Cryptonite)getActivity()).initEncFS(
                                         ((Cryptonite)getActivity()).currentReturnPath);
                                 break;
                            }
                        } else {
                            ((Cryptonite)getActivity()).showAlert(R.string.error, R.string.empty_password);
                        }
                    }
                });
            return builder.create();
        }
    }
    
    public static class OIUnavailableDialogFragment extends SherlockDialogFragment {

        public static OIUnavailableDialogFragment newInstance() {
            OIUnavailableDialogFragment frag = new OIUnavailableDialogFragment();
            return frag;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder((Cryptonite) getActivity())
                    .setIcon(R.drawable.ic_launcher_folder)
                    .setTitle(R.string.app_oi_missing)
                    .setPositiveButton(R.string.app_oi_get,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                        int whichButton) {
                                    Intent intent = new Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("market://details?id=org.openintents.filemanager"));
                                    try {
                                        startActivity(intent);
                                    } catch (ActivityNotFoundException e) {
                                        ((Cryptonite) getActivity()).showAlert(R.string.warning,
                                                R.string.market_missing);
                                    }
                                }
                            })
                    .setNegativeButton(R.string.app_oi_builtin,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                        int whichButton) {
                                    ((Cryptonite) getActivity()).launchBuiltinFileBrowser();
                                }
                            }).create();
        }
    }

    public static class TermUnavailableDialogFragment extends SherlockDialogFragment {

        public static TermUnavailableDialogFragment newInstance() {
            TermUnavailableDialogFragment frag = new TermUnavailableDialogFragment();
            return frag;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder((Cryptonite)getActivity())
                    .setIcon(R.drawable.app_terminal)
                    .setTitle(R.string.app_terminal_missing)
                    .setPositiveButton(R.string.app_terminal_get,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                        int whichButton) {
                                    Intent intent = new Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("market://details?id=jackpal.androidterm"));
                                    try {
                                        startActivity(intent);
                                    } catch (ActivityNotFoundException e) {
                                        ((Cryptonite)getActivity()).showAlert(R.string.warning,
                                                R.string.market_missing);
                                    }
                                }
                            }).create();
        }
    }

    /* Native methods are implemented by the
     * 'cryptonite' native library, which is packaged
     * with this application.
     */
    public native int     jniFailure();
    public static native int jniSuccess();
    public static native int     jniIsValidEncFS(String srcDir);
    public static native int jniVolumeLoaded();
    public static native int jniResetVolume();
    public native int     jniBrowse(String srcDir, String destDir, String password);
    public native int     jniInit(String srcDir, String password);
    public static native int jniCreate(String srcDir, String password, int config);
    public native int     jniExport(String[] exportPaths, String exportRoot, String destDir);
    public static native int jniDecrypt(String encodedName, String destDir, boolean forceReadable);
    public static native byte[] jniDecryptToBuffer(String encodedName);
    public static native int jniEncrypt(String decodedPath, String srcPath, boolean forceReadable);
    public static native String jniDecode(String name);
    public static native String jniEncode(String name);
    public native String  jniEncFSVersion();
    public native String  jniOpenSSLVersion();
    public native String  jniFullKey();
    public static native String  jniFullPw();
    public native String  jniFolderKey();
    public native String  jniFolderPw();

    private void jniFail() {
        AlertDialog.Builder builder = new AlertDialog.Builder(Cryptonite.this);
        builder.setIcon(R.drawable.ic_launcher_cryptonite)
        .setTitle(R.string.error)
        .setMessage(R.string.jni_fail)
        .setPositiveButton(R.string.send_email,
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,
                    int which) {
                Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
                emailIntent.setType("plain/text");
                emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL,
                        new String[]{"christoph.schmidthieber@googlemail.com"});
                emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
                        Cryptonite.this.getString(R.string.crash_report));
                emailIntent.putExtra(android.content.Intent.EXTRA_TEXT,
                        getString(R.string.crash_report_content));
                Cryptonite.this.startActivity(Intent.createChooser(emailIntent, "Send mail..."));
                finish();
            }   
        })  
        .setNeutralButton(R.string.send_report,
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,
                    int which) {
                Intent reportIntent = new Intent(android.content.Intent.ACTION_VIEW);
                String url = "https://code.google.com/p/cryptonite/issues/detail?id=9";
                reportIntent.setData(Uri.parse(url));
                Cryptonite.this.startActivity(reportIntent);
                finish();
            }
        })
        .setNegativeButton(R.string.cancel,
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,
                    int which) {
                finish();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /* this is used to load the 'cryptonite' library on application
     * startup. The library has already been unpacked into
     * /data/data/csh.cryptonite/lib/libcryptonite.so at
     * installation time by the package manager.
     */
    static {
        try {
            System.loadLibrary("cryptonite");
            Cryptonite.hasJni = true;
        } catch (java.lang.UnsatisfiedLinkError e) {
            Cryptonite.hasJni = false;
        }
    }

}
