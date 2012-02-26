package csh.cryptonite.storage;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import csh.cryptonite.Cryptonite;
import csh.cryptonite.CryptoniteApp;
import csh.cryptonite.R;
import android.content.Context;
import android.util.Log;

public class LocalStorage extends Storage {

    public LocalStorage(Context context, CryptoniteApp app) {
        super(context, app);
        type = "local";
    }

    @Override
    public String encodedExists(String stripstr) {
        /* Convert current path to encoded file name */
        String encodedPath = Cryptonite.jniEncode(stripstr);
        final File encodedFile = new File(encodedPath);

        /* Does the encrypted file exist? */
        if (encodedFile.exists()) {
            /* get next available file name */
            File decodedFile = new File(stripstr);

            String decodedFileParent = decodedFile.getParent() + "/";
            String decodedFileTrunk = decodedFileParent + 
                    fileNameTrunk(decodedFile.getPath());
            String decodedFileExt = fileExt(decodedFile.getPath());
            int ntry = 1;
            while (true) {
                String nextFilePath = decodedFileTrunk + " (" + ntry + ")" +
                        decodedFileExt;
                /* encode */
                String nextEncodedPath = Cryptonite.jniEncode(nextFilePath);
                File nextEncodedFile = new File(nextEncodedPath);
                if (!nextEncodedFile.exists()) {
                    return nextFilePath;
                }
                ntry++;
            }
        } else {
            return stripstr;
        }
    }
    
    @Override
    public boolean uploadEncFSFile(String stripstr, String srcPath) {
        return (Cryptonite.jniEncrypt(stripstr, srcPath, true) == Cryptonite.jniSuccess());
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
            handleUIToastRequest(mAppContext.getString(R.string.encfs6_exists));
            return false;
        }
        if (Cryptonite.jniCreate(currentReturnPath, passwordString, config) == Cryptonite.jniSuccess()) {
            handleUIToastRequest(mAppContext.getString(R.string.create_success_local));
        } else {
            handleUIToastRequest(mAppContext.getString(R.string.create_failure));
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
        /* normalise path names */
        String bRoot = new VirtualFile(exportRoot).getPath();
        String bPath = new VirtualFile(currentPath).getPath();
        String stripstr = bPath.substring(bRoot.length());

        /* Convert current path to encoded file name */
        String encodedPath = Cryptonite.jniEncode(stripstr);
        
        /* Does the _en_coded filename exist? */
        if (new File(encodedPath).exists()) {

            if (new File(encodedPath).isDirectory()) {
                /* get _en_coded file name list */
                for (File f : new File(encodedPath).listFiles()) {
                    /* convert to _de_coded file name */
                    String decodedPath = Cryptonite.jniDecode(f.getPath());
                    localRecTree(bRoot + "/" + decodedPath, errorList, exportRoot, destDir, localEncFSPath);
                }

            } else {
                /* Set up dir for decrypted file */
                String finalPath = destDir + (new File(bPath)).getParent().substring(bRoot.length());
                File finalDir = new File(finalPath);
                if (!finalDir.exists()) {
                    finalDir.mkdirs();
                }

                if (Cryptonite.jniDecrypt(encodedPath, destDir, false) != Cryptonite.jniSuccess()) {
                    errorList.add(stripstr);
                    Log.e(Cryptonite.TAG, "Couldn't copy " + encodedPath + " to " + destDir);
                }
            }
        }
    }

}
