/*
 * Copyright (c) 2011, Christoph Schmidt-Hieber
 * Distributed under the modified 3-clause BSD license:
 * See the LICENSE file that accompanies this code.
 */

package csh.cryptonite;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
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

public class Cryptonite extends Activity
{

    private static final int REQUEST_SAVE=0, REQUEST_LOAD=1, REQUEST_PREFS=2;
    private static final int MOUNT_MODE=0, BROWSE_MODE=1;
    private static final int MY_PASSWORD_DIALOG_ID = 0;
    public static final String MNTPNT = "/csh.cryptonite/mnt";
    public static final String BINDIR = "/data/data/csh.cryptonite";
    public static final String ENCFSBIN = BINDIR + "/encfs";
    public static final String ENCFSCTLBIN = BINDIR + "/encfsctl";
    public static final String TAG = "cryptonite";
    private String currentPath = "/";
    private String curPassword = "";
    private String mntDir = "/sdcard" + MNTPNT;
    private ProgressDialog pd;
    private AlertDialog.Builder ad;
    private TextView tv;
    private TextView tvMountInfo;
    private String encfsversion, encfsoutput, cursrcdir, fdlabel;
    private Button buttonBrowse, buttonMount, buttonUnmount;
    private int op_mode = -1;
    
    /** Called when the activity is first created. */
    @Override public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        getResources();

        if (externalStorageIsWritable()) {
            mntDir = Environment.getExternalStorageDirectory().getPath() + MNTPNT;
            File mntDirF = new File(mntDir);
            if (!mntDirF.exists()) {
                mntDirF.mkdirs();
            }
        }

        tvMountInfo = (TextView)findViewById(R.id.tvMountInfo);
        if (!externalStorageIsWritable() || !supportsFuse()) {
            tv.setText(this.getString(R.string.mount_info_unsupported));
        }
        
        /* Copy the encfs binaries to binDir and make executable.
         */
        pd = ProgressDialog.show(this,
                                 this.getString(R.string.wait_msg),
                                 this.getString(R.string.copying_bins), true);
        new Thread(new Runnable(){
                public void run(){
                    cpEncFSBin();
                    runOnUiThread(new Runnable(){
                            @Override public void run() {
                                if (pd.isShowing())
                                    pd.dismiss();
                                /* Get version information from EncFS */
                                encfsversion = "EncFS " + encfsVersion();
                                Log.v(TAG, "EncFS version: " + encfsVersion());
                                tv = (TextView)findViewById(R.id.tvVersion);
                                tv.setText(encfsversion);
                            }
                        });
                }
            }).start();
        
        /* Select source directory using a simple file dialog */
        buttonBrowse = (Button)findViewById(R.id.btnBrowse);
        fdlabel = this.getString(R.string.select_enc);
        buttonBrowse.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    op_mode = BROWSE_MODE;
                    if (!externalStorageIsWritable()) {
                        ad.setIcon(R.drawable.ic_launcher_cryptonite);
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
                        intent.putExtra(FileDialog.LABEL, fdlabel);
                        startActivityForResult(intent, SelectionMode.MODE_OPEN);
                    }
                }});

        /* Select source directory using a simple file dialog */
        buttonMount = (Button)findViewById(R.id.btnMount);
        fdlabel = this.getString(R.string.select_enc);
        buttonMount.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    op_mode = MOUNT_MODE;
                    if (!externalStorageIsWritable()) {
                        ad.setIcon(R.drawable.ic_launcher_cryptonite);
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
                        intent.putExtra(FileDialog.LABEL, fdlabel);
                        startActivityForResult(intent, SelectionMode.MODE_OPEN);
                    }
                }});

        /* Select source directory using a simple file dialog */
        buttonUnmount = (Button)findViewById(R.id.btnUnmount);
        buttonUnmount.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    String[] umountlist = {"umount", mntDir};
                    runBinary(umountlist, BINDIR, null, true);
                    updateButtons();
                }});
        updateButtons();
    }

    private void updateButtons() {
        boolean ism = isMounted();
        boolean sf = supportsFuse();
        Log.v(TAG, "EncFS mount state: " + ism + " FUSE support: " + sf);
        buttonMount.setEnabled(!ism && sf);
        buttonUnmount.setEnabled(ism && sf);
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

                 showDialog(MY_PASSWORD_DIALOG_ID);

             } else if (resultCode == Activity.RESULT_CANCELED) {
                 Log.v(TAG, "file not selected");
             }
             break;
         case REQUEST_PREFS:
             SharedPreferences prefs = getBaseContext().getSharedPreferences("csh.cryptonite_preferences", 0);
             break;
         default:
             Log.e(TAG, "Unknown request code");
        }
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
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        String[] chmodlist = {getChmod(), "755", binName};
        runBinary(chmodlist, BINDIR);
    }

    public static String runBinary(String[] binName, String encfsHome) {
        return runBinary(binName, encfsHome, null, false);
    }

    /** Run a binary using binDir as the wd. Return stdout
     *  and optinally stderr
     */
    public static String runBinary(String[] binName, String encfsHome, String toStdIn, boolean root) {
        try {
            File binDir = new File(BINDIR);
            if (!binDir.exists()) {
                binDir.mkdirs();
            }
            
            String NL = System.getProperty("line.separator");
            ProcessBuilder pb = new ProcessBuilder(binName);
            pb.directory(binDir);
            pb.redirectErrorStream(true);
            Process process;
            
            if (root) {
                String[] sucmd = {"su", "-c", join(binName, " ")};
                pb.command(sucmd);
                process = pb.start();
            } else {
                pb.command(binName);
                process = pb.start();
            }
            
            if (toStdIn != null) {
                BufferedWriter writer = new BufferedWriter(
                                                           new OutputStreamWriter(process.getOutputStream()) );
                writer.write(toStdIn + "\n");
                writer.flush();
            }

            process.waitFor();
                
            String output = "";
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

    public void runEncFS(String srcdir, String pwd) {
        tv.setText(encfsversion);
        tv.invalidate();
        pd = ProgressDialog.show(this,
                                 this.getString(R.string.wait_msg),
                                 this.getString(R.string.running_encfs), true);
        cursrcdir = srcdir;
        Log.v(TAG, "Running encfs with" + srcdir + mntDir);
        new Thread(new Runnable(){
                public void run(){
                    String[] cmdlist = {ENCFSBIN, "--stdinpass", cursrcdir, mntDir};
                    encfsoutput = runBinary(cmdlist, BINDIR, curPassword, true);
                    runOnUiThread(new Runnable(){
                            @Override public void run() {
                                if (pd.isShowing())
                                    pd.dismiss();
                                if (encfsoutput.length() > 0) {
                                    tv.setText(encfsversion + "\n" + encfsoutput);
                                }
                                updateButtons();
                            }
                        });
                }
            }).start();
            
    }
    
    public static String join(String[] sa, String delimiter) {
        Collection s = Arrays.asList(sa);
        StringBuffer buffer = new StringBuffer();
        Iterator iter = s.iterator();
        while (iter.hasNext()) {
            buffer.append(iter.next());
            if (iter.hasNext()) {
                buffer.append(delimiter);
            }
        }
        return buffer.toString();
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
                         curPassword = password.getText().toString();
                         Log.v(TAG, curPassword);
                         removeDialog(MY_PASSWORD_DIALOG_ID);
                         if (op_mode == MOUNT_MODE) {
                             runEncFS(currentPath, curPassword);
                         }
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

    public static boolean isMounted() {
        boolean isMounted = false;
        try {
            /* Read mounted info */
            FileInputStream fis = new FileInputStream("/proc/mounts");
            Scanner scanner = new Scanner(fis);
            try {
                Log.v(TAG, "Parsing /proc/mounts for mounted encfs devices");
                while (scanner.hasNextLine()) {
                    if (!isMounted && scanner.findInLine("fuse.encfs")!=null) {
                        Log.v(TAG, "Found mounted EncFS volume");
                        isMounted = true;
                    }
                    Log.v(TAG, scanner.nextLine());
                }
            } finally {
                scanner.close();
            }
        } catch (IOException e) {
            return isMounted;
        }
        return isMounted;
    }

    /* Native methods are implemented by the
     * 'cryptonite' native library, which is packaged
     * with this application.
     */
    public native int     encfsMount();
    public native String  encfsVersion();
    
    /* this is used to load the 'cryptonite' library on application
     * startup. The library has already been unpacked into
     * /data/data/csh.cryptonite/lib/libcryptonite.so at
     * installation time by the package manager.
     */
    static {
        System.loadLibrary("cryptonite");
    }

}
