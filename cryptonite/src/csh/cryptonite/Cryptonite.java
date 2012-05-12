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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
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
import android.content.pm.PackageManager.NameNotFoundException;

import android.graphics.drawable.Drawable;

import android.net.Uri;

import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;

import android.support.v4.view.ViewPager;

import android.text.SpannableString;
import android.text.util.Linkify;
import android.text.method.ScrollingMovementMethod;

import android.util.Base64;
import android.util.Log;

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

import csh.cryptonite.storage.DropboxInterface;
import csh.cryptonite.storage.DropboxFragment;
import csh.cryptonite.storage.LocalFragment;
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
    public static final int RESULT_RETRY = 1;
    public static final int RESULT_ERROR = 2;
    
    private static final int DIRPICK_MODE=0;
    public static final int FILEPICK_MODE=1;
    protected static final int MSG_SHOW_TOAST = 0;
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
    private String currentUploadTargetPath = "/";
    private String encfsBrowseRoot = "/";
    private String[] currentReturnPathList = {};
    public int currentDialogMode = SelectionMode.MODE_OPEN_ENCFS;

    public int opMode = -1;
    public int prevMode = -1;
    private boolean alert = false;
    private String alertMsg = "";
 
    public boolean mLoggedIn = false;
    public boolean hasFuse = false;
    public boolean triedLogin = false;
    private boolean mInstrumentation = false;
    private boolean mUseAppFolder;
    private boolean disclaimerShown = false;

    private TabHost mTabHost;
    private ViewPager  mViewPager;
    private TabsAdapter mTabsAdapter;

    private DropboxFragment dbFragment;
    private LocalFragment localFragment;

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
        setupReadDirs(prefs.getBoolean("cb_extcache", false));
        StorageManager.INSTANCE.initLocalStorage(this);

        if (!externalStorageIsWritable() || !ShellUtils.supportsFuse()) {
            mountInfo = getString(R.string.mount_info_unsupported);
        } else {
            mountInfo = getString(R.string.mount_info);
            DirectorySettings.INSTANCE.mntDir = prefs.getString("txt_mntpoint", Cryptonite.defaultMntDir());
            File mntDirF = new File(DirectorySettings.INSTANCE.mntDir);
            if (!mntDirF.exists()) {
                mntDirF.mkdirs();
            }
        }

        DirectorySettings.INSTANCE.binDirPath = getFilesDir().getParentFile().getPath();
        DirectorySettings.INSTANCE.encFSBin = DirectorySettings.INSTANCE.binDirPath + "/encfs";

        hasFuse = ShellUtils.supportsFuse();
        
        /* Running from Instrumentation? */
        if (getIntent() != null) {
            mInstrumentation = getIntent().getBooleanExtra("csh.cryptonite.instrumentation", false);
        } else {
            mInstrumentation = false;
        }
        
        if (needsEncFSBinary()) {
            ProgressDialogFragment.showDialog(this, R.string.copying_bins, "copyingBins");
            new Thread(new Runnable(){
                public void run(){
                    cpBin("encfs");
                    cpBin("truecrypt");
                    runOnUiThread(new Runnable(){
                        public void run() {
                            ProgressDialogFragment.dismissDialog(Cryptonite.this, "copyingBins");
                            setEncFSBinaryVersion();
                        }
                    });
                }
            }).start();
        }
        
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
            opMode = savedInstanceState.getInt("opMode");
            if (savedInstanceState.getString("currentReturnPath") != null) {
                currentReturnPath = savedInstanceState.getString("currentReturnPath");
            }
            if (savedInstanceState.getString("currentDialogStartPath") != null) {
                currentDialogStartPath = savedInstanceState.getString("currentDialogStartPath");
            }
            if (savedInstanceState.getString("currentDialogRoot") != null) {
                currentDialogRoot = savedInstanceState.getString("currentDialogRoot");
            }
            if (savedInstanceState.getString("currentDialogLabel") != null) {
                currentDialogLabel = savedInstanceState.getString("currentDialogLabel");
            }
            if (savedInstanceState.getString("currentDialogButtonLabel") != null) {
                currentDialogButtonLabel = savedInstanceState.getString("currentDialogButtonLabel");
            }
            if (savedInstanceState.getString("currentDialogRootName") != null) {
                currentDialogRootName = savedInstanceState.getString("currentDialogRootName");
            }
            if (savedInstanceState.getString("currentOpenPath") != null) {
                currentOpenPath = savedInstanceState.getString("currentOpenPath");
            }
            if (savedInstanceState.getString("currentUploadTargetPath") != null) {
                currentUploadTargetPath = savedInstanceState.getString("currentUploadTargetPath");
            }
            if (savedInstanceState.getString("encfsBrowseRoot") != null) {
                encfsBrowseRoot = savedInstanceState.getString("encfsBrowseRoot");
            }
            if (savedInstanceState.getStringArray("currentReturnPathList") != null) {
                currentReturnPathList = savedInstanceState.getStringArray("currentReturnPathList");
            }
            if (savedInstanceState.getInt("currentDialogMode") != 0) {
                currentDialogMode = savedInstanceState.getInt("currentDialogMode");
            }
            disclaimerShown = savedInstanceState.getBoolean("disclaimerShown");
            if (currentReturnPath != null && currentDialogStartPath != null) {
                DirectorySettings.INSTANCE.currentBrowsePath = currentReturnPath;
                DirectorySettings.INSTANCE.currentBrowseStartPath = currentDialogStartPath;
            }
            int storageType = savedInstanceState.getInt("storageType");
            if (storageType != Storage.STOR_UNDEFINED) {
                StorageManager.INSTANCE.initEncFSStorage(this, storageType);
                if (savedInstanceState.getString("encFSPath") != null) {
                    StorageManager.INSTANCE.setEncFSPath(savedInstanceState.getString("encFSPath"));
                }
            }
            
        }
        
        String[] stored = getKeys();
        if (stored != null && stored.length >= 2) {
            if (prefs.getBoolean("dbDecided", false)) {            
                setSession(prefs.getBoolean("cb_appfolder", false), false);
                updateDecryptButtons();
            }
        }

        if (DropboxInterface.INSTANCE.getDBApi() != null && 
                DropboxInterface.INSTANCE.getDBApi().getSession() != null)
        {
            mLoggedIn = DropboxInterface.INSTANCE.getDBApi().getSession().isLinked();
        } else {
            mLoggedIn = false;
        }

        if (!mInstrumentation && !prefs.getBoolean("cb_norris", false) && !disclaimerShown) {
            AlertDialog.Builder builder = new AlertDialog.Builder(Cryptonite.this);
            builder.setIcon(R.drawable.ic_launcher_cryptonite)
                .setTitle(R.string.disclaimer)
                .setMessage(R.string.no_warranty)
                .setPositiveButton(R.string.understand,
                                   new DialogInterface.OnClickListener() {
                                       public void onClick(DialogInterface dialog,
                                                           int which) {
                                           disclaimerShown = true;
                                       }
                                   });
            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mTabHost != null) {
            outState.putString("tab", mTabHost.getCurrentTabTag());
        }
        outState.putInt("opMode", opMode);
        outState.putString("currentReturnPath", currentReturnPath);
        outState.putString("currentDialogStartPath", currentDialogStartPath);
        outState.putString("currentDialogRoot", currentDialogRoot);
        outState.putString("currentDialogLabel", currentDialogLabel);
        outState.putString("currentDialogButtonLabel", currentDialogButtonLabel);
        outState.putString("currentDialogRootName", currentDialogRootName);
        outState.putString("currentOpenPath", currentOpenPath);
        outState.putString("currentUploadTargetPath", currentUploadTargetPath);
        outState.putString("encfsBrowseRoot", encfsBrowseRoot);
        outState.putStringArray("currentReturnPathList", currentReturnPathList);
        outState.putInt("currentDialogMode", currentDialogMode);
        outState.putBoolean("disclaimerShown", disclaimerShown);
        if (StorageManager.INSTANCE.getEncFSStorage() != null) {
            outState.putInt("storageType", StorageManager.INSTANCE.getEncFSStorageType());
            outState.putString("encFSPath", StorageManager.INSTANCE.getEncFSPath());
        } else {
            outState.putInt("storageType", Storage.STOR_UNDEFINED);
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (!hasJni) {
            return;
        }

        finishAuthentication();   
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
        case SelectionMode.MODE_OPEN_ENCFS:
        case SelectionMode.MODE_OPEN_ENCFS_DB:
        case SelectionMode.MODE_OPEN_UPLOAD_SOURCE:
        case SelectionMode.MODE_OPEN_CREATE:
            /* file dialog */
            if (resultCode == Activity.RESULT_OK && data != null) {
                currentReturnPath = data.getStringExtra(FileDialog.RESULT_EXPORT_PATHS);
                if (currentReturnPath != null ) {
                    switch (opMode) {
                    case MOUNT_MODE:
                        opMode = prevMode;
                        if (localFragment != null) {
                            localFragment.updateMountButtons();
                        }
                        break;
                    case SELECTLOCALENCFS_MODE:
                    case SELECTDBENCFS_MODE:
                        StorageManager.INSTANCE.setEncFSPath(currentReturnPath
                                .substring(currentDialogStartPath.length()));
                        updateDecryptButtons();
                        break;
                    case LOCALEXPORT_MODE:
                    case DBEXPORT_MODE:
                    case SELECTDBUPLOAD_MODE:
                    case SELECTLOCALUPLOAD_MODE:
                        break;
                    }
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
            } else if (resultCode == RESULT_RETRY) {
                launchBuiltinFileBrowser();
            }
            break;
        case SelectionMode.MODE_OPEN_MULTISELECT:
        case SelectionMode.MODE_OPEN_MULTISELECT_DB:
            if (resultCode == Activity.RESULT_OK && data != null) {
                currentReturnPathList = data
                        .getStringArrayExtra(FileDialog.RESULT_EXPORT_PATHS);
                if (currentReturnPathList != null
                        && currentReturnPathList.length > 0)
                {
                    /* Select destination directory for exported files */
                    currentDialogLabel = Cryptonite.this
                            .getString(R.string.select_exp);
                    currentDialogButtonLabel = Cryptonite.this
                            .getString(R.string.select_exp_short);
                    currentDialogMode = SelectionMode.MODE_OPEN_EXPORT_TARGET;
                    if (externalStorageIsWritable()) {
                        currentDialogStartPath = getDownloadDir().getPath();
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
                    if (StorageManager.INSTANCE.getEncFSStorage() != null) {
                        opMode = StorageManager.INSTANCE.getEncFSStorage().exportMode;
                    } else {
                        return;
                    }
                    launchBuiltinFileBrowser();
                } else {
                    currentOpenPath = data
                            .getStringExtra(FileDialog.RESULT_OPEN_PATH);
                    if (currentOpenPath != null && currentOpenPath.length() > 0) {
                        /* */
                    } else {
                        currentUploadTargetPath = data
                                .getStringExtra(FileDialog.RESULT_UPLOAD_PATH);
                        if (currentUploadTargetPath != null
                                && currentUploadTargetPath.length() > 0)
                        {
                            /* select file to upload */
                            currentDialogLabel = Cryptonite.this
                                    .getString(R.string.select_upload);
                            currentDialogButtonLabel = Cryptonite.this
                                    .getString(R.string.select_upload_short);
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
                                opMode = StorageManager.INSTANCE
                                        .getEncFSStorage().uploadMode;
                            } else {
                                return;
                            }
                            launchBuiltinFileBrowser();
                        }
                    }
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {

            } else if (resultCode == RESULT_ERROR) {
                Toast.makeText(this, R.string.decode_failure, Toast.LENGTH_LONG).show();
                jniResetVolume();
                StorageManager.INSTANCE.resetEncFSStorage();
                VirtualFileSystem.INSTANCE.clear();
            }
            break;
        case REQUEST_PREFS:
            SharedPreferences prefs = getBaseContext().getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
            Editor prefEdit = prefs.edit();
            
            setupReadDirs(prefs.getBoolean("cb_extcache", false));
            
            DirectorySettings.INSTANCE.mntDir = prefs.getString("txt_mntpoint", defaultMntDir());

            /* If app folder settings have changed, we'll have to log out the user
             * from his Dropbox and restart the authentication from scratch during
             * the next login:
             */
            if (prefs.getBoolean("cb_appfolder", false) != mUseAppFolder) {
                prefEdit.putBoolean("dbDecided", true);
                prefEdit.commit();
                /* enforce re-authentication */
                DropboxInterface.INSTANCE.setDBApi(null);
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

    public void finishAuthentication() {
        if (DropboxInterface.INSTANCE.getDBApi() != null && 
                DropboxInterface.INSTANCE.getDBApi().getSession() != null)
        {
            if (triedLogin) {
                AndroidAuthSession session = DropboxInterface.INSTANCE.getDBApi().getSession();
                // The next part must be inserted in the onResume() method of the
                // activity from which session.startAuthentication() was called, so
                // that Dropbox authentication completes properly.
                // Make sure we're returning from an authentication attempt at all.
                triedLogin = false;
                if (session.authenticationSuccessful()) {
                    try {
                        // Mandatory call to complete the auth
                        session.finishAuthentication();
        
                        // Store it locally in our app for later use
                        TokenPair tokens = session.getAccessTokenPair();
                        storeKeys(tokens.key, tokens.secret);
                        
                        DropboxInterface.INSTANCE.clearDBHashMap();
                        
                        setLoggedIn(true);
                    } catch (IllegalStateException e) {
                        Toast.makeText(Cryptonite.this, 
                                getString(R.string.dropbox_auth_fail) + ": " + e.getLocalizedMessage(), 
                                Toast.LENGTH_LONG).show();
                    }
                } else {
                    if (!session.isLinked()) {
                        logOut();
                    }
                }
            } else {
                mLoggedIn = DBInterface.INSTANCE.getDBApi().getSession().isLinked();
            }
        } else {
            setLoggedIn(false);
        }
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
        final VirtualFile browseDirF = new VirtualFile(VirtualFile.VIRTUAL_TAG
                + "/" + StorageManager.INSTANCE.getEncFSStorage().browsePnt);
        browseDirF.mkdirs();

        new Thread(new Runnable() {
            public void run() {
                Log.i(TAG, "Dialog root is " + browsePath);
                currentDialogStartPath = browseDirF.getPath();
                currentDialogLabel = getString(R.string.select_file_export);
                currentDialogButtonLabel = getString(R.string.export);
                currentDialogRoot = currentDialogStartPath;
                encfsBrowseRoot = currentDialogRoot;
                currentDialogRootName = getString(R.string.encfs_root);
                currentDialogMode = StorageManager.INSTANCE.getEncFSStorage().fdSelectionMode;
                runOnUiThread(new Runnable() {
                    public void run() {
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
        intent.putExtra(FileDialog.CURRENT_UPLOAD_TARGET_PATH, currentUploadTargetPath);
        intent.putExtra(FileDialog.CURRENT_EXPORT_PATH_LIST, currentReturnPathList);
        intent.putExtra(FileDialog.ENCFS_BROWSE_ROOT, encfsBrowseRoot);
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
        if (DropboxInterface.INSTANCE.getDBApi() != null) {
            DropboxInterface.INSTANCE.getDBApi().getSession().unlink();
            DropboxInterface.INSTANCE.clearDBHashMap();
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
            // Clear our stored keys
            clearKeys();
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
        final SharedPreferences prefs = 
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
                    setSessionProgressDialog(false);
                }   
            })  
            .setNegativeButton(R.string.dropbox_folder,
                    new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog,
                        int which) {
                    setSessionProgressDialog(true);
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        } else {
            setSessionProgressDialog(prefs.getBoolean("cb_appfolder", false));
        }
    }

    private void setSessionProgressDialog(final boolean useAppFolder) {
        ProgressDialogFragment.showDialog(this, R.string.dropbox_connecting, "dbSession");
        new Thread(new Runnable(){
            public void run(){
                setSession(useAppFolder, true);
                runOnUiThread(new Runnable(){
                    public void run() {
                        ProgressDialogFragment.dismissDialog(Cryptonite.this, "dbSession");
                    }
                });
            }
        }).start();
        
    }
    
    private void setSession(final boolean useAppFolder, final boolean authenticate)
    {
        SharedPreferences prefs = getBaseContext().getSharedPreferences(
                ACCOUNT_PREFS_NAME, 0);
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
            AccessTokenPair accessToken = new AccessTokenPair(stored[0],
                    stored[1]);
            session = new AndroidAuthSession(appKeyPair, accessType,
                    accessToken);
        } else {
            session = new AndroidAuthSession(appKeyPair, accessType);

        }
        DropboxInterface.INSTANCE.setDBApi(new DropboxAPI<AndroidAuthSession>(
                session));

        if (authenticate) {
            dbAuthenticate();
        }

    }
    
    public void dbAuthenticate() {
        DropboxInterface.INSTANCE.getDBApi()
            .getSession().startAuthentication(Cryptonite.this);                
        triedLogin = true;
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
                    } else if (patchCode < 43) {
                        Intent intent = new Intent(Intent.ACTION_MAIN);
                        intent.setComponent(termComp);
                        runTerm(intent, extTermRunning(), root);
                    } else {
                        ComponentName remoteComp = 
                                new ComponentName("jackpal.androidterm", "jackpal.androidterm.RemoteInterface");
                        Intent intent = new Intent("jackpal.androidterm.RUN_SCRIPT");
                        intent.setComponent(remoteComp);
                        runTerm(intent, false, root);
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
        
        String initCmd = "export PATH=" + DirectorySettings.INSTANCE.binDirPath + ":${PATH};";
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

    /** Copy encfs to binDirPath and make executable */
    public void cpBin(String trunk) {
        String arch = "armeabi";
        /* if (withVfp) {
            arch += "-v7a";
            } */
            
        File binDir = new File(DirectorySettings.INSTANCE.binDirPath);
        if (!binDir.exists()) {
            throw new RuntimeException("Couldn't find binary directory");
        }
        String binName = binDir + "/" + trunk;

        /* Catenate split files */
        try {
            String[] assetsFiles = getAssets().list(arch);
            File newf = new File(binName);
            FileOutputStream os = new FileOutputStream(newf);
            for (String assetsFile : assetsFiles) {
                if (assetsFile.substring(0, assetsFile.indexOf(".")).compareTo(trunk) == 0) {
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
            Log.e(Cryptonite.TAG, "Problem while copying binary: " + e.toString());
        } catch (InterruptedException e) {
            Log.e(Cryptonite.TAG, "Problem while copying binary: " + e.toString());
        }

    }

    public void cleanUpDecrypted() {
        Cryptonite.jniResetVolume();
        
        /* Delete directories */
        Cryptonite.deleteDir(getBaseContext().getFilesDir());
        Cryptonite.deleteDir(getBaseContext().getDir(
                DirectorySettings.BROWSEPNT, Context.MODE_PRIVATE));
        Cryptonite.deleteDir(getBaseContext().getDir(
                DirectorySettings.DROPBOXPNT, Context.MODE_PRIVATE));
        Cryptonite.deleteDir(DirectorySettings.INSTANCE.openDir);
        Cryptonite.deleteDir(DirectorySettings.INSTANCE.readDir);
        
        /* Delete virtual file system */
        VirtualFileSystem.INSTANCE.clear();
    }
    
    public void setupReadDirs(boolean external) {
        setupReadDirs(external, this);
    }
    
    public static void setupReadDirs(boolean external, Context context) {
        if (DirectorySettings.INSTANCE.openDir != null) {
            Cryptonite.deleteDir(DirectorySettings.INSTANCE.openDir);
        }
        if (DirectorySettings.INSTANCE.readDir != null) {
            Cryptonite.deleteDir(DirectorySettings.INSTANCE.readDir);
        }   
        
        if (external && Cryptonite.externalStorageIsWritable()) {
            context.getExternalCacheDir().mkdirs();
            DirectorySettings.INSTANCE.openDir = new File(
                    context.getExternalCacheDir().getPath() + "/" + DirectorySettings.OPENPNT);
            DirectorySettings.INSTANCE.readDir = new File(
                    context.getExternalCacheDir().getPath() + "/" + DirectorySettings.READPNT);
            DirectorySettings.INSTANCE.openDir.mkdirs();
            DirectorySettings.INSTANCE.readDir.mkdirs();
        } else {
            DirectorySettings.INSTANCE.openDir = context.getDir(DirectorySettings.OPENPNT, Context.MODE_PRIVATE);
            DirectorySettings.INSTANCE.readDir = context.getDir(DirectorySettings.READPNT, Context.MODE_WORLD_READABLE);
        }
    }

    public boolean needsEncFSBinary() {
        if (!(new File(DirectorySettings.INSTANCE.encFSBin)).exists()) {
            return true;
        }
        
        PackageInfo pInfo;
        try {
            pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch (NameNotFoundException e) {
            return true;
        }
        String appVersion = pInfo.versionName;
        
        SharedPreferences prefs = getBaseContext().getSharedPreferences(Cryptonite.ACCOUNT_PREFS_NAME, 0);
        String binVersion = prefs.getString("binVersion", "");
        return !binVersion.equals(appVersion);
    }

    public void setEncFSBinaryVersion() {
        PackageInfo pInfo;
        try {
            pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch (NameNotFoundException e) {
            return;
        }
        String appVersion = pInfo.versionName;
    
        SharedPreferences prefs = getBaseContext().getSharedPreferences(Cryptonite.ACCOUNT_PREFS_NAME, 0);
        Editor prefEdit = prefs.edit();
        prefEdit.putString("binVersion", appVersion);
        prefEdit.commit();
    }

    public static class DecodedBuffer {
        public final String fileName;
        public final byte[] contents;
        
        public DecodedBuffer(String fn, byte[] buf) {
            fileName = fn;
            contents = buf;
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
    public native int     jniBrowse(String srcDir, String destDir, String password, boolean useAnyKey);
    public static native int jniInit(String srcDir, String password, boolean useAnyKey);
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
            Log.e(TAG, e.getMessage());
            Cryptonite.hasJni = false;
        }
    }

    public boolean hasStored(String storagePrefsKey) {
        SharedPreferences prefs = getBaseContext().getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
        return prefs.getBoolean(storagePrefsKey, false);
    }
    
}
