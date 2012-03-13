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

import android.content.Context;

import android.os.Bundle;

import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.CheckBoxPreference;
import android.preference.Preference.OnPreferenceClickListener;

import android.widget.Toast;

public class Preferences extends PreferenceActivity {

    private CheckBoxPreference chkEnableBuiltin;
    private CheckBoxPreference chkNorris;
    private CheckBoxPreference chkExtCache;
    private CheckBoxPreference chkAppFolder;
    
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        
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
                    if (chkExtCache.isChecked()) {
                        String extDir = Cryptonite.getExternalCacheDir(getBaseContext()).getPath();
                        Toast.makeText(Preferences.this, getString(R.string.cb_extcache_enabled) + extDir, 
                                Toast.LENGTH_SHORT).show();
                        return true;
                    } else {
                        String intDir = getBaseContext().getDir(Cryptonite.OPENPNT, 
                                Context.MODE_WORLD_WRITEABLE).getParent();
                        Toast.makeText(Preferences.this, getString(R.string.cb_extcache_disabled) + intDir, 
                                Toast.LENGTH_SHORT).show();
                        return false;
                    }
                }});
        /* Do we have external storage at all? */
        if (!Cryptonite.externalStorageIsWritable()) {
            chkExtCache.setEnabled(false);
            chkExtCache.setChecked(false);
            chkExtCache.setSummary(getBaseContext().getString(R.string.cb_no_external_storage));
        }
        
        chkAppFolder = (CheckBoxPreference)getPreferenceScreen().findPreference("cb_appfolder");
        chkAppFolder.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    if (chkAppFolder.isChecked()) {
                        Toast.makeText(Preferences.this, getString(R.string.cb_appfolder_enabled), Toast.LENGTH_SHORT).show();
                        return true;
                    } else {
                        Toast.makeText(Preferences.this, getString(R.string.cb_appfolder_disabled), Toast.LENGTH_SHORT).show();
                        return false;
                    }
                }});

    }
}
