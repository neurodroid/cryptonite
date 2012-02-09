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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Scanner;

import android.content.Context;
import android.content.Intent;
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
        mHasTestVolumes = false;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Intent instrumentationIntent = new Intent();
        instrumentationIntent.putExtra("csh.cryptonite.instrumentation", true);
        setActivityIntent(instrumentationIntent);
        mActivity = this.getActivity();

        String[] encfsTypes = {"aes-256", "blowfish-128"};
        
        for (String encfsType : encfsTypes) {
            File targetDir = mActivity.getDir(encfsType, Context.MODE_WORLD_WRITEABLE);
            if (!targetDir.exists()) {
                Log.e(Cryptonite.TAG, targetDir.getPath() + " wasn't created");
            }
        }

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
                if (!dir.exists()) {
                    dir.mkdirs();
                }
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

    private boolean copyFile(String assetFileName, String targetPath) {
        Context context = getInstrumentation().getContext();
        AssetManager assetManager = context.getAssets();

        InputStream in = null;
        OutputStream out = null;
        try {
            in = assetManager.open(assetFileName);
            
            File srcFile = new File(assetFileName);
            String srcFileName = srcFile.getName();
            String srcFileParentPath = srcFile.getParent();
            File srcFileParent = srcFile.getParentFile();
            
            String encfsXmlRegex = "encfs.\\.xml";
            String prepend = "";
            if (srcFileName.matches(encfsXmlRegex)) {
                prepend = ".";
            }
            
            String targetFileName = targetPath + "/" + srcFileParentPath + "/" + prepend + srcFileName;
            File targetFile = new File(targetFileName);
            File targetFileParent = targetFile.getParentFile();
            String targetFileParentPath = targetFile.getParent();
            
            if (!targetFileParent.exists()) {
                if (!targetFileParent.mkdirs()) {
                    Log.e(Cryptonite.TAG, "Couldn't create " + targetFileParentPath);
                    return false;
                }
            }
            out = new FileOutputStream(targetFile);
            
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
        
        String[] encfsTypes = {"aes-256", "blowfish-128"};
        
        for (String encfsType : encfsTypes) {
            File targetDir = mActivity.getDir(encfsType, Context.MODE_WORLD_WRITEABLE);
            if (!targetDir.exists()) {
                Log.e(Cryptonite.TAG, targetDir.getPath() + " wasn't created");
                return false;
            }
            if (!copyFileOrDir(encfsType, targetDir.getPath())) {
                Log.e(Cryptonite.TAG, "Couldn't copy " + encfsType + " to " + targetDir.getPath());
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Verifies that the activity under test can be launched.
     */
    public void testEncFS_0_Preconditions() {
        assertNotNull("activity should be launched successfully", mActivity);
        assertEquals(true, this.mHasTestVolumes);
    }
    
    public void testEncFS_1_Init() {
        String[] encfsTypes = {"aes-256", "blowfish-128"};
        
        for (String encfsType : encfsTypes) {
            File targetDir = mActivity.getDir(encfsType, Context.MODE_WORLD_WRITEABLE);
            assertEquals(mActivity.jniSuccess(), 
                    mActivity.jniInit(targetDir.getPath() + "/" + encfsType, "password"));
        }
    }
    
    public void testEncFS_2_Export() {
        String encFSType = "blowfish-128";
        
        File encFSDir = mActivity.getDir(encFSType, Context.MODE_WORLD_WRITEABLE);
        assertEquals(mActivity.jniSuccess(), 
                mActivity.jniInit(encFSDir.getPath() + "/" + encFSType, "password"));
        
        File decryptedDir = mActivity.getDir("decrypted", Context.MODE_WORLD_WRITEABLE);
        String encodedName = "7F2jGY68wlYqw1/e4cKk0dWOKz5k,";
        String decodedName = mActivity.jniDecode(encodedName);
        String targetPath = decryptedDir.getPath() + "/" + decodedName;
        (new File(targetPath)).getParentFile().mkdirs();
        
        assertEquals(mActivity.jniSuccess(), 
                mActivity.jniDecrypt(encodedName, decryptedDir.getPath(), true));
        assertEquals(true, (new File(targetPath)).exists());
        
        /* Read decrypted file */
        String NL = System.getProperty("line.separator");
        String output = "";
        try {
            Scanner outscanner  = new Scanner(new FileInputStream(targetPath));
            try {
                while (outscanner.hasNextLine()) {
                    output += outscanner.nextLine();                    
                }
            } finally {
                outscanner.close();
            }
        } catch (FileNotFoundException e) {
            
        }
        assertEquals("6*8", output);
    }
    
}
