package csh.cryptonite.database;

import java.util.Arrays;
import java.util.HashSet;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

public class CryptoniteContentProvider extends ContentProvider {

    // database
    private DatabaseHelper database;

    // Used for the UriMacher
    private static final int VOLUMES = 10;
    private static final int VOLUME_ID = 20;

    private static final String AUTHORITY = "csh.cryptonite.contentprovider";

    private static final String BASE_PATH = "todos";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY
            + "/" + BASE_PATH);

    public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE
            + "/todos";
    public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE
            + "/todo";

    private static final UriMatcher sURIMatcher = new UriMatcher(
            UriMatcher.NO_MATCH);
    static {
        sURIMatcher.addURI(AUTHORITY, BASE_PATH, VOLUMES);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/#", VOLUME_ID);
    }

    @Override
    public boolean onCreate() {
        database = new DatabaseHelper(getContext());
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {

        // Using SQLiteQueryBuilder instead of query() method
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

        // Check if the caller has requested a column which does not exists
        checkColumns(projection);

        // Set the table
        queryBuilder.setTables(CryptoniteTable.TABLE_CRYPTONITE);

        int uriType = sURIMatcher.match(uri);
        switch (uriType) {
        case VOLUMES:
            break;
        case VOLUME_ID:
            // Adding the ID to the original query
            queryBuilder.appendWhere(CryptoniteTable.COLUMN_ID + "="
                    + uri.getLastPathSegment());
            break;
        default:
            throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        SQLiteDatabase db = database.getWritableDatabase();
        Cursor cursor = queryBuilder.query(db, projection, selection,
                selectionArgs, null, null, sortOrder);
        // Make sure that potential listeners are getting notified
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase sqlDB = database.getWritableDatabase();
        long id = 0;
        switch (uriType) {
        case VOLUMES:
            id = sqlDB.insert(CryptoniteTable.TABLE_CRYPTONITE, null, values);
            break;
        default:
            throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return Uri.parse(BASE_PATH + "/" + id);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase sqlDB = database.getWritableDatabase();
        int rowsDeleted = 0;
        switch (uriType) {
        case VOLUMES:
            rowsDeleted = sqlDB.delete(CryptoniteTable.TABLE_CRYPTONITE, selection,
                    selectionArgs);
            break;
        case VOLUME_ID:
            String id = uri.getLastPathSegment();
            if (TextUtils.isEmpty(selection)) {
                rowsDeleted = sqlDB.delete(
                        CryptoniteTable.TABLE_CRYPTONITE,
                        CryptoniteTable.COLUMN_ID + "=" + id, 
                        null);
            } else {
                rowsDeleted = sqlDB.delete(
                        CryptoniteTable.TABLE_CRYPTONITE,
                        CryptoniteTable.COLUMN_ID + "=" + id 
                        + " and " + selection,
                        selectionArgs);
            }
            break;
        default:
            throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsDeleted;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {

        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase sqlDB = database.getWritableDatabase();
        int rowsUpdated = 0;
        switch (uriType) {
        case VOLUMES:
            rowsUpdated = sqlDB.update(CryptoniteTable.TABLE_CRYPTONITE, 
                    values, 
                    selection,
                    selectionArgs);
            break;
        case VOLUME_ID:
            String id = uri.getLastPathSegment();
            if (TextUtils.isEmpty(selection)) {
                rowsUpdated = sqlDB.update(CryptoniteTable.TABLE_CRYPTONITE, 
                        values,
                        CryptoniteTable.COLUMN_ID + "=" + id, 
                        null);
            } else {
                rowsUpdated = sqlDB.update(CryptoniteTable.TABLE_CRYPTONITE, 
                        values,
                        CryptoniteTable.COLUMN_ID + "=" + id 
                        + " and " 
                        + selection,
                        selectionArgs);
            }
            break;
        default:
            throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsUpdated;
    }

    private void checkColumns(String[] projection) {
        String[] available = { CryptoniteTable.COLUMN_CATEGORY,
                CryptoniteTable.COLUMN_SUMMARY, CryptoniteTable.COLUMN_DESCRIPTION,
                CryptoniteTable.COLUMN_ID };
        if (projection != null) {
            HashSet<String> requestedColumns = new HashSet<String>(
                    Arrays.asList(projection));
            HashSet<String> availableColumns = new HashSet<String>(
                    Arrays.asList(available));
            // Check if all columns which are requested are available
            if (!availableColumns.containsAll(requestedColumns)) {
                throw new IllegalArgumentException(
                        "Unknown columns in projection");
            }
        }
    }

}