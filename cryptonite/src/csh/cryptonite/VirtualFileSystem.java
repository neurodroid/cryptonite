package csh.cryptonite;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Singleton Virtual File System */
public enum VirtualFileSystem {
    VFS;
    
    private VirtualFile root;
    
    VirtualFileSystem() {
        root = new VirtualFile(VirtualFile.VIRTUAL_TAG + "/");
        root.mkdirs();
    }
    
    public boolean exists(VirtualFile file) {
        return (findFile(file, root) != null);
    }
    
    private VirtualFile findFile(VirtualFile file, VirtualFile currentFile) {
        if (file.getPath() == currentFile.getPath()) {
            return currentFile;
        }
        
        if (currentFile.isDirectory()) {
            for (VirtualFile child : currentFile.listFiles()) {
                VirtualFile res = findFile(file, child);
                
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
    
    private void recMkDir(VirtualFile dir, VirtualFile currentFile, boolean isDir) {
        if (dir.getPath() == currentFile.getPath()) {
            return;
        }
        for (VirtualFile child : currentFile.listFiles()) {
            if (child.isDirectory()) {
                if (child.getPath() == dir.getParent()) {
                    dir.setIsDir(isDir);
                    child.addChild(dir);
                    return;
                } else {
                    recMkDir(dir, child, isDir);
                }
            }
        }
    }
    
    public boolean mkdirs(VirtualFile file, boolean isDir) {
        String currentFilePath = file.getPath();
        List<String> chain = new ArrayList<String>();
        boolean existed = true;
        
        while (currentFilePath != root.getPath()) {
            chain.add(currentFilePath);
            currentFilePath = new VirtualFile(currentFilePath).getParent();
        }
        
        for (String createPath : chain) {
            VirtualFile currentFile = new VirtualFile(createPath);
            if (!currentFile.exists()) {
                existed = false;
                recMkDir(currentFile, root, isDir);
            }
        }
        
        return existed;
    }

    public Set<VirtualFile> listFiles(VirtualFile file) {
        VirtualFile fromRoot = findFile(file, root);
        if (fromRoot == null) {
            return null;
        } else {
            return fromRoot.getChildren();
        }
    }
    
    public boolean isDirectory(VirtualFile file) {
        VirtualFile fromRoot = findFile(file, root);
        if (fromRoot == null) {
            return false;
        } else {
            return fromRoot.isDirectory();
        }
    }
}
