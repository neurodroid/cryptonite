package csh.cryptonite.storage;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import csh.cryptonite.Cryptonite;
import csh.cryptonite.R;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;

public class LocalStorage extends Storage {

    public LocalStorage(Context context) {
        super(context);
        type = "local";
    }

    @Override
    public boolean uploadEncFSFile(String encFSFilePath, String fileRoot,
            String dbEncFSPath, String srcPath) {
        File srcFile = new File(srcPath);
        if (!srcFile.isFile()) {
            handleUIRequest(mContext.getString(R.string.only_files));
            return false;
        }
        String srcFileName = srcFile.getName();
        
        /* normalise path names */
        final String bRoot = new File(fileRoot).getPath();
        String bPath = new File(encFSFilePath).getPath();
        String stripstrtmp = bPath.substring(bRoot.length()) + "/" + srcFileName;
        if (!stripstrtmp.startsWith("/")) {
            stripstrtmp = "/" + stripstrtmp;
        }
        final String stripstr = stripstrtmp;

        /* Convert current path to encoded file name */
        final String encodedPath = Cryptonite.jniEncode(stripstr);
        final File encodedFile = new File(encodedPath);
        final String fSrcPath = srcPath;
        /* Does the encrypted file exist? */
        if (encodedFile.exists()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setIcon(R.drawable.ic_launcher_cryptonite)
                .setTitle(R.string.file_exists)
                .setMessage(R.string.file_exists_options_short)
                .setPositiveButton(R.string.overwrite,
                        new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                            int which) {
                        if (Cryptonite.jniEncrypt(stripstr, fSrcPath, true) != Cryptonite.jniSuccess()) {
                            handleUIRequest(mContext.getString(R.string.upload_failure));
                        }
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
            return true;
        } else {
            return (Cryptonite.jniEncrypt(stripstr, srcPath, true) == Cryptonite.jniSuccess());
        }
    }

    @Override
    public boolean decryptEncFSFile(String encodedPath, String targetPath, String encfsPath) {
        /* Copy decrypted file */
        return (Cryptonite.jniDecrypt(encodedPath, targetPath, true) == Cryptonite.jniSuccess());
    }

    /** Walks a local file tree, copying decrypted files to a local
     * directory.
     * 
     * @param exportPaths Decoded file path array
     * @param exportRoot Root path up to encFS volume
     * @param destDir Destination directory
     * @param localEncFSPath encFS path from local root
     * @return true upon success
     */
    @Override
    public boolean export(String[] exportPaths, String exportRoot,
            String destDir, String encFSPath) {
        Set<String> errorList = new HashSet<String>();

        for (String path : exportPaths) {
            localRecTree(path, errorList, exportRoot, destDir, encFSPath);
        }

        return errorList.size() == 0;
    }
    
    @Override
    public boolean createEncFS(String currentReturnPath, String passwordString, 
            File browseRoot, int config) {
        String encfs6Path = currentReturnPath + "/" + ".encfs6.xml";
        if (new File(encfs6Path).exists()) {
            handleUIRequest(mContext.getString(R.string.encfs6_exists));
            return false;
        }
        if (Cryptonite.jniCreate(currentReturnPath, passwordString, config) == Cryptonite.jniSuccess()) {
            handleUIRequest(mContext.getString(R.string.create_success_local));
        } else {
            handleUIRequest(mContext.getString(R.string.create_failure));
            return false;
        }
        return true;
    }
    
    /** Walks a local file tree, copying decrypted files to a local
     * directory.
     * 
     * @param currentPath Decoded file path
     * @param errorList File paths that couldn't be copied
     * @param exportRoot Root path up to encFS volume
     * @param destDir Destination directory
     * @param localEncFSPath encFS path from local root
     */
    private void localRecTree(String currentPath, Set<String> errorList, String exportRoot,
            String destDir, String localEncFSPath)
    {
        if (new VirtualFile(currentPath).exists()) {

            /* normalise path names */
            String bRoot = new VirtualFile(exportRoot).getPath();
            String bPath = new VirtualFile(currentPath).getPath();
            String stripstr = bPath.substring(bRoot.length());

            if (new VirtualFile(bPath).isDirectory()) {

                for (VirtualFile f : new VirtualFile(bPath).listFiles()) {
                    localRecTree(f.getPath(), errorList, exportRoot, destDir, localEncFSPath);
                }

            } else {
                /* Set up dir for decrypted file */
                String finalPath = destDir + (new File(bPath)).getParent().substring(bRoot.length());
                File finalDir = new File(finalPath);
                if (!finalDir.exists()) {
                    finalDir.mkdirs();
                }
                /* Convert current path to encoded file name */
                String encodedPath = Cryptonite.jniEncode(stripstr);

                if (Cryptonite.jniDecrypt(encodedPath, destDir, false) != Cryptonite.jniSuccess()) {
                    errorList.add(stripstr);
                    Log.e(Cryptonite.TAG, "Couldn't copy " + encodedPath + " to " + destDir);
                }
            }
        }
    }

}
