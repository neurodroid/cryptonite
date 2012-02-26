package csh.cryptonite.storage;

import java.io.File;
import java.io.IOException;

import csh.cryptonite.Cryptonite;
import csh.cryptonite.CryptoniteApp;
import csh.cryptonite.R;
import csh.cryptonite.UploadEncrypted;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

public abstract class Storage {

    public Context mCallingContext;
    public Context mAppContext;
    public CryptoniteApp mApp;
    public String type;
    
    private UIHandler uiHandler;
    
    public Storage(Context context, CryptoniteApp app) {
        mCallingContext = context;
        mAppContext = context.getApplicationContext();
        mApp = app;
        type = "";
        Thread uiThread = new HandlerThread("UIHandler");
        uiThread.start();
        uiHandler = new UIHandler(((HandlerThread) uiThread).getLooper());
    }
            
    abstract public boolean uploadEncFSFile(String stripstr, String srcPath);
    
    abstract public boolean decryptEncFSFile(String encodedPath, String targetPath, String encfsPath);
    
    abstract public boolean export(String[] exportPaths, String exportRoot, 
            String destDir, String encFSPath);

    abstract public boolean createEncFS(String currentReturnPath, String passwordString, 
            File browseRoot, int config);
    
    abstract public String encodedExists(String stripstr);
    
    public String stripStr(String encFSFilePath, String fileRoot, String srcPath) {
        File srcFile = new File(srcPath);
        if (!srcFile.isFile()) {
            handleUIToastRequest(mAppContext.getString(R.string.only_files));
            return "";
        }
        String srcFileName = srcFile.getName();

        /* normalise path names */
        String bRoot = new File(fileRoot).getPath();
        String bPath = new File(encFSFilePath).getPath();

        String stripstr = bPath.substring(bRoot.length()) + "/" + srcFileName;
        if (!stripstr.startsWith("/")) {
            stripstr = "/" + stripstr;
        }
        return stripstr;
    }
    
    /** Create an empty file with a decoded file name
     * 
     * @param encodedPath The full encoded source path
     * @param destRoot Root destination directory path
     * @param isDir true for directories
     * @throws IOException
     */
    public static void decode(String encodedPath, String destRoot, boolean isDir) throws IOException {

        Log.i(Cryptonite.TAG, "encodedPath is " + encodedPath);
        
        /* Decoded name */
        String decodedPath = Cryptonite.jniDecode(encodedPath);
        
        Log.i(Cryptonite.TAG, "Creating new file" + destRoot + "/" + decodedPath);
        VirtualFile file = new VirtualFile(destRoot + "/" + decodedPath);
        if (isDir) {
            file.mkdirs();
        } else {
            file.createNewFile();
        }
        
    }

    public static String fileExt(String url) {
        /* file name part: */
        String rawFileName = new File(url).getName();
        
        /* Does the file name have an extension at all? */
        if (rawFileName.lastIndexOf(".") == -1) {
            return "";
        }

        String ext = url.substring(url.lastIndexOf(".") );
        if (ext.indexOf("?")>-1) {
            ext = ext.substring(0,ext.indexOf("?"));
        }
        if (ext.indexOf("%")>-1) {
            ext = ext.substring(0,ext.indexOf("%"));
        }
        return ext;
    }
    
    public static String fileNameTrunk(String url) {
        /* file name part: */
        String rawFileName = new File(url).getName();
        
        /* Does the file name have an extension at all? */
        if (rawFileName.lastIndexOf(".") == -1) {
            return rawFileName;
        }
        
        String trunk = url.substring(0, url.lastIndexOf("."));
        return new File(trunk).getName();
    }
    
    public File getPrivateDir(String label, int mode) {
        /* Tear down and recreate the browse directory to make
         * sure we have appropriate permissions */
        File browseDirF = mAppContext.getDir(label, mode);
        if (browseDirF.exists()) {
            if (!Cryptonite.deleteDir(browseDirF)) {
                handleUIToastRequest(mAppContext.getString(R.string.target_dir_cleanup_failure));
                return null;
            }
        }
        browseDirF = mAppContext.getDir(label, mode);
        return browseDirF;
    }
    
    private final class UIHandler extends Handler
    {
        public static final int DISPLAY_UI_TOAST = 0;
        public static final int DISPLAY_UI_UPLOAD = 1;

        public UIHandler(Looper looper)
        {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg)
        {
            switch(msg.what)
            {
            case UIHandler.DISPLAY_UI_TOAST:
            {
                Toast t = Toast.makeText(mAppContext, (String)msg.obj, Toast.LENGTH_LONG);
                t.show();
                break;
            }
            case UIHandler.DISPLAY_UI_UPLOAD:
            {
                String[] paths = (String[])msg.obj;
                if (paths.length < 2) {
                    Toast t = Toast.makeText(mAppContext, R.string.upload_failure, Toast.LENGTH_LONG);
                    t.show();
                    break;
                }
                String targetPath = paths[0];
                String encodedFilePath = paths[1];
                UploadEncrypted upload = new UploadEncrypted(mCallingContext, 
                        mApp.getDBApi(),
                        new File(targetPath).getParent() + "/",
                        new File(encodedFilePath));
                upload.execute();
                break;
            }
            default:
                break;
            }
        }
    }

    protected void handleUIToastRequest(String message)
    {
        Message msg = uiHandler.obtainMessage(UIHandler.DISPLAY_UI_TOAST);
        msg.obj = message;
        uiHandler.sendMessage(msg);
    }

    protected void handleUIUploadEncrypted(String[] filePaths)
    {
        Message msg = uiHandler.obtainMessage(UIHandler.DISPLAY_UI_UPLOAD);
        msg.obj = filePaths;
        uiHandler.sendMessage(msg);
    }


}
