package com.example.imanicommunityapp.Sync.Queue;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Lightweight SQLite implementation of EventQueueDb.
 * Runs all DB ops on a single-thread executor to simplify concurrency.
 */
public class SqliteEventQueueDb extends SQLiteOpenHelper implements EventQueueDb {
    private static final String TAG = "SqliteEventQueueDb";
    private static final String DB_NAME = "event_queue.db";
    private static final int DB_VERSION = 1;
    private static final String TABLE = "events";

    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor(r -> new Thread(r, "EventQueueDbThread"));
    private final Context context;
    private SQLiteStatement insertStmt;

    public SqliteEventQueueDb(@NonNull Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        this.context = context.getApplicationContext();
        // Open database once to prepare statements
        SQLiteDatabase db = getWritableDatabase();
        prepareStatements(db);
    }

    private void prepareStatements(SQLiteDatabase db) {
        db.execSQL("PRAGMA journal_mode=WAL;");
        db.execSQL("PRAGMA synchronous=NORMAL;");
        insertStmt = db.compileStatement("INSERT INTO " + TABLE + "(timestamp, payload, uid) VALUES (?, ?, ?)");
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "timestamp INTEGER NOT NULL, " +
                "payload TEXT, " +
                "uid TEXT" +
                ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_events_ts_id ON " + TABLE + "(timestamp ASC, id ASC)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // No-op for now
    }

    @Override
    public long insert(@NonNull EventItem event) {
        try {
            Future<Long> f = dbExecutor.submit(() -> {
                SQLiteDatabase db = getWritableDatabase();
                db.beginTransaction();
                try {
                    insertStmt.clearBindings();
                    insertStmt.bindLong(1, event.getTimestamp());
                    if (event.getPayload() != null) insertStmt.bindString(2, event.getPayload());
                    else insertStmt.bindNull(2);
                    if (event.getUid() != null) insertStmt.bindString(3, event.getUid());
                    else insertStmt.bindNull(3);
                    long rowId = insertStmt.executeInsert();
                    db.setTransactionSuccessful();
                    return rowId;
                } finally {
                    db.endTransaction();
                }
            });
            return f.get(5, TimeUnit.SECONDS);
        } catch (Exception ex) {
            Log.w(TAG, "insert failed", ex);
            return -1L;
        }
    }

    @NonNull
    @Override
    public List<EventItem> fetchNext(int limit) {
        try {
            Future<List<EventItem>> f = dbExecutor.submit(() -> {
                List<EventItem> out = new ArrayList<>();
                SQLiteDatabase db = getReadableDatabase();
                Cursor c = db.query(TABLE, new String[]{"id", "timestamp", "payload", "uid"}, null, null, null, null, "timestamp ASC, id ASC", Integer.toString(limit));
                try {
                    while (c.moveToNext()) {
                        long id = c.getLong(0);
                        long ts = c.getLong(1);
                        String payload = c.isNull(2) ? null : c.getString(2);
                        String uid = c.isNull(3) ? null : c.getString(3);
                        out.add(new EventItem(id, ts, payload, uid));
                    }
                } finally {
                    c.close();
                }
                return out;
            });
            return f.get(5, TimeUnit.SECONDS);
        } catch (Exception ex) {
            Log.w(TAG, "fetchNext failed", ex);
            return new ArrayList<>();
        }
    }

    @Override
    public void deleteByIds(@NonNull List<Long> ids) {
        if (ids.isEmpty()) return;
        dbExecutor.execute(() -> {
            SQLiteDatabase db = getWritableDatabase();
            db.beginTransaction();
            try {
                StringBuilder sb = new StringBuilder();
                sb.append("DELETE FROM ").append(TABLE).append(" WHERE id IN (");
                for (int i = 0; i < ids.size(); i++) {
                    if (i != 0) sb.append(',');
                    sb.append('?');
                }
                sb.append(')');
                Object[] args = new Object[ids.size()];
                for (int i = 0; i < ids.size(); i++) args[i] = Long.toString(ids.get(i));
                db.execSQL(sb.toString(), args);
                db.setTransactionSuccessful();
            } catch (Exception ex) {
                Log.w(TAG, "deleteByIds failed", ex);
            } finally {
                db.endTransaction();
            }
        });
    }

    @Override
    public int count() {
        try {
            Future<Integer> f = dbExecutor.submit(() -> {
                SQLiteDatabase db = getReadableDatabase();
                Cursor c = db.rawQuery("SELECT COUNT(1) FROM " + TABLE, null);
                try {
                    if (c.moveToFirst()) return c.getInt(0);
                    return 0;
                } finally {
                    c.close();
                }
            });
            return f.get(5, TimeUnit.SECONDS);
        } catch (Exception ex) {
            Log.w(TAG, "count failed", ex);
            return 0;
        }
    }

    @Override
    public void compactOlderThan(long timestamp) {
        dbExecutor.execute(() -> {
            SQLiteDatabase db = getWritableDatabase();
            db.beginTransaction();
            try {
                db.execSQL("DELETE FROM " + TABLE + " WHERE timestamp < ?", new Object[]{Long.toString(timestamp)});
                db.setTransactionSuccessful();
            } catch (Exception ex) {
                Log.w(TAG, "compactOlderThan failed", ex);
            } finally {
                db.endTransaction();
            }
        });
    }

    public void closeDb() {
        try {
            dbExecutor.shutdown();
            dbExecutor.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {}
        super.close();
    }
}
