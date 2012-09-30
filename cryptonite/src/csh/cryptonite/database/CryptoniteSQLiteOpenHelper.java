package csh.cryptonite.database;

import android.content.Context;
import android.util.Log;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class CryptoniteSQLiteOpenHelper extends SQLiteOpenHelper {

    public static final String TABLE_VOLUMES = "volumes";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_STORTYPE = "stortype";
    public static final String COLUMN_MOUNTTYPE = "mounttype";
    public static final String COLUMN_LABEL = "label";
    public static final String COLUMN_SOURCE = "source";
    public static final String COLUMN_TARGET = "target";
    public static final String COLUMN_ENCFS_CONFIG = "encfs_config";

    private static final String DATABASE_NAME = "volumes.db";
    private static final int DATABASE_VERSION = 1;

    // Database creation sql statement
    private static final String DATABASE_CREATE = "create table "
        + TABLE_VOLUMES + "("
        + COLUMN_ID + " integer primary key autoincrement, "
        + COLUMN_STORTYPE + " integer, " 
        + COLUMN_MOUNTTYPE + " integer, " 
        + COLUMN_LABEL + " text not null, "
        + COLUMN_SOURCE + " text not null, "
        + COLUMN_TARGET + " text not null, "
        + COLUMN_ENCFS_CONFIG + " text not null);";

    public CryptoniteSQLiteOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DATABASE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(CryptoniteSQLiteOpenHelper.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_VOLUMES);
        onCreate(db);
    }

}
