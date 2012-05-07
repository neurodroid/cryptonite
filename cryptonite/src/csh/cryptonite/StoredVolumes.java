package csh.cryptonite;

import com.actionbarsherlock.app.SherlockFragmentActivity;

import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import com.actionbarsherlock.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import csh.cryptonite.contentprovider.CryptoniteContentProvider;
import csh.cryptonite.database.CryptoniteTable;

/*
 * TodosOverviewActivity displays the existing todo items
 * in a list
 * 
 * You can create new ones via the ActionBar entry "Insert"
 * You can delete existing ones via a long press on the item
 */

public class StoredVolumes extends SherlockFragmentActivity implements
        LoaderManager.LoaderCallbacks<Cursor>
{
    private static final int ACTIVITY_CREATE = 0;
    private static final int ACTIVITY_EDIT = 1;
    private static final int DELETE_ID = Menu.FIRST + 1;
    // private Cursor cursor;
    private SimpleCursorAdapter adapter;
    private ListView mListView;

    
/** Called when the activity is first created. */

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.stored_volumes);

        mListView = (ListView) findViewById(android.R.id.list);
        
        this.mListView.setDividerHeight(2);
        fillData();
        registerForContextMenu(mListView);

        mListView.setOnItemClickListener(new ListView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> l, View v, int position,
                    long id) {
                onListItemClick((ListView)l, v, position, id);
            }
            
        });
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case DELETE_ID:
            AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
                    .getMenuInfo();
            Uri uri = Uri.parse(CryptoniteContentProvider.CONTENT_URI + "/"
                    + info.id);
            getContentResolver().delete(uri, null, null);
            fillData();
            return true;
        }
        return super.onContextItemSelected(item);
    }

    // Opens the second activity if an entry is clicked
    private void onListItemClick(ListView l, View v, int position, long id) {
        /*Intent i = new Intent(this, TodoDetailActivity.class);
        Uri todoUri = Uri.parse(CryptoniteContentProvider.CONTENT_URI + "/" + id);
        i.putExtra(CryptoniteContentProvider.CONTENT_ITEM_TYPE, todoUri);

        // Activity returns an result if called with startActivityForResult
        startActivityForResult(i, ACTIVITY_EDIT);*/
    }

    // Called with the result of the other activity
    // requestCode was the origin request code send to the activity
    // resultCode is the return code, 0 is everything is ok
    // intend can be used to get data
    @Override
    protected void onActivityResult(int requestCode, int resultCode,
            Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
    }

    private void fillData() {

        // Fields from the database (projection)
        // Must include the _id column for the adapter to work
        String[] from = new String[] { CryptoniteTable.COLUMN_SUMMARY };
        // Fields on the UI to which we map
        int[] to = new int[] { R.id.stored_rowtext};

        getLoaderManager().initLoader(0, null, this);
        adapter = new SimpleCursorAdapter(this, R.layout.stored_volumes_row, null, from,
                to, 0);

        mListView.setAdapter(adapter);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0, DELETE_ID, 0, R.string.delete_entry);
    }

    // Creates a new loader after the initLoader () call
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] projection = { CryptoniteTable.COLUMN_ID, CryptoniteTable.COLUMN_SUMMARY };
        CursorLoader cursorLoader = new CursorLoader(this,
                CryptoniteContentProvider.CONTENT_URI, projection, null, null, null);
        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        adapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // data is not available anymore, delete reference
        adapter.swapCursor(null);
    }

}
