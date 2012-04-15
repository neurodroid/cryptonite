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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;


/* TODO: Implement all File functions, or throw RuntimeException
 *       from unimplemented functions
 */

public class VirtualFile extends File {

    public static final String VIRTUAL_TAG = "<virtual>";
    private boolean isVirtual = false;
    private CopyOnWriteArraySet<VirtualFile> children;
    private boolean isDir;
    
    public VirtualFile(String path) {
        
        super(path);
        
        if (path.startsWith(VIRTUAL_TAG)) {
            isVirtual = true;
            /* normalise path name */
            if (VirtualFileSystem.INSTANCE.isInitialized()) {
                // virtualPath = this.getPath().substring(VIRTUAL_TAG.length());
                isDir = VirtualFileSystem.INSTANCE.isDirectory(path);
                children = VirtualFileSystem.INSTANCE.listFiles(path);
            }
        }
    }

    @Override
    public boolean exists() {
        if (!isVirtual) {
            return super.exists();
        } else {
            return VirtualFileSystem.INSTANCE.exists(this.getPath());
        }
    }
    
    @Override
    public VirtualFile[] listFiles() {
        
        if (!isVirtual) {
            List<VirtualFile> cryptFiles = new ArrayList<VirtualFile>();
            File[] files = super.listFiles();
            for (File file : files) {
                cryptFiles.add(new VirtualFile(file.getPath()));
            }
            return cryptFiles.toArray(new VirtualFile[0]);
        } else {
            if (isDirectory()) {
                return children.toArray(new VirtualFile[0]);
            } else {
                return null;
            }
        }
    }
    
    @Override
    public String getParent() {
        return super.getParent();
    }
    
    @Override
    public VirtualFile getParentFile() {
        return new VirtualFile(super.getParent());
    }
    
    @Override
    public String getPath() {
        return super.getPath();
    }
    
    @Override
    public boolean isDirectory() {
        if (!isVirtual) {
            return super.isDirectory();
        } else {
            return isDir;
        }
    }
    
    @Override
    public String getName() {
        return super.getName();
    }
    
    @Override
    public boolean canRead() {
        if (!isVirtual) {
            return super.canRead();
        } else {
            return true;
        }
    }
    
    @Override
    public boolean mkdirs() {
        if (!isVirtual) {
            return super.mkdirs();
        } else {
            boolean res = VirtualFileSystem.INSTANCE.mkdirs(this, true);
            
            return res;
        }
    }
 
    @Override
    public boolean mkdir() {
        if (!isVirtual) {
            return super.mkdir();
        } else {
            isDir = false;
            if (exists()) {
                return false;
            }
            /* Does the parent directory exist? */
            if (!getParentFile().exists()) {
                return false;
            }
            VirtualFileSystem.INSTANCE.mkdirs(this, true);
            return false;
        }
        
    }

    @Override
    public boolean createNewFile() throws IOException {
        if (!isVirtual) {
            return super.createNewFile();
        } else {
            if (exists()) {
                return false;
            }
            /* Does the parent directory exist? */
            if (!getParentFile().exists()) {
                throw new IOException("parent directory doesn't exist");
            }

            return VirtualFileSystem.INSTANCE.mkdirs(this, false);
        }
        
    }
    
    @Override
    public boolean delete() {
        if (!isVirtual) {
            return super.delete();
        } else {
            return VirtualFileSystem.INSTANCE.delete(this);
        }
    }
    
    protected boolean addChild(VirtualFile child) {
        if (!isDir) {
            return false;
        }
        children.add(child);
        return true;
    }
    
    protected void setIsDir(boolean setDir) {
        children = new CopyOnWriteArraySet<VirtualFile>();
        isDir = setDir;
    }
    
    protected CopyOnWriteArraySet<VirtualFile> getChildren() {
        return children;
    }
    
    private static final long serialVersionUID = 1L;

}
