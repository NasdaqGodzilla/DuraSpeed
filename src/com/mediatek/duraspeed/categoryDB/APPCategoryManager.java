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
package com.mediatek.duraspeed.categoryDB;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.mediatek.duraspeed.R;
import com.mediatek.duraspeed.presenter.APPCategory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class APPCategoryManager {

    private static final String TAG = "APPCategoryManager";

    private APPCategoryDatabaseHelper dbHelper;
    private SQLiteDatabase db;
    public boolean isNeedUpgrade = false;
    public String DB_PATH = "";
    private Map<String, APPCategory> mDbCache = new HashMap<String, APPCategory>();

    public APPCategoryManager(Context context) {
        dbHelper = new APPCategoryDatabaseHelper(context);
        initDbCache();
    }

    private void initDbCache() {
        db = dbHelper.getWritableDatabase();
        upGradeAPPCategoryTableIfNeed(db);
        loadDataFromAppCategoryDb(db);
        loadDataFromAppCategoryInLocal(db);
    }

    private void loadDataFromAppCategoryInLocal(SQLiteDatabase db) {
        getCacheFromDb(db, APPCategoryDatabaseHelper.TABLE_APPCATEGORY_LOCAL);
    }

    private void loadDataFromAppCategoryDb(SQLiteDatabase db) {
        getCacheFromDb(db, APPCategoryDatabaseHelper.TABLE_APPCATEGORY);
    }

    private void getCacheFromDb(SQLiteDatabase db, String tableName) {
        Cursor cursor = db.query(tableName,
                new String[]{APPCategoryDatabaseHelper.PACKAGE_VOLUME_NAME,
                        APPCategoryDatabaseHelper.CATEGORY_VOLUME_NAME},
                null, null, null, null, null);
        int categoryType;
        String pkgname;
        try {
            if (cursor != null && cursor.moveToNext()) {
                do {
                    categoryType = cursor.getInt(cursor.getColumnIndex("category"));
                    pkgname = cursor.getString(cursor.getColumnIndex("packageName"));
                    mDbCache.put(pkgname, APPCategory.values()[categoryType]);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            cursor.close();
        }
    }

    public APPCategory queryAPPCategory(String packageName) {
        APPCategory category = mDbCache.get(packageName);
        if (category == null) {
            category = APPCategory.UNKNOWN;
        }
        return category;
    }

    private APPCategory queryAPPCategory(SQLiteDatabase db, String tableName, String packageName) {
        Cursor cursor = db.query(tableName,
                new String[]{APPCategoryDatabaseHelper.CATEGORY_VOLUME_NAME},
                APPCategoryDatabaseHelper.PACKAGE_VOLUME_NAME + "=?",
                new String[]{packageName}, null, null, null);
        int categoryType = 25;
        if (cursor != null && cursor.getCount() == 1 && cursor.moveToNext()) {
            categoryType = cursor.getInt(cursor.getColumnIndex("category"));
        }
        return APPCategory.values()[categoryType];
    }

    private void upGradeAPPCategoryTableIfNeed(SQLiteDatabase db) {
        if (isNeedUpgrade) {
            try {
                db.execSQL("delete from appCategory");
                String tempDBPath = DB_PATH + APPCategoryDatabaseHelper.DB_NAME_UPGRADE_TEMP;
                db.execSQL("attach database ? as newDB",
                        new String[]{tempDBPath});
                db.execSQL("insert into appCategory select * from newDB.appCategory");
                db.execSQL("detach database newDB");
                isNeedUpgrade = false;
                File tempDBFile = new File(tempDBPath);
                if (tempDBFile.exists()) {
                    tempDBFile.delete();
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw e;
            } finally {

            }
        }
    }

    //=========APPCategoryDatabaseHelper==========
    class APPCategoryDatabaseHelper extends SQLiteOpenHelper {
        public static final String DB_NAME_UPGRADE_TEMP = "app_category_temp.db";

        private static final int VERSION = 1;
        private static final String DB_NAME = "app_category.db";
        private static final String TABLE_APPCATEGORY = "appCategory";
        private static final String TABLE_APPCATEGORY_LOCAL = "appCategory_local";
        private static final String CATEGORY_VOLUME_NAME = "category";
        private static final String PACKAGE_VOLUME_NAME = "packageName";
        private Context mContext = null;

        public APPCategoryDatabaseHelper(Context context) {
            super(context, DB_NAME, null, VERSION);
            mContext = context;
            initDatabase();
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            try {
                String dbPath = db.getPath();
                DB_PATH = dbPath.substring(0, dbPath.lastIndexOf("/") + 1);
                copyDBFile(DB_PATH + DB_NAME_UPGRADE_TEMP);
                isNeedUpgrade = true;
            } catch (IOException e) {
                Log.e(TAG, e.toString());
            }
        }

        @Override
        public void onOpen(SQLiteDatabase db) {
            super.onOpen(db);
        }

        private void initDatabase() {
            try {
                copyDatabase();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void copyDatabase() throws IOException {
            boolean dbExist = checkDatabase();
            if (!dbExist) {
                copyDBFile(DB_PATH + DB_NAME);
            }
        }

        private boolean checkDatabase() {
            boolean flag = false;
            SQLiteDatabase db = this.getReadableDatabase();
            String dbPath = db.getPath();
            DB_PATH = dbPath.substring(0, dbPath.lastIndexOf("/") + 1);
            String myPath = DB_PATH + DB_NAME;
            if (new File(myPath).exists()) {
                //select tbl_name from sqlite_master where tbl_name="appCategory"
                Cursor cr = db.query("sqlite_master",
                        new String[]{"tbl_name"},
                        "tbl_name=?",
                        new String[]{"appCategory"},
                        null, null, null);
                if (cr != null && cr.getCount() > 0) {
                    flag = true;
                }

                if (cr != null) {
                    cr.close();
                }
            }
            db.close();
            return flag;
        }

        private void copyDBFile(String outFileName) throws IOException {
            InputStream inputStream = null;
            OutputStream outputStream = null;
            try {
                inputStream = mContext.getResources().openRawResource(R.raw.app_category);
                outputStream = new FileOutputStream(outFileName);
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }
                outputStream.flush();
                outputStream.close();
                inputStream.close();
            } finally {
                if (outputStream != null) {
                    outputStream.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
            }
        }
    }
}
