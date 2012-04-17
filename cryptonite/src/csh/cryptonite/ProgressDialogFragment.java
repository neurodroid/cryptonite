package csh.cryptonite;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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
    
    public static void showDialog(SherlockFragmentActivity activity, int msgId, String tag) {
        Fragment prev = activity.getSupportFragmentManager().findFragmentByTag(tag);
        FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        SherlockDialogFragment pdFragment = ProgressDialogFragment.newInstance(msgId);
        pdFragment.show(ft, tag);
    }

    public static void dismissDialog(SherlockFragmentActivity activity, String tag) {
        Fragment prev = activity.getSupportFragmentManager().findFragmentByTag(tag);
        if (prev != null) {
            FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
            ft.remove(prev);
            ft.commit();
        }
    }
}
