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

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
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

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
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

import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class Cryptonite extends Activity
{

    private static final int REQUEST_PREFS=0, REQUEST_CODE_PICK_FILE_OR_DIRECTORY=1;
    private static final int MOUNT_MODE=0, BROWSE_MODE=1, DROPBOX_MODE=2, VIEWMOUNT_MODE=3;
    private static final int DIRPICK_MODE=0, FILEPICK_MODE=1;
    private static final int MY_PASSWORD_DIALOG_ID = 0;
    private static final int DIALOG_MARKETNOTFOUND=1, DIALOG_OI_UNAVAILABLE=2;
    public static final String MNTPNT = "/csh.cryptonite/mnt";
    public static final String BINDIR = "/data/data/csh.cryptonite";
    public static final String ENCFSBIN = BINDIR + "/encfs";
    public static final String ENCFSCTLBIN = BINDIR + "/encfsctl";
    public static final String TAG = "cryptonite";
    private String currentReturnPath = "/";
    private String currentDialogStartPath = "/";
    private String curPassword = "";
    private String mntDir = "/sdcard" + MNTPNT;
    private ProgressDialog pd;
    private AlertDialog.Builder ad;
    private TextView tv;
    private TextView tvMountInfo;
    private String encfsversion, encfsoutput, cursrcdir, fdlabel;
    private Button buttonDropbox, buttonBrowse, buttonMount, buttonUnmount, buttonViewMount;
    private int opMode = -1;
    
    /** Called when the activity is first created. */
    @Override public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        getResources();

        encfsversion = "EncFS " + encfsVersion();

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
        if (!(new File(ENCFSBIN)).exists()) {
            pd = ProgressDialog.show(this,
                                     this.getString(R.string.wait_msg),
                                     this.getString(R.string.copying_bins), true);
            new Thread(new Runnable(){
                    public void run(){
                        cpEncFSBin();
                        runOnUiThread(new Runnable(){
                                public void run() {
                                    if (pd.isShowing())
                                        pd.dismiss();
                                    /* Get version information from EncFS */
                                    Log.v(TAG, encfsversion);
                                    tv = (TextView)findViewById(R.id.tvVersion);
                                    tv.setText(encfsversion);
                                }
                            });
                    }
                }).start();
        }
        
        /* Select source directory using a simple file dialog */
        buttonDropbox = (Button)findViewById(R.id.btnDropbox);
        fdlabel = this.getString(R.string.select_enc);
        buttonDropbox.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    opMode = DROPBOX_MODE;
                    if (!externalStorageIsWritable()) {
                        ad.setIcon(R.drawable.ic_launcher_cryptonite);
                        ad.setTitle(R.string.sdcard_not_writable);
                        ad.setPositiveButton("OK",
                                                  new DialogInterface.OnClickListener() {
                                                      public void onClick(DialogInterface dialog,
                                                                              int which) {
                                                          
                                                      }
                                                  });
                        ad.show();
                    } else {
                    }
                }});

        buttonDropbox.setEnabled(false);
        
        /* Select source directory using a simple file dialog */
        buttonBrowse = (Button)findViewById(R.id.btnBrowse);
        fdlabel = this.getString(R.string.select_enc);
        buttonBrowse.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    opMode = BROWSE_MODE;
                    if (externalStorageIsWritable()) {
                        currentDialogStartPath = Environment.getExternalStorageDirectory().getPath();
                    } else {
                        currentDialogStartPath = "/";
                    }
                    if (!externalStorageIsWritable()) {
                        ad.setIcon(R.drawable.ic_launcher_cryptonite);
                        ad.setTitle(R.string.sdcard_not_writable);
                        ad.setPositiveButton("OK",
                                                  new DialogInterface.OnClickListener() {
                                                      public void onClick(DialogInterface dialog,
                                                                              int which) {
                                                          
                                                      }
                                                  });
                        ad.show();
                    } else {
                        launchFileBrowser(DIRPICK_MODE);
                    }
                }});

        buttonBrowse.setEnabled(false);
        
        /* Select source directory using a simple file dialog */
        buttonMount = (Button)findViewById(R.id.btnMount);
        fdlabel = this.getString(R.string.select_enc);
        buttonMount.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    opMode = MOUNT_MODE;
                    if (externalStorageIsWritable()) {
                        currentDialogStartPath = Environment.getExternalStorageDirectory().getPath();
                    } else {
                        currentDialogStartPath = "/";
                    }
                    if (!externalStorageIsWritable()) {
                        ad.setIcon(R.drawable.ic_launcher_cryptonite);
                        ad.setTitle(R.string.sdcard_not_writable);
                        ad.setPositiveButton("OK",
                                                  new DialogInterface.OnClickListener() {
                                                      public void onClick(DialogInterface dialog,
                                                                              int which) {
                                                          
                                                      }
                                                  });
                        ad.show();
                    } else {
                        launchFileBrowser(DIRPICK_MODE);
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

        /* Select source directory using a simple file dialog */
        buttonViewMount = (Button)findViewById(R.id.btnViewMount);
        fdlabel = this.getString(R.string.select_enc);
        buttonViewMount.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    opMode = VIEWMOUNT_MODE;
                    currentDialogStartPath = mntDir;
                    if (!externalStorageIsWritable()) {
                        ad.setIcon(R.drawable.ic_launcher_cryptonite);
                        ad.setTitle(R.string.sdcard_not_writable);
                        ad.setPositiveButton("OK",
                                                  new DialogInterface.OnClickListener() {
                                                      public void onClick(DialogInterface dialog,
                                                                              int which) {
                                                          
                                                      }
                                                  });
                        ad.show();
                    } else {
                        launchFileBrowser(FILEPICK_MODE);
                    }
                }});
        updateButtons();
    }

    private void updateButtons() {
        boolean ism = isMounted();
        boolean sf = supportsFuse();
        Log.v(TAG, "EncFS mount state: " + ism + " FUSE support: " + sf);
        buttonMount.setEnabled(!ism && sf);
        buttonUnmount.setEnabled(ism && sf);
        buttonViewMount.setEnabled(ism && sf);
    }

    /** Called upon exit from other activities */
    public synchronized void onActivityResult(final int requestCode,
                                              int resultCode, final Intent data) {

        switch (requestCode) {
         case SelectionMode.MODE_OPEN:
             /* file dialog */
             if (resultCode == Activity.RESULT_OK) {
                 currentReturnPath = data.getStringExtra(FileDialog.RESULT_PATH);
                 if (currentReturnPath != null && opMode == MOUNT_MODE || opMode == BROWSE_MODE) {
                     showDialog(MY_PASSWORD_DIALOG_ID);
                 }
             } else if (resultCode == Activity.RESULT_CANCELED) {
                 Log.v(TAG, "file not selected");
             }
             break;
         case REQUEST_PREFS:
             @SuppressWarnings("unused")
             SharedPreferences prefs = getBaseContext().getSharedPreferences("csh.cryptonite_preferences", 0);
             break;
         case REQUEST_CODE_PICK_FILE_OR_DIRECTORY:
             if (resultCode == RESULT_OK && data != null) {
                 // obtain the filename
                 Uri fileUri = data.getData();
                 if (fileUri != null) {
                     currentReturnPath = fileUri.getPath();
                     if (currentReturnPath != null && opMode == MOUNT_MODE || opMode == BROWSE_MODE) {
                         showDialog(MY_PASSWORD_DIALOG_ID);
                     }
                 }
             }
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
                    String[] cmdlist = {ENCFSBIN, "--public", "--stdinpass", cursrcdir, mntDir};
                    encfsoutput = runBinary(cmdlist, BINDIR, curPassword, true);
                    runOnUiThread(new Runnable(){
                            public void run() {
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
        Collection<String> s = Arrays.asList(sa);
        StringBuffer buffer = new StringBuffer();
        Iterator<String> iter = s.iterator();
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
                         removeDialog(MY_PASSWORD_DIALOG_ID);
                         if (opMode == MOUNT_MODE) {
                             runEncFS(currentReturnPath, curPassword);
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

    private void launchFileBrowser(int mode) {
        SharedPreferences prefs = getBaseContext().getSharedPreferences("csh.cryptonite_preferences", 0);
        boolean useBuiltin = prefs.getBoolean("cb_builtin", false);
        if (!useBuiltin) {
            // Note the different intent: PICK_DIRECTORY
            String oiIntent = "org.openintents.action.PICK_FILE";
            String btnLabel = this.getString(R.string.back);
            String fdtitle = "OI File Manager";
            if (mode == DIRPICK_MODE) {
                oiIntent = "org.openintents.action.PICK_DIRECTORY";
                fdtitle = this.getString(R.string.select_enc);
                btnLabel = this.getString(R.string.select_enc_short);
            }
            fdlabel = fdtitle; // To make it generally available
            Intent intent = new Intent(oiIntent);

            // Construct URI from file name.
            File file = new File(currentDialogStartPath);
            intent.setData(Uri.fromFile(file));

            intent.putExtra("org.openintents.extra.TITLE", fdtitle);
            intent.putExtra("org.openintents.extra.BUTTON_TEXT", btnLabel);

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
        Intent intent = new Intent(getBaseContext(),
                                   FileDialog.class);
        intent.putExtra(FileDialog.START_PATH, currentDialogStartPath);
        intent.putExtra(FileDialog.LABEL, fdlabel);
        startActivityForResult(intent, SelectionMode.MODE_OPEN);
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
