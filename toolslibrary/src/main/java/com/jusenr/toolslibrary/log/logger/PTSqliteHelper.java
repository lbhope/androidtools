/*
 * Copyright 2017 androidtools Jusenr
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jusenr.toolslibrary.log.logger;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Created by xiaopeng on 2017/8/7.
 */

public class PTSqliteHelper extends SQLiteOpenHelper {
    public static final String DB_NAME = "ptlog";
    public static final String TABLE_NAME = "t_log";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_LEVEL = "level";
    public static final String COLUMN_CONTENT = "content";
    public static final String COLUMN_DATE = "date";

    private static final int DB_VERSION = 1;

    private PTSqliteHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    private static PTSqliteHelper instance;

    public static synchronized PTSqliteHelper getInstance(Context context) {
        if (instance == null)
            instance = new PTSqliteHelper(context);

        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        try {
            String sql = "CREATE TABLE " + TABLE_NAME
                    + "(" + COLUMN_ID +
                    " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COLUMN_LEVEL +
                    " INTEGER, " +
                    COLUMN_CONTENT +
                    " VARCHAR, " +
                    COLUMN_DATE +
                    " INTEGER);";
            sqLiteDatabase.execSQL(sql);
        } catch (SQLException ex) {
            Log.d("PTSqliteHelper", ex.getMessage());
            return;
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        String sql = "DROP TABLE IF EXISTS " + TABLE_NAME;
        sqLiteDatabase.execSQL(sql);
        onCreate(sqLiteDatabase);
    }

    public void insertLog(int priority, String message) {
        SQLiteDatabase db = null;
        try {
            db = this.getWritableDatabase();
            ContentValues contentValues = new ContentValues();

            contentValues.put(PTSqliteHelper.COLUMN_LEVEL, priority);
            contentValues.put(PTSqliteHelper.COLUMN_CONTENT, message);
            contentValues.put(PTSqliteHelper.COLUMN_DATE, new Date().getTime());

            db.insert(TABLE_NAME, null, contentValues);
        } catch (SQLException ex) {
            return;
        }
    }

    public List<PTLogBean> queryLog(int priority, Date begin, Date end, int limit) {
        SQLiteDatabase db = null;
        List<PTLogBean> logList = new ArrayList<PTLogBean>();
        try {
            db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery(String.format("SELECT * FROM %s WHERE %s>=? AND %s>=? AND %s<? ORDER BY %s DESC LIMIT ?", TABLE_NAME, COLUMN_LEVEL, COLUMN_DATE, COLUMN_DATE, COLUMN_DATE),
                    new String[]{
                            Integer.toString(priority),
                            Long.toString(begin.getTime()),
                            Long.toString(end.getTime()),
                            Integer.toString(limit)
                    }
            );

            while (cursor.moveToNext()) {
                PTLogBean logBean = new PTLogBean();
                logBean.setLevel(cursor.getInt(cursor.getColumnIndex(PTSqliteHelper.COLUMN_LEVEL)));
                logBean.setContent(cursor.getString(cursor.getColumnIndex(PTSqliteHelper.COLUMN_CONTENT)));

                String dateString = cursor.getString(cursor.getColumnIndex(PTSqliteHelper.COLUMN_DATE));
                Date date = new Date();
                date.setTime(Long.parseLong(dateString));
                logBean.setDate(date);

                logList.add(logBean);
            }
        } catch (SQLException ex) {
            return logList;
        }

        // reverse
        List<?> shallowCopy = logList.subList(0, logList.size());
        Collections.reverse(shallowCopy);
        return logList;
    }

    public int deleteLog(int priority, Date date) {
        SQLiteDatabase db = null;
        int deleteCount = -1;
        try {
            db = this.getReadableDatabase();
            deleteCount = db.delete(TABLE_NAME,
                    COLUMN_LEVEL + ">=? AND " + COLUMN_DATE + "<?",
                    new String[]{
                            Integer.toString(priority),
                            Long.toString(date.getTime()),
                    });

        } catch (SQLException ex) {
            return deleteCount;
        } finally {
            if (db != null) {
                db.close();
            }
        }
        return deleteCount;
    }
}