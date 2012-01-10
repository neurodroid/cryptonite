/*
 * Copyright (c) 2011, Christoph Schmidt-Hieber
 * Distributed under the modified 3-clause BSD license:
 * See the LICENSE file that accompanies this code.
 */

package csh.encfsandroid;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;

import java.util.Scanner;

import android.app.Activity;
import android.app.AlertDialog;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;

import android.os.Bundle;
import android.os.Environment;

import android.util.Log;

import android.view.View;
import android.view.View.OnClickListener;

import android.widget.Button;

public class EncFSAndroid extends Activity
{

    private static final int REQUEST_SAVE=0, REQUEST_LOAD=1, REQUEST_PREFS=2;
    public static final String MNTDIR = "/data/data/csh.encfsandroid/mnt";
    public static final String BINDIR = "/data/data/csh.encfsandroid";
    public static final String ENCFSBIN = BINDIR + "/encfs";
    public static final String TAG = "encfs-android";
    private String currentPath = "/";
    private boolean mExternalStorageAvailable = false;
    private boolean mExternalStorageWriteable = false;

    /** Called when the activity is first created. */
    @Override public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        getResources();

        /* Check sd card state */
        String state = Environment.getExternalStorageState();

        if (Environment.MEDIA_MOUNTED.equals(state)) {
            // We can read and write the media
            mExternalStorageAvailable = mExternalStorageWriteable = true;
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            // We can only read the media
            mExternalStorageAvailable = true;
            mExternalStorageWriteable = false;
        } else {
            // Something else is wrong. It may be one of many other states, but all we need
            //  to know is we can neither read nor write
            mExternalStorageAvailable = mExternalStorageWriteable = false;
        }

        if (!mExternalStorageAvailable || !mExternalStorageWriteable) {
            new AlertDialog.Builder(this)
                .setIcon(R.drawable.ic_launcher_encfsandroid)
                .setTitle(R.string.sdcard_not_writable)
                .setPositiveButton("OK",
                                   new DialogInterface.OnClickListener() {
                                       @Override
                                           public void onClick(DialogInterface dialog,
                                                               int which) {
                                           
                                       }
                                   }).show();
            finish();
        }

        /* Copy the encfs binary to binDir and make executable.
         */
        cpEncFSBin();

        /* Load hoc file using a simple file dialog */
        Button buttonLoadFile = (Button)findViewById(R.id.btnLoadFile);
        buttonLoadFile.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    Intent intent = new Intent(getBaseContext(),
                                               FileDialog.class);
                    intent.putExtra(FileDialog.START_PATH, "/");
                    startActivityForResult(intent, SelectionMode.MODE_OPEN);
                }});

    }

    /** Called upon exit from other activities */
    public synchronized void onActivityResult(final int requestCode,
                                              int resultCode, final Intent data) {

        switch (requestCode) {
         case REQUEST_SAVE:
         case SelectionMode.MODE_OPEN:
             /* file dialog */
             if (resultCode == Activity.RESULT_OK) {
                     
                 if (requestCode == REQUEST_SAVE) {
                     System.out.println("Saving...");
                 } else if (requestCode == REQUEST_LOAD) {
                     System.out.println("Loading...");
                 }
                     
                 currentPath = data.getStringExtra(FileDialog.RESULT_PATH);
                 Log.v(TAG, currentPath);
                     
             } else if (resultCode == Activity.RESULT_CANCELED) {
                 Log.v(TAG, "file not selected");
             }
             break;
         case REQUEST_PREFS:
             SharedPreferences prefs = getBaseContext().getSharedPreferences("csh.encfsandroid_preferences", 0);
             break;
         default:
             Log.e(TAG, "Unknown request code");
        }
    }

    /** Copy encfs to binDir and make executable */
    public void cpEncFSBin() {
        String arch = "armeabi";
        /* if (withVfp) {
            arch += "-v7a";
            } */
            
        File binDir = new File(BINDIR);
        if (!binDir.exists()) {
            throw new RuntimeException("Couldn't find binary directory");
        }

        /* Catenate split files */
        Log.v(TAG, "Looking for assets in " + arch);
        try {
            String[] assetsFiles = getAssets().list(arch);

            File newf = new File(ENCFSBIN);
            FileOutputStream os = new FileOutputStream(newf);
            for (String assetsFile : assetsFiles) {
                Log.v(TAG, "Found NEURON binary part: " + assetsFile);
                InputStream is = getAssets().open(arch + "/" + assetsFile);

                byte[] buffer = new byte[is.available()]; 

                is.read(buffer);

                os.write(buffer);

                is.close();
            }
            os.close();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        String[] chmodlist = {getChmod(), "755", ENCFSBIN};
        runBinary(chmodlist, BINDIR);
    }

    public static String runBinary(String[] binName, String nrnHome) {
        return runBinary(binName, nrnHome, false);
    }

    /** Run a binary using binDir as the wd. Return stdout
     *  and optinally stderr
     */
    public static String runBinary(String[] binName, String nrnHome, boolean stderr) {
        try {
            File binDir = new File(BINDIR);
            if (!binDir.exists()) {
                binDir.mkdirs();
            }
            
            /* Can't set the environment on Android <= 2.2 with
             * ProcessBuilder. Resorting back to old-school exec.
             */
            String[] envp = {"NEURONHOME="+nrnHome};
            Process process = Runtime.getRuntime().exec(binName, envp, binDir);
            process.waitFor();
            
            Scanner outscanner = new Scanner(process.getInputStream());
            Scanner errscanner = new Scanner(process.getErrorStream());
            String NL = System.getProperty("line.separator");
            
            String output = "";
            
            try {
                while (outscanner.hasNextLine()) {
                    output += outscanner.nextLine();
                    output += NL;
                }
            }
            finally {
                outscanner.close();
            }
            if (stderr) {
                output += NL + "stderr:" + NL;
                try {
                    while (errscanner.hasNextLine()) {
                        output += errscanner.nextLine() + NL;
                    }
                }
                finally {
                    errscanner.close();
                }
            }

            return output;

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getChmod() {
        String chmod = "/system/bin/chmod";
        if (!(new File(chmod)).exists()) {
            chmod = "/system/xbin/chmod";
            if (!(new File(chmod)).exists()) {
                throw new RuntimeException("Couldn't find chmod on your system");
            }
        }
        return chmod;
    }

}
