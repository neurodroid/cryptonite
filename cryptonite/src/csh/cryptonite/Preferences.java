/*
 * Copyright (c) 2011, Christoph Schmidt-Hieber
 * Distributed under the modified 3-clause BSD license:
 * See the LICENSE file that accompanies this code.
 */

package csh.cryptonite;

import java.io.IOException;

import android.content.SharedPreferences;

import android.os.Bundle;

import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.CheckBoxPreference;
import android.preference.Preference.OnPreferenceClickListener;

import android.widget.Toast;

public class Preferences extends PreferenceActivity {

    private SharedPreferences prefs;
    
    private CheckBoxPreference chkEnableBuiltin;
    
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        /* Get previous states */
        prefs = getBaseContext().getSharedPreferences("csh.cryptonite_preferences", 0);
        boolean prevBuiltin = prefs.getBoolean("cb_builtin", false);
        
        chkEnableBuiltin = (CheckBoxPreference)getPreferenceScreen().findPreference("cb_builtin");
        /* Initialise builtin file browser status */
        chkEnableBuiltin.setChecked(prevBuiltin);
        chkEnableBuiltin.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    if (chkEnableBuiltin.isChecked()) {
                        Toast.makeText(Preferences.this, "Always using built-in file browser", Toast.LENGTH_SHORT).show();
                        return true;
                    } else {
                        Toast.makeText(Preferences.this, "Using OpenIntents File Browser (if available)", Toast.LENGTH_SHORT).show();
                        return false;
                    }
                }});
    }
}
