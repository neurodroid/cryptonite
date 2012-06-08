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

import com.actionbarsherlock.app.SherlockPreferenceActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import android.os.Bundle;
import android.os.Environment;

import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;

import android.util.Log;
import android.widget.Toast;

public class Preferences extends SherlockPreferenceActivity {

    private CheckBoxPreference chkEnableBuiltin;
    private CheckBoxPreference chkNorris;
    private CheckBoxPreference chkExtCache;
    private CheckBoxPreference chkAppFolder;
    private CheckBoxPreference chkAnyKey;
    
    private Preference txtMntPoint;
    private Preference clearCache;
    
    private String currentReturnPath = "";
    
    @SuppressWarnings("deprecation")
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        
        SharedPreferences prefs = getSharedPreferences(Cryptonite.ACCOUNT_PREFS_NAME, 0);
        
        chkEnableBuiltin = (CheckBoxPreference)getPreferenceScreen().findPreference("cb_builtin");
        chkEnableBuiltin.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    if (chkEnableBuiltin.isChecked()) {
                        Toast.makeText(Preferences.this, getString(R.string.cb_builtin_enabled), Toast.LENGTH_SHORT).show();
                        return true;
                    } else {
                        Toast.makeText(Preferences.this, getString(R.string.cb_builtin_disabled), Toast.LENGTH_SHORT).show();
                        return false;
                    }
                }});
        
        chkNorris = (CheckBoxPreference)getPreferenceScreen().findPreference("cb_norris");
        chkNorris.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    if (chkNorris.isChecked()) {
                        Toast.makeText(Preferences.this, getString(R.string.cb_norris_enabled), Toast.LENGTH_SHORT).show();
                        return true;
                    } else {
                        Toast.makeText(Preferences.this, getString(R.string.cb_norris_disabled), Toast.LENGTH_SHORT).show();
                        return false;
                    }
                }});

        chkExtCache = (CheckBoxPreference)getPreferenceScreen().findPreference("cb_extcache");
        chkExtCache.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    /* Delete cache in current location before changing it */
                    Cryptonite.cleanCache(getBaseContext());
                    
                    /* Make sure the external storage is still available */
                    if (!chkExternal()) {
                        return false;
                    }
                    
                    if (chkExtCache.isChecked()) {
                        String extDir = Cryptonite.getExternalCacheDir(getBaseContext()).getPath();
                        Toast.makeText(Preferences.this, getString(R.string.cb_extcache_enabled) + extDir, 
                                Toast.LENGTH_SHORT).show();
                        return true;
                    } else {
                        String intDir = getBaseContext().getDir(DirectorySettings.OPENPNT, 
                                Context.MODE_WORLD_WRITEABLE).getParent();
                        Toast.makeText(Preferences.this, getString(R.string.cb_extcache_disabled) + intDir, 
                                Toast.LENGTH_SHORT).show();
                        return false;
                    }
                }});
        
        chkExternal();
        
        chkAppFolder = (CheckBoxPreference)getPreferenceScreen().findPreference("cb_appfolder");
        chkAppFolder.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    if (chkAppFolder.isChecked()) {
                        Toast.makeText(Preferences.this, R.string.cb_appfolder_enabled, Toast.LENGTH_SHORT).show();
                        return true;
                    } else {
                        Toast.makeText(Preferences.this, R.string.cb_appfolder_disabled, Toast.LENGTH_SHORT).show();
                        return false;
                    }
                }});
        
        chkAnyKey = (CheckBoxPreference)getPreferenceScreen().findPreference("cb_anykey");
        chkAnyKey.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    if (chkAnyKey.isChecked()) {
                        Toast.makeText(Preferences.this, R.string.cb_anykey_enabled, Toast.LENGTH_SHORT).show();
                        return true;
                    } else {
                        Toast.makeText(Preferences.this, R.string.cb_anykey_disabled, Toast.LENGTH_SHORT).show();
                        return false;
                    }
                }});
        
        txtMntPoint = getPreferenceScreen().findPreference("txt_mntpoint");
        txtMntPoint.setSummary(prefs.getString("txt_mntpoint", Cryptonite.defaultMntDir()));
        txtMntPoint.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                // Don't change mount point while a volume is mounted:
                if (ShellUtils.isMounted("fuse.encfs")) {
                    Toast.makeText(Preferences.this, R.string.umount_first,
                            Toast.LENGTH_LONG).show();
                    return false;
                }
                String dialogLabel = getString(R.string.select_mount);
                String dialogButtonLabel = getString(R.string.select_mount_short);
                int dialogMode = SelectionMode.MODE_OPEN_CREATE;
                String dialogStartPath = "/";
                if (Cryptonite.externalStorageIsWritable()) {
                    dialogStartPath = Environment
                            .getExternalStorageDirectory()
                            .getPath();
                }
                String dialogRoot = "/";
                String dialogRootName = dialogRoot;
                launchBuiltinFileBrowser(dialogRoot, dialogRootName,
                        dialogButtonLabel, dialogStartPath, dialogLabel, dialogMode);
                
                return false;
            }
        });
        String encfsBin = getFilesDir().getParentFile().getPath() + "/encfs";        
        if (!ShellUtils.supportsFuse() || !new File(encfsBin).exists()) {
            txtMntPoint.setEnabled(false);
            txtMntPoint.setSummary(R.string.mount_info_unsupported);
        }

        clearCache = getPreferenceScreen().findPreference("txt_clearcache");
        clearCache.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                Cryptonite.cleanCache(getBaseContext());
                Toast.makeText(Preferences.this, R.string.txt_clearcache_enabled, Toast.LENGTH_SHORT).show();
                return true;
            }
        });
        

    }

    private boolean chkExternal() {
        /* Do we have external storage at all? */
        if (!Cryptonite.externalStorageIsWritable() || Cryptonite.getExternalCacheDir(getBaseContext())==null) {
            chkExtCache.setEnabled(false);
            chkExtCache.setChecked(false);
            chkExtCache.setSummary(getBaseContext().getString(R.string.cb_no_external_storage));
            return false;
        } else {
            return true;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        chkExternal();
    }
    
    /** Called upon exit from other activities */
    public synchronized void onActivityResult(final int requestCode,
                                              int resultCode, final Intent data) {

        switch (requestCode) {
        case SelectionMode.MODE_OPEN_CREATE:
            /* file dialog */
            if (resultCode == Activity.RESULT_OK && data != null) {
                currentReturnPath = data.getStringExtra(FileDialog.RESULT_EXPORT_PATHS);
                if (currentReturnPath != null) {
                    File newMntDir = new File(currentReturnPath);
                    if (Cryptonite.isValidMntDir(Preferences.this, newMntDir)) {
                        SharedPreferences prefs = getSharedPreferences(Cryptonite.ACCOUNT_PREFS_NAME, 0);
                        Editor prefEdit = prefs.edit();
                        prefEdit.putString("txt_mntpoint", currentReturnPath);
                        prefEdit.commit();
                        txtMntPoint.setSummary(prefs.getString("txt_mntpoint", Cryptonite.defaultMntDir()));
                    }
                }
            }
            break;
        default:
            Log.e(Cryptonite.TAG, "Unknown request code");
        }
    }
    
    private void launchBuiltinFileBrowser(String dialogRoot, String dialogRootName,
            String dialogButtonLabel, String dialogStartPath, String dialogLabel, int dialogMode) 
    {
        Intent intent = new Intent(getBaseContext(), FileDialog.class);
        intent.putExtra(FileDialog.CURRENT_ROOT, dialogRoot);
        intent.putExtra(FileDialog.CURRENT_ROOT_NAME, dialogRootName);
        intent.putExtra(FileDialog.BUTTON_LABEL, dialogButtonLabel);
        intent.putExtra(FileDialog.START_PATH, dialogStartPath);
        intent.putExtra(FileDialog.LABEL, dialogLabel);
        intent.putExtra(FileDialog.SELECTION_MODE, dialogMode);
        startActivityForResult(intent, dialogMode);
    }
    
}
