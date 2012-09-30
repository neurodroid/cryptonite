package csh.cryptonite.database;

import java.util.List;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockFragmentActivity;

import csh.cryptonite.R;

public class VolumesList extends SherlockFragmentActivity {

    private ListView mListView;
    private VolumesDataSource mDataSource;

    /** Called when the activity is first created. */
    @Override public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.volumes);

        mListView = (ListView) findViewById(android.R.id.list);

        mDataSource = new VolumesDataSource(this);
        mDataSource.open();

        List<Volume> values = mDataSource.getAllVolumes();
        ArrayAdapter<Volume> adapter = new ArrayAdapter<Volume>(this,
                android.R.layout.simple_list_item_1, values);
        mListView.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        mDataSource.open();
        super.onResume();
    }

    @Override
    protected void onPause() {
        mDataSource.close();
        super.onPause();
    }
}
