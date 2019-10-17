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
package com.mediatek.duraspeed.view;

import android.app.Fragment;
import android.app.Notification;
import android.app.Notification.Action.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Log;

import com.mediatek.duraspeed.presenter.APPCategory;

import static com.mediatek.duraspeed.presenter.APPCategory.COMMUNICATION;
import static com.mediatek.duraspeed.presenter.APPCategory.ENTERTAINMENT;
import static com.mediatek.duraspeed.presenter.APPCategory.GAME;
import static com.mediatek.duraspeed.presenter.APPCategory.LIFESTYLE;
import static com.mediatek.duraspeed.presenter.APPCategory.TOOLS;
import static com.mediatek.duraspeed.presenter.APPCategory.UNKNOWN;

import com.mediatek.duraspeed.R;
import com.mediatek.duraspeed.model.DatabaseManager;
import com.mediatek.duraspeed.presenter.AppRecord;
import com.mediatek.duraspeed.presenter.AppShowedCategory;
import com.mediatek.duraspeed.presenter.BoosterContract;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

public final class ViewUtils {
    private static final String TAG = "ViewUtils";
    public static final String SETTING_DURASPEED_ENABLED = "setting.duraspeed.enabled";
    public static final int DURASPEED_DEFAULT_VALUE =
            SystemProperties.getInt("persist.vendor.duraspeed.app.on", 0);
    public static final String ACTION_START_DURASPEED_APP_SERVICE = "start_appservice";
    public static final String KEY_FROM_CATEGORY = "from_category";
    private static final String SHARED_PREFERENCE_DS = "DSSharedPreference";
    private static final String SHARED_PREFERENCE_KEY_STATUS = "status";
    private static final String SHARED_PREFERENCE_KEY_INITIAL = "initial";
    private static final String SHARED_PREFERENCE_KEY_REMIND = "remind";
    private static final String SHARED_PREFERENCE_KEY_DISCLAIMER = "disclaimer";

    public static final int INITIAL_STATUS_NOT = 0;
    public static final int INITIAL_STATUS_STARTUP = 1;
    public static final int FUNCTION_STATE_OFF = 0;
    public static final int FUNCTION_STATE_ON = 1;
    public static final int FUNCTION_STATE_DISABLE = 2;

    private static final String NOTIFICATION_TAG = "DuraSpeedNotification";
    private static final int NOTIFICATION_ID = 1;

    public static void setDuraSpeedStatus(Context context, boolean isEnable) {
        Settings.System.putInt(context.getContentResolver(),
                SETTING_DURASPEED_ENABLED, isEnable ? 1 : 0);
        Settings.Global.putInt(context.getContentResolver(),
                SETTING_DURASPEED_ENABLED, isEnable ? 1 : 0);
    }

    public static boolean getDuraSpeedStatus(Context context) {
        int value = Settings.System.getInt(context.getContentResolver(),
                SETTING_DURASPEED_ENABLED, DURASPEED_DEFAULT_VALUE);
        return value == 1;
    }

    public static int getStatus(Context context) {
        SharedPreferences sharedPrf = context.getSharedPreferences(
                SHARED_PREFERENCE_DS, Context.MODE_PRIVATE);
        int value = sharedPrf.getInt(SHARED_PREFERENCE_KEY_STATUS, context
                .getResources().getInteger(R.integer.feature_default_status));
        Log.d(TAG, "getStatus, value = " + value);
        return value;
    }

    public static void setRemindStatus(Context context, boolean isRemindAgain) {
        SharedPreferences sharedPrf = context.getSharedPreferences(
                SHARED_PREFERENCE_DS, Context.MODE_PRIVATE);
        Editor editor = sharedPrf.edit();
        int value = isRemindAgain ? 1 : 0;
        editor.putInt(SHARED_PREFERENCE_KEY_REMIND, value);
        editor.commit();
    }

    public static boolean getRemindStatus(Context context) {
        SharedPreferences sharedPrf = context.getSharedPreferences(
                SHARED_PREFERENCE_DS, Context.MODE_PRIVATE);
        int value = sharedPrf.getInt(SHARED_PREFERENCE_KEY_REMIND, context
                .getResources().getInteger(R.integer.feature_default_remind));
        return value == 1;
    }


    public static void registerSharedPreferenceListener(Context context, SharedPreferences
            .OnSharedPreferenceChangeListener listener) {
        SharedPreferences sharedPref = context.getSharedPreferences(SHARED_PREFERENCE_DS, Context
                .MODE_PRIVATE);
        sharedPref.registerOnSharedPreferenceChangeListener(listener);
    }

    public static void unregisterSharedPreferenceListener(Context context, SharedPreferences
            .OnSharedPreferenceChangeListener listener) {
        SharedPreferences sharedPref = context.getSharedPreferences(SHARED_PREFERENCE_DS, Context
                .MODE_PRIVATE);
        sharedPref.unregisterOnSharedPreferenceChangeListener(listener);
    }

    public static void setAppInitialStatus(Context context, int initialStatus) {
        SharedPreferences sharedPrf = context.getSharedPreferences(
                SHARED_PREFERENCE_DS, Context.MODE_PRIVATE);
        if (sharedPrf.getInt(SHARED_PREFERENCE_KEY_INITIAL, INITIAL_STATUS_NOT) != initialStatus) {
            Editor editor = sharedPrf.edit();
            editor.putInt(SHARED_PREFERENCE_KEY_INITIAL, initialStatus);
            editor.commit();
        }
    }

    public static boolean isAppStartup(Context context) {
        SharedPreferences sharedPrf = context.getSharedPreferences(
                SHARED_PREFERENCE_DS, Context.MODE_PRIVATE);
        int value = sharedPrf.getInt(SHARED_PREFERENCE_KEY_INITIAL, INITIAL_STATUS_NOT);
        return value == INITIAL_STATUS_STARTUP;
    }

    public static boolean isWebQueryEnable(Context context) {
        boolean isEnable = context.getResources().getBoolean(R.bool.enable_web_query);
        Log.d(TAG, "isWebQueryEnable, enable = " + isEnable);
        return isEnable;
    }

    public static int getFunctionState(Context context) {
        int state = FUNCTION_STATE_DISABLE;
        state = getDuraSpeedStatus(context) ? FUNCTION_STATE_ON : FUNCTION_STATE_OFF;
        return state;
    }

    /**
     * @param configStr String from config.xml, such as pkg1/pkg2/..
     * @return
     */
    public static HashSet<String> getConfigSet(String configStr) {
        HashSet<String> hs = new HashSet<String>();
        String[] strArray = configStr.trim().split("/");
        for (String str1 : strArray) {
            hs.add(str1);
        }
        return hs;
    }

    public static HashSet<String> arrayConvertToSet(String[] array) {
        if (array == null) {
            return null;
        }
        HashSet<String> set = new HashSet<String>();
        for (String str : array) {
            set.add(str);
        }
        return set;
    }

    public static ArrayList<String> arrayConvertToList(String[] array) {
        if (array == null) {
            return null;
        }
        return new ArrayList<String>(Arrays.asList(array));
    }

    public static ArrayList<String> setConvertToList(HashSet<String> set) {
        ArrayList<String> list = new ArrayList<String>();
        Iterator<String> it = set.iterator();
        while (it.hasNext()) {
            list.add(it.next());
        }
        return list;
    }

    public static String getCategoryName(AppShowedCategory category, Resources res) {
        String categoryName;
        switch (category) {
            case COMMUNICATION_SOCAIL:
                categoryName = res.getString(R.string.comm_category);
                break;
            case ENTERTAINMENT:
                categoryName = res.getString(R.string.entertain_category);
                break;
            case LIFESTYLE:
                categoryName = res.getString(R.string.life_category);
                break;
            case TOOLS:
                categoryName = res.getString(R.string.tools_category);
                break;
            case GAME:
                categoryName = res.getString(R.string.game_category);
                break;
            case READING:
                categoryName = res.getString(R.string.read_category);
                break;
            default:
                categoryName = res.getString(R.string.other_category);
                break;
        }
        return categoryName;
    }

    public static int getCategoryIcon(AppShowedCategory category) {
        int categoryIcon;
        switch (category) {
            case COMMUNICATION_SOCAIL:
                categoryIcon = R.drawable.ic_category_communications;
                break;
            case ENTERTAINMENT:
                categoryIcon = R.drawable.ic_category_entertainment;
                break;
            case LIFESTYLE:
                categoryIcon = R.drawable.ic_category_lifestyle;
                break;
            case TOOLS:
                categoryIcon = R.drawable.ic_category_tools;
                break;
            case GAME:
                categoryIcon = R.drawable.ic_category_game;
                break;
            case READING:
                categoryIcon = R.drawable.ic_category_reading;
                break;
            default:
                categoryIcon = R.drawable.ic_category_others;
                break;
        }
        return categoryIcon;
    }

    public static String getAppLabel(Context context, String pkg) {
        PackageManager pm = context.getPackageManager();
        int retrieveFlags = PackageManager.GET_DISABLED_COMPONENTS;
        ApplicationInfo appInfo;
        try {
            appInfo = pm.getApplicationInfo(pkg, retrieveFlags);
        } catch (NameNotFoundException ex) {
            Log.w(TAG, "ApplicationInfo cannot be found for pkg:" + pkg, ex);
            return "";
        }
        return pm.getApplicationLabel(appInfo).toString();
    }

    public static Drawable getAppDrawable(Context context, String pkg) {
        PackageManager pm = context.getPackageManager();
        int retrieveFlags = PackageManager.GET_DISABLED_COMPONENTS;
        ApplicationInfo appInfo;
        try {
            appInfo = pm.getApplicationInfo(pkg, retrieveFlags);
        } catch (NameNotFoundException ex) {
            Log.w(TAG, "ApplicationInfo cannot be found for pkg:" + pkg, ex);
            return null;
        }
        return pm.getApplicationIcon(appInfo);
    }

    public static void showNotify(Context context) {
        boolean isRemindAgain = ViewUtils.getRemindStatus(context);
        Log.d(TAG, "show notification ? = " + isRemindAgain);
        if (isRemindAgain) {
            Notification.Builder builder = new Notification.Builder(context);
            builder.setStyle(new Notification.BigTextStyle()
                    .bigText(context.getString(R.string.notification_content)));
            builder.setSmallIcon(R.drawable.ic_settings_rb);
            builder.setContentTitle(context.getString(R.string.notification_title));
            builder.setContentText(context.getString(R.string.notification_content));
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, new Intent(
                    context, NotRemindReceiver.class), 0);
            builder.addAction(new Builder(R.drawable.ic_settings_rb,
                    context.getString(R.string.notification_btn), pendingIntent).build());
            // set click action, enter to the main UI
            Intent enterIntent = new Intent("com.mediatek.duraspeed.APP_ENTRANCE");
            enterIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent enterPendIntent = PendingIntent.getActivity(context, 0, enterIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            builder.setContentIntent(enterPendIntent);
            Notification notification = builder.build();
            getNotificationMgr(context).notify(NOTIFICATION_TAG, NOTIFICATION_ID, notification);
        } else {
            Log.d(TAG, "User has clicked the btn: Get it and do not remind again");
        }
    }

    public static void cancelNotify(Context context) {
        getNotificationMgr(context).cancel(NOTIFICATION_TAG, NOTIFICATION_ID);
    }

    private static NotificationManager getNotificationMgr(Context context) {
        return (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public static Map<String, APPCategory> getAppCategoryMap(ArrayList<AppRecord> appList) {
        Map<String, APPCategory> appCategoryInfo = new HashMap<String, APPCategory>();
        for (AppRecord item : appList) {
            String pkgName = item.getPkgName();
            APPCategory category = convertCategory(item.getCategory());
            appCategoryInfo.put(pkgName, category);
        }
        return appCategoryInfo;
    }

    private static APPCategory convertCategory(AppShowedCategory category) {
        APPCategory appCategory;
        switch (category) {
            case COMMUNICATION_SOCAIL:
                appCategory = COMMUNICATION;
                break;
            case ENTERTAINMENT:
                appCategory = ENTERTAINMENT;
                break;
            case LIFESTYLE:
                appCategory = LIFESTYLE;
                break;
            case TOOLS:
                appCategory = TOOLS;
                break;
            case GAME:
                appCategory = GAME;
                break;
            default:
                appCategory = UNKNOWN;
                break;
        }
        return appCategory;
    }

    public static class NotRemindReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive, don't remind again");
            ViewUtils.setRemindStatus(context, false);
            cancelNotify(context);
        }
    }

    public static void setDisclaimerStatus(Context context, boolean showDisclaimer) {
        SharedPreferences sharedPrf = context.getSharedPreferences(
                SHARED_PREFERENCE_DS, Context.MODE_PRIVATE);
        Editor editor = sharedPrf.edit();
        int value = showDisclaimer ? 1 : 0;
        editor.putInt(SHARED_PREFERENCE_KEY_DISCLAIMER, value);
        editor.commit();
    }

    public static boolean getDisclaimerStatus(Context context) {
        SharedPreferences sharedPrf = context.getSharedPreferences(
                SHARED_PREFERENCE_DS, Context.MODE_PRIVATE);
        int value = sharedPrf.getInt(SHARED_PREFERENCE_KEY_DISCLAIMER, context
                .getResources().getInteger(R.integer.feature_default_disclaimer));
        return value == 1;
    }

    /* get the version code for pkg
    * */
    public static String getAppVersionName(Context context, String pkg) {
        PackageManager pm = context.getPackageManager();
        PackageInfo packageInfo;
        try {
            packageInfo = pm.getPackageInfo(pkg, PackageManager.GET_CONFIGURATIONS);
        } catch (NameNotFoundException ex) {
            Log.w(TAG,
                    "PackageInfo cannot be found for pkg:" + pkg, ex);
            return null;
        }
        return packageInfo.versionName;
    }


    /* update the follow after the action bar switch changed
    * */
    public static void updateSwitchChangeUI(Context context, boolean isChecked, Fragment
            fragment, BoosterContract.IViewPresenter viewPresenter) {
        if (isChecked) {
            //show Notification
            ViewUtils.showNotify(context);
        } else {
            ViewUtils.cancelNotify(context);
        }
        // put the status to shared preference
        ViewUtils.setDuraSpeedStatus(context, isChecked);
        if (fragment instanceof WhiteListFragment) {
            // UI start to load whitelist
            viewPresenter.startAppList(isChecked);
        } else if (fragment instanceof CategoryFragment) {
            viewPresenter.startCategoryList(isChecked);
        }
    }

    /* the normal logic follow when activity created if there is no disclaimer dialog
    * */
    public static void initNormalCreateFollow(Context context) {
        Log.d(TAG, "initNormalCreateFollow()");
        // Web query only enabled after app start
        // or user click "OK" on Disclaimer dialog
        if (!ViewUtils.isAppStartup(context) || ViewUtils.getDisclaimerStatus(context)) {
            ViewUtils.setAppInitialStatus(context, ViewUtils.INITIAL_STATUS_STARTUP);
            DatabaseManager.getInstance(context).queryOtherAppsCategory();
        }
    }
}