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

package csh.cryptonite;

import android.content.Context;
import android.test.ActivityInstrumentationTestCase2;

import java.io.File;


/**
 * Make sure that the main launcher activity opens up properly, which will be
 * verified by {@link #testActivityTestCaseSetUpProperly}.
 */
public class CryptoniteTest extends ActivityInstrumentationTestCase2<Cryptonite> {

    private Cryptonite mActivity;
    
    /**
     * Creates an {@link ActivityInstrumentationTestCase2} for the Cryptonite activity.
     */
    public CryptoniteTest() {
        super(Cryptonite.class);
    }

    @Override
        protected void setUp() throws Exception {
        super.setUp();
        mActivity = this.getActivity();
        cpEncFSTest();
    }
    
    /** Copy EncFS test volumes */
    public void cpEncFSTest() {
        String[] encfsTypes = {"aes-256", "blowfish-128"};

        for (String encfsType : encfsTypes) {
            File targetDir = mActivity.getDir(encfsType, Context.MODE_PRIVATE);
        }
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
    }
    
}
