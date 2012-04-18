/*
 * Copyright (c) 2011 Dropbox, Inc.
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */


package csh.cryptonite;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.UploadRequest;
import com.dropbox.client2.ProgressListener;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.exception.DropboxFileSizeException;
import com.dropbox.client2.exception.DropboxIOException;
import com.dropbox.client2.exception.DropboxParseException;
import com.dropbox.client2.exception.DropboxPartialFileException;
import com.dropbox.client2.exception.DropboxServerException;
import com.dropbox.client2.exception.DropboxUnlinkedException;

/**
 * Here we show uploading a file in a background thread, trying to show
 * typical exception handling and flow of control for an app that uploads a
 * file from Dropbox.
 */
public class UploadEncrypted extends AsyncTask<Void, Long, Boolean> {

    public static final String DIALOG_TAG = "dbProgDlg";
    
    private DropboxAPI<?> mApi;
    private String mPath;
    private File mFile;

    private long mFileLen;
    private UploadRequest mRequest;
    private Context mContext;
    private SherlockFragmentActivity mActivity;

    private String mErrorMsg;


    public UploadEncrypted(final SherlockFragmentActivity activity, DropboxAPI<?> api, String dropboxPath,
            File file)
    {
        mActivity = activity;
        
        // We set the context this way so we don't accidentally leak activities
        mContext = mActivity.getApplicationContext();

        mFileLen = file.length();
        mApi = api;
        mPath = dropboxPath;
        mFile = file;
        
    }
    
    @Override
    protected void onPreExecute() {
        FragmentTransaction ft = mActivity.getSupportFragmentManager().beginTransaction();
        try {
            DBProgressDialogFragment prev = 
                    (DBProgressDialogFragment)mActivity.getSupportFragmentManager()
                    .findFragmentByTag(UploadEncrypted.DIALOG_TAG);
            if (prev != null) {
                ft.remove(prev);
            }
        } catch (NullPointerException e) {
            
        }

        /* ft.addToBackStack(null); */

        DBProgressDialogFragment pdFragment = 
                DBProgressDialogFragment.newInstance(mFile.getName());
        pdFragment.show(ft, UploadEncrypted.DIALOG_TAG);
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        try {
            // By creating a request, we get a handle to the putFile operation,
            // so we can cancel it later if we want to
            
            FileInputStream fis = new FileInputStream(mFile);
            String path = mPath + mFile.getName();
            mRequest = mApi.putFileOverwriteRequest(path, fis, mFile.length(),
                    new ProgressListener()
            {
                @Override
                public long progressInterval() {
                    // Update the progress bar every half-second or so
                    return 500;
                }

                @Override
                public void onProgress(long bytes, long total) {
                    publishProgress(bytes);
                }
            });

            if (mRequest != null) {
                mRequest.upload();
                return true;
            }

        } catch (DropboxUnlinkedException e) {
            // This session wasn't authenticated properly or user unlinked
            mErrorMsg = "This app wasn't authenticated properly.";
        } catch (DropboxFileSizeException e) {
            // File size too big to upload via the API
            mErrorMsg = "This file is too big to upload";
        } catch (DropboxPartialFileException e) {
            // We canceled the operation
            mErrorMsg = "Upload canceled";
        } catch (DropboxServerException e) {
            // Server-side exception.  These are examples of what could happen,
            // but we don't do anything special with them here.
            if (e.error == DropboxServerException._401_UNAUTHORIZED) {
                // Unauthorized, so we should unlink them.  You may want to
                // automatically log the user out in this case.
            } else if (e.error == DropboxServerException._403_FORBIDDEN) {
                // Not allowed to access this
            } else if (e.error == DropboxServerException._404_NOT_FOUND) {
                // path not found (or if it was the thumbnail, can't be
                // thumbnailed)
            } else if (e.error == DropboxServerException._507_INSUFFICIENT_STORAGE) {
                // user is over quota
            } else {
                // Something else
            }
            // This gets the Dropbox error, translated into the user's language
            mErrorMsg = e.body.userError;
            if (mErrorMsg == null) {
                mErrorMsg = e.body.error;
            }
        } catch (DropboxIOException e) {
            // Happens all the time, probably want to retry automatically.
            mErrorMsg = "Network error.  Try again.";
        } catch (DropboxParseException e) {
            // Probably due to Dropbox server restarting, should retry
            mErrorMsg = "Dropbox error.  Try again.";
        } catch (DropboxException e) {
            // Unknown error
            mErrorMsg = "Unknown error.  Try again.";
        } catch (FileNotFoundException e) {
            mErrorMsg = "Source file not readable: " + e.getMessage();
        }
        return false;
    }

    @Override
    protected void onProgressUpdate(Long... progress) {
        int percent = (int)(100.0*(double)progress[0]/mFileLen + 0.5);
        DBProgressDialogFragment prev = null;
        try {
            prev = (DBProgressDialogFragment)mActivity
                    .getSupportFragmentManager().findFragmentByTag(DIALOG_TAG);
        } catch (NullPointerException e) {
            
        }
        if (prev != null) {
            ((ProgressDialog)prev.getDialog()).setProgress(percent);
        }
    }

    @Override
    protected void onPostExecute(Boolean result) {
        DBProgressDialogFragment.dismissDialog(mActivity, DIALOG_TAG);
        if (mRequest != null) {
            mRequest.abort();
        }
        if (result) {
            showToast(mContext.getString(R.string.dropbox_upload_successful));
        } else {
            showToast(mErrorMsg);
        }
    }

    private void showToast(String msg) {
        Toast error = Toast.makeText(mContext, msg, Toast.LENGTH_LONG);
        error.show();
    }
    
    public static class DBProgressDialogFragment extends SherlockDialogFragment {

        public static DBProgressDialogFragment newInstance(String fileName) {
            DBProgressDialogFragment frag = new DBProgressDialogFragment();
            Bundle args = new Bundle();
            args.putString("fileName", fileName);
            frag.setArguments(args);
            return frag;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            setCancelable(false);
            final String fileName = getArguments().getString("fileName");
            final ProgressDialog pDialog = new ProgressDialog(getActivity());
            pDialog.setTitle(getString(R.string.wait_msg));
            pDialog.setMax(100);
            pDialog.setMessage(getActivity().getString(R.string.dropbox_uploading) + " " + fileName);
            pDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            pDialog.setProgress(0);
            pDialog.setCancelable(false);
            pDialog.setButton(DialogInterface.BUTTON_NEGATIVE, 
                    getActivity().getString(R.string.cancel), new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    DBProgressDialogFragment.dismissDialog((SherlockFragmentActivity)getActivity(), DIALOG_TAG);
                }
            });
            return pDialog;
        }
        
        public static void dismissDialog(SherlockFragmentActivity activity, String tag) {
            DBProgressDialogFragment prev = null;
            try {
                prev = (DBProgressDialogFragment)activity.getSupportFragmentManager().findFragmentByTag(tag);
            } catch (NullPointerException e) {
                
            }
            if (prev != null) {
                FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
                ft.remove(prev);
                ft.commit();
            }
        }
    }    
}
