package csh.cryptonite;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class ExpertFragment extends Fragment {
    
    public TextView tv;
    
    public Button buttonTermPrompt, buttonTermPromptRoot;
    
    private Cryptonite mAct;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState)
    {
        View v = inflater.inflate(R.layout.expert_tab, container, false);

        mAct = (Cryptonite)getActivity();
        
        tv = (TextView)v.findViewById(R.id.tvVersionExpert);
        tv.setText(mAct.textOut);


        /* Run terminal with environment set up */
        buttonTermPrompt = (Button)v.findViewById(R.id.btnTermPrompt);
        buttonTermPrompt.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    mAct.launchTerm();
                }});

        /* Run terminal with environment set up */
        buttonTermPromptRoot = (Button)v.findViewById(R.id.btnTermPromptRoot);
        buttonTermPromptRoot.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    mAct.launchTerm(true);
                }});

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (tv != null) {
            tv.setText(mAct.textOut);
            tv.invalidate();
        }
    }
}

