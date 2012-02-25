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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

import android.content.Intent;
import android.os.Environment;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import csh.cryptonite.CreateEncFS;
import csh.cryptonite.Cryptonite;

/**
 * Make sure that the main launcher activity opens up properly, which will be
 * verified by {@link #testActivityTestCaseSetUpProperly}.
 */
public class CryptoniteTest extends ActivityInstrumentationTestCase2<Cryptonite> {

    private Cryptonite mActivity;
    private static final int[] ENCFS_CONFIG_TYPES =
        {CreateEncFS.CONFIG_PARANOIA, CreateEncFS.CONFIG_COMPATIBLE};
    private static final String TEST_STRING = "6*8";
    private static final String TEST_DIR="csh.cryptonite.test";
    private static final String DECRYPTED_DIR_NAME = "/arthur/dent"; 
    private static final String DECRYPTED_FILE_NAME = "42"; 
    
    /**
     * Creates an {@link ActivityInstrumentationTestCase2} for the Cryptonite activity.
     */
    public CryptoniteTest() {
        super("csh.cryptonite", Cryptonite.class);
        for (int encfsConfigType : ENCFS_CONFIG_TYPES) {
            File targetDir = getConfigDir(encfsConfigType);
            Cryptonite.deleteDir(targetDir);
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Intent instrumentationIntent = new Intent();
        instrumentationIntent.putExtra("csh.cryptonite.instrumentation", true);
        setActivityIntent(instrumentationIntent);
        mActivity = this.getActivity();
    }
    
    private File getConfigDir(int encfsConfigType) {
        String typeString = TEST_DIR + "config_" + encfsConfigType;
        File targetDir = new File(Environment.getExternalStorageDirectory().getPath() + "/" + typeString);
        return targetDir;
    }
    
    private File getCacheDir() {
        return new File(Environment.getExternalStorageDirectory().getPath() + 
                "/" + TEST_DIR + "cache");
    }
    
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    /**
     * Verifies that the activity under test can be launched.
     */
    public void testEncFS_0_Preconditions() {
        assertNotNull("activity should be launched successfully", mActivity);
    }
    
    public void testEncFS_1_Create() {
        for (int encfsConfigType : ENCFS_CONFIG_TYPES) {
            File targetDir = getConfigDir(encfsConfigType);
            Log.d(Cryptonite.TAG, "Creating in " + targetDir.getPath());
            if (targetDir.exists()) {
                assertTrue(Cryptonite.deleteDir(targetDir));
            }
            assertTrue(targetDir.mkdir());
            assertEquals(Cryptonite.jniSuccess(),
                    Cryptonite.jniCreate(targetDir.getPath(), "password", encfsConfigType));
            Cryptonite.jniResetVolume();
        }
    }
    
    public void testEncFS_2_Upload() {
        /* Create cache file */
        File cacheDir = getCacheDir();
        if (!cacheDir.exists()) {
            assertTrue(cacheDir.mkdir());
        }
        File cacheFile = new File(cacheDir.getPath() + "/" + DECRYPTED_FILE_NAME);
        if (cacheFile.exists()) {
            assertTrue(cacheFile.delete());
        }
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(cacheFile));
            bw.write(TEST_STRING);
            bw.close();
        } catch (FileNotFoundException e) {
            fail();
        } catch (IOException e) {
            fail();
        }
        
        /* Encrypt cache file */
        for (int encfsConfigType : ENCFS_CONFIG_TYPES) {
            /* Initialise volume */
            File targetDir = getConfigDir(encfsConfigType);
            assertEquals(Cryptonite.jniSuccess(), 
                    mActivity.jniInit(targetDir.getPath(), "password"));

            /* Make directories */

            /* Convert target path to encoded file name */
            String encodedDirPath = Cryptonite.jniEncode(DECRYPTED_DIR_NAME);
            File encodedDir = new File(encodedDirPath);
            
            assertTrue(encodedDir.mkdirs());
            
            /* Upload file */
            /* Convert current path to encoded file name */
            assertEquals(Cryptonite.jniSuccess(),
                    mActivity.jniEncrypt(
                            DECRYPTED_DIR_NAME + "/" + DECRYPTED_FILE_NAME, 
                            cacheDir + "/" + DECRYPTED_FILE_NAME, true));
            Cryptonite.jniResetVolume();
        }
        Cryptonite.deleteDir(getCacheDir());
    }
    
    public void testEncFS_3_Export() {
        for (int encfsConfigType : ENCFS_CONFIG_TYPES) {
            File targetDir = getConfigDir(encfsConfigType);
            assertEquals(Cryptonite.jniSuccess(), 
                    mActivity.jniInit(targetDir.getPath(), "password"));
        
            File decryptedCacheDir = getCacheDir();
            if (decryptedCacheDir.exists()) {
                assertTrue(Cryptonite.deleteDir(decryptedCacheDir));
            }
            assertTrue(decryptedCacheDir.mkdir());
            String encodedName = Cryptonite.jniEncode(DECRYPTED_DIR_NAME + "/" + DECRYPTED_FILE_NAME);
            String targetPath = decryptedCacheDir.getPath() 
                    + DECRYPTED_DIR_NAME + "/" + DECRYPTED_FILE_NAME;
            if (!new File(targetPath).getParentFile().exists()) {
                assertTrue(new File(targetPath).getParentFile().mkdirs());
            } else {
                assertTrue(Cryptonite.deleteDir(new File(targetPath).getParentFile()));
            }
            assertEquals(Cryptonite.jniSuccess(), 
                    mActivity.jniDecrypt(encodedName, decryptedCacheDir.getPath(), true));
            assertTrue((new File(targetPath)).exists());
        
            /* Read decrypted file */
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
                fail();
            }
            assertEquals(TEST_STRING, output);
            Cryptonite.jniResetVolume();
            Cryptonite.deleteDir(targetDir);
        }
    }
}
