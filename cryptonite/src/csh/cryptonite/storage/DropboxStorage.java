package csh.cryptonite.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.DropboxAPI.UploadRequest;
import com.dropbox.client2.exception.DropboxException;

import csh.cryptonite.Cryptonite;
import csh.cryptonite.CryptoniteApp;
import csh.cryptonite.R;
import android.content.Context;
import android.util.Log;

public class DropboxStorage extends Storage {

    public DropboxStorage(Context context, CryptoniteApp app) {
        super(context, app);
        type = "dropbox";
    }
    
    @Override
    public String encodedExists(String stripstr) {

        /* Upload file to DB */
        File browseRoot = mAppContext.getDir(Cryptonite.BROWSEPNT, Context.MODE_PRIVATE);

        /* Convert current path to encoded file name */
        String encodedPath = Cryptonite.jniEncode(stripstr);
        File encodedFile = new File(encodedPath);
        String targetPath = encodedPath.substring(browseRoot.getPath().length());

        /* Does the _en_crypted file exist on Dropbox? */
        String dbPath = new File(targetPath).getParent() + "/" + encodedFile.getName();
        boolean fileExists = true;
        try {
            fileExists = mApp.dbFileExists(dbPath);
        } catch (DropboxException e) {
            handleUIToastRequest(e.toString());
            return "";
        }
        if (fileExists) {
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
                String nextDbPath = new File(targetPath).getParent() + "/" + nextEncodedFile.getName();
                boolean nextFileExists = true;
                try {
                    nextFileExists = mApp.dbFileExists(nextDbPath);
                } catch (DropboxException e) {
                    handleUIToastRequest(e.toString());
                    break;
                }
                if (!nextFileExists) {
                    return nextFilePath;
                }
                ntry++;
            }
            return "";
        } else {
            return stripstr;
        }
    }

    @Override
    public boolean uploadEncFSFile(String stripstr, String srcPath) {
        File srcFile = new File(srcPath);
        if (!srcFile.isFile()) {
            handleUIToastRequest(mAppContext.getString(R.string.only_files));
            return false;
        }

        /* Convert current path to encoded file name */
        String encodedPath = Cryptonite.jniEncode(stripstr);
        File encodedFile = new File(encodedPath);

        /* Create temporary cache dirs for Dropbox upload */
        getPrivateDir(Cryptonite.BROWSEPNT, Context.MODE_PRIVATE);
        encodedFile.getParentFile().mkdirs();

        /* Encrypt file to temporary cache */
        if (Cryptonite.jniEncrypt(stripstr, srcPath, true) != Cryptonite.jniSuccess()) {
            handleUIToastRequest(mAppContext.getString(R.string.upload_failure));
            return false;
        }
            
        /* Upload file to DB */
        File browseRoot = mAppContext.getDir(Cryptonite.BROWSEPNT, Context.MODE_PRIVATE);
        String targetPath = encodedPath.substring(browseRoot.getPath().length());
            
        /* rename _de_coded file name if the _en_crypted file exists on Dropbox */
        handleUIUploadEncrypted(new String[] {targetPath, encodedFile.getPath()});
        return true;
    }

    @Override
    public boolean decryptEncFSFile(String encodedPath, String targetPath, String encfsPath) {
        /* Remove local root directory to get Dropbox path */
        String encFSLocalRoot = Cryptonite.jniEncode("/");
        String dbLocalRoot = encFSLocalRoot.substring(0, 
                encFSLocalRoot.length()-encfsPath.length());
        String encFSDBRoot = encFSLocalRoot.substring(dbLocalRoot.length());
        String dbPath = "/" + encodedPath.substring(dbLocalRoot.length()) ;
        
        try {
            if (!dbDownloadDecode(dbPath.substring(encfsPath.length()),
                                  targetPath, encFSDBRoot, encFSLocalRoot, true))
            {
                Log.e(Cryptonite.TAG, "Error while attempting to copy " + encodedPath);
                return false;
            }
        } catch (IOException e) {
            Log.e(Cryptonite.TAG, "Dropbox read fail: " + e.getMessage());
            return false;
        } catch (DropboxException e) {
            Log.e(Cryptonite.TAG, "Dropbox read fail: " + e.getMessage());
            return false;
        }
        return true;
    }

    /** Walks a Dropbox file tree, copying decrypted files to a local
     * directory.
     * Recursion is only used within encrypted subfolders!
     * See https://www.dropbox.com/developers/reference/bestpractice
     * 
     * @param exportPaths Decoded file path array
     * @param exportRoot Root path up to encFS volume
     * @param destDir Destination directory
     * @param dbEncFSPath encFS path from Dropbox root
     * @return true upon success
     * @throws IOException
     * @throws DropboxException
     */
    @Override
    public boolean export(String[] exportPaths, String exportRoot,
            String destDir, String encFSPath) {
        try {
            for (String path : exportPaths) {
                dbRecTree(path, exportRoot, destDir, encFSPath);
            }
        } catch (IOException e) {
            handleUIToastRequest(mAppContext.getString(R.string.export_failed) + e.toString());
            return false;
        } catch (DropboxException e) {
            handleUIToastRequest(mAppContext.getString(R.string.export_failed) + e.toString());
            return false;
        }
        return true;
    }

    @Override
    public boolean createEncFS(String currentReturnPath, String passwordString, 
            File browseRoot, int config) {
        /* Create encrypted folder in temporary directory */
        String cachePath = browseRoot + "/.encfs6.xml";
        new File(cachePath).delete();
        
        /* Upload file to DB */
        String targetPath = currentReturnPath.substring(browseRoot.getPath().length());
        if (!targetPath.endsWith("/")) {
            targetPath += "/";
        }
        /* Is there an existing EncFSVolume? */
        String dbPath = targetPath + ".encfs6.xml";

        boolean fileExists = true;
        try {
            fileExists = mApp.dbFileExists(dbPath);
        } catch (DropboxException e) {
            handleUIToastRequest(e.toString());
            return false;
        }
        if (fileExists) {
            handleUIToastRequest(mAppContext.getString(R.string.encfs6_exists));
            return false;
        }
        
        /* Create encfs6.xml in temporary folder */
        if (Cryptonite.jniCreate(browseRoot.getPath(), passwordString, config) == Cryptonite.jniSuccess()) {
            handleUIToastRequest(mAppContext.getString(R.string.create_success_dropbox));
        } else {
            handleUIToastRequest(mAppContext.getString(R.string.create_failure));
            return false;
        }

        /* Upload to Dropbox */
        /* Problems with AsyncTask when used from here;
         * Therefore uploading directly. xml file is small
         * anyway.
         */
        File cacheFile = new File(cachePath);
        
        try {
            // By creating a request, we get a handle to the putFile operation,
            // so we can cancel it later if we want to
            
            FileInputStream fis = new FileInputStream(cacheFile);
            String path = targetPath + cacheFile.getName();
            UploadRequest request = mApp.getDBApi()
                    .putFileOverwriteRequest(path, fis, cacheFile.length(), null);

            if (request != null) {
                request.upload();
            }

        } catch (DropboxException e) {
            // Unknown error
            handleUIToastRequest(
                    mAppContext.getString(R.string.dropbox_upload_fail) + 
                            ": " + e.getMessage());
            return false;
        } catch (FileNotFoundException e) {
            handleUIToastRequest(
                    mAppContext.getString(R.string.file_not_found) + ": " + 
                            e.getMessage());
            return false;
        }
        handleUIToastRequest(mAppContext.getString(R.string.dropbox_create_successful));        
        return true;
    }

    /** Create an empty file in a local destination directory
     * preserving relative file paths from Dropbox
     * @param dbEntry Dropbox entry
     * @param destRoot Destination target root path
     * @throws IOException
     */
    public static void dbTouch(Entry dbEntry, String destRoot) throws IOException {
        File file = new File(destRoot + dbEntry.path);
        if (dbEntry.isDir) {
            file.mkdirs();
        } else {
            file.createNewFile();
        }
    }
        
    /** Download an encrypted file from Dropbox and save a decoded
     * version to a local directory.
     * 
     * @param srcPath Full encoded source path
     * @param targetDir Target root directory
     * @param encFSDBRoot encFS path from Dropbox root
     * @param encFSLocalRoot encFS path from local root
     * @return true upon success
     * @throws IOException
     * @throws DropboxException
     */
    private boolean dbDownloadDecode(String srcPath, String targetDir, String encFSDBRoot, 
            String encFSLocalRoot)
            throws IOException, DropboxException
    {
        return dbDownloadDecode(srcPath, targetDir, encFSDBRoot, encFSLocalRoot, false);
    }

    /** Download an encrypted file from Dropbox and save a decoded
     * version to a local directory.
     * 
     * @param srcPath Full encoded source path
     * @param targetDir Target root directory
     * @param encFSDBRoot encFS path from Dropbox root
     * @param encFSLocalRoot encFS path from local root
     * @param forceReadable enforce readable file creation mode
     * @return true upon success
     * @throws IOException
     * @throws DropboxException
     */
    private boolean dbDownloadDecode(String srcPath, String targetDir, String encFSDBRoot, 
                                     String encFSLocalRoot, boolean forceReadable)
            throws IOException, DropboxException
    {
        String cachePath = encFSLocalRoot + srcPath;
        (new File(cachePath)).getParentFile().mkdirs();

        /* Download encoded file to cache dir */
        FileOutputStream fos = new FileOutputStream(cachePath);
        mApp.getDBApi().getFile(encFSDBRoot + srcPath, null, fos, null);
        fos.close();
        
        return (Cryptonite.jniDecrypt(encFSLocalRoot + srcPath, targetDir, forceReadable) == Cryptonite.jniSuccess());
    }
    
    /** Walks a Dropbox file tree, copying decrypted files to a local
     * directory (recursive part).
     * Recursion is only used within encrypted subfolders!
     * See https://www.dropbox.com/developers/reference/bestpractice
     * 
     * @param currentPath Decoded file path
     * @param exportRoot Root path up to encFS volume
     * @param destDir Destination directory
     * @param dbEncFSPath encFS path from Dropbox root
     * @throws IOException
     * @throws DropboxException
     */
    private void dbRecTree(String currentPath, String exportRoot, String destDir,
            String dbEncFSPath) throws IOException, DropboxException 
    {
        /* normalise path names */
        String bRoot = new VirtualFile(exportRoot).getPath();
        String bPath = new VirtualFile(currentPath).getPath();
        String stripstr = bPath.substring(bRoot.length());
        if (!stripstr.startsWith("/")) {
            stripstr = "/" + stripstr;
        }
        
        /* Convert current path to encoded file name */
        String encodedPath = Cryptonite.jniEncode(stripstr);
        String destPath = destDir + stripstr;
        
        /* Remove local root directory to get Dropbox path */
        String encFSLocalRoot = Cryptonite.jniEncode("/");
        String dbLocalRoot = encFSLocalRoot.substring(0, 
                encFSLocalRoot.length()-dbEncFSPath.length());
        String encFSDBRoot = encFSLocalRoot.substring(dbLocalRoot.length());
        String dbPath = "/" + encodedPath.substring(dbLocalRoot.length()) ;
                
        /* Find file in Dropbox */
        Entry dbEntry = mApp.getDBEntry(dbPath);
        
        if (dbEntry.isDir) {
            /* Create the decoded directory */
            (new File(destPath)).mkdirs();
            
            /* fullList.add(stripstr); */
            if (dbEntry.contents != null) {
                if (dbEntry.contents.size()>0) {
                    for (Entry dbChild : dbEntry.contents) {
                        if (dbChild.isDir) {
                            String decodedChildPath = bRoot /*dbLocalRoot */ +
                                    Cryptonite.jniDecode(dbChild.path.substring(dbEncFSPath.length()));
                            dbRecTree(decodedChildPath, exportRoot, destDir, dbEncFSPath);
                        } else {
                            /* Download all children non-dirs right here
                             * so that we don't have to retrieve the metadata every time
                             * 
                             * Download and decode file
                             */
                            (new File(destPath)).getParentFile().mkdirs();
                            if (!dbDownloadDecode(dbChild.path.substring(dbEncFSPath.length()), destDir, encFSDBRoot,
                                    encFSLocalRoot))
                            {
                                handleUIToastRequest(mAppContext.getString(R.string.file_not_found));
                            }
                        }
                    }
                }
            }

        } else {
            
            (new File(destPath)).getParentFile().mkdirs();
            if (!dbDownloadDecode(dbEntry.path.substring(dbEncFSPath.length()), destDir, encFSDBRoot,
                    encFSLocalRoot))
            {
                handleUIToastRequest(mAppContext.getString(R.string.file_not_found));
            }
        }
    }
    
}
