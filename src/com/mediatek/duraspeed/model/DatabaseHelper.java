/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 *
 * MediaTek Inc. (C) 2018. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */
package com.mediatek.duraspeed.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.mediatek.duraspeed.presenter.AppRecord;
import com.mediatek.duraspeed.presenter.AppShowedCategory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "DatabaseHelper";
    private static final String DB_NAME = "app_whitelist.db";
    private static final int DB_VERSION = 1;
    // table name
    private static final String TABLE_WHITELIST = "app_whitelist";
    // item list
    private static final String FIELD_ID = "_id";
    private static final String FIELD_NAME_PACKAGE = "packages_name";
    private static final String FIELD_NAME_CATEGORY = "category_name"; // UI category
    private static final String FIELD_NAME_ISWHITELIST_STATUS = "whitelist_status";

    private HashMap<String, List<AppRecord>> mPkgKeyCache = new HashMap<String, List<AppRecord>>();
    private HashMap<String, List<AppRecord>> mCategoryKeyCache = new HashMap<String,
            List<AppRecord>>();
    private ArrayList<AppRecord> mAppRecordsCache = new ArrayList<AppRecord>();

    private SQLiteDatabase mDb;

    /**
     * Construct of DatabaseHelper, this construct to init database and access
     * data base.
     *
     * @param context
     */
    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        mDb = getWritableDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_WHITELIST
                + " (" + FIELD_ID + " INTEGER primary key autoincrement," + " "
                + FIELD_NAME_PACKAGE + " text," + " " + FIELD_NAME_CATEGORY
                + " text," + " " + FIELD_NAME_ISWHITELIST_STATUS + " INTEGER)";
        db.execSQL(sql);
    }

    /**
     * Because current there is no new add into db so do nothing
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "onUpgrade from " + oldVersion + " to " + newVersion);
    }

    /**
     * The data base cache with package name as key.
     *
     * @return the cache map
     */
    public HashMap<String, List<AppRecord>> getPkgKeyCache() {
        return mPkgKeyCache;
    }

    /**
     * The data base cache with category name as key.
     *
     * @return the cache map
     */
    public HashMap<String, List<AppRecord>> getPermKeyCache() {
        return mCategoryKeyCache;
    }

    public ArrayList<AppRecord> getAppRecordsCache() {
        return mAppRecordsCache;
    }

    // update table according to the pkgName
    public void update(String pkgName, String category, int status) {
        String where = FIELD_NAME_PACKAGE + "= ?" + " AND " + FIELD_NAME_CATEGORY + "= ?";
        String[] whereValue = new String[]{pkgName, category};
        ContentValues cv = new ContentValues();
        cv.put(FIELD_NAME_ISWHITELIST_STATUS, status);
        mDb.update(TABLE_WHITELIST, cv, where, whereValue);
    }

    public void delete(String pkgName) {
        String where = FIELD_NAME_PACKAGE + "= ?";
        String[] whereValue = new String[]{pkgName};
        mDb.delete(TABLE_WHITELIST, where, whereValue);
    }

    // update table according to the pkgName
    public void updateCategory(String pkgName, String category, int status) {
        String where = FIELD_NAME_PACKAGE + "= ?";
        String[] whereValue = new String[]{pkgName};
        ContentValues cv = new ContentValues();
        cv.put(FIELD_NAME_CATEGORY, category);
        cv.put(FIELD_NAME_ISWHITELIST_STATUS, status);
        mDb.update(TABLE_WHITELIST, cv, where, whereValue);
    }

    public long insert(String pkgName, String category, int status) {
        ContentValues cv = new ContentValues();
        cv.put(FIELD_NAME_PACKAGE, pkgName);
        cv.put(FIELD_NAME_CATEGORY, category);
        cv.put(FIELD_NAME_ISWHITELIST_STATUS, status);
        long row = mDb.insert(TABLE_WHITELIST, null, cv);
        return row;
    }

    public void initCacheData() {
        Cursor cursor = null;
        List<AppRecord> appRecordListByPkg = null;
        List<AppRecord> appRecordListByCategory = null;
        mPkgKeyCache.clear();
        mCategoryKeyCache.clear();
        mAppRecordsCache.clear();
        try {
            cursor = getCursor(TABLE_WHITELIST);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String packageName = cursor.getString(cursor.getColumnIndex
                            (FIELD_NAME_PACKAGE));
                    String categoryName = cursor.getString(cursor.getColumnIndex
                            (FIELD_NAME_CATEGORY));
                    String status = cursor.getString(cursor.getColumnIndex
                            (FIELD_NAME_ISWHITELIST_STATUS));
                    mAppRecordsCache.add(new AppRecord(packageName, AppShowedCategory.valueOf
                            (categoryName), Integer.valueOf(status)));
                    if (mPkgKeyCache.containsKey(packageName)) {
                        appRecordListByPkg = mPkgKeyCache.get(packageName);
                        appRecordListByPkg.add(new AppRecord(packageName, AppShowedCategory
                                .valueOf(categoryName), Integer.valueOf(status)));
                    } else {
                        appRecordListByPkg = new ArrayList<AppRecord>();
                        appRecordListByPkg.add(new AppRecord(packageName, AppShowedCategory
                                .valueOf(categoryName), Integer.valueOf(status)));
                        mPkgKeyCache.put(packageName, appRecordListByPkg);
                    }
                    if (mCategoryKeyCache.containsKey(categoryName)) {
                        appRecordListByCategory = mCategoryKeyCache.get(categoryName);
                        appRecordListByCategory.add(new AppRecord(packageName, AppShowedCategory
                                .valueOf(categoryName), Integer.valueOf(status)));
                    } else {
                        appRecordListByCategory = new ArrayList<AppRecord>();
                        appRecordListByCategory.add(new AppRecord(packageName, AppShowedCategory
                                .valueOf(categoryName), Integer.valueOf(status)));
                        mCategoryKeyCache.put(categoryName, appRecordListByCategory);
                    }
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }


    /**
     * Get the cursor of table
     *
     * @param tableName name of table
     * @return cursor of the table
     */
    public Cursor getCursor(String tableName) {
        Cursor cursor = mDb.query(tableName, null, null, null, null, null, null);
        return cursor;
    }

    public HashSet<String> queryPkgListFromDB() {
        HashSet<String> whiteList = new HashSet<String>();
        Cursor cursor = null;
        try {
            String[] columns = new String[]{FIELD_NAME_PACKAGE};
            cursor = mDb.query(TABLE_WHITELIST, columns, null,
                    null, null, null, null);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String packageName = cursor.getString(cursor
                            .getColumnIndex(FIELD_NAME_PACKAGE));
                    whiteList.add(packageName);
                }

            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return whiteList;
    }

    public int geCurrentStatus(String pkgName) {
        int status = 0;
        Cursor cursor = null;
        try {
            String[] columns = new String[]{FIELD_NAME_ISWHITELIST_STATUS};
            String where = FIELD_NAME_PACKAGE + "= ?";
            String[] whereValue = new String[]{pkgName};
            cursor = mDb.query(TABLE_WHITELIST, columns, where,
                    whereValue, null, null, null);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    status = cursor.getInt(cursor
                            .getColumnIndex(FIELD_NAME_ISWHITELIST_STATUS));
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return status;
    }
}