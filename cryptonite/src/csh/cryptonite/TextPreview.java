package csh.cryptonite;

import com.actionbarsherlock.app.SherlockFragmentActivity;

import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class TextPreview extends SherlockFragmentActivity {

    public static final String PREVIEW_TITLE="pv_title", PREVIEW_BODY="pv_body";
    public static final int REQUEST_PREVIEW=31;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.text_preview);

        EditText etBody = (EditText)findViewById(R.id.preview_body);
        Button backButton = (Button) findViewById(R.id.preview_back);
       
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String title = extras.getString(PREVIEW_TITLE);
            String body = extras.getString(PREVIEW_BODY);
            if (title != null) {
                setTitle(title);
            }
            if (body != null) {
                etBody.setText(body);
            }
        }
        
        backButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                setResult(RESULT_OK);
                finish();
            }
            
        });
    }
    
}
