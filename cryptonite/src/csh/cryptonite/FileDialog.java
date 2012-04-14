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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem.OnMenuItemClickListener;

import csh.cryptonite.Cryptonite.ProgressDialogFragment;
import csh.cryptonite.storage.Storage;
import csh.cryptonite.storage.StorageManager;
import csh.cryptonite.storage.VirtualFile;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
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
    public static final String RESULT_PREVIEW_PATH = "csh.cryptonite.RESULT_PREVIEW_PATH";
    public static final String RESULT_UPLOAD_PATH = "csh.cryptonite.RESULT_UPLOAD_PATH";
    public static final String RESULT_SELECTED_FILE = "csh.cryptonite.RESULT_SELECTED_FILE";
    public static final String SELECTION_MODE = "SELECTION_MODE";
    public static final String LABEL = "LABEL";
    public static final String BUTTON_LABEL = "BUTTON_LABEL";
    public static final String CURRENT_ROOT = "CURRENT_ROOT";
    public static final String CURRENT_ROOT_NAME = "CURRENT_ROOT_NAME";

    private String currentRoot = ROOT;
    private String currentRootLabel = ROOT;
    private List<String> pathList = null;
    private TextView myPath;
    private ArrayList<HashMap<String, Object>> mList;

    private Button selectButton;

    private LinearLayout layoutSelect;
    
    private InputMethodManager inputManager;
    private String parentPath;
    private String currentPath;

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

        selectionMode = getIntent().getIntExtra(SELECTION_MODE, SelectionMode.MODE_OPEN);

        currentRoot = getIntent().getStringExtra(CURRENT_ROOT);
        if (currentRoot == null) {
            currentRoot = ROOT;
        }
        currentPath = currentRoot;

        currentRootLabel = getIntent().getStringExtra(CURRENT_ROOT_NAME);
        if (currentRootLabel == null) {
            currentRootLabel = currentRoot;
        }

        String startPath = getIntent().getStringExtra(START_PATH);

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
            if (savedInstanceState.getString("currentRoot") != null) {
                currentRoot = savedInstanceState.getString("currentRoot");
            }
            if (savedInstanceState.getString("currentRootLabel") != null) {
                currentRootLabel = savedInstanceState.getString("currentRootLabel");
            }
            if (savedInstanceState.getInt("selectionMode") != 0) {
                selectionMode = savedInstanceState.getInt("selectionMode");
            }
        }
        
        switch (selectionMode) {
        case SelectionMode.MODE_OPEN_CREATE_DB:
        case SelectionMode.MODE_OPEN_DB:
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
                    case SelectionMode.MODE_OPEN:
                    case SelectionMode.MODE_OPEN_DB:
                    case SelectionMode.MODE_OPEN_UPLOAD_SOURCE:
                    case SelectionMode.MODE_OPEN_CREATE:
                    case SelectionMode.MODE_OPEN_CREATE_DB:
                        if (currentPath != null) {
                            getIntent().putExtra(RESULT_OPEN_PATH, (String)null);
                            getIntent().putExtra(RESULT_PREVIEW_PATH, (String)null);
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
        case SelectionMode.MODE_OPEN:
        case SelectionMode.MODE_OPEN_DB:
        case SelectionMode.MODE_OPEN_UPLOAD_SOURCE:
        case SelectionMode.MODE_OPEN_CREATE:
        case SelectionMode.MODE_OPEN_CREATE_DB:
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
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        switch (selectionMode) {
        case SelectionMode.MODE_CREATE:
        case SelectionMode.MODE_OPEN_MULTISELECT:
        case SelectionMode.MODE_OPEN_MULTISELECT_DB:
        case SelectionMode.MODE_OPEN_CREATE:
        case SelectionMode.MODE_OPEN_CREATE_DB:

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
            .setIcon(android.R.drawable.ic_menu_revert).setOnMenuItemClickListener(new OnMenuItemClickListener() {

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
        outState.putString("currentRoot", currentRoot);
        outState.putString("currentRootLabel", currentRootLabel);
        outState.putInt("selectionMode", selectionMode);
    }

    private void getDir(String dirPath, String rootPath, String rootName) {

        boolean useAutoSelection = dirPath.length() < currentPath.length();

        Integer position = lastPositions.get(parentPath);
        
        switch (selectionMode) {
        case SelectionMode.MODE_OPEN_CREATE_DB:
        case SelectionMode.MODE_OPEN_DB:
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
        if (files == null) {
            currentPath = currentRoot;
            f = new VirtualFile(currentPath);
            files = f.listFiles();
        }
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

        /* getPath() returns full path including file name */
        for (VirtualFile file : files) {
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
        case SelectionMode.MODE_OPEN:
        case SelectionMode.MODE_OPEN_DB:
        case SelectionMode.MODE_OPEN_UPLOAD_SOURCE:
        case SelectionMode.MODE_OPEN_CREATE:
        case SelectionMode.MODE_OPEN_CREATE_DB: {
            mListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            break;
        }
        default: {
            mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
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

            /* if (layoutCreate.getVisibility() == View.VISIBLE) {
                layoutCreate.setVisibility(View.GONE);
                layoutSelect.setVisibility(View.VISIBLE);
            } else {*/
                if (!currentPath.equals(currentRoot)) {
                    getDir(parentPath, currentRoot, currentRootLabel);
                } else {
                    return super.onKeyDown(keyCode, event);
                }
            //}

            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenuInfo menuInfo) {
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
        getIntent().putExtra(RESULT_EXPORT_PATHS, (String[])null);
        getIntent().putExtra(RESULT_UPLOAD_PATH, (String)null);
        getIntent().putExtra(RESULT_OPEN_PATH, pathName);
        getIntent().putExtra(RESULT_PREVIEW_PATH, (String)null);
        setResult(RESULT_OK, getIntent());
        finish();
        return true;
      case R.id.context_preview:
          getIntent().putExtra(RESULT_EXPORT_PATHS, (String[])null);
          getIntent().putExtra(RESULT_UPLOAD_PATH, (String)null);
          getIntent().putExtra(RESULT_OPEN_PATH, (String)null);
          getIntent().putExtra(RESULT_PREVIEW_PATH, pathName);
          setResult(RESULT_OK, getIntent());
          finish();
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

    public static class ProgressDialogFragment extends SherlockDialogFragment {

        public static ProgressDialogFragment newInstance(int titleId, int msgId) {
            ProgressDialogFragment frag = new ProgressDialogFragment();
            Bundle args = new Bundle();
            args.putInt("titleId", titleId);
            args.putInt("msgId", msgId);
            frag.setArguments(args);
            return frag;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final int titleId = getArguments().getInt("titleId");
            final int msgId = getArguments().getInt("msgId");
            final ProgressDialog pd = new ProgressDialog(getActivity());
            pd.setTitle(getString(titleId));
            pd.setMessage(getString(msgId));
            return pd;
        }
    }

    private void showProgressDialog(int titleId, int msgId) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment prev = getSupportFragmentManager().findFragmentByTag("progdialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
        final SherlockDialogFragment pdFragment = 
                ProgressDialogFragment.newInstance(titleId, msgId);
        pdFragment.show(ft, "progdialog");
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
        
        showProgressDialog(R.string.wait_msg, getStorage().waitStringId);
        new Thread(new Runnable(){
            public void run(){
                switch (selectionMode) {
                case SelectionMode.MODE_OPEN_MULTISELECT:
                case SelectionMode.MODE_OPEN_MULTISELECT_DB:
                    getStorage().mkVisibleDecoded(path, fEncFSRoot, rootPath);
                    break;
                default:
                    getStorage().mkVisiblePlain(path, rootPath);
                }
                runOnUiThread(new Runnable(){
                    public void run() {
                        Cryptonite.dismissProgressDialog(FileDialog.this);
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
                    getIntent().putExtra(RESULT_PREVIEW_PATH, (String)null);
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
            getIntent().putExtra(RESULT_PREVIEW_PATH, (String)null);
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
                    getIntent().putExtra(RESULT_PREVIEW_PATH, (String)null);
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
            getIntent().putExtra(RESULT_PREVIEW_PATH, (String)null);
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
    
}