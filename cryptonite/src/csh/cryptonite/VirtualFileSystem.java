package csh.cryptonite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/** Singleton Virtual File System */
public enum VirtualFileSystem {
    INSTANCE;
    
    private VirtualFile root;
    private boolean isInit = false;
    
    public void init() {
        root = new VirtualFile(VirtualFile.VIRTUAL_TAG + "/");
        root.setIsDir(true);
        isInit = true;
    }
    
    public boolean exists(String filepath) {
        return (findFile(filepath, root) != null);
    }
    
    private VirtualFile findFile(String filepath, VirtualFile currentFile) {
        if (filepath.equals(currentFile.getPath())) {
            return currentFile;
        }
        
        if (currentFile.isDirectory()) {
            for (VirtualFile child : currentFile.listFiles()) {
                VirtualFile res = findFile(filepath, child);
                
                if (res != null) {
                    return res;
                }
            }
        }
        
        return null;
        
    }
    
    public String getRootPath() {
        return root.getPath();
    }
    
    public VirtualFile getRoot() {
        return root;
    }
    
    public boolean mkdirs(VirtualFile file, boolean isDir) {
        String filePath = file.getPath();
        List<String> chain = new ArrayList<String>();
        boolean existed = true;
        
        String rootPath = root.getPath();
        
        while (!filePath.equals(rootPath)) {
            chain.add(filePath);
            filePath = new VirtualFile(filePath).getParent();
        }

        Collections.reverse(chain);
        
        for (String createPath : chain) {
            VirtualFile currentFile = new VirtualFile(createPath);
            if (!currentFile.exists()) {
                existed = false;
                VirtualFile parentFile = findFile(currentFile.getParent(), root);
                if (!currentFile.getPath().equals(file.getPath()) || isDir) {
                    currentFile.setIsDir(true);
                }
                parentFile.addChild(currentFile);
            }
        }
        
        return existed;
    }

    public Set<VirtualFile> listFiles(String filepath) {
        VirtualFile fromRoot = findFile(filepath, root);
        if (fromRoot == null) {
            return null;
        } else {
            return fromRoot.getChildren();
        }
    }
    
    public boolean isDirectory(String filepath) {
        VirtualFile fromRoot = findFile(filepath, root);
        if (fromRoot == null) {
            return false;
        } else {
            return fromRoot.isDirectory();
        }
    }
    
    public boolean isInitialized() {
        return isInit;
    }
    
    public void clear() {
        if (isInit) {
            root.getChildren().clear();
        }
    }
}
