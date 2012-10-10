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

package csh.cryptonite.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.DropboxAPI.UploadRequest;
import com.dropbox.client2.exception.DropboxException;

import csh.cryptonite.Cryptonite;
import csh.cryptonite.DirectorySettings;
import csh.cryptonite.R;
import csh.cryptonite.SelectionMode;
import csh.cryptonite.UploadEncrypted;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

public class DropboxStorage extends Storage {

    public DropboxStorage(SherlockFragmentActivity activity) {
        super(activity);
        type = STOR_DROPBOX;
        fdSelectionMode = SelectionMode.MODE_OPEN_MULTISELECT_DB;
        selectExportMode = Cryptonite.SELECTDBEXPORT_MODE;
        exportMode = Cryptonite.DBEXPORT_MODE;
        uploadMode = Cryptonite.SELECTDBUPLOAD_MODE;
        waitStringId = R.string.dropbox_reading;
        browsePnt = DirectorySettings.DROPBOXPNT;
    }
    
    @Override
    public boolean initEncFS(String srcDir, String initRoot, String configPath) {
        if (configPath != null) {
            if (configPath.length() > 0) {
                return true;
            }
        }
        /* Download encfs*.xml from Dropbox 
         * to browse folder */
        String dbPath = srcDir.substring(initRoot.length());
        try {
            Entry dbEntry = DropboxInterface.INSTANCE.getDBEntry(dbPath); 
            if (dbEntry != null) {
                if (dbEntry.isDir) {
                    if (dbEntry.contents != null) {
                        if (dbEntry.contents.size() > 0) {
                            for (Entry dbChild : dbEntry.contents) {
                                if (!dbChild.isDir) {
                                    if (dbChild.fileName().matches(ENCFS_XML_V6_REGEX) ||
                                            dbChild.fileName().matches(ENCFS_XML_V7_REGEX)) {
                                        String localEncfsXmlPath = initRoot + dbChild.path;
                                        FileOutputStream fos = new FileOutputStream(localEncfsXmlPath);
                                        DropboxInterface.INSTANCE.getDBApi()
                                            .getFile(dbChild.path, null, fos, null);
                                        Log.i(Cryptonite.TAG, "Downloaded " + dbChild.fileName() + " to " + localEncfsXmlPath);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (DropboxException e) {
            handleUIToastRequest(e.getMessage() + " " + 
                    mAppContext.getString(R.string.dropbox_read_fail));
            return false;
        } catch (FileNotFoundException e) {
            handleUIToastRequest(e.getMessage() + " " + 
                    mAppContext.getString(R.string.dropbox_read_fail));
            return false;
        }
        
        if (Cryptonite.jniIsValidEncFS(srcDir) != Cryptonite.jniSuccess()) {
            handleUIToastRequest(R.string.invalid_encfs);
            Log.e(Cryptonite.TAG, "Invalid EncFS");
            return false;
        }

        return true;
    }

    @Override
    public String encodedExists(String stripstr) {

        /* Upload file to DB */
        File browseRoot = mAppContext.getDir(DirectorySettings.BROWSEPNT, Context.MODE_PRIVATE);

        /* Convert current path to encoded file name */
        String encodedPath = Cryptonite.jniEncode(stripstr);
        
        if (encodedPath == null) {
            return "";
        }

        File encodedFile = new File(encodedPath);
        String targetPath = encodedPath.substring(browseRoot.getPath().length());

        /* Does the _en_crypted file exist on Dropbox? */
        String dbPath = new File(targetPath).getParent() + "/" + encodedFile.getName();
        boolean fileExists = true;
        try {
            fileExists = DropboxInterface.INSTANCE.dbFileExists(dbPath);
        } catch (DropboxException e) {
            handleUIToastRequest(e.toString());
            return "";
        } catch (NullPointerException e) {
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
                    nextFileExists = DropboxInterface.INSTANCE.dbFileExists(nextDbPath);
                } catch (DropboxException e) {
                    handleUIToastRequest(e.toString());
                    break;
                } catch (NullPointerException e) {
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
    public boolean encryptEncFSFile(String stripstr, String srcPath) {
        File srcFile = new File(srcPath);
        if (!srcFile.isFile()) {
            handleUIToastRequest(R.string.only_files);
            return false;
        }

        /* Convert current path to encoded file name */
        String encodedPath = Cryptonite.jniEncode(stripstr);
        File encodedFile = new File(encodedPath);

        /* Create temporary cache dirs for Dropbox upload */
        getPrivateDir(DirectorySettings.BROWSEPNT, Context.MODE_PRIVATE);
        encodedFile.getParentFile().mkdirs();

        /* Encrypt file to temporary cache */
        if (Cryptonite.jniEncrypt(stripstr, srcPath, true) != Cryptonite.jniSuccess()) {
            handleUIToastRequest(R.string.upload_failure);
            return false;
        }
            
        return true;
    }
    
    @Override
    public AsyncTask<Void, Long, Boolean> uploadEncFSFile(SherlockFragmentActivity activity, String stripstr) {
        /* Convert current path to encoded file name */
        String encodedPath = Cryptonite.jniEncode(stripstr);
        File encodedFile = new File(encodedPath);

        /* Upload file to DB */
        File browseRoot = mAppContext.getDir(DirectorySettings.BROWSEPNT, Context.MODE_PRIVATE);
        String targetPath = encodedPath.substring(browseRoot.getPath().length());
            
        /* rename _de_coded file name if the _en_crypted file exists on Dropbox */
        UploadEncrypted upload = new UploadEncrypted(activity, DropboxInterface.INSTANCE.getDBApi(),
                new File(targetPath).getParent() + "/",
                new File(encodedFile.getPath()));
        return upload;
    }

    @Override
    public boolean decryptEncFSFile(String encodedPath, String targetPath) {
        /* Remove local root directory to get Dropbox path */
        String encFSLocalRoot = Cryptonite.jniEncode("/");
        String dbLocalRoot = encFSLocalRoot.substring(0, 
                encFSLocalRoot.length()-encFSPath.length());
        String encFSDBRoot = encFSLocalRoot.substring(dbLocalRoot.length());
        String dbPath = "/" + encodedPath.substring(dbLocalRoot.length()) ;
        
        try {
            if (!dbDownloadDecode(dbPath.substring(encFSPath.length()),
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

    @Override
    public Cryptonite.DecodedBuffer decryptEncFSFileToBuffer(String encodedPath) {
        /* Remove local root directory to get Dropbox path */
        String encFSLocalRoot = Cryptonite.jniEncode("/");
        String dbLocalRoot = encFSLocalRoot.substring(0, 
                encFSLocalRoot.length()-encFSPath.length());
        String encFSDBRoot = encFSLocalRoot.substring(dbLocalRoot.length());
        String dbPath = "/" + encodedPath.substring(dbLocalRoot.length()) ;
        
        try {
            return dbDownloadDecodeToBuffer(dbPath.substring(encFSPath.length()),
                                  encFSDBRoot, encFSLocalRoot);
        } catch (IOException e) {
            Log.e(Cryptonite.TAG, "Dropbox read fail: " + e.getMessage());
            return null;
        } catch (DropboxException e) {
            Log.e(Cryptonite.TAG, "Dropbox read fail: " + e.getMessage());
            return null;
        }
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
    public boolean exportEncFSFiles(String[] exportPaths, String exportRoot,
            String destDir) {
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
    public boolean deleteFile(String path) {
        /* Create dir on DB */
        File browseRoot = mAppContext.getDir(DirectorySettings.BROWSEPNT, Context.MODE_PRIVATE);
        String targetPath = path.substring(browseRoot.getPath().length());
        
        /* Does the _en_crypted directory exist on Dropbox? */
        String dbPath = new File(targetPath).getParent() + "/" + new File(path).getName();
        boolean fileExists = true;
        try {
            fileExists = DropboxInterface.INSTANCE.dbFileExists(dbPath);
        } catch (DropboxException e) {
            handleUIToastRequest(mAppContext.getString(R.string.delete_fail) + ": " + e.toString());
            return false;
        } catch (NullPointerException e) {
            handleUIToastRequest(e.toString());
            return false;
        }
        if (fileExists) {
            try {
                DropboxInterface.INSTANCE.getDBApi().delete(dbPath);
                DropboxInterface.INSTANCE.removeDBHashMapEntry(dbPath);
                DropboxInterface.INSTANCE.removeDBHashMapEntry(new File(dbPath).getParent());
            } catch (DropboxException e) {
                handleUIToastRequest(mAppContext.getString(R.string.delete_fail) +
                        ": " + e.toString());
                return false;
            }
        } else {
            handleUIToastRequest(R.string.delete_fail_nexists);
            return false;
        }
        return true;
    }

    @Override
    public boolean createEncFS(String currentReturnPath, String passwordString, 
            File browseRoot, int config) {
        /* Create encrypted folder in temporary directory */
        String cachePath = browseRoot + "/" + ENCFS_XML_CURRENT;
        new File(cachePath).delete();
        
        /* Upload file to DB */
        String targetPath = currentReturnPath.substring(browseRoot.getPath().length());
        if (!targetPath.endsWith("/")) {
            targetPath += "/";
        }
        /* Is there an existing EncFSVolume? */
        String dbPath = targetPath + "/" + ENCFS_XML_CURRENT;

        boolean fileExists = true;
        try {
            fileExists = DropboxInterface.INSTANCE.dbFileExists(dbPath);
        } catch (DropboxException e) {
            handleUIToastRequest(e.toString());
            return false;
        } catch (NullPointerException e) {
            handleUIToastRequest(e.toString());
            return false;
        }
        if (fileExists) {
            handleUIToastRequest(R.string.encfs_exists);
            return false;
        }
        
        /* Create encfs6.xml in temporary folder */
        if (Cryptonite.jniCreate(browseRoot.getPath(), passwordString, config) == Cryptonite.jniSuccess()) {
            handleUIToastRequest(R.string.create_success_dropbox);
        } else {
            handleUIToastRequest(R.string.create_failure);
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
            UploadRequest request = DropboxInterface.INSTANCE.getDBApi()
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
        handleUIToastRequest(R.string.dropbox_create_successful);
        return true;
    }

    @Override
    public boolean mkVisibleDecoded(String path, String encFSRoot, String rootPath) {

        if (encFSRoot == null || encFSPath == null) {
            Log.e(Cryptonite.TAG, "Path is null in DropboxStorage.mkVisibleDecoded");
            return false;
        }
        
        String prevRoot = "";
        String encodedPath = "";
        
        try {
            prevRoot = encFSRoot.substring(0, encFSRoot.length()-encFSPath.length());
            encodedPath = Cryptonite.jniEncode(path).substring(prevRoot.length()-1);
        } catch (IndexOutOfBoundsException e) {
            Log.e(Cryptonite.TAG, "Index out of bounds" + 
                    ": encFSRoot = " + encFSRoot + 
                    ", encFSPath = " + encFSPath + 
                    ", path = " + path + 
                    ", prevRoot = " + prevRoot + 
                    ", jniEncode(path) = " + Cryptonite.jniEncode(path));
        }

        try {

            Entry dbEntry = DropboxInterface.INSTANCE.getDBEntry(encodedPath);
            
            if (dbEntry != null) {
                if (dbEntry.isDir && !dbEntry.isDeleted) {
                    if (dbEntry.contents != null) {
                        if (dbEntry.contents.size()>0) {
                            for (Entry dbChild : dbEntry.contents) {
                                if (!dbChild.isDeleted) {
                                    decode(dbChild.path.substring(encFSPath.length()), 
                                            rootPath, dbChild.isDir);
                                }
                            }
                        }
                    }
                }
            }
            return true;
        } catch (DropboxException e) {
            String alertMsg = mAppContext.getString(R.string.dropbox_read_fail) + ": " + e.getMessage();
            handleUIToastRequest(alertMsg);
            return false;
        } catch (IOException e) {
            String alertMsg = mAppContext.getString(R.string.dropbox_read_fail) + ": " + e.getMessage();
            handleUIToastRequest(alertMsg);
            return false;
        }
    }
    
    @Override
    public void mkVisiblePlain(String path, String rootPath) {
        try {

            Entry dbEntry = DropboxInterface.INSTANCE.getDBEntry(path);

            if (dbEntry != null) {
                if (dbEntry.isDir && !dbEntry.isDeleted) {
                    if (dbEntry.contents != null) {
                        if (dbEntry.contents.size()>0) {
                            for (Entry dbChild : dbEntry.contents) {
                                if (!dbChild.isDeleted) {
                                    DropboxStorage.dbTouch(dbChild, rootPath);
                                }
                            }
                        }
                    }
                }
            }
        } catch (DropboxException e) {
            String alertMsg = mAppContext.getString(R.string.dropbox_read_fail) + ": " + e.getMessage();
            handleUIToastRequest(alertMsg);
        } catch (IOException e) {
            String alertMsg = mAppContext.getString(R.string.dropbox_read_fail) + ": " + e.getMessage();
            handleUIToastRequest(alertMsg);
        }
    }

    @Override
    public boolean mkDirEncrypted(String encodedPath) {
        /* Create dir on DB */
        File browseRoot = mAppContext.getDir(DirectorySettings.BROWSEPNT, Context.MODE_PRIVATE);
        String targetPath = encodedPath.substring(browseRoot.getPath().length());
        
        /* Does the _en_crypted directory exist on Dropbox? */
        String dbPath = new File(targetPath).getParent() + "/" + new File(encodedPath).getName();
        boolean fileExists = true;
        try {
            fileExists = DropboxInterface.INSTANCE.dbFileExists(dbPath);
        } catch (DropboxException e) {
            handleUIToastRequest(mAppContext.getString(R.string.new_folder_fail) + ": " + e.toString());
            return false;
        } catch (NullPointerException e) {
            handleUIToastRequest(mAppContext.getString(R.string.new_folder_fail) + ": " + e.toString());
            return false;
        }
        if (fileExists) {
            handleUIToastRequest(R.string.new_folder_exists);
            return false;
        } else {
            try {
                DropboxInterface.INSTANCE.getDBApi().createFolder(dbPath);
            } catch (DropboxException e) {
                handleUIToastRequest(mAppContext.getString(R.string.new_folder_fail) +
                        ": " + e.toString());
                return false;
            }
        }
        return true;
    }
    
    @Override
    public boolean mkDirPlain(String plainPath) {
        /* Create dir on DB */
        File browseRoot = mAppContext.getDir(DirectorySettings.BROWSEPNT, Context.MODE_PRIVATE);
        String targetPath = plainPath.substring(browseRoot.getPath().length());
        boolean fileExists = true;
        try {
            fileExists = DropboxInterface.INSTANCE.dbFileExists(targetPath);
        } catch (DropboxException e) {
            handleUIToastRequest(mAppContext.getString(R.string.new_folder_fail) + ": " + e.toString());
            return false;
        } catch (NullPointerException e) {
            handleUIToastRequest(mAppContext.getString(R.string.new_folder_fail) + ": " + e.toString());
            return false;
        }
        if (fileExists) {
            handleUIToastRequest(R.string.new_folder_exists);
            return false;
        } else {
            try {
                DropboxInterface.INSTANCE.getDBApi().createFolder(targetPath);
            } catch (DropboxException e) {
                handleUIToastRequest(mAppContext.getString(R.string.new_folder_fail) +
                        ": " + e.toString());
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean exists(String plainPath) {
        boolean fileExists = true;
        try {
            fileExists = DropboxInterface.INSTANCE.dbFileExists(plainPath);
        } catch (DropboxException e) {
            return false;
        } catch (NullPointerException e) {
            return false;
        }
        return fileExists;
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
            /* Does the parent exist? */
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
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
        DropboxInterface.INSTANCE.getDBApi().getFile(encFSDBRoot + srcPath, null, fos, null);
        fos.close();
        
        return (Cryptonite.jniDecrypt(encFSLocalRoot + srcPath, targetDir, forceReadable) == Cryptonite.jniSuccess());
    }

    /** Download an encrypted file from Dropbox return the decrypted content.
     * 
     * @param srcPath Full encoded source path
     * @param encFSDBRoot encFS path from Dropbox root
     * @param encFSLocalRoot encFS path from local root
     * @return decrypted content
     * @throws IOException
     * @throws DropboxException
     */
    private Cryptonite.DecodedBuffer dbDownloadDecodeToBuffer(String srcPath, String encFSDBRoot,
            String encFSLocalRoot) throws IOException, DropboxException
    {
        String cachePath = encFSLocalRoot + srcPath;
        File cacheFile = new File(cachePath);
        cacheFile.getParentFile().mkdirs();

        /* Download encoded file to cache dir */
        FileOutputStream fos = new FileOutputStream(cachePath);
        DropboxInterface.INSTANCE.getDBApi().getFile(encFSDBRoot + srcPath, null, fos, null);
        fos.close();
        
        byte[] buf = Cryptonite.jniDecryptToBuffer(encFSLocalRoot + srcPath);
        
        cacheFile.delete();
        
        return new Cryptonite.DecodedBuffer(
                Cryptonite.jniDecode(encFSLocalRoot + srcPath),
                buf);
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
        Entry dbEntry = DropboxInterface.INSTANCE.getDBEntry(dbPath);
        if (dbEntry == null) {
            return;
        }
        if (dbEntry.isDeleted) {
            return;
        }
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
                                handleUIToastRequest(R.string.file_not_found);
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
                handleUIToastRequest(R.string.file_not_found);
            }
        }
    }

}
