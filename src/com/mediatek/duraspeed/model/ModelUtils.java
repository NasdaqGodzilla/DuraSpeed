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
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import com.mediatek.duraspeed.R;
import com.mediatek.duraspeed.categoryDB.APPCategoryManager;
import com.mediatek.duraspeed.presenter.APPCategory;
import com.mediatek.duraspeed.presenter.AppRecord;
import com.mediatek.duraspeed.presenter.AppShowedCategory;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class ModelUtils {
    private static final String TAG = "ModelUtils";
    static APPCategoryManager sAppCategoryMgr;
    public static int CPU_COUNT = getNumCores();

    public static AppShowedCategory getAppShowedCategory(Context context, String pkgName) {
        AppShowedCategory showedType;
        if (sAppCategoryMgr == null) {
            sAppCategoryMgr = new APPCategoryManager(context);
        }
        APPCategory category = sAppCategoryMgr.queryAPPCategory(pkgName);
        showedType = convertAppCategory(category);
        return showedType;
    }

    /*@param type AppShowedCategory
     *@return the default status
     */
    public static int getDefaultStatus(AppShowedCategory type) {
        return AppRecord.STATUS_DISABLED;
    }

    public static int getDefaultStatus(Context context, AppShowedCategory type) {
        String[] specialAppList = context.getResources().getStringArray(R.array.whitelist_category);
        List<String> whiteListCategory = Arrays.asList(specialAppList);
        Log.d(TAG, "type.toString() " + type.toString());
        if (whiteListCategory.contains(type.toString())) {
            Log.d(TAG, "contained, enable");
            return AppRecord.STATUS_ENABLED;
        } else {
            return AppRecord.STATUS_DISABLED;
        }
    }

    /* get the origin type not UI show type
     because the type of apps will be reclassified
* */
    public static APPCategory getOrignalType(Context context, String pkgName) {
        if (sAppCategoryMgr == null) {
            sAppCategoryMgr = new APPCategoryManager(context);
        }
        APPCategory category = sAppCategoryMgr.queryAPPCategory(pkgName);
        return category;
    }

    /*@param type APPCategory , the category from the raw database or web
 *    @return the default status according to the original type
 */
    public static int getDefaultStatusByOrignalType(Context context, String pkgName, APPCategory
            type) {
        switch (type) {
            case COMMUNICATION:
                if (isSystemApp(context, pkgName)) {
                    Log.d(TAG, "join whitelist for " + pkgName + ", as it's system and " +
                            "communication");
                    return AppRecord.STATUS_ENABLED;
                } else {
                    return AppRecord.STATUS_DISABLED;
                }
            case SOCIAL:
                return AppRecord.STATUS_DISABLED;
            default:
                return AppRecord.STATUS_DISABLED;
        }
    }

    /* @param context Context
       @param pkgName String
       @return the app is system app or not
    * */
    private static boolean isSystemApp(Context context, String pkgName) {
        PackageManager pm = context.getPackageManager();
        try {
            ApplicationInfo applicationInfo = pm.getApplicationInfo(pkgName, 0);
            if ((applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0
                    || (applicationInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
                return true;
            }
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
        return false;
    }

    public static int getNumCores() {
        class CpuFilter implements FileFilter {
            @Override
            public boolean accept(File pathname) {
                //Check if filename is "cpu", followed by a single digit number
                if (Pattern.matches("cpu[0-9]", pathname.getName())) {
                    return true;
                }
                return false;
            }
        }

        try {
            //Get directory containing CPU info
            File dir = new File("/sys/devices/system/cpu/");
            //Filter to only list the devices we care about
            File[] files = dir.listFiles(new CpuFilter());
            Log.d(TAG, "CPU Count: " + files.length);
            //Return the number of cores (virtual CPU devices)
            return files.length;
        } catch (Exception e) {
            //Print exception
            Log.d(TAG, "CPU Count: Failed.");
            e.printStackTrace();
            //Default to return 1 core
            return 1;
        }
    }

    public static AppShowedCategory convertAppCategory(APPCategory category) {
        AppShowedCategory showedType;
        switch (category) {
            case COMMUNICATION: // communications
            case SOCIAL: // social
                showedType = AppShowedCategory.COMMUNICATION_SOCAIL;
                break;
            case ENTERTAINMENT: //Entertainment
            case MUSIC_AUDIO: //music_audio
            case MEDIA_VIDEO: //media_video
                showedType = AppShowedCategory.ENTERTAINMENT;
                break;
            case LIFESTYLE: // lifestyle
            case HEALTH_FITNESS: // health_fitness
            case MEDICAL:// medical
            case PERSONALIZATION: // personalization
            case PHOTOGRAPHY: // photography
            case SHOPPING: //shopping
            case SPORTS: //sports
            case TRAVEL_LOCAL: // travel & local
                showedType = AppShowedCategory.LIFESTYLE;
                break;
            case TOOLS: // tools
            case TRANSPORTATION: // transportation
            case WEATHER: // weather
            case LIBRARIES_DEMO: // libraries&Demo
            case BUSINESS: // business
            case FINANCE: // finance
            case PRODUCTIVITY: // productivity
                showedType = AppShowedCategory.TOOLS;
                break;
            case GAME: // game
                showedType = AppShowedCategory.GAME;
                break;
            case NEWS_MAGAZINES: // news&magazines
            case BOOKS_REFERENCE: // books&reference
            case EDUCATION: // education
            case COMICS: //comics
                showedType = AppShowedCategory.READING;
                break;
            case UNKNOWN:
                showedType = AppShowedCategory.OTHERS;
                break;
            default:
                showedType = AppShowedCategory.OTHERS;
                break;
        }
        return showedType;
    }
}
