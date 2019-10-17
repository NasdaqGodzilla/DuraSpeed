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

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.ServiceManager;
import android.util.Log;

import com.mediatek.duraspeed.manager.IDuraSpeedService;
import com.mediatek.duraspeed.R;
import com.mediatek.duraspeed.presenter.AppRecord;
import com.mediatek.duraspeed.presenter.AppShowedCategory;
import com.mediatek.duraspeed.view.QueryCategoryJobService;
import com.mediatek.duraspeed.view.ViewUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class DatabaseManager {
    private static final String TAG = "DatabaseManager";
    private static DatabaseManager sManager;
    private Context mContext;
    private DatabaseHelper mDataBaseHelper;
    private PackageManager mPm;
    private int mRetrieveFlags;
    private static Object sLock = new Object();

    private IDuraSpeedService mDuraSpeedService;

    private HashMap<String, List<AppRecord>> mPkgKeyCache;
    private HashMap<String, List<AppRecord>> mCategoryKeyCache;
    private ArrayList<AppRecord> mAppRecordsCache;
    private List<String> mPlatformWhitelist;
    private ArrayList<String> mInvisibleAppWhitelist;
    private List<String> mDefaultAppWhitelist;

    public static DatabaseManager getInstance(Context context) {
        if (sManager == null) {
            sManager = new DatabaseManager(context);
        }
        return sManager;
    }

    public DatabaseManager(Context context) {
        Log.d(TAG, "DatabaseManager init start");
        mContext = context.getApplicationContext();
        mPm = mContext.getPackageManager();
        mRetrieveFlags = PackageManager.GET_UNINSTALLED_PACKAGES |
                PackageManager.GET_DISABLED_COMPONENTS;
        mDuraSpeedService =
                IDuraSpeedService.Stub.asInterface(ServiceManager.getService("duraspeed"));
        mPlatformWhitelist = new ArrayList<String>();
        mInvisibleAppWhitelist = new ArrayList<String>();
        mDefaultAppWhitelist =
                Arrays.asList(context.getResources().getStringArray(R.array.app_whitelist));
        createDataBase(mContext);
        setDataIntoCache();
        updateDataBase(mContext);
        Log.d(TAG, "DatabaseManager init end");
    }

    public Context getAppContext() {
        return mContext;
    }

    private void createDataBase(Context context) {
        synchronized (sLock) {
            if (mDataBaseHelper == null) {
                Log.d(TAG, "new DatabaseHelper");
                mDataBaseHelper = new DatabaseHelper(context);
            }
        }
    }

    private void setDataIntoCache() {
        synchronized (sLock) {
            mDataBaseHelper.initCacheData();
            mPkgKeyCache = mDataBaseHelper.getPkgKeyCache();
            mCategoryKeyCache = mDataBaseHelper.getPermKeyCache();
            mAppRecordsCache = mDataBaseHelper.getAppRecordsCache();
        }
    }

    private boolean isSystemApp(ApplicationInfo appInfo) {
        boolean isSystem = false;
        if ((appInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0 ||
                (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
            isSystem = true;
        }
        return isSystem;
    }

    public void updateDataBase(Context context) {
        // get all the installed app list, exclude system white list and platform white list
        List<ApplicationInfo> originalAppList = mPm
                .getInstalledApplications(mRetrieveFlags);
        List<String> hideList = ViewUtils.arrayConvertToList(
                context.getResources().getStringArray(R.array.app_hidelist));
        for (String data : hideList) {
            Log.i(TAG, "hide list = " + data);
        }
        mInvisibleAppWhitelist.addAll(hideList);
        try {
            mPlatformWhitelist = mDuraSpeedService.getPlatformWhitelist();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Intent launchIntent = new Intent(Intent.ACTION_MAIN, null)
                .addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> intents = mPm.queryIntentActivities(
                launchIntent, PackageManager.GET_DISABLED_COMPONENTS);
        boolean insertData = false;
        synchronized (sLock) {
            for (ApplicationInfo appInfo : originalAppList) {
                String pkgName = appInfo.packageName;
                if (!isSystemApp(appInfo) && hasLauncherEntry(pkgName, intents)) {
                    if (mInvisibleAppWhitelist.contains(pkgName)) {
                        Log.d(TAG, "not insert whitelist package: " + pkgName);
                        continue;
                    }
                    if (!pkgExistInDB(pkgName)) {
                        if (insert(pkgName)) {
                            insertData = true;
                        }
                    }
                }
            }
            if (insertData) {
                Log.d(TAG, "insert DB when update Database");
                queryOtherAppsCategory();
            }
        }
    }

    private boolean hasLauncherEntry(String pkgName, List<ResolveInfo> intents) {
        for (int j = 0; j < intents.size(); j++) {
            if (pkgName != null && intents.get(j).activityInfo != null) {
                String intentPackageName = intents.get(j).activityInfo.packageName;
                if (pkgName.equals(intentPackageName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean pkgExistInDB(String pkgName) {
        // Need query from DB, but we cached DB data
        synchronized (sLock) {
            return mPkgKeyCache.containsKey(pkgName);
        }
    }

    public boolean isInvisibleWhitelist(String pkgName) {
        return (mInvisibleAppWhitelist.contains(pkgName) || mPlatformWhitelist.contains(pkgName));
    }

    /**
     * Get all categories names store in database.
     *
     * @return a String list includes all the categories in database
     */
    public HashSet<AppShowedCategory> getCategories() {
        HashSet<AppShowedCategory> categoryList = new HashSet<AppShowedCategory>();
        synchronized (sLock) {
            Set<String> keys = mCategoryKeyCache.keySet();
            for (String categoryName : keys) {
                categoryList.add(AppShowedCategory.valueOf(categoryName));
            }
        }
        return categoryList;
    }

    public ArrayList<AppRecord> getAppRecords() {
        synchronized (sLock) {
            return mAppRecordsCache;
        }
    }

    public ArrayList<String> getAppWhiteList() {
        ArrayList<String> whiteList = new ArrayList<String>();
        synchronized (sLock) {
            Iterator<AppRecord> ite = this.mAppRecordsCache.iterator();
            while (ite.hasNext()) {
                AppRecord i = ite.next();
                if (i.getStatus() == AppRecord.STATUS_ENABLED) {
                    whiteList.add(i.getPkgName());
                }
            }
        }
        whiteList.addAll(mInvisibleAppWhitelist);
        return whiteList;
    }

    public ArrayList<String> getBlackList() {
        ArrayList<String> blackList = new ArrayList<String>();
        synchronized (sLock) {
            Iterator<AppRecord> ite = this.mAppRecordsCache.iterator();
            while (ite.hasNext()) {
                AppRecord i = ite.next();
                if (i.getStatus() == AppRecord.STATUS_DISABLED) {
                    blackList.add(i.getPkgName());
                }
            }
        }
        return blackList;
    }

    public int getStatus(String pkgName) {
        synchronized (sLock) {
            List<AppRecord> appRecordList = mPkgKeyCache.get(pkgName);
            if (appRecordList != null) {
                for (AppRecord record : appRecordList) {
                    return record.getStatus();
                }
            }
            return AppRecord.STATUS_DISABLED;
        }
    }

    public AppShowedCategory getCategory(String pkgName) {
        synchronized (sLock) {
            List<AppRecord> appRecordList = mPkgKeyCache.get(pkgName);
            if (appRecordList != null) {
                for (AppRecord appRecord : appRecordList) {
                    return appRecord.getCategory();
                }
            }
            return AppShowedCategory.OTHERS;
        }
    }

    public boolean queryOtherAppsCategory() {
        boolean ret = false;
        ArrayList<String> otherPkgList = new ArrayList<String>();
        List<AppRecord> otherAppList = mCategoryKeyCache.get(AppShowedCategory.OTHERS.toString());
        if (otherAppList != null && otherAppList.size() > 0) {
            for (AppRecord record : otherAppList) {
                otherPkgList.add(record.getPkgName());
            }
            ret = QueryCategoryJobService.scheduleQueryCategoryTask(getAppContext(), otherPkgList);
            Log.d(TAG, "query other apps category list size: " + otherPkgList.size() + " " +
                    "sche? " + ret);
        }
        return ret;
    }

    /**
     * Get a list category record contain the specific category name.
     *
     * @param category the category name of some apps
     * @return the list of AppRecord
     */
    public ArrayList<AppRecord> getAppRecordListByCategory(String category) {
        synchronized (sLock) {
            List<AppRecord> categoryRecordList = mCategoryKeyCache.get(category);
            if (categoryRecordList != null) {
                return new ArrayList<AppRecord>(categoryRecordList);
            } else {
                return null;
            }
        }
    }

    /**
     * Modify the status of package. Since it write db, call it in.
     * @param pkgName package name
     * @param category category
     * @param status status
     * @return
     */
    public boolean modify(String pkgName, String category, int status) {
        synchronized (sLock) {
            Log.d(TAG, "modify package: " + pkgName + ", category: " + category +
                    ", to state: " + status);
            modifyCache(pkgName, category, status);
            mDataBaseHelper.update(pkgName, category, status);
        }
        return true;
    }


    public boolean modifyCategory(String pkgName, String category) {
        synchronized (sLock) {
            AppRecord record = null;
            for (int i = 0; i < mAppRecordsCache.size(); i++) {
                record = mAppRecordsCache.get(i);
                if (record.getPkgName().equals(pkgName)) {
                    break;
                }
            }
            if (record != null) {
                if (!record.getCategory().equals(category)) {
                    int status = record.getStatus();
                    if (status == AppRecord.STATUS_DISABLED) {
                        // Check default status
                        status = ModelUtils.getDefaultStatus(AppShowedCategory.valueOf(category));
                    }
                    modifyCacheCategory(pkgName, record.getCategory().toString(), category, status);
                    Log.d(TAG, "modify package name: " + pkgName + ", to category: " + category +
                            ", state: " + status);
                    mDataBaseHelper.updateCategory(pkgName, category, status);
                    return true;
                }
            } else {
                Log.d(TAG, "can't find pkg: " + pkgName);
            }
            return false;
        }
    }

    public boolean delete(String pkgName) {
        if (isInvisibleWhitelist(pkgName)) {
            return false;
        }
        synchronized (sLock) {
            Log.d(TAG, "delete package: " + pkgName);
            deleteCache(pkgName, this.getCategory(pkgName).toString());
            mDataBaseHelper.delete(pkgName);
        }
        return true;
    }

    public boolean insert(String pkgName) {
        if (isInvisibleWhitelist(pkgName)) {
            return false;
        }
        synchronized (sLock) {
            Context context = getAppContext();
            int defaultStatus;
            AppShowedCategory category = ModelUtils.getAppShowedCategory(context, pkgName);
            defaultStatus = ModelUtils.getDefaultStatus(context, category);
            if (mDefaultAppWhitelist.contains(pkgName)) {
                defaultStatus = AppRecord.STATUS_ENABLED;
            }
            if (pkgExistInDB(pkgName)) {
                Log.d(TAG, "insert pkg but exists: " + pkgName + ", only update info");
                return modify(pkgName, category.toString(), defaultStatus);
            }
            Log.d(TAG, "insert package: " + pkgName + " with category: " + category +
                    ", state: " + defaultStatus);
            insertCache(pkgName, category.toString(), defaultStatus);
            long ret = mDataBaseHelper.insert(pkgName, category.toString(),
                    defaultStatus);
        }
        return true;
    }

    private void insertCache(String pkgName, String category, int defaultStatus) {
        ArrayList<AppRecord> arrayList = new ArrayList<AppRecord>();
        arrayList.add(new AppRecord(pkgName, AppShowedCategory.valueOf(category),
                defaultStatus));
        mPkgKeyCache.put(pkgName, arrayList);
        mAppRecordsCache.add(new AppRecord(pkgName, AppShowedCategory.valueOf(category),
                defaultStatus));
        List<AppRecord> valueList = mCategoryKeyCache.get(category);
        if (valueList != null) {
            valueList.add(new AppRecord(pkgName, AppShowedCategory.valueOf(category),
                    defaultStatus));
        } else {
            valueList = new ArrayList<AppRecord>();
            valueList.add(new AppRecord(pkgName, AppShowedCategory.valueOf(category),
                    defaultStatus));
            mCategoryKeyCache.put(category, valueList);
        }
    }

    private void deleteCache(String pkgName, String category) {
        mPkgKeyCache.remove(pkgName);
        List<AppRecord> valueList = mCategoryKeyCache.get(category);
        if (valueList == null) {
            Log.w(TAG, "app list is null for categary = " + category);
            return;
        }
        ArrayList<Integer> matchedIndex = new ArrayList<Integer>();
        for (int i = 0; i < mAppRecordsCache.size(); i++) {
            if (mAppRecordsCache.get(i).getPkgName().equals(pkgName)) {
                matchedIndex.add(i);
            }
        }
        if (matchedIndex.size() > 0) {
            for (int i = matchedIndex.size() - 1; i >= 0; i--) {
                mAppRecordsCache.remove((int) matchedIndex.get(i));
            }
        }
        matchedIndex.clear();
        for (int i = 0; i < valueList.size(); i++) {
            if (valueList.get(i).getPkgName().equals(pkgName)) {
                matchedIndex.add(i);
            }
        }
        if (matchedIndex.size() > 0) {
            for (int i = matchedIndex.size() - 1; i >= 0; i--) {
                // Parameter is index, not object
                valueList.remove((int) matchedIndex.get(i));
            }
            if (valueList.size() == 0) {
                mCategoryKeyCache.remove(category);
                Log.d(TAG, "remove from category category = " + category);
            }
        } else {
            Log.d(TAG, "Warning: delete not find pkg: " + pkgName + " in category: " +
                    category);
        }
    }

    private void modifyCache(String pkgName, String category, int status) {
        List<AppRecord> appRecordList = mPkgKeyCache.get(pkgName);
        if (appRecordList != null) {
            for (AppRecord record : appRecordList) {
                if (record.getCategory().toString().equals(category)) {
                    record.setStatus(status);
                }
            }
        } else {
            Log.e(TAG, "Something not right need to check for = " + pkgName);
        }
        for (AppRecord appRecord : mAppRecordsCache) {
            if (appRecord.getPkgName().equals(pkgName)) {
                appRecord.setStatus(status);
            }
        }
        appRecordList = mCategoryKeyCache.get(category);
        if (appRecordList != null) {
            for (AppRecord record : appRecordList) {
                if (record.getPkgName().equals(pkgName)) {
                    record.setStatus(status);
                }
            }
        } else {
            Log.e(TAG, "Something not right need to check for = " + pkgName);
        }
    }

    private void modifyCacheCategory(String pkgName, String oldCate, String category, int status) {
        for (AppRecord appRecord : mAppRecordsCache) {
            if (appRecord.getPkgName().equals(pkgName)) {
                appRecord.setCategory(AppShowedCategory.valueOf(category));
                appRecord.setStatus(status);
            }
        }
        List<AppRecord> appRecordList = mPkgKeyCache.get(pkgName);
        if (appRecordList != null) {
            for (AppRecord record : appRecordList) {
                record.setCategory(AppShowedCategory.valueOf(category));
                record.setStatus(status);
            }
        } else {
            Log.e(TAG, "Something not right need to check for = " + pkgName);
        }
        // remove from old category
        appRecordList = mCategoryKeyCache.get(oldCate);
        ArrayList<Integer> matchedIndex = new ArrayList<Integer>();
        if (appRecordList != null) {
            for (int i = 0; i < appRecordList.size(); i++) {
                if (appRecordList.get(i).getPkgName().equals(pkgName)) {
                    matchedIndex.add(i);
                }
            }
            for (int i = matchedIndex.size() - 1; i >= 0; i--) {
                // Parameter is index, not object
                appRecordList.remove((int) matchedIndex.get(i));
            }
            if (appRecordList.size() == 0) {
                mCategoryKeyCache.remove(oldCate);
                Log.d(TAG, "remove from category category = " + oldCate);
            }
        } else {
            Log.e(TAG, "Something not right need to check for = " + pkgName);
        }
        // Add to new category
        appRecordList = mCategoryKeyCache.get(category);
        if (appRecordList != null) {
            appRecordList.add(new AppRecord(pkgName, AppShowedCategory.valueOf(category), status));
        } else {
            appRecordList = new ArrayList<AppRecord>();
            appRecordList.add(new AppRecord(pkgName, AppShowedCategory.valueOf(category), status));
            mCategoryKeyCache.put(category, appRecordList);
        }
    }
}
