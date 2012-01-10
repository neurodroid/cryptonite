/*
 * Copyright (c) 2011, Christoph Schmidt-Hieber
 * Distributed under the modified 3-clause BSD license:
 * See the LICENSE file that accompanies this code.
 */

package csh.encfsandroid;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;

import java.util.Scanner;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;

import android.os.Bundle;
import android.os.Environment;

import android.util.Log;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;

import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class EncFSAndroid extends Activity
{

    private static final int REQUEST_SAVE=0, REQUEST_LOAD=1, REQUEST_PREFS=2;
    private static final int MY_PASSWORD_DIALOG_ID = 0;
    public static final String MNTPNT = "csh.encfsandroid/mnt";
    public static final String BINDIR = "/data/data/csh.encfsandroid";
    public static final String ENCFSBIN = BINDIR + "/encfs";
    public static final String TAG = "encfs-android";
    private String currentPath = "/";
    private ProgressDialog pd;
    private AlertDialog.Builder ad;
    private TextView tv;
    private String encfsversion, encfsoutput, cursrcdir;

    /** Called when the activity is first created. */
    @Override public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        ad = new AlertDialog.Builder(this);
        if (!supportsFuse()) {
            ad.setIcon(R.drawable.ic_launcher_encfsandroid);
            ad.setTitle(R.string.no_fuse);
            ad.setPositiveButton("OK",
                                 new DialogInterface.OnClickListener() {
                                     @Override
                                         public void onClick(DialogInterface dialog,
                                                             int which) {
                                         
                                         finish();
                                     }
                                 });
            ad.show();
        }
        
        setContentView(R.layout.main);

        getResources();

        /* Copy the encfs binary to binDir and make executable.
         */
        cpEncFSBin();

        /* Get version information from EncFS (goes to stderr) */
        String[] cmdlist = {ENCFSBIN, "--version"};
        encfsversion = runBinary(cmdlist, BINDIR, true);
        Log.v(TAG, "EncFS version: " + encfsversion);

        tv = (TextView)findViewById(R.id.txtOutput);
        tv.setText(encfsversion);
        
        
        /* Select source directory using a simple file dialog */
        Button buttonLoadFile = (Button)findViewById(R.id.btnLoadFile);
        buttonLoadFile.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    if (!externalStorageIsWritable()) {
                        ad.setIcon(R.drawable.ic_launcher_encfsandroid);
                        ad.setTitle(R.string.sdcard_not_writable);
                        ad.setPositiveButton("OK",
                                                  new DialogInterface.OnClickListener() {
                                                      @Override
                                                          public void onClick(DialogInterface dialog,
                                                                              int which) {
                                                          
                                                      }
                                                  });
                        ad.show();
                    } else {

                        Intent intent = new Intent(getBaseContext(),
                                                   FileDialog.class);
                        intent.putExtra(FileDialog.START_PATH, "/");
                        startActivityForResult(intent, SelectionMode.MODE_OPEN);
                    }
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

                 showDialog(MY_PASSWORD_DIALOG_ID);
                 
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
                Log.v(TAG, "Found EncFS binary part: " + assetsFile);
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

    public static String runBinary(String[] binName, String encfsHome) {
        return runBinary(binName, encfsHome, false);
    }

    /** Run a binary using binDir as the wd. Return stdout
     *  and optinally stderr
     */
    public static String runBinary(String[] binName, String encfsHome, boolean stderr) {
        try {
            File binDir = new File(BINDIR);
            if (!binDir.exists()) {
                binDir.mkdirs();
            }
            
            /* Can't set the environment on Android <= 2.2 with
             * ProcessBuilder. Resorting back to old-school exec.
             */
            String[] envp = {"NEURONHOME="+encfsHome};
            Process process = Runtime.getRuntime().exec(binName, envp, binDir);
            process.waitFor();
            
            String NL = System.getProperty("line.separator");
            
            String output = "";
            
            if (stderr) {
                Scanner errscanner = new Scanner(new BufferedInputStream(process.getErrorStream()));
                try {
                    while (errscanner.hasNextLine()) {
                        output += errscanner.nextLine() + NL;
                    }
                }
                finally {
                    errscanner.close();
                }
            } else {
                Scanner outscanner = new Scanner(new BufferedInputStream(process.getInputStream()));
                try {
                    while (outscanner.hasNextLine()) {
                        output += outscanner.nextLine();
                        output += NL;
                    }
                }
                finally {
                    outscanner.close();
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

    public void runEncfs(String[] options, String srcdir, String pwd) {
        tv.setText(encfsversion + "\n");
        tv.invalidate();
        pd = ProgressDialog.show(this,
                                 this.getString(R.string.wait_msg), this.getString(R.string.running_encfs), true);
        cursrcdir = srcdir;
        new Thread(new Runnable(){
                public void run(){
                    String[] cmdlist = {ENCFSBIN, cursrcdir};
                    encfsoutput = runBinary(cmdlist, BINDIR, false);
                    runOnUiThread(new Runnable(){
                            @Override public void run() {
                                if (pd.isShowing())
                                    pd.dismiss();
                                tv.setText(encfsversion + "\n" + encfsoutput);
                            }
                        });
                }
            }).start();
            
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
                         String strPassword = password.getText().toString();
                         Toast.makeText(EncFSAndroid.this,
                                        "password="+strPassword, Toast.LENGTH_SHORT).show();
                         removeDialog(MY_PASSWORD_DIALOG_ID);
                     }
                 });
             return builder.create();

             
        }

        return null;
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
    
    public static boolean supportsFuse() {
        return (new File("/dev/fuse")).exists();
    }

    public static boolean isMounted() throws IOException {
        /* Read mounted info */
        FileInputStream fis = new FileInputStream("/proc/mounts");
        Scanner scanner = new Scanner(fis);
        System.getProperty("line.separator");

        try {
            Log.v(TAG, "Parsing /proc/mounts for mounted encfs devices");
            while (scanner.hasNextLine()) {
                if (scanner.findInLine("fuse.encfs")!=null) {
                    scanner.close();
                    return true;
                }
                Log.v(TAG, scanner.nextLine());
            }
        }
        finally {
            scanner.close();
        }
        
        return false;
    }
    
}
