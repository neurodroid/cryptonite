/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package csh.cryptonite.test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.content.res.AssetManager;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import csh.cryptonite.Cryptonite;


/**
 * Make sure that the main launcher activity opens up properly, which will be
 * verified by {@link #testActivityTestCaseSetUpProperly}.
 */
public class CryptoniteTest extends ActivityInstrumentationTestCase2<Cryptonite> {

    private Cryptonite mActivity;
    private boolean mHasTestVolumes;
    
    /**
     * Creates an {@link ActivityInstrumentationTestCase2} for the Cryptonite activity.
     */
    public CryptoniteTest() {
        super("csh.cryptonite", Cryptonite.class);
        Log.i(Cryptonite.TAG, "In constructor");
        mHasTestVolumes = false;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        String[] encfsTypes = {"aes-256", "blowfish-128"};
        
        for (String encfsType : encfsTypes) {
            File targetDir = getActivity().getDir(encfsType, Context.MODE_WORLD_WRITEABLE);
            if (!targetDir.exists()) {
                Log.e(Cryptonite.TAG, targetDir.getPath() + " wasn't created");
            }
        }

        Log.i(Cryptonite.TAG, "In setUp");
        
        mActivity = this.getActivity();
        mHasTestVolumes = cpEncFSTest();
    }

    private boolean copyFileOrDir(String srcPath, String targetPath) {
        Context context = getInstrumentation().getContext();
        AssetManager assetManager = context.getAssets();
        String assets[] = null;
        try {
            assets = assetManager.list(srcPath);
            if (assets.length == 0) {
                if (!copyFile(srcPath, targetPath)) {
                    return false;
                }
            } else {
                String fullPath = targetPath + "/" + srcPath;
                File dir = new File(fullPath);
                if (!dir.exists())
                    dir.mkdirs();
                for (int i = 0; i < assets.length; ++i) {
                    if (!copyFileOrDir(srcPath + "/" + assets[i], targetPath)) {
                        return false;
                    }
                }
            }
        } catch (IOException ex) {
            Log.e(Cryptonite.TAG, ex.toString());
            return false;
        }
        
        return true;
    }

    private boolean copyFile(String filename, String targetPath) {
        Context context = getInstrumentation().getContext();
        AssetManager assetManager = context.getAssets();

        InputStream in = null;
        OutputStream out = null;
        try {
            in = assetManager.open(filename);
            String newFileName = targetPath + "/" + filename;
            Log.i(Cryptonite.TAG, "Copying " + filename + " to " + newFileName);
            if (!(new File(newFileName)).getParentFile().exists()) {
                if (!(new File(newFileName)).getParentFile().mkdirs()) {
                    Log.e(Cryptonite.TAG, "Couldn't create " + (new File(newFileName)).getParentFile().getPath());
                    return false;
                }
            }
            out = new FileOutputStream(newFileName);
            
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            in = null;
            out.flush();
            out.close();
            out = null;
        } catch (Exception e) {
            Log.e(Cryptonite.TAG, e.toString());
            return false;
        }
        return true;
    }    
    /** Copy EncFS test volumes */
    public boolean cpEncFSTest() {
        Log.i(Cryptonite.TAG, "Entering cpEncFSTest()");
        
        String[] encfsTypes = {"aes-256", "blowfish-128"};
        
        for (String encfsType : encfsTypes) {
            File targetDir = getActivity().getDir(encfsType, Context.MODE_WORLD_WRITEABLE);
            if (!targetDir.exists()) {
                Log.e(Cryptonite.TAG, targetDir.getPath() + " wasn't created");
                return false;
            } else {
                Log.i(Cryptonite.TAG, "Successfully created " + targetDir.getPath());
            }
            Log.i(Cryptonite.TAG, "Copying to " + targetDir.getPath());
            if (!copyFileOrDir(encfsType, targetDir.getPath())) {
                Log.e(Cryptonite.TAG, "Couldn't copy " + encfsType + " to " + targetDir.getPath());
                return false;
            }
        }
        
        return true;
                /*File binDir = new File(BINDIR);
        if (!binDir.exists()) {
            throw new RuntimeException("Couldn't find binary directory");
        }
        String binName = BINDIR + "/" + trunk;

        /* Catenate split files
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

            ShellUtils.chmod(binName, "755");
            
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }*/

    }
    
    /**
     * Verifies that the activity under test can be launched.
     */
    public void testPreconditions() {
        assertNotNull("activity should be launched successfully", mActivity);
        assertEquals(true, this.mHasTestVolumes);
    }
    
    public void testEncFS() {
        String[] encfsTypes = {"aes-256", "blowfish-128"};
        
        for (String encfsType : encfsTypes) {
            File targetDir = getActivity().getDir(encfsType, Context.MODE_WORLD_WRITEABLE);
            assertEquals(getActivity().jniSuccess(), 
                    getActivity().jniInit(targetDir.getPath() + "/" + encfsType, "password"));
        }
    }
    
}
