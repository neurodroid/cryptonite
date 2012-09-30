package csh.cryptonite.database;

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

    public long getId() {
      return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getStorType() {
        return storType;
    }

    public void setStorType(long storType) {
        this.storType = storType;
    }

    public long getMountType() {
        return mountType;
    }

    public void setMountType(long mountType) {
        this.mountType = mountType;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getSource() {
      return src;
    }

    public void setSource(String src) {
      this.src = src;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getEncfsConfig() {
        return encfsConfig;
    }

    public void setEncfsConfig(String encfsConfig) {
        this.encfsConfig = encfsConfig;
    }

    // Will be used by the ArrayAdapter in the ListView
    @Override
    public String toString() {
        return label;
    }
}
