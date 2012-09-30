// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.

// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

// Copyright (c) 2012, Christoph Schmidt-Hieber

// Based on android-file-dialog
// http://code.google.com/p/android-file-dialog/
// alexander.ponomarev.1@gmail.com

package csh.cryptonite;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLConnection;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem.OnMenuItemClickListener;

import csh.cryptonite.Cryptonite.DecodedBuffer;
import csh.cryptonite.storage.Storage;
import csh.cryptonite.storage.StorageManager;
import csh.cryptonite.storage.VirtualFile;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;

import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class FileDialog extends SherlockFragmentActivity {

    private static final String ITEM_KEY = "key";
    private static final String ITEM_IMAGE = "image";
    private static final String ITEM_CHECK = "check";
    private static final String ITEM_ENABLED = "enabled";
    private static final String ITEM_FILE = "file";
    private static final String ROOT = "/";

    public static final String START_PATH = "START_PATH";
    public static final String RESULT_EXPORT_PATHS = "csh.cryptonite.RESULT_EXPORT_PATHS";
    public static final String RESULT_OPEN_PATH = "csh.cryptonite.RESULT_OPEN_PATH";
    public static final String RESULT_UPLOAD_PATH = "csh.cryptonite.RESULT_UPLOAD_PATH";
    public static final String RESULT_SELECTED_FILE = "csh.cryptonite.RESULT_SELECTED_FILE";
    public static final String SELECTION_MODE = "SELECTION_MODE";
    public static final String LABEL = "LABEL";
    public static final String BUTTON_LABEL = "BUTTON_LABEL";
    public static final String CURRENT_ROOT = "CURRENT_ROOT";
    public static final String CURRENT_ROOT_NAME = "CURRENT_ROOT_NAME";
    public static final String CURRENT_UPLOAD_TARGET_PATH = "CURRENT_UPLOAD_TARGET_PATH";
    public static final String ENCFS_BROWSE_ROOT = "ENCFS_BROWSE_ROOT";
    public static final String CURRENT_EXPORT_PATH_LIST = "CURRENT_EXPORT_PATH_LIST";

    private String currentRoot = ROOT;
    private String currentRootLabel = ROOT;
    private String alertMsg = "";
    private boolean alert = false;
    private List<String> pathList = null;
    private TextView myPath;
    private ArrayList<HashMap<String, Object>> mList;

    private Button selectButton;

    private LinearLayout layoutSelect;
    
    private InputMethodManager inputManager;
    private String parentPath;
    private String currentPath;
    private String intentStartPath;
    private String currentPassword = "\0";
    private String currentUploadTargetPath;
    private String encfsBrowseRoot;
    private String[] currentExportPathList;
    
    private int selectionMode;

    private VirtualFile selectedFile;
    private HashMap<String, Integer> lastPositions = new HashMap<String, Integer>();

    private Set<String> selectedPaths = new HashSet<String>();

    private boolean localFilePickerStorage;

    private ListView mListView;

    /** Called when the activity is first created. */
    @Override
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED, getIntent());

        setContentView(R.layout.file_dialog_main);
        
        mListView = (ListView) findViewById(android.R.id.list);

        myPath = (TextView) findViewById(R.id.path);

        inputManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

        selectionMode = getIntent().getIntExtra(SELECTION_MODE, SelectionMode.MODE_OPEN_ENCFS);

        currentRoot = getIntent().getStringExtra(CURRENT_ROOT);
        if (currentRoot == null) {
            currentRoot = ROOT;
        }
        currentPath = currentRoot;

        currentRootLabel = getIntent().getStringExtra(CURRENT_ROOT_NAME);
        if (currentRootLabel == null) {
            currentRootLabel = currentRoot;
        }

        intentStartPath = getIntent().getStringExtra(START_PATH);
        String startPath = intentStartPath;
        
        currentUploadTargetPath = getIntent().getStringExtra(CURRENT_UPLOAD_TARGET_PATH);
        encfsBrowseRoot = getIntent().getStringExtra(ENCFS_BROWSE_ROOT);
        currentExportPathList= getIntent().getStringArrayExtra(CURRENT_EXPORT_PATH_LIST);

        if (savedInstanceState != null) {
            if (savedInstanceState.getString("parentPath") != null) {
                parentPath = savedInstanceState.getString("parentPath");
            }
            if (savedInstanceState.getStringArrayList("pathList") != null) {
                pathList = savedInstanceState.getStringArrayList("pathList");
            }
            if (savedInstanceState.getString("currentPath") != null) {
                currentPath = savedInstanceState.getString("currentPath");
                startPath = currentPath;
            }
            if (savedInstanceState.getString("currentUploadTargetPath") != null) {
                currentUploadTargetPath = savedInstanceState.getString("currentUploadTargetPath");
            }
            if (savedInstanceState.getString("encfsBrowseRoot") != null) {
                encfsBrowseRoot = savedInstanceState.getString("encfsBrowseRoot");
            }
            if (savedInstanceState.getStringArray("currentExportPathList") != null) {
                currentExportPathList = savedInstanceState.getStringArray("currentExportPathList");
            }
            if (savedInstanceState.getString("currentRoot") != null) {
                currentRoot = savedInstanceState.getString("currentRoot");
            }
            if (savedInstanceState.getString("currentRootLabel") != null) {
                currentRootLabel = savedInstanceState.getString("currentRootLabel");
            }
            if (savedInstanceState.getInt("selectionMode") != 0) {
                selectionMode = savedInstanceState.getInt("selectionMode");
            }

            int storageType = savedInstanceState.getInt("storageType");
            if (storageType != Storage.STOR_UNDEFINED) {
                StorageManager.INSTANCE.initEncFSStorage(this, storageType);
                if (savedInstanceState.getString("encFSPath") != null) {
                    StorageManager.INSTANCE.setEncFSPath(savedInstanceState.getString("encFSPath"));
                }
            }
            
        }
        
        switch (selectionMode) {
        case SelectionMode.MODE_OPEN_CREATE_DB:
        case SelectionMode.MODE_OPEN_ENCFS_DB:
        case SelectionMode.MODE_OPEN_DEFAULT_DB:
        case SelectionMode.MODE_OPEN_MULTISELECT_DB:
        case SelectionMode.MODE_OPEN_MULTISELECT:
            localFilePickerStorage = false;
            break;
        default:
            localFilePickerStorage = true;
        }

        String buttonLabel = getIntent().getStringExtra(BUTTON_LABEL);
        selectButton = (Button) findViewById(R.id.fdButtonSelect);
        selectButton.setEnabled(selectionMode != SelectionMode.MODE_OPEN_UPLOAD_SOURCE);
        selectButton.setText(buttonLabel);
        selectButton.setOnClickListener(new OnClickListener() {

                public void onClick(View v) {
                    switch (selectionMode) {
                    case SelectionMode.MODE_OPEN_ENCFS:
                    case SelectionMode.MODE_OPEN_ENCFS_DB:
                    case SelectionMode.MODE_OPEN_DEFAULT:
                    case SelectionMode.MODE_OPEN_DEFAULT_DB:
                    case SelectionMode.MODE_OPEN_ENCFS_MOUNT:
                        showPasswordDialog();
                        break;
                    case SelectionMode.MODE_OPEN_UPLOAD_SOURCE:
                        if (selectedFile != null && selectedFile.getPath() != null) {
                            encryptEncFSFile(selectedFile.getPath());
                        } else {
                            if (selectedFile.getPath() != null) {
                                showToast(getString(R.string.file_not_found)+ selectedFile.getPath());
                            } else {
                                showToast(getString(R.string.file_not_found)+ "null");
                            }
                        }
                        break;
                    case SelectionMode.MODE_OPEN_CREATE:
                    case SelectionMode.MODE_OPEN_CREATE_DB:
                        if (currentPath != null) {
                            getIntent().putExtra(RESULT_OPEN_PATH, (String)null);
                            getIntent().putExtra(RESULT_UPLOAD_PATH, (String)null);
                            getIntent().putExtra(RESULT_EXPORT_PATHS, currentPath);
                            if (selectedFile != null) {
                                getIntent().putExtra(RESULT_SELECTED_FILE, selectedFile.getPath());
                            } else {
                                getIntent().putExtra(RESULT_SELECTED_FILE, (String)null);
                            }
                            setResult(RESULT_OK, getIntent());
                            finish();
                        }
                        break;
                    case SelectionMode.MODE_OPEN_EXPORT_TARGET:
                        exportEncFSFiles(currentExportPathList);
                        break;
                    default:
                        showExportWarning(selectedPaths.toArray(new String[0]));
                    }
                }
            });

        final Button uploadButton = (Button) findViewById(R.id.fdButtonUpload);
        uploadButton.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                showUploadWarning(currentPath);
            }
            
        });

        layoutSelect = (LinearLayout) findViewById(R.id.fdLinearLayoutSelect);
        
        switch (selectionMode) {
        case SelectionMode.MODE_OPEN_ENCFS:
        case SelectionMode.MODE_OPEN_ENCFS_DB:
        case SelectionMode.MODE_OPEN_DEFAULT:
        case SelectionMode.MODE_OPEN_DEFAULT_DB:
        case SelectionMode.MODE_OPEN_ENCFS_MOUNT:
        case SelectionMode.MODE_OPEN_UPLOAD_SOURCE:
        case SelectionMode.MODE_OPEN_CREATE:
        case SelectionMode.MODE_OPEN_CREATE_DB:
        case SelectionMode.MODE_OPEN_EXPORT_TARGET:
            uploadButton.setVisibility(View.GONE);
            uploadButton.setWidth(0);
            selectButton.setWidth(selectButton.getWidth()*2);
        default:
        }

        if (startPath != null) {
            getDir(startPath, currentRoot, currentRootLabel);
        } else {
            getDir(currentRoot, currentRoot, currentRootLabel);
        }
        String label = getIntent().getStringExtra(LABEL);
        this.setTitle(label);
        switch (selectionMode) {
        case SelectionMode.MODE_OPEN_MULTISELECT:
        case SelectionMode.MODE_OPEN_MULTISELECT_DB:
            registerForContextMenu(mListView);
            break;
        }
        
        mListView.setOnItemClickListener(new ListView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> l, View v, int position,
                    long id) {
                onListItemClick((ListView)l, v, position, id);
            }
            
        });

        switch (selectionMode) {
        case SelectionMode.MODE_OPEN_DEFAULT:
        case SelectionMode.MODE_OPEN_DEFAULT_DB:
            showPasswordDialog();
            
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        switch (selectionMode) {
        case SelectionMode.MODE_CREATE:
        case SelectionMode.MODE_OPEN_MULTISELECT:
        case SelectionMode.MODE_OPEN_MULTISELECT_DB:
        case SelectionMode.MODE_OPEN_CREATE:
        case SelectionMode.MODE_OPEN_CREATE_DB:
        case SelectionMode.MODE_OPEN_EXPORT_TARGET:

            menu.add(getString(R.string.new_folder_short))
                    .setIcon(R.drawable.ic_menu_add_folder)
                    .setOnMenuItemClickListener(new OnMenuItemClickListener() {

                        @Override
                        public boolean onMenuItemClick(
                                com.actionbarsherlock.view.MenuItem item)
                        {
                            SherlockDialogFragment newFragment = CreateFolderDialogFragment.newInstance();
                            newFragment.show(getSupportFragmentManager(), "dialog");
                            return true;
                        }

                    }).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            break;
        default:
            break;
        }
        menu.add(getString(R.string.cancel))
            .setIcon(R.drawable.ic_menu_close_clear_cancel).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                @Override
                public boolean onMenuItemClick(
                        com.actionbarsherlock.view.MenuItem item)
                {
                    setResult(RESULT_CANCELED, getIntent());
                    finish();
                    return true;
                }
                
            })
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("parentPath", parentPath);
        outState.putStringArrayList("pathList", (ArrayList<String>) pathList);
        outState.putString("currentPath", currentPath);
        outState.putString("currentUploadTargetPath", currentUploadTargetPath);
        outState.putString("encfsBrowseRoot", encfsBrowseRoot);
        outState.putStringArray("currentExportPathList", currentExportPathList);
        outState.putString("currentRoot", currentRoot);
        outState.putString("currentRootLabel", currentRootLabel);
        outState.putInt("selectionMode", selectionMode);
        if (StorageManager.INSTANCE.getEncFSStorage() != null) {
            outState.putInt("storageType", StorageManager.INSTANCE.getEncFSStorageType());
            outState.putString("encFSPath", StorageManager.INSTANCE.getEncFSPath());
        } else {
            outState.putInt("storageType", Storage.STOR_UNDEFINED);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private void getDir(String dirPath, String rootPath, String rootName) {

        boolean useAutoSelection = dirPath.length() < currentPath.length();

        Integer position = lastPositions.get(parentPath);
        
        switch (selectionMode) {
        case SelectionMode.MODE_OPEN_CREATE_DB:
        case SelectionMode.MODE_OPEN_ENCFS_DB:
        case SelectionMode.MODE_OPEN_DEFAULT_DB:
        case SelectionMode.MODE_OPEN_MULTISELECT_DB:
        case SelectionMode.MODE_OPEN_MULTISELECT:
            buildDir(dirPath, rootPath, rootName);
            break;
        default:
            getDirImpl(dirPath, rootPath, rootName);
        }
        if (position != null && useAutoSelection) {
            mListView.setSelection(position);
        }

    }

    private void getDirImpl(final String dirPath, final String rootPath, final String rootName) {

        currentPath = dirPath;
        currentRoot = rootPath;
        currentRootLabel = rootName;
        String currentPathFromRoot = currentPath.substring(currentRoot.length());
        
        final List<String> item = new ArrayList<String>();
        pathList = new ArrayList<String>();
        mList = new ArrayList<HashMap<String, Object>>();
        
        VirtualFile f = new VirtualFile(currentPath);
        VirtualFile[] files = f.listFiles();

        myPath.setText(getText(R.string.location) + ": " + currentRootLabel +
                       currentPathFromRoot);

        if (!currentPath.equals(currentRoot)) {

            item.add(currentRoot);
            addItem(new VirtualFile(currentRoot), R.drawable.ic_launcher_folder, currentRootLabel);
            pathList.add(currentRoot);

            item.add("../");
            addItem(new VirtualFile(f.getParent()), R.drawable.ic_launcher_folder, "../");
            pathList.add(f.getParent());
            parentPath = f.getParent();

        }

        TreeMap<String, String> dirsMap = new TreeMap<String, String>();
        TreeMap<String, String> dirsPathMap = new TreeMap<String, String>();
        TreeMap<String, String> filesMap = new TreeMap<String, String>();
        TreeMap<String, String> filesPathMap = new TreeMap<String, String>();

        if (files != null) {

            /* getPath() returns full path including file name */
            for (VirtualFile file : files) {
                if (file != null) {
                    if (file.isDirectory()) {
                        String dirName = file.getName();
                        dirsMap.put(dirName, dirName);
                        dirsPathMap.put(dirName, file.getPath());
                    } else {
                        filesMap.put(file.getName(), file.getName());
                        filesPathMap.put(file.getName(), file.getPath());
                    }
                }
            }

            item.addAll(dirsMap.tailMap("").values());
            item.addAll(filesMap.tailMap("").values());
            pathList.addAll(dirsPathMap.tailMap("").values());
            pathList.addAll(filesPathMap.tailMap("").values());

            for (String dirpath : dirsPathMap.tailMap("").keySet()) {
                addItem(new VirtualFile(dirsPathMap.tailMap("").get(dirpath)),
                        R.drawable.ic_launcher_folder);
            }

            for (String filepath : filesPathMap.tailMap("").keySet()) {
                addItem(new VirtualFile(filesPathMap.tailMap("").get(filepath)),
                        R.drawable.ic_launcher_file);
            }

            ArrayAdapter<HashMap<String, Object>> fileList = 
                    new FileDialogArrayAdapter(this, mList);
            fileList.notifyDataSetChanged();
            mListView.setAdapter(fileList);

            switch (selectionMode) {
            case SelectionMode.MODE_OPEN_ENCFS:
            case SelectionMode.MODE_OPEN_ENCFS_DB:
            case SelectionMode.MODE_OPEN_DEFAULT:
            case SelectionMode.MODE_OPEN_DEFAULT_DB:
            case SelectionMode.MODE_OPEN_ENCFS_MOUNT:
            case SelectionMode.MODE_OPEN_UPLOAD_SOURCE:
            case SelectionMode.MODE_OPEN_CREATE:
            case SelectionMode.MODE_OPEN_CREATE_DB: 
            case SelectionMode.MODE_OPEN_EXPORT_TARGET:
            {
                mListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
                break;
            }
            default: {
                mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
            }
            }
        }
    }

    private void addItem(VirtualFile file, Integer imageId) {
        addItem(file, imageId, file.getName());
    }
    
    private void addItem(VirtualFile file, Integer imageId, String filelabel) {
        HashMap<String, Object> item = new HashMap<String, Object>();
        
        item.put(ITEM_KEY, filelabel);
        item.put(ITEM_IMAGE, imageId);
        item.put(ITEM_FILE, file);
        item.put(ITEM_ENABLED,
                (Boolean) (!file.getPath().equals(currentRoot) &&
                           !file.getPath().equals(new VirtualFile(currentPath).getParent())));
        switch (selectionMode) {
        case SelectionMode.MODE_OPEN_MULTISELECT:
        case SelectionMode.MODE_OPEN_MULTISELECT_DB:
            item.put(ITEM_CHECK, getChecked(file));
            break;
        }
        mList.add(item);
    }

    /** returns true if the parent directory is checked and/or
     *  the path itself is checked
     */
    private boolean getChecked(VirtualFile file) {
        boolean allChildrenSelected = file.isDirectory();
        if (file.isDirectory()) {
            VirtualFile[] children = file.listFiles();
            if (children != null) {
                for (VirtualFile child : file.listFiles()) {
                    if (!selectedPaths.contains(child.getPath())) {
                        allChildrenSelected = false;
                        break;
                    }
                }
                if (children.length == 0) {
                    allChildrenSelected = false;
                }
            } else {
                allChildrenSelected = false;
            }
        }
        if (selectedPaths.contains(file.getParent()) ||
            selectedPaths.contains(file.getPath()) ||
            allChildrenSelected) {
            selectedPaths.add(file.getPath());
            return true;
        } else {
            return false;
        }
    }
    
    static class ViewHolder {
        protected CheckBox checkbox;
        protected TextView text;
        protected ImageView image;
    }

    public class FileDialogArrayAdapter extends ArrayAdapter<HashMap<String, Object>> {

        private final List<HashMap<String, Object>> list;
        private final Activity context;

        public FileDialogArrayAdapter(Activity context, List<HashMap<String, Object>> list) {
            super(context, R.layout.file_dialog_row_multi, list);
            this.context = context;
            this.list = list;
        }
        
        @Override
            public View getView(int position, View convertView, ViewGroup parent) {
            View view = null;
            if (convertView == null) {
                LayoutInflater inflator = context.getLayoutInflater();
                switch (selectionMode) {
                case SelectionMode.MODE_OPEN_MULTISELECT:
                case SelectionMode.MODE_OPEN_MULTISELECT_DB:
                    view = inflator.inflate(R.layout.file_dialog_row_multi, null);
                    break;
                default:
                    view = inflator.inflate(R.layout.file_dialog_row_single, null);
                }
                final ViewHolder viewHolder = new ViewHolder();
                viewHolder.image = (ImageView) view
                        .findViewById(R.id.fdrowimage);
                viewHolder.text = (TextView) view.findViewById(R.id.fdrowtext);
                switch (selectionMode) {
                case SelectionMode.MODE_OPEN_MULTISELECT:
                case SelectionMode.MODE_OPEN_MULTISELECT_DB:
                    viewHolder.checkbox = (CheckBox) view
                            .findViewById(R.id.fdrowcheck);
                    viewHolder.checkbox
                            .setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

                                public void onCheckedChanged(
                                        CompoundButton buttonView,
                                        boolean isChecked) {
                                    @SuppressWarnings("unchecked")
                                    HashMap<String, Object> element = (HashMap<String, Object>) viewHolder.checkbox
                                            .getTag();
                                    element.put(ITEM_CHECK,
                                            buttonView.isChecked());
                                    VirtualFile f = (VirtualFile) element
                                            .get(ITEM_FILE);
                                    /* Avoid recursion for performance reasons */
                                    if (buttonView.isChecked()) {
                                        selectedPaths.add(f.getPath());
                                    } else {
                                        /*
                                         * Is this a bug in the SDK ?? This will
                                         * get fired upon scrolling from
                                         * disabled elements, which is why we
                                         * have to check whether the item is
                                         * enabled.
                                         */
                                        if ((Boolean) element.get(ITEM_ENABLED)) {
                                            removePath(f);
                                        }
                                    }
                                }
                            });
                    viewHolder.checkbox.setTag(list.get(position));
                    break;
                }
                view.setTag(viewHolder);
            } else {
                view = convertView;
                switch (selectionMode) {
                case SelectionMode.MODE_OPEN_MULTISELECT:
                case SelectionMode.MODE_OPEN_MULTISELECT_DB:
                    ((ViewHolder) view.getTag()).checkbox.setTag(list
                            .get(position));
                    break;
                }
            }
            ViewHolder holder = (ViewHolder) view.getTag();
            String filelabel = (String) list.get(position).get(ITEM_KEY);
            VirtualFile curFile = (VirtualFile) list.get(position).get(ITEM_FILE);
            if (curFile != null && selectedFile != null) {
                if (curFile.getPath()==selectedFile.getPath()) {
                    holder.text.setBackgroundResource(R.drawable.list_selector_selected);
                } else {
                    holder.text.setBackgroundResource(R.drawable.list_selector_normal);
                }
            } else {
                if (holder.text != null) {
                    holder.text.setBackgroundResource(R.drawable.list_selector_normal);
                }
            }
            holder.text.setText(filelabel);
            switch (selectionMode) {
            case SelectionMode.MODE_OPEN_MULTISELECT:
            case SelectionMode.MODE_OPEN_MULTISELECT_DB:
                Boolean enabled = (Boolean) list.get(position)
                        .get(ITEM_ENABLED);
                holder.checkbox.setEnabled(enabled);
                holder.checkbox.setChecked((Boolean) list.get(position).get(
                        ITEM_CHECK)
                        && enabled);
            }
            holder.image.setImageResource((Integer) list.get(position).get(
                    ITEM_IMAGE));
            return view;
        }
    }
    
    private void removePath(VirtualFile f) {
        /* Remove all paths that have f as parent */
        if (f.isDirectory()) {
            Set<String> newSelectedPaths = new HashSet<String>();
            for (String path : selectedPaths) {
                if (path.indexOf(f.getPath()) == -1) {
                    newSelectedPaths.add(path);
                }
            }
            selectedPaths = newSelectedPaths;
        } else {
            selectedPaths.remove(f.getPath());
        }

        /* Remove all parent directories checkmarks */
        if (!f.getPath().equals(currentRoot)) {
            String parent = f.getParent();
            while (parent != null) {
                selectedPaths.remove(parent);
                if (parent.equals(currentRoot)) {
                    parent = null;
                } else {
                    parent = new VirtualFile(parent).getParent();
                }
            }
        }
    }

    private void onListItemClick(ListView l, View v, int position, long id) {

        VirtualFile file = new VirtualFile(pathList.get(position));

        setSelectVisible(v);

        if (file.isDirectory()) {
            switch (selectionMode) {
            case SelectionMode.MODE_OPEN_UPLOAD_SOURCE:
                selectButton.setEnabled(false);
                break;
            default:
                selectButton.setEnabled(true);
            }

            if (file.canRead()) {
                lastPositions.put(currentPath, position);
                getDir(pathList.get(position), currentRoot, currentRootLabel);
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
            selectButton.setEnabled(true);
            switch (selectionMode) {
            case SelectionMode.MODE_OPEN_MULTISELECT:
            case SelectionMode.MODE_OPEN_MULTISELECT_DB:
                break;
            default:
                selectedFile = file;
                /* v.setSelected(true); */
            }
        }
    }

    @Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            selectButton.setEnabled(true);
            if (!currentPath.equals(currentRoot)) {
                getDir(parentPath, currentRoot, currentRootLabel);
            } else {
                return super.onKeyDown(keyCode, event);
            }
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo)
    {
      super.onCreateContextMenu(menu, v, menuInfo);
      MenuInflater inflater = getMenuInflater();
      AdapterContextMenuInfo info = (AdapterContextMenuInfo)menuInfo;
      if ((new VirtualFile(pathList.get(info.position))).isDirectory()) {
          inflater.inflate(R.menu.file_dialog_context_menu_dir, menu);
      } else {
          inflater.inflate(R.menu.file_dialog_context_menu, menu);
      }
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
      AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
      String pathName = pathList.get(info.position);
      switch (item.getItemId()) {
      case R.id.context_open:
          openEncFSFile(pathName, encfsBrowseRoot);
          return true;
      case R.id.context_preview:
          previewEncFSFile(pathName, encfsBrowseRoot);
          return true;
      case R.id.context_export:
          showExportWarning(new String[]{pathName});
          return true;
      case R.id.context_delete:
          showDeleteWarning(pathName);
          return true;
      default:
        return super.onContextItemSelected(item);
      }
    }

    public static class CreateFolderDialogFragment extends SherlockDialogFragment {

        public static CreateFolderDialogFragment newInstance() {
            CreateFolderDialogFragment frag = new CreateFolderDialogFragment();
            return frag;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            LayoutInflater inflater = (LayoutInflater) ((FileDialog)getActivity())
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            final View layout = inflater.inflate(R.layout.new_folder_dialog,
                    (ViewGroup) ((FileDialog)getActivity()).findViewById(R.id.new_folder_root));
            final EditText newFolder = (EditText) layout.findViewById(R.id.EditText_NewFolder);

            AlertDialog.Builder builder = new AlertDialog.Builder((FileDialog)getActivity());
            builder.setTitle(R.string.title_new_folder);
            builder.setView(layout);
            builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                    }
                });
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String newFolderString = newFolder.getText().toString();
                        if (newFolderString.length() > 0) {
                            switch (((FileDialog)getActivity()).selectionMode) {
                            case SelectionMode.MODE_OPEN_CREATE:
                            case SelectionMode.MODE_OPEN_CREATE_DB:
                            case SelectionMode.MODE_OPEN_EXPORT_TARGET:
                                ((FileDialog)getActivity()).mkDir(newFolderString);
                                break;
                            default:
                                ((FileDialog)getActivity()).showCreateEncFSFolderWarning(newFolderString);
                            }
                        } else {
                            ((FileDialog)getActivity()).showToast(R.string.new_folder_fail);
                        }
                    }
                });
            return builder.create();
        }
    }

    public static class PasswordDialogFragment extends SherlockDialogFragment {

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
                             case SelectionMode.MODE_OPEN_DEFAULT:
                             case SelectionMode.MODE_OPEN_DEFAULT_DB:
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

    private void showPasswordDialog() {
        SherlockDialogFragment newFragment = PasswordDialogFragment.newInstance();
        newFragment.show(getSupportFragmentManager(), "pwdialog");
    }

    private void nullPassword() {
        char[] fill = new char[currentPassword.length()];
        Arrays.fill(fill, '\0');
        currentPassword = new String(fill);
    }

    private void setSelectVisible(View v) {
        // layoutCreate.setVisibility(View.GONE);
        layoutSelect.setVisibility(View.VISIBLE);

        inputManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
        selectButton.setEnabled(false);
    }
    
    private void buildDir(final String dirPath, final String rootPath, final String rootName)
    {
        final String path = "/" + dirPath.substring(rootPath.length());
        String encFSRoot = "";
        switch (selectionMode) {
        case SelectionMode.MODE_OPEN_MULTISELECT:
        case SelectionMode.MODE_OPEN_MULTISELECT_DB:
            /* Is the encfs volume still OK? */
            if (Cryptonite.jniVolumeLoaded() != Cryptonite.jniSuccess()) {
                showToast(R.string.browse_failed);
                finish();
            }
            /* Full path of previous encFSRoot */
            encFSRoot = Cryptonite.jniEncode("/");
            break;
        }
        
        final String fEncFSRoot = encFSRoot;
        
        ProgressDialogFragment.showDialog(this, getStorage().waitStringId, "buildDir");
        new Thread(new Runnable(){
            public void run(){
                switch (selectionMode) {
                case SelectionMode.MODE_OPEN_MULTISELECT:
                case SelectionMode.MODE_OPEN_MULTISELECT_DB:
                    if (!getStorage().mkVisibleDecoded(path, fEncFSRoot, rootPath)) {
                        setResult(Cryptonite.RESULT_ERROR, getIntent());
                        finish();
                    }
                    break;
                default:
                    getStorage().mkVisiblePlain(path, rootPath);
                }
                runOnUiThread(new Runnable(){
                    public void run() {
                        ProgressDialogFragment.dismissDialog(FileDialog.this, "buildDir");
                        getDirImpl(dirPath, rootPath, rootName);
                    }
                });
            }
        }).start();

    }
    
    private void showExportWarning(final String[] exportPaths) {
        SharedPreferences prefs = getBaseContext().getSharedPreferences(Cryptonite.ACCOUNT_PREFS_NAME, 0);
        if (!prefs.getBoolean("cb_norris", false)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(FileDialog.this);
            builder.setIcon(R.drawable.ic_launcher_cryptonite)
            .setTitle(R.string.warning)
            .setMessage(R.string.export_warning)
            .setPositiveButton(R.string.export_short,
                    new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog,
                        int which) {
                    getIntent().putExtra(RESULT_OPEN_PATH, (String)null);
                    getIntent().putExtra(RESULT_UPLOAD_PATH, (String)null);
                    getIntent().putExtra(RESULT_EXPORT_PATHS, exportPaths);
                    setResult(RESULT_OK, getIntent());
                    finish();
                }
            })
            .setNegativeButton(R.string.cancel,
                    new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog,
                        int which) {
                }
            });  
            AlertDialog dialog = builder.create();
            dialog.show();
        } else {
            getIntent().putExtra(RESULT_OPEN_PATH, (String)null);
            getIntent().putExtra(RESULT_UPLOAD_PATH, (String)null);
            getIntent().putExtra(RESULT_EXPORT_PATHS, exportPaths);
            setResult(RESULT_OK, getIntent());
            finish();
        }
    }
    
    private void showDeleteWarning(final String deletePath) {
        AlertDialog.Builder builder = new AlertDialog.Builder(FileDialog.this);
        builder.setIcon(R.drawable.ic_launcher_cryptonite)
        .setTitle(R.string.warning)
        .setMessage(R.string.delete_warning)
        .setPositiveButton(R.string.delete_short,
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,
                    int which) {
                /* normalise path names */
                String bRoot = new File(currentRoot).getPath();
                String stripstr = deletePath.substring(bRoot.length());
                if (!stripstr.startsWith("/")) {
                    stripstr = "/" + stripstr;
                }
                
                /* Convert current path to encoded file name */
                final String encodedPath = Cryptonite.jniEncode(stripstr);
                /* Run in separate thread in case a network operation is involved */
                new Thread(new Runnable(){
                    public void run(){
                        final boolean deleted = getStorage().deleteFile(encodedPath);
                        runOnUiThread(new Runnable(){
                            public void run() {
                                if (deleted) {
                                    if (!new VirtualFile(deletePath).delete()) {
                                        showToast(R.string.delete_fail_virtual);
                                    }
                                    
                                    /* reload current directory */
                                    getDir(currentPath, currentRoot, currentRootLabel);
                                }
                            }
                        });
                    }
                }).start();
                
            }
        })
        .setNegativeButton(R.string.cancel,
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,
                    int which) {
            }
        });  
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    
    private void showUploadWarning(final String uploadPath) {
        SharedPreferences prefs = getBaseContext().getSharedPreferences(Cryptonite.ACCOUNT_PREFS_NAME, 0);
        if (!prefs.getBoolean("cb_norris", false)) {

            AlertDialog.Builder builder = new AlertDialog.Builder(FileDialog.this);
            builder.setIcon(R.drawable.ic_launcher_cryptonite)
            .setTitle(R.string.warning)
            .setMessage(R.string.upload_warning)
            .setPositiveButton(R.string.upload_short,
                    new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog,
                        int which) {
                    getIntent().putExtra(RESULT_OPEN_PATH, (String)null);
                    getIntent().putExtra(RESULT_EXPORT_PATHS, (String[])null);
                    getIntent().putExtra(RESULT_UPLOAD_PATH, uploadPath);
                    setResult(RESULT_OK, getIntent());
                    finish();
                }
            })
            .setNegativeButton(R.string.cancel,
                    new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog,
                        int which) {
                }
            });  
            AlertDialog dialog = builder.create();
            dialog.show();
        } else {
            getIntent().putExtra(RESULT_OPEN_PATH, (String)null);
            getIntent().putExtra(RESULT_EXPORT_PATHS, (String[])null);
            getIntent().putExtra(RESULT_UPLOAD_PATH, uploadPath);
            setResult(RESULT_OK, getIntent());
            finish();
        }
    }

    private void showCreateEncFSFolderWarning(final String decodedFolderName) {
        SharedPreferences prefs = getBaseContext().getSharedPreferences(Cryptonite.ACCOUNT_PREFS_NAME, 0);
        if (!prefs.getBoolean("cb_norris", false)) {

            AlertDialog.Builder builder = new AlertDialog.Builder(FileDialog.this);
            builder.setIcon(R.drawable.ic_launcher_cryptonite)
            .setTitle(R.string.warning)
            .setMessage(R.string.new_folder_warning)
            .setPositiveButton(R.string.new_folder_short,
                    new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog,
                        int which) {
                    createEncFSFolder(decodedFolderName);
                }
            })
            .setNegativeButton(R.string.cancel,
                    new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog,
                        int which) {
                }
            });  
            AlertDialog dialog = builder.create();
            dialog.show();
        } else {
            createEncFSFolder(decodedFolderName);
        }
    }

    private void createEncFSFolder(String decodedFolderName) {
        /* normalise path names */
        String bRoot = new File(currentRoot).getPath();
        String bPath = new File(currentPath).getPath();
        String stripstr = bPath.substring(bRoot.length()) + "/" + decodedFolderName;
        if (!stripstr.startsWith("/")) {
            stripstr = "/" + stripstr;
        }
        /* Remove trailing spaces */
        while (stripstr.endsWith(" ")) {
            stripstr = stripstr.substring(0, stripstr.length()-1);
        }

        final String fStripStr = stripstr;
        /* Run in separate thread in case a network operation is involved */
        new Thread(new Runnable(){
            public void run(){
                /* Convert current path to encoded file name */
                String encodedPath = Cryptonite.jniEncode(fStripStr);
                final boolean folderMade = getStorage().mkDirEncrypted(encodedPath);
                runOnUiThread(new Runnable(){
                    public void run() {
                        if (folderMade) {
                            /* reload current directory */
                            getDir(currentPath, currentRoot, currentRootLabel);
                        }
                    }
                });
            }
        }).start();
    }
    
    protected void mkDir(String newFolderString) {
        /* normalise path names */
        String bPath = new File(currentPath).getPath();
        String stripstr = bPath + "/" + newFolderString;
        if (!stripstr.startsWith("/")) {
            stripstr = "/" + stripstr;
        }
        /* Remove trailing spaces */
        while (stripstr.endsWith(" ")) {
            stripstr = stripstr.substring(0, stripstr.length()-1);
        }
        final String fStripStr = stripstr;
        
        /* Run in separate thread in case a network operation is involved */
        new Thread(new Runnable(){
            public void run(){
                final boolean madeDir = getStorage().mkDirPlain(fStripStr);
                runOnUiThread(new Runnable(){
                    public void run() {
                        if (madeDir) {
                            /* reload current directory */
                            getDir(currentPath, currentRoot, currentRootLabel);
                        }
                    }
                });
            }
        }).start();
        return;
    }
    
    private void showToast(int resId) {
        showToast(getString(resId));
    }
    
    private void showToast(final String msg) {
        runOnUiThread(new Runnable() {
            public void run() {
                Toast err = Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG);
                err.show();
            }
        });
    }
    
    private Storage getStorage() {
        if (localFilePickerStorage) {
            return StorageManager.INSTANCE.getLocalStorage();
        } else {
            return StorageManager.INSTANCE.getEncFSStorage();
        }
    }
    
    /** Initialize an EncFS volume. This will check
     * whether the EncFS volume is valid an initialize the EncFS
     * root information
     * 
     * @param srcDir Path to EncFS volume
     * @param pwd password
     */
    private void initEncFS(final String srcDir) {
        alertMsg = "";

        ProgressDialogFragment.showDialog(this, R.string.running_encfs, "initEncFS");
        new Thread(new Runnable() {
            public void run() {
                if (StorageManager.INSTANCE.getEncFSStorage() == null) {
                    alertMsg = getString(R.string.internal_error);
                } else if (!StorageManager.INSTANCE.getEncFSStorage()
                        .initEncFS(srcDir, currentRoot)) {
                    alertMsg = getString(R.string.invalid_encfs);
                    // StorageManager.INSTANCE.resetEncFSStorage();
                } else {
                    DirectorySettings.INSTANCE.currentBrowsePath = currentPath;
                    switch (selectionMode) {
                    case SelectionMode.MODE_OPEN_DEFAULT_DB:
                        DirectorySettings.INSTANCE.currentBrowseStartPath = currentRoot;
                        break;
                    default:
                        DirectorySettings.INSTANCE.currentBrowseStartPath = intentStartPath;
                    }
                    try {
                        StorageManager.INSTANCE.setEncFSPath(currentPath
                                .substring(intentStartPath.length()));
                    } catch (StringIndexOutOfBoundsException e) {
                        alertMsg = getString(R.string.decrypt_failure) + 
                                " currentPath: " + currentPath + 
                                " intentStartPath: " + intentStartPath;
                        Log.v(Cryptonite.TAG, alertMsg);
                    }
                    Log.i(Cryptonite.TAG, "Dialog root is " + currentPath);
                    
                    SharedPreferences prefs = getBaseContext().getSharedPreferences(Cryptonite.ACCOUNT_PREFS_NAME, 0);
                    if (Cryptonite.jniInit(srcDir, currentPassword, prefs.getBoolean("cb_anykey", false)) != Cryptonite.jniSuccess()) {
                        Log.v(Cryptonite.TAG, getString(R.string.browse_failed));
                        alertMsg = getString(R.string.browse_failed);
                        // StorageManager.INSTANCE.resetEncFSStorage();
                    } else {
                        Log.v(Cryptonite.TAG, "Decoding succeeded");
                    }
                }
                runOnUiThread(new Runnable() {
                    public void run() {
                        ProgressDialogFragment.dismissDialog(FileDialog.this, "initEncFS");
                        nullPassword();
                        if (alertMsg.length() != 0) {
                            showToast(alertMsg);
                            getIntent().putExtra(RESULT_OPEN_PATH, (String)null);
                            getIntent().putExtra(RESULT_UPLOAD_PATH, (String)null);
                            getIntent().putExtra(RESULT_EXPORT_PATHS, currentPath);
                            getIntent().putExtra(RESULT_SELECTED_FILE, (String)null);
                            setResult(Cryptonite.RESULT_RETRY, getIntent());
                            finish();            
                        } else {
                            showToast(R.string.encfs_success);
                            getIntent().putExtra(RESULT_OPEN_PATH, (String)null);
                            getIntent().putExtra(RESULT_UPLOAD_PATH, (String)null);
                            getIntent().putExtra(RESULT_EXPORT_PATHS, currentPath);
                            getIntent().putExtra(RESULT_SELECTED_FILE, (String)null);
                            setResult(RESULT_OK, getIntent());
                            finish();            
                        }
                    }
                });
            }
        }).start();
    }

    
    /** This will run the shipped encfs binary and spawn a daemon on rooted devices
     */
    private void mountEncFS(final String srcDir) {
        if (Cryptonite.jniIsValidEncFS(srcDir) != Cryptonite.jniSuccess()) {
            showToast(R.string.invalid_encfs);
            Log.v(Cryptonite.TAG, "Invalid EncFS");
            return;
        }
        
        if (!Cryptonite.isValidMntDir(this, new File(DirectorySettings.INSTANCE.mntDir), true)) {
            showToast(R.string.mount_point_invalid);
            return;
        }
        ProgressDialogFragment.showDialog(this, R.string.running_encfs, "mountEncFS");
        Log.v(Cryptonite.TAG, "Running encfs with " + srcDir + " " + DirectorySettings.INSTANCE.mntDir);
        alertMsg = "";
        new Thread(new Runnable() {
            public void run() {
                SharedPreferences prefs = getBaseContext().getSharedPreferences(Cryptonite.ACCOUNT_PREFS_NAME, 0);
                String encfsoutput = "";
                String[] cmdlist = { DirectorySettings.INSTANCE.encFSBin,
                        "--public", prefs.getBoolean("cb_anykey", false) ? "--anykey" : "", "--stdinpass", "\"" + srcDir + "\"",
                        "\"" + DirectorySettings.INSTANCE.mntDir + "\"" };
                try {
                    encfsoutput = ShellUtils.runBinary(cmdlist,
                            DirectorySettings.INSTANCE.binDirPath,
                            currentPassword, true);
                } catch (IOException e) {
                    alertMsg = getString(R.string.mount_fail) + ": "
                            + e.getMessage();
                } catch (InterruptedException e) {
                    alertMsg = getString(R.string.mount_fail) + ": "
                            + e.getMessage();
                }
                final String fEncFSOutput = encfsoutput;
                /* Mounted? */
                if (!ShellUtils.isMounted("fuse.encfs")) {
                    alertMsg = getString(R.string.mount_fail);
                }
                runOnUiThread(new Runnable() {
                    public void run() {
                        ProgressDialogFragment.dismissDialog(FileDialog.this, "mountEncFS");
                        nullPassword();
                        if (!alertMsg.equals("")) {
                            showToast(alertMsg + ": " + fEncFSOutput);
                            getIntent().putExtra(RESULT_OPEN_PATH, (String)null);
                            getIntent().putExtra(RESULT_UPLOAD_PATH, (String)null);
                            getIntent().putExtra(RESULT_EXPORT_PATHS, currentPath);
                            getIntent().putExtra(RESULT_SELECTED_FILE, (String)null);
                            setResult(Cryptonite.RESULT_RETRY, getIntent());
                            finish();            
                        } else {
                            showToast(getString(R.string.encfs_success_mount)
                                    + " " + DirectorySettings.INSTANCE.mntDir);
                            getIntent().putExtra(RESULT_OPEN_PATH,
                                    (String) null);
                            getIntent().putExtra(RESULT_UPLOAD_PATH,
                                    (String) null);
                            getIntent().putExtra(RESULT_EXPORT_PATHS,
                                    currentPath);
                            getIntent().putExtra(RESULT_SELECTED_FILE,
                                    (String) null);
                            setResult(RESULT_OK, getIntent());
                            finish();
                        }
                    }
                });
            }
        }).start();
    }

    private void encryptEncFSFile(final String srcPath) {
        final String stripstr = StorageManager.INSTANCE.getEncFSStorage().stripStr(
                currentUploadTargetPath, encfsBrowseRoot, srcPath);
        
        /* Run in separate thread in case this involves a network operation */
        new Thread(new Runnable(){
            public void run(){
                final String nextFilePath = StorageManager.INSTANCE.getEncFSStorage().encodedExists(stripstr);
                if (nextFilePath == "") {
                    showToast(R.string.empty_encoded_filename);
                    return;
                }
                runOnUiThread(new Runnable(){
                    public void run() {
                        if (!nextFilePath.equals(stripstr)) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(FileDialog.this);
                            builder.setIcon(R.drawable.ic_launcher_cryptonite)
                            .setTitle(R.string.file_exists)
                            .setMessage(R.string.file_exists_options)
                            .setPositiveButton(R.string.overwrite,
                                    new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                        int which) {
                                    encryptEncFSFileExec(stripstr, srcPath);
                                }
                            })
                            .setNeutralButton(R.string.rename, 
                                    new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                        int which) {
                                    encryptEncFSFileExec(nextFilePath, srcPath);
                                }
                            })
                            .setNegativeButton(R.string.cancel,
                                    new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                        int which) {

                                }
                            });  
                            AlertDialog dialog = builder.create();
                            dialog.show();
                        }else {
                            encryptEncFSFileExec(stripstr, srcPath);
                        }
                    }
                });
            }
        }).start();
    }

    private void encryptEncFSFileExec(final String targetPath, final String srcPath) {
        ProgressDialogFragment.showDialog(this, R.string.encrypting, "uploadEncFS");
        alertMsg = "";
        new Thread(new Runnable(){
            public void run(){
                if (!StorageManager.INSTANCE.getEncFSStorage().encryptEncFSFile(targetPath, srcPath)) {
                    alertMsg = getString(R.string.encrypt_failure);
                }        
                runOnUiThread(new Runnable(){
                    public void run() {
                        ProgressDialogFragment.dismissDialog(FileDialog.this, "uploadEncFS");
                        if (!alertMsg.equals("")) {
                            showToast(alertMsg);
                            finishSetResults();
                        } else {
                            showToast(R.string.encrypt_success);
                            uploadEncFSFile(targetPath);
                        }
                    }
                });
            }
        }).start();
    }

    private void uploadEncFSFile(final String targetPath) {
        final AsyncTask<Void, Long, Boolean> upload =
                StorageManager.INSTANCE.getEncFSStorage().uploadEncFSFile(this, targetPath);
        if (upload != null) {
            upload.execute();
            new Thread(new Runnable(){
                public void run(){
                    alertMsg = "";
                    Boolean res = false;
                    try {
                        res = upload.get();
                    } catch (InterruptedException e) {
                        alertMsg = e.toString();
                    } catch (ExecutionException e) {
                        alertMsg = e.toString();
                    }
                    if (!res) {
                        alertMsg = " ";
                    }
                    runOnUiThread(new Runnable(){
                        public void run() {
                            if (!alertMsg.equals("")) {
                                showToast(getString(R.string.upload_failure) + ": " + alertMsg);
                            } else {
                                showToast(R.string.upload_success);
                            }
                            finishSetResults();
                        }
                    });
                }
            }).start();
        } else {
            finishSetResults();
        }
    }

    private void finishSetResults() {
        getIntent().putExtra(RESULT_OPEN_PATH, (String)null);
        getIntent().putExtra(RESULT_UPLOAD_PATH, (String)null);
        getIntent().putExtra(RESULT_EXPORT_PATHS, currentPath);
        if (selectedFile != null) {
            getIntent().putExtra(RESULT_SELECTED_FILE, selectedFile.getPath());
        } else {
            getIntent().putExtra(RESULT_SELECTED_FILE, (String)null);
        }
        setResult(RESULT_OK, getIntent());
        finish();
    }

    /* From https://gist.github.com/889747 by mrenouf */
    private static void copyFile(File sourceFile, File destFile) throws IOException {
        if (!destFile.exists()) {
            destFile.createNewFile();
        }
        FileInputStream fIn = null;
        FileOutputStream fOut = null;
        FileChannel source = null;
        FileChannel destination = null;
        try {
            fIn = new FileInputStream(sourceFile);
            source = fIn.getChannel();
            fOut = new FileOutputStream(destFile);
            destination = fOut.getChannel();
            long transfered = 0;
            long bytes = source.size();
            while (transfered < bytes) {
                transfered += destination.transferFrom(source, 0, source.size());
                destination.position(transfered);
            }
        } finally {
            if (source != null) {
                source.close();
            } else if (fIn != null) {
                fIn.close();
            }
            if (destination != null) {
                destination.close();
            } else if (fOut != null) {
                fOut.close();
            }
        }
    }
    
    private boolean openEncFSFile(final String encFSFilePath, String fileRoot) {

        SharedPreferences prefs = getBaseContext().getSharedPreferences(Cryptonite.ACCOUNT_PREFS_NAME, 0);
        Cryptonite.setupReadDirs(prefs.getBoolean("cb_extcache", false), this);

        /* normalise path names */
        String bRoot = new File(fileRoot).getPath();
        String bPath = new File(encFSFilePath).getPath();
        String stripstrtmp = bPath.substring(bRoot.length());
        if (!stripstrtmp.startsWith("/")) {
            stripstrtmp = "/" + stripstrtmp;
        }

        final String stripstr = stripstrtmp;
        
        /* Convert current path to encoded file name */
        final String encodedPath = Cryptonite.jniEncode(stripstr);

        /* Set up temp dir for decrypted file */
        String destPath = DirectorySettings.INSTANCE.openDir.getPath() + 
                (new File(bPath)).getParent().substring(bRoot.length());

        (new File(destPath)).mkdirs();
        alertMsg = "";
        ProgressDialogFragment.showDialog(this, R.string.decrypting, "openEncFS");
        new Thread(new Runnable() {
            public void run() {
                if (!StorageManager.INSTANCE.getEncFSStorage()
                        .decryptEncFSFile(encodedPath,
                                DirectorySettings.INSTANCE.openDir.getPath())) {
                    alertMsg = getString(R.string.decrypt_failure);
                    Log.e(Cryptonite.TAG, "Error while attempting to copy " + encodedPath);
                }
                runOnUiThread(new Runnable() {
                    public void run() {
                        ProgressDialogFragment.dismissDialog(FileDialog.this, "openEncFS");
                        if (!alertMsg.equals("")) {
                            showToast(alertMsg);
                            return;
                        }
                        /* Copy the resulting file to a readable folder */
                        String openFilePath = DirectorySettings.INSTANCE.openDir.getPath()
                                + stripstr;
                        String readableName = (new File(encFSFilePath))
                                .getName();
                        String readablePath = DirectorySettings.INSTANCE.readDir.getPath() + "/"
                                + readableName;
                        File readableFile = new File(readablePath);

                        /* Make sure the readable Path exists */
                        readableFile.getParentFile().mkdirs();

                        try {
                            copyFile(new File(openFilePath), readableFile);
                            
                            /* Make world readable */
                            try {
                                ShellUtils.chmod(readablePath, "644");
                            } catch (InterruptedException e) {
                                Log.e(Cryptonite.TAG, e.toString());
                            }

                            /* Delete tmp directory */
                            Cryptonite.deleteDir(DirectorySettings.INSTANCE.openDir);
                        } catch (IOException e) {
                            showToast("Error while attempting to open " + readableName + ": " + e.toString());
                            return;
                        } catch (OutOfMemoryError e) {
                            showToast(getString(R.string.out_of_memory) + " (" + e.toString() + ").");
                        }

                        fileOpen(readablePath);

                    }
                });
            }
        }).start();
        return true;        
    }
    
    private String getMimeTypeFromExtension(String filePath) {
        /* Guess MIME type */
        MimeTypeMap myMime = MimeTypeMap.getSingleton();
        String extension = Storage.fileExt(filePath);
        String contentType;
        if (extension.length() == 0) {
            contentType = null;
        } else {
            contentType = myMime.getMimeTypeFromExtension(extension.substring(1));
        }
        
        return contentType;
    }
    
    private boolean fileOpen(String filePath) {
        String contentType = getMimeTypeFromExtension(filePath);
        
        Uri data = Uri.fromFile(new File(filePath));

        /* Attempt to guess file type from content; seemingly very unreliable */
        if (contentType == null) {
            try {
                FileInputStream fis = new FileInputStream(filePath);
                contentType = URLConnection.guessContentTypeFromStream(fis);
            } catch (IOException e) {
                Log.e(Cryptonite.TAG, "Error while attempting to guess MIME type of " + filePath
                        + ": " + e.toString());
                contentType = null;
            }
        }

        if (contentType == null) {
            Log.e(Cryptonite.TAG, "Couldn't find content type; resorting to text/plain");
            contentType = "text/plain";
        }
        
        Intent intent = new Intent();
        
        intent.setAction(android.content.Intent.ACTION_VIEW);
        intent.setDataAndType(data, contentType);

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            showToast(getString(R.string.activity_not_found_title) + 
                    getString(R.string.activity_not_found_msg));
            Log.e(Cryptonite.TAG, "Couldn't find activity: " + e.toString());
            return false;
        }
        
        return true;
    }

    private boolean previewEncFSFile(final String encFSFilePath, String fileRoot) {

        SharedPreferences prefs = getBaseContext()
                .getSharedPreferences(Cryptonite.ACCOUNT_PREFS_NAME, 0);
        Cryptonite.setupReadDirs(prefs.getBoolean("cb_extcache", false), this);

        /* normalise path names */
        String bRoot = new File(fileRoot).getPath();
        String bPath = new File(encFSFilePath).getPath();
        String stripstrtmp = bPath.substring(bRoot.length());
        if (!stripstrtmp.startsWith("/")) {
            stripstrtmp = "/" + stripstrtmp;
        }

        final String stripstr = stripstrtmp;

        /* Convert current path to encoded file name */
        final String encodedPath = Cryptonite.jniEncode(stripstr);
        alertMsg = "";

        ProgressDialogFragment.showDialog(this, R.string.decrypting, "previewEncFS");
        new Thread(new Runnable() {
            public void run() {
                DecodedBuffer buf = null;
                try {
                    buf = StorageManager.INSTANCE
                            .getEncFSStorage()
                            .decryptEncFSFileToBuffer(encodedPath);
                } catch (OutOfMemoryError e) {
                    alertMsg = getString(R.string.out_of_memory) + ". ";
                    buf = null;
                }
                if (buf == null) {
                    alertMsg += getString(R.string.decrypt_failure);
                    Log.e(Cryptonite.TAG, "Error while attempting to decrypt"
                            + encodedPath);
                }
                final DecodedBuffer fBuf = buf;
                runOnUiThread(new Runnable() {
                    public void run() {
                        ProgressDialogFragment.dismissDialog(FileDialog.this, "previewEncFS");
                        if (!alertMsg.equals("")) {
                            showToast(alertMsg);
                            return;
                        }
                        bufferPreview(fBuf);
                    }
                });
            }
        }).start();
        return true;
    }
 
    private void bufferPreview(final DecodedBuffer buf) {
        /* Attempt to guess file type from extension */
        String contentType = getMimeTypeFromExtension(buf.fileName);
        
        if (contentType != "text/plain" && Storage.fileExt(buf.fileName).length() > 0) {
            new AlertDialog.Builder(this)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setTitle(R.string.warning)
            .setMessage(R.string.not_plain_text)
            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    textPreview(buf);
                }
            })
            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    return;
                }
            })
            .create().show();
        } else {
            textPreview(buf);
        }
    }
    
    private void textPreview(DecodedBuffer buf) {
        String str = "";
        try {
            str = new String(buf.contents, "utf-8");
        } catch (UnsupportedEncodingException e) {
            showToast(R.string.unsupported_encoding);
            return;
        }
        String truncated = "";
        /* Truncate at 100KB; couldn't find any documentation
         * what the limit really is.
         */
        if (str.length() > 1e5) {
            str = str.substring(0, (int)1e5);
            truncated = " (truncated)";
        }
        Intent intent = new Intent(getBaseContext(), TextPreview.class);
        intent.putExtra(TextPreview.PREVIEW_TITLE, buf.fileName + truncated);
        intent.putExtra(TextPreview.PREVIEW_BODY, str);
        startActivityForResult(intent, TextPreview.REQUEST_PREVIEW);
    }
    
    private void exportEncFSFiles(final String[] pathList) {
        if (pathList != null) {
            ProgressDialogFragment.showDialog(this, R.string.running_export, "exportEncFS");
            new Thread(new Runnable(){
                public void run(){
                    String exportName = currentPath + "/Cryptonite";
                    Log.v(Cryptonite.TAG, "Exporting to " + exportName);
                    if (!new File(exportName).exists()) {
                        new File(exportName).mkdirs();
                    }
                    if (!new File(exportName).exists()) {
                        alert = true;
                    } else {
                        alert = !StorageManager.INSTANCE.getEncFSStorage()
                                .exportEncFSFiles(pathList, encfsBrowseRoot, 
                                    currentPath + "/Cryptonite");
                    }
                    runOnUiThread(new Runnable(){
                        public void run() {
                            ProgressDialogFragment.dismissDialog(FileDialog.this, "exportEncFS");
                            if (alert) {
                                showToast(R.string.export_failed);
                                alert = false;
                            } else {
                                showToast(R.string.export_success);
                            }
                            getIntent().putExtra(RESULT_OPEN_PATH, (String)null);
                            getIntent().putExtra(RESULT_UPLOAD_PATH, (String)null);
                            getIntent().putExtra(RESULT_EXPORT_PATHS, currentPath);
                            if (selectedFile != null) {
                                getIntent().putExtra(RESULT_SELECTED_FILE, selectedFile.getPath());
                            } else {
                                getIntent().putExtra(RESULT_SELECTED_FILE, (String)null);
                            }
                            setResult(RESULT_OK, getIntent());
                            finish();
                        }
                    });
                }
            }).start();
        }
    }
    
}
