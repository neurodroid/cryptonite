package csh.cryptonite;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CryptFile extends File {

    public CryptFile(String path) {
        super(path);
        // TODO Auto-generated constructor stub
    }

    @Override
    public CryptFile[] listFiles() {
        List<CryptFile> cryptFiles = new ArrayList<CryptFile>();
        File[] files = super.listFiles();
        
        for (File file : files) {
            cryptFiles.add(new CryptFile(file.getPath()));
        }
        
        return cryptFiles.toArray(new CryptFile[0]);
    }
    
    private static final long serialVersionUID = 1L;

}
