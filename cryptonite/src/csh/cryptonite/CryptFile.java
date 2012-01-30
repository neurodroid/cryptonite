package csh.cryptonite;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CryptFile extends File {

    public static final String CRYPT_TAG = "<crypt>";
    private boolean isCrypt = false;
    private String cryptPath = "";
    private File cryptFile = null;
    
    public CryptFile(String path) {
        
        super(path);
        
        if (path.startsWith(CRYPT_TAG)) {
            isCrypt = true;
            cryptPath = path.substring(CRYPT_TAG.length());
            cryptFile = new File(cryptPath);
        }
        

    }

    @Override
    public CryptFile[] listFiles() {
        List<CryptFile> cryptFiles = new ArrayList<CryptFile>();
        File[] files;
        
        if (!isCrypt) {
            files = super.listFiles();
        } else {
            files = cryptFile.listFiles();
        }

        for (File file : files) {
            cryptFiles.add(new CryptFile(file.getPath()));
        }
        
        return cryptFiles.toArray(new CryptFile[0]);
    }
    
    @Override
    public String getParent() {
        if (!isCrypt) {
            return super.getParent();
        } else {
            return cryptFile.getParent();
        }
    }
    
    @Override
    public String getPath() {
        if (!isCrypt) {
            return super.getPath();
        } else {
            return cryptFile.getPath();
        }
    }

    @Override
    public boolean isDirectory() {
        if (!isCrypt) {
            return super.isDirectory();
        } else {
            return cryptFile.isDirectory();
        }
    }
    
    @Override
    public String getName() {
        if (!isCrypt) {
            return super.getName();
        } else {
            return cryptFile.getName();
        }
    }
    
    @Override
    public boolean canRead() {
        if (!isCrypt) {
            return super.canRead();
        } else {
            return cryptFile.canRead();
        }
    }
    
    private static final long serialVersionUID = 1L;

}
