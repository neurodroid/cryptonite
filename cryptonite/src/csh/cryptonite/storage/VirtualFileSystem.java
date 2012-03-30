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

package csh.cryptonite.storage;

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

    private boolean recDel(String filepath, VirtualFile currentFile) {
        if (currentFile.isDirectory()) {
            for (VirtualFile child : currentFile.listFiles()) {
                if (child.getPath().equals(filepath)) {
                    return currentFile.getChildren().remove(child);
                } else {
                    if (recDel(filepath, child)) {
                        return true;
                    }
                }
            }
        }
        
        return false;
        
    }
    
    public boolean delete(VirtualFile delFile) {
        return recDel(delFile.getPath(), root);
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
