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
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import com.actionbarsherlock.app.SherlockFragmentActivity;

import csh.cryptonite.Cryptonite;
import csh.cryptonite.DirectorySettings;
import csh.cryptonite.R;
import csh.cryptonite.SelectionMode;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

public class LocalStorage extends Storage {

    public LocalStorage(SherlockFragmentActivity activity) {
        super(activity);
        type = STOR_LOCAL;
        fdSelectionMode = SelectionMode.MODE_OPEN_MULTISELECT;
        selectExportMode = Cryptonite.SELECTLOCALEXPORT_MODE;
        exportMode = Cryptonite.LOCALEXPORT_MODE;
        uploadMode = Cryptonite.SELECTLOCALUPLOAD_MODE;
        waitStringId = R.string.local_reading;
        browsePnt = DirectorySettings.BROWSEPNT;
    }

    @Override
    public boolean initEncFS(String srcDir, String initRoot) {
        if (Cryptonite.jniIsValidEncFS(srcDir) != Cryptonite.jniSuccess()) {
            handleUIToastRequest(R.string.invalid_encfs);
            Log.e(Cryptonite.TAG, mAppContext.getString(R.string.invalid_encfs));
            return false;
        }
        return true;
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
    public boolean encryptEncFSFile(String stripstr, String srcPath) {
        return (Cryptonite.jniEncrypt(stripstr, srcPath, true) == Cryptonite.jniSuccess());
    }
    
    @Override
    public AsyncTask<Void, Long, Boolean> uploadEncFSFile(SherlockFragmentActivity activity, String stripstr) {
        return null;
    }

    @Override
    public boolean decryptEncFSFile(String encodedPath, String targetPath) {
        /* Copy decrypted file */
        return (Cryptonite.jniDecrypt(encodedPath, targetPath, true) == Cryptonite.jniSuccess());
    }

    @Override
    public Cryptonite.DecodedBuffer decryptEncFSFileToBuffer(String encodedPath) {
        return new Cryptonite.DecodedBuffer(
                    Cryptonite.jniDecode(encodedPath),
                    Cryptonite.jniDecryptToBuffer(encodedPath));
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
    public boolean exportEncFSFiles(String[] exportPaths, String exportRoot,
            String destDir) {
        Set<String> errorList = new HashSet<String>();

        for (String path : exportPaths) {
            localRecTree(path, errorList, exportRoot, destDir, encFSPath);
        }

        return errorList.size() == 0;
    }
    
    @Override
    public boolean deleteFile(String path) {
        return (new File(path)).delete();
    }
    
    @Override
    public boolean createEncFS(String currentReturnPath, String passwordString, 
            File browseRoot, int config) {
        String encfsPath = currentReturnPath + "/" + ENCFS_XML_CURRENT;
        if (new File(encfsPath).exists()) {
            handleUIToastRequest(R.string.encfs_exists);
            return false;
        }
        if (Cryptonite.jniCreate(currentReturnPath, passwordString, config) == Cryptonite.jniSuccess()) {
            handleUIToastRequest(R.string.create_success_local);
        } else {
            handleUIToastRequest(R.string.create_failure);
            return false;
        }
        return true;
    }

    @Override
    public boolean mkVisibleDecoded(String path, String encFSRoot, String rootPath) {

        if (encFSRoot == null || encFSPath == null) {
            return false;
        }
        
        String prevRoot = encFSRoot.substring(0, encFSRoot.length()-encFSPath.length());
        String encodedPath = Cryptonite.jniEncode(path).substring(prevRoot.length()-1);
        
        try {

            VirtualFile localEntry = new VirtualFile(prevRoot + encodedPath);

            if (localEntry.exists()) {
                if (localEntry.isDirectory()) {
                    if (localEntry.listFiles() != null) {
                        if (localEntry.listFiles().length > 0) {
                            for (VirtualFile localChild : localEntry.listFiles()) {
                                decode(localChild.getPath().substring(encFSRoot.length()), 
                                        rootPath, localChild.isDirectory());
                            }
                        }
                    }
                }
            }
            return true;
        } catch (IOException e) {
            String alertMsg = mAppContext.getString(R.string.local_read_fail) + e.toString();
            handleUIToastRequest(alertMsg);
            Log.e(Cryptonite.TAG, alertMsg);
            return false;
        }
    }
    
    @Override
    public void mkVisiblePlain(String path, String rootPath) {
        /* Does nothing because the files are already visible */
        return;
    }

    @Override
    public boolean mkDirEncrypted(String encodedPath) {
        File encodedFile = new File(encodedPath);
        /* Does the encrypted file exist? */
        if (encodedFile.exists()) {
            handleUIToastRequest(R.string.new_folder_exists);
            return false;
        } else {
            if (!encodedFile.mkdir()) {
                handleUIToastRequest(R.string.new_folder_fail);
                return false;
            } else {
                return true;
            }
        }
    }        

    @Override
    public boolean mkDirPlain(String plainPath) {
        File targetDir = new File(plainPath);
        if (targetDir.exists()) {
            handleUIToastRequest(R.string.new_folder_exists);
            return false;
        }
        if (!targetDir.mkdir()) {
            handleUIToastRequest(R.string.new_folder_fail);
            return false;
        }
        return true;
    }

    @Override
    public boolean exists(String plainPath) {
        return new File(Environment.getExternalStorageDirectory().getPath() + "/" + plainPath).exists();
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
