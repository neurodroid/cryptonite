package csh.cryptonite.database;

import csh.cryptonite.Cryptonite;
import android.content.Context;

public class Volume {
    
    public static final long VIRTUAL=0, MOUNT = 1;

    private long id;
    private long storType;
    private long mountType;
    private String label;
    private String src;
    private String target;
    private String encfsConfig;
    private Context mContext;
    
    public Volume(Context context) {
        mContext = context;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getId() {
      return id;
    }

    public void setStorType(long storType) {
        this.storType = storType;
    }

    public long getStorType() {
        return storType;
    }

    public void setMountType(long mountType) {
        this.mountType = mountType;
    }

    public long getMountType() {
        return mountType;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return Cryptonite.decrypt(label, mContext);
    }

    public void setSource(String src) {
        this.src = src;
    }

    public String getSource() {
      return Cryptonite.decrypt(src, mContext);
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getTarget() {
        return Cryptonite.decrypt(target, mContext);
    }

    public void setEncfsConfig(String encfsConfig) {
        this.encfsConfig = encfsConfig;
    }

    public String getEncfsConfig() {
        return Cryptonite.decrypt(encfsConfig, mContext);
    }

    // Will be used by the ArrayAdapter in the ListView
    @Override
    public String toString() {
        return Cryptonite.decrypt(label, mContext);
    }
}
