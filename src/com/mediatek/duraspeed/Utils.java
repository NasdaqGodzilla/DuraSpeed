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
package com.mediatek.duraspeed;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Log;

import com.mediatek.duraspeed.manager.IDuraSpeedService;

import java.util.List;

public final class Utils {
    private static final String TAG = "Utils";
    private static final String SETTING_DURASPEED_ENABLED = "setting.duraspeed.enabled";
    private static final int sDuraSpeedDefaultStatus =
            SystemProperties.getInt("persist.vendor.duraspeed.app.on", 0);
    public static final boolean sLowRamDevice =
            SystemProperties.getBoolean("ro.config.low_ram", false);
    public static boolean sStarted = false;

    private static PackageManager sPackageManager;
    private static IDuraSpeedService sDuraSpeedManager;
    public static DatabaseManager sDatabaseManager;

    public static void setDuraSpeedStatus(Context context, boolean isEnable) {
        Settings.System.putInt(context.getContentResolver(),
                SETTING_DURASPEED_ENABLED, isEnable ? 1 : 0);
        Settings.Global.putInt(context.getContentResolver(),
                SETTING_DURASPEED_ENABLED, isEnable ? 1 : 0);
    }

    public static boolean getDuraSpeedStatus(Context context) {
        int value = Settings.System.getInt(context.getContentResolver(),
                SETTING_DURASPEED_ENABLED, sDuraSpeedDefaultStatus);
        return value == 1;
    }

    public static PackageManager getPackageManager(Context context) {
        if (sPackageManager == null) {
            sPackageManager = context.getPackageManager();
        }
        return sPackageManager;
    }

    public static boolean isSystemApp(ApplicationInfo appInfo) {
        boolean isSystem = false;
        if ((appInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0 ||
                (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
            isSystem = true;
        }
        return isSystem;
    }

    public static boolean hasLauncherEntry(String pkgName, List<ResolveInfo> intents) {
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

    public static String getAppLabel(Context context, String pkg) {
        PackageManager pm = getPackageManager(context);
        int retrieveFlags = PackageManager.GET_DISABLED_COMPONENTS;
        ApplicationInfo appInfo;
        try {
            appInfo = pm.getApplicationInfo(pkg, retrieveFlags);
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "ApplicationInfo cannot be found for pkg:" + pkg, e);
            return "";
        }
        return pm.getApplicationLabel(appInfo).toString();
    }

    public static Drawable getAppDrawable(Context context, String pkg) {
        PackageManager pm = getPackageManager(context);
        int retrieveFlags = PackageManager.GET_DISABLED_COMPONENTS;
        ApplicationInfo appInfo;
        try {
            appInfo = pm.getApplicationInfo(pkg, retrieveFlags);
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "ApplicationInfo cannot be found for pkg:" + pkg, e);
            return null;
        }
        return pm.getApplicationIcon(appInfo);
    }

    public static IDuraSpeedService getDuraSpeedManager() {
        if (sDuraSpeedManager == null) {
            sDuraSpeedManager = IDuraSpeedService.Stub.asInterface(
                        ServiceManager.getService("duraspeed"));
        }
        return sDuraSpeedManager;
    }

    public static void setAppWhitelist(List<String> list) {
        IDuraSpeedService manager = getDuraSpeedManager();
        try {
            manager.setAppWhitelist(list);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void createDatabaseManager(Context context) {
        if (sDatabaseManager == null ) {
            sDatabaseManager = DatabaseManager.getInstance(context);
        }
    }
}
