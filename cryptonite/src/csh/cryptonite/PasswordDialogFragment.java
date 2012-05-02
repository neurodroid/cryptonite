package csh.cryptonite;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.actionbarsherlock.app.SherlockDialogFragment;

public class PasswordDialogFragment extends SherlockDialogFragment {

    public static PasswordDialogFragment newInstance() {
        PasswordDialogFragment frag = new PasswordDialogFragment();
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = (LayoutInflater) getActivity()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.password_dialog, 
                (ViewGroup) getActivity().findViewById(R.id.root));
        final EditText password = (EditText) layout.findViewById(R.id.EditText_Pwd);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.title_password);
        builder.setView(layout);
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                }
            });
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    ((FileDialog)getActivity()).currentPassword = password.getText().toString();
                    if (((FileDialog)getActivity()).currentPassword.length() > 0) {
                        switch (((FileDialog)getActivity()).selectionMode) {
                         case SelectionMode.MODE_OPEN_ENCFS_MOUNT:
                             ((FileDialog)getActivity()).mountEncFS(
                                     ((FileDialog)getActivity()).currentPath);
                             break;
                         case SelectionMode.MODE_OPEN_ENCFS:
                         case SelectionMode.MODE_OPEN_ENCFS_DB:
                             ((FileDialog)getActivity()).initEncFS(
                                     ((FileDialog)getActivity()).currentPath);
                             break;
                        }
                    } else {
                        ((FileDialog)getActivity()).showToast(R.string.empty_password);
                    }
                }
            });
        return builder.create();
    }
}
