package csh.cryptonite.database;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class VolumesDataSource {

    // Database fields
    private SQLiteDatabase database;
    private CryptoniteSQLiteOpenHelper dbHelper;
    private String[] allColumns = { 
            CryptoniteSQLiteOpenHelper.COLUMN_ID,
            CryptoniteSQLiteOpenHelper.COLUMN_STORTYPE,
            CryptoniteSQLiteOpenHelper.COLUMN_MOUNTTYPE,
            CryptoniteSQLiteOpenHelper.COLUMN_LABEL,
            CryptoniteSQLiteOpenHelper.COLUMN_SOURCE,
            CryptoniteSQLiteOpenHelper.COLUMN_TARGET,
            CryptoniteSQLiteOpenHelper.COLUMN_ENCFS_CONFIG};
    private Context mContext;

    public VolumesDataSource(Context context) {
        dbHelper = new CryptoniteSQLiteOpenHelper(context);
        mContext = context;
    }

    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public Volume createVolume(long storType, long mountType, String label, 
            String src, String target, String encfsConfig)
    {
        ContentValues values = new ContentValues();
        values.put(CryptoniteSQLiteOpenHelper.COLUMN_STORTYPE, storType);
        values.put(CryptoniteSQLiteOpenHelper.COLUMN_MOUNTTYPE, mountType);
        values.put(CryptoniteSQLiteOpenHelper.COLUMN_LABEL, label);
        values.put(CryptoniteSQLiteOpenHelper.COLUMN_SOURCE, src);
        values.put(CryptoniteSQLiteOpenHelper.COLUMN_TARGET, target);
        values.put(CryptoniteSQLiteOpenHelper.COLUMN_ENCFS_CONFIG, encfsConfig);
        
        /* Delete any existing entries of this type */
        database.delete(CryptoniteSQLiteOpenHelper.TABLE_VOLUMES,
                CryptoniteSQLiteOpenHelper.COLUMN_STORTYPE + " = " + storType
                + " AND " + CryptoniteSQLiteOpenHelper.COLUMN_MOUNTTYPE + " = " + mountType, null);
        
        long insertId = database.insert(CryptoniteSQLiteOpenHelper.TABLE_VOLUMES, null,
                values);
        Cursor cursor = database.query(CryptoniteSQLiteOpenHelper.TABLE_VOLUMES,
                allColumns, CryptoniteSQLiteOpenHelper.COLUMN_ID + " = " + insertId, null,
                null, null, null);
        cursor.moveToFirst();
        Volume newVolume = cursorToVolume(cursor);
        cursor.close();
        return newVolume;
    }
    
    public Volume getVolume(long storType, long mountType) {
        Cursor cursor = database.query(CryptoniteSQLiteOpenHelper.TABLE_VOLUMES,
                allColumns, CryptoniteSQLiteOpenHelper.COLUMN_STORTYPE + " = " + storType
                + " AND " + CryptoniteSQLiteOpenHelper.COLUMN_MOUNTTYPE + " = " + mountType, null,
                null, null, null);
        cursor.moveToFirst();
        int count = cursor.getCount();
        if (count == 0) {
            cursor.close();
            return null;
        } else {
            Volume volume = cursorToVolume(cursor);
            cursor.close();
            return volume;
        }
    }

    public void deleteVolume(Volume volume) {
        long id = volume.getId();
        System.out.println("Volume deleted with id: " + id);
        database.delete(CryptoniteSQLiteOpenHelper.TABLE_VOLUMES, CryptoniteSQLiteOpenHelper.COLUMN_ID
                + " = " + id, null);
    }

    public List<Volume> getAllVolumes() {
        List<Volume> volumes = new ArrayList<Volume>();

        Cursor cursor = database.query(CryptoniteSQLiteOpenHelper.TABLE_VOLUMES,
                allColumns, null, null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            Volume volume = cursorToVolume(cursor);
            volumes.add(volume);
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();
        return volumes;
    }

    private Volume cursorToVolume(Cursor cursor) {
        Volume volume = new Volume(mContext);
        int nid = 0;
        volume.setId(cursor.getLong(nid++));
        volume.setStorType(cursor.getLong(nid++));
        volume.setMountType(cursor.getLong(nid++));
        volume.setLabel(cursor.getString(nid++));
        volume.setSource(cursor.getString(nid++));
        volume.setTarget(cursor.getString(nid++));
        volume.setEncfsConfig(cursor.getString(nid++));
        return volume;
    }
}
