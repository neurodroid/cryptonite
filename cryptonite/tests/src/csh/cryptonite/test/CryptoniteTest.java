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
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
    private int[] encfs_config_types;
    private static final String TEST_STRING = "6*8 und sieben Füchse außerdem";
    private static final String TEST_DIR="csh.cryptonite.test";
    private static final String DECRYPTED_DIR_NAME = "/arthur/dent"; 
    private static final String DECRYPTED_FILE_NAME = "42";
    private static final String LARGE_FILE_DIR = "/mnt/sdcard";
    private static final String LARGE_FILE_NAME = "2012-01-21-24-52-07.jpg";
    
    /**
     * Creates an {@link ActivityInstrumentationTestCase2} for the Cryptonite activity.
     */
    public CryptoniteTest() {
        super(Cryptonite.class);

        encfs_config_types = new int[2];
        encfs_config_types[0] = CreateEncFS.CONFIG_PARANOIA;
        encfs_config_types[1] = CreateEncFS.CONFIG_COMPATIBLE;

        for (int encfsConfigType : encfs_config_types) {
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

    private String md5(byte[] b) {
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(b);
            return new BigInteger(1, digest.digest()).toString(16);
            
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    // Returns the contents of the file in a byte array.
    private static byte[] getBytesFromFile(File file) throws IOException {
        InputStream is = new FileInputStream(file);

        // Get the size of the file
        long length = file.length();

        // You cannot create an array using a long type.
        // It needs to be an int type.
        // Before converting to an int type, check
        // to ensure that file is not larger than Integer.MAX_VALUE.
        if (length > Integer.MAX_VALUE) {
            // File is too large
        }

        // Create the byte array to hold the data
        byte[] bytes = new byte[(int)length];

        // Read in the bytes
        int offset = 0;
        int numRead = 0;
        while (offset < bytes.length
               && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
            offset += numRead;
        }

        // Ensure all the bytes have been read in
        if (offset < bytes.length) {
            throw new IOException("Could not completely read file "+file.getName());
        }

        // Close the input stream and return bytes
        is.close();
        return bytes;
    }
    
    /**
     * Verifies that the activity under test can be launched.
     */
    public void testEncFS_0_Preconditions() {
        assertNotNull("activity should be launched successfully", mActivity);
    }
    
    public void testEncFS_1_Create() {
        for (int encfsConfigType : encfs_config_types) {
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
        for (int encfsConfigType : encfs_config_types) {
            /* Initialise volume */
            File targetDir = getConfigDir(encfsConfigType);
            assertEquals(Cryptonite.jniSuccess(), 
                         Cryptonite.jniInit(targetDir.getPath(), "password", false, ""));

            /* Make directories */

            /* Convert target path to encoded file name */
            String encodedDirPath = Cryptonite.jniEncode(DECRYPTED_DIR_NAME);
            File encodedDir = new File(encodedDirPath);
            
            assertTrue(encodedDir.mkdirs());
            
            /* Upload small file */
            /* Convert current path to encoded file name */
            assertEquals(Cryptonite.jniSuccess(),
                    Cryptonite.jniEncrypt(
                            DECRYPTED_DIR_NAME + "/" + DECRYPTED_FILE_NAME, 
                            cacheDir + "/" + DECRYPTED_FILE_NAME, true));
            
            /* Upload large file */
            assertEquals(Cryptonite.jniSuccess(),
                    Cryptonite.jniEncrypt(
                            DECRYPTED_DIR_NAME + "/" + LARGE_FILE_NAME, 
                            LARGE_FILE_DIR + "/" + LARGE_FILE_NAME, true));
                        
            Cryptonite.jniResetVolume();
        }
        Cryptonite.deleteDir(getCacheDir());
    }
    
    public void testEncFS_3_Export() {
        for (int encfsConfigType : encfs_config_types) {
            File targetDir = getConfigDir(encfsConfigType);
            assertEquals(Cryptonite.jniSuccess(), 
                         Cryptonite.jniInit(targetDir.getPath(), "password", false, ""));
        
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
                    Cryptonite.jniDecrypt(encodedName, decryptedCacheDir.getPath(), true));
            assertTrue((new File(targetPath)).exists());
            
            /* Decrypt to buffer */
            byte[] buf = Cryptonite.jniDecryptToBuffer(encodedName);
        
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
            
            /* Check decrypted buffer */
            String outputFromBuffer = "";
            try {
                outputFromBuffer = new String(buf, "utf-8");
            } catch (UnsupportedEncodingException e) {
                fail();
            }
            Log.v(Cryptonite.TAG, outputFromBuffer);
            assertEquals(TEST_STRING, outputFromBuffer);
            
            /* Check md5 sum of large file */
            encodedName = Cryptonite.jniEncode(DECRYPTED_DIR_NAME + "/" + LARGE_FILE_NAME);
            targetPath = decryptedCacheDir.getPath() 
                    + DECRYPTED_DIR_NAME + "/" + LARGE_FILE_NAME;

            assertEquals(Cryptonite.jniSuccess(), 
                    Cryptonite.jniDecrypt(encodedName, decryptedCacheDir.getPath(), true));
            assertTrue((new File(targetPath)).exists());
            
            /* Compare md5 sums */
            String md5original = "A";
            String md5decrypted = "B";
            try {
                md5original = md5(getBytesFromFile(new File(LARGE_FILE_DIR + "/" + LARGE_FILE_NAME)));
                md5decrypted = md5(getBytesFromFile(new File(targetPath)));
            } catch (IOException e) {
                fail();
            }
            Log.i(Cryptonite.TAG, "Original checksum: " + md5original);
            Log.i(Cryptonite.TAG, "Decrypted checksum: " + md5decrypted);
            assertEquals(md5original, md5decrypted);
            
            Cryptonite.jniResetVolume();
            Cryptonite.deleteDir(targetDir);
        }
    }
    
}
