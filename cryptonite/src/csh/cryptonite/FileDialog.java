/*
  Based on android-file-dialog
  http://code.google.com/p/android-file-dialog/
  alexander.ponomarev.1@gmail.com
  New BSD License
*/

package csh.cryptonite;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

public class FileDialog extends ListActivity {

    private static final String ITEM_KEY = "key";
    private static final String ITEM_IMAGE = "image";
    private static final String ROOT = "/";

    public static final String START_PATH = "START_PATH";
    public static final String RESULT_PATH = "RESULT_PATH";
    public static final String SELECTION_MODE = "SELECTION_MODE";
    public static final String LABEL = "LABEL";
    public static final String BUTTON_LABEL = "BUTTON_LABEL";
    public static final String CURRENT_ROOT = "CURRENT_ROOT";
    public static final String CURRENT_ROOT_NAME = "CURRENT_ROOT_NAME";

    private String currentRoot = ROOT;
    private String currentRootName = ROOT;
    private List<String> path = null;
    private TextView myPath;
    private EditText mFileName;
    private ArrayList<HashMap<String, Object>> mList;

    private Button selectButton;

    private LinearLayout layoutSelect;
    private LinearLayout layoutCreate;
    private InputMethodManager inputManager;
    private String parentPath;
    private String currentPath = currentRoot;
    
    @SuppressWarnings("unused")
    private int selectionMode = SelectionMode.MODE_CREATE;

    @SuppressWarnings("unused")
    private File selectedFile;
    private HashMap<String, Integer> lastPositions = new HashMap<String, Integer>();

    /** Called when the activity is first created. */
    @Override
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED, getIntent());

        setContentView(R.layout.file_dialog_main);
        myPath = (TextView) findViewById(R.id.path);
        mFileName = (EditText) findViewById(R.id.fdEditTextFile);

        inputManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

        String currentRoot = getIntent().getStringExtra(CURRENT_ROOT);
        if (currentRoot == null) {
            currentRoot = ROOT;
        }
        currentPath = currentRoot;

        String currentRootName = getIntent().getStringExtra(CURRENT_ROOT_NAME);
        if (currentRootName == null) {
            currentRootName = currentRoot;
        }

        String buttonLabel = getIntent().getStringExtra(BUTTON_LABEL);
        selectButton = (Button) findViewById(R.id.fdButtonSelect);
        selectButton.setEnabled(true);
        selectButton.setText(buttonLabel);
        selectButton.setOnClickListener(new OnClickListener() {

                public void onClick(View v) {
                    /*
                      if (selectedFile != null) {
                      getIntent().putExtra(RESULT_PATH, selectedFile.getPath());
                      setResult(RESULT_OK, getIntent());
                      finish();
                      } else {
                    */
                    /* get current path */
                    if (currentPath != null) {
                        getIntent().putExtra(RESULT_PATH, currentPath);
                        setResult(RESULT_OK, getIntent());
                        finish();
                    }
                    /* } */
                }
            });
        selectionMode = getIntent().getIntExtra(SELECTION_MODE,
                                                SelectionMode.MODE_CREATE);
        /*
          final Button newButton = (Button) findViewById(R.id.fdButtonNew);
          newButton.setOnClickListener(new OnClickListener() {

          @Override
          public void onClick(View v) {
          setCreateVisible(v);

          mFileName.setText("");
          mFileName.requestFocus();
          }
          });

          if (selectionMode == SelectionMode.MODE_OPEN) {
          newButton.setEnabled(false);
          }
        */
        layoutSelect = (LinearLayout) findViewById(R.id.fdLinearLayoutSelect);
        layoutCreate = (LinearLayout) findViewById(R.id.fdLinearLayoutCreate);
        layoutCreate.setVisibility(View.GONE);

        final Button cancelButton = (Button) findViewById(R.id.fdButtonCancel);
        cancelButton.setOnClickListener(new OnClickListener() {

                public void onClick(View v) {
                    setSelectVisible(v);
                }

            });
        final Button createButton = (Button) findViewById(R.id.fdButtonCreate);
        createButton.setOnClickListener(new OnClickListener() {

                public void onClick(View v) {
                    if (mFileName.getText().length() > 0) {
                        getIntent().putExtra(RESULT_PATH,
                                             currentPath + "/" + mFileName.getText());
                        setResult(RESULT_OK, getIntent());
                        finish();
                    }
                }
            });

        String startPath = getIntent().getStringExtra(START_PATH);
        if (startPath != null) {
            getDir(startPath, currentRoot, currentRootName);
        } else {
            getDir(currentRoot, currentRoot, currentRootName);
        }
        String label = getIntent().getStringExtra(LABEL);
        this.setTitle(label);
    }

    private void getDir(String dirPath, String rootPath, String rootName) {

        boolean useAutoSelection = dirPath.length() < currentPath.length();

        Integer position = lastPositions.get(parentPath);

        getDirImpl(dirPath, rootPath, rootName);

        if (position != null && useAutoSelection) {
            getListView().setSelection(position);
        }

    }

    private void getDirImpl(final String dirPath, final String rootPath, final String rootName) {

        currentPath = dirPath;
        currentRoot = rootPath;
        currentRootName = rootName;
        
        final List<String> item = new ArrayList<String>();
        path = new ArrayList<String>();
        mList = new ArrayList<HashMap<String, Object>>();

        File f = new File(currentPath);
        File[] files = f.listFiles();
        if (files == null) {
            Log.v(Cryptonite.TAG, "No files in current path");
            currentPath = currentRoot;
            f = new File(currentPath);
            files = f.listFiles();
        }
        myPath.setText(getText(R.string.location) + ": " + currentRootName +
                       currentPath.substring(currentRoot.length()));

        if (!currentPath.equals(currentRoot)) {

            item.add(currentRoot);
            addItem(currentRootName, R.drawable.folder);
            path.add(currentRoot);

            item.add("../");
            addItem("../", R.drawable.folder);
            path.add(f.getParent());
            parentPath = f.getParent();

        }

        TreeMap<String, String> dirsMap = new TreeMap<String, String>();
        TreeMap<String, String> dirsPathMap = new TreeMap<String, String>();
        TreeMap<String, String> filesMap = new TreeMap<String, String>();
        TreeMap<String, String> filesPathMap = new TreeMap<String, String>();
        for (File file : files) {
            if (file.isDirectory()) {
                String dirName = file.getName();
                dirsMap.put(dirName, dirName);
                dirsPathMap.put(dirName, file.getPath());
            } else {
                filesMap.put(file.getName(), file.getName());
                filesPathMap.put(file.getName(), file.getPath());
            }
        }
        item.addAll(dirsMap.tailMap("").values());
        item.addAll(filesMap.tailMap("").values());
        path.addAll(dirsPathMap.tailMap("").values());
        path.addAll(filesPathMap.tailMap("").values());

        SimpleAdapter fileList = new SimpleAdapter(this, mList,
                                                   R.layout.file_dialog_row,
                                                   new String[] { ITEM_KEY, ITEM_IMAGE }, new int[] {
                                                       R.id.fdrowtext, R.id.fdrowimage });

        for (String dir : dirsMap.tailMap("").values()) {
            addItem(dir, R.drawable.folder);
        }

        for (String file : filesMap.tailMap("").values()) {
            addItem(file, R.drawable.file);
        }

        fileList.notifyDataSetChanged();

        setListAdapter(fileList);

    }

    private void addItem(String fileName, int imageId) {
        HashMap<String, Object> item = new HashMap<String, Object>();
        item.put(ITEM_KEY, fileName);
        item.put(ITEM_IMAGE, imageId);
        mList.add(item);
    }

    @Override
	protected void onListItemClick(ListView l, View v, int position, long id) {

        File file = new File(path.get(position));

        setSelectVisible(v);

        if (file.isDirectory()) {
            selectButton.setEnabled(true);
            if (file.canRead()) {
                lastPositions.put(currentPath, position);
                getDir(path.get(position), currentRoot, currentRootName);
            } else {
                new AlertDialog.Builder(this)
                    .setIcon(R.drawable.icon)
                    .setTitle(
                              "[" + file.getName() + "] "
                              + getText(R.string.cant_read_folder))
                    .setPositiveButton("OK",
                                       new DialogInterface.OnClickListener() {

                                           public void onClick(DialogInterface dialog,
                                                                   int which) {

                                           }
                                       }).show();
            }
        } else {
            selectedFile = file;
            v.setSelected(true);
            selectButton.setEnabled(true);
        }
    }

    @Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            selectButton.setEnabled(true);

            if (layoutCreate.getVisibility() == View.VISIBLE) {
                layoutCreate.setVisibility(View.GONE);
                layoutSelect.setVisibility(View.VISIBLE);
            } else {
                if (!currentPath.equals(currentRoot)) {
                    getDir(parentPath, currentRoot, currentRootName);
                } else {
                    return super.onKeyDown(keyCode, event);
                }
            }

            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

//    private void setCreateVisible(View v) {
//        layoutCreate.setVisibility(View.VISIBLE);
//        layoutSelect.setVisibility(View.GONE);
//
//        inputManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
//        selectButton.setEnabled(false);
//    }

    private void setSelectVisible(View v) {
        layoutCreate.setVisibility(View.GONE);
        layoutSelect.setVisibility(View.VISIBLE);

        inputManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
        selectButton.setEnabled(false);
    }
}