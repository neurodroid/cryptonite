package csh.cryptonite;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;

public class ProgressDialogFragment extends SherlockDialogFragment {

    public static ProgressDialogFragment newInstance(int msgId) {
        ProgressDialogFragment frag = new ProgressDialogFragment();
        Bundle args = new Bundle();
        args.putInt("msgId", msgId);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        setCancelable(false);
        final int msgId = getArguments().getInt("msgId");
        final ProgressDialog pd = new ProgressDialog(getActivity());
        pd.setTitle(getString(R.string.wait_msg));
        pd.setMessage(getString(msgId));
        pd.setIndeterminate(true);
        return pd;
    }
    
    /* This brainf*k is required to survive orientation changes without fc */
    public static void showDialog(SherlockFragmentActivity activity, int msgId, String tag) {
        FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
        try {
            ProgressDialogFragment prev = (ProgressDialogFragment)activity.getSupportFragmentManager().findFragmentByTag(tag);
            if (prev != null) {
                ft.remove(prev);
            }
        } catch (NullPointerException e) {
            
        }

        ft.addToBackStack(null);

        ProgressDialogFragment pdFragment = ProgressDialogFragment.newInstance(msgId);
        pdFragment.show(ft, tag);
    }

    public static void dismissDialog(SherlockFragmentActivity activity, String tag) {
        ProgressDialogFragment prev = null;
        try {
            prev = (ProgressDialogFragment)activity.getSupportFragmentManager().findFragmentByTag(tag);
        } catch (NullPointerException e) {
            
        }
        if (prev != null) {
            FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
            ft.remove(prev);
            ft.commit();
        }
    }
}
