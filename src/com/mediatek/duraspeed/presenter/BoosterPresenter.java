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
package com.mediatek.duraspeed.presenter;

import android.content.Context;
import android.content.res.Resources;
import android.os.ServiceManager;
import android.util.Log;

import com.mediatek.duraspeed.manager.IDuraSpeedService;
import com.mediatek.duraspeed.model.DatabaseManager;
import com.mediatek.duraspeed.view.ViewUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class BoosterPresenter implements BoosterContract.IPolicyPresenter,
        BoosterContract.IViewPresenter {
    private static final String TAG = "BoosterPresenter";
    private static final boolean DEBUG = true;
    private Context mContext;
    private BoosterContract.View mViewTask;
    private Resources mRes;
    private IDuraSpeedService mDuraSpeedService;
    private static String sPkgName = null;

    public BoosterPresenter(Context context, BoosterContract.View view) {
        mContext = context;
        sPkgName = context.getPackageName();
        mViewTask = view;
        mRes = context.getResources();
        mDuraSpeedService =
                IDuraSpeedService.Stub.asInterface(ServiceManager.getService("duraspeed"));
    }

    public void setAppWhitelist() {
        List<String> appWhitelist = DatabaseManager.getInstance(mContext).getAppWhiteList();
        try {
            mDuraSpeedService.setAppWhitelist(appWhitelist);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public ArrayList<AppRecord> getControlAppList() {
        return DatabaseManager.getInstance(mContext).getAppRecords();
    }

    @Override
    public void startAppList(boolean checked) {
        DatabaseManager.getInstance(mContext).updateDataBase(mContext);
        setAppWhitelist();
        // View task to do
        if (checked) {
            ArrayList<AppRecord> list = getControlAppList();
            mViewTask.showControlAppList(list);
        } else {
            mViewTask.showEmptyView();
        }
    }

    @Override
    public void restartAppList(String category) {
        ArrayList<AppRecord> appRecordList = getAppListOfCategory(category);
        mViewTask.showControlAppList(appRecordList);
    }

    @Override
    public ArrayList<AppRecord> getAppListOfCategory(String type) {
        // get info from DB cache
        return DatabaseManager.getInstance(mContext).getAppRecordListByCategory(type);
    }

    @Override
    public void startShowDisclaimerDlg() {
        mViewTask.showDisclaimerDlg();
    }

    @Override
    public boolean isInAppWhitelist(String pkgName) {
        int status = DatabaseManager.getInstance(mContext).getStatus(pkgName);
        return status == AppRecord.STATUS_ENABLED;
    }

    @Override
    public void setInAppWhitelist(AppRecord appRecord, int status) {
        DatabaseManager.getInstance(mContext).modify(appRecord.getPkgName(), appRecord
                .getCategory().toString(), status);
        setAppWhitelist();
    }


    @Override
    public void addAllToAppWhitelist(List<AppRecord> toAddAppList) {
        for (AppRecord app : toAddAppList) {
            if (app.getStatus() != AppRecord.STATUS_ENABLED) {
                app.setStatus(AppRecord.STATUS_ENABLED);
                DatabaseManager.getInstance(mContext).modify(app.getPkgName(), app.getCategory()
                                .toString(),
                        AppRecord.STATUS_ENABLED);
            }
        }
        setAppWhitelist();
        mViewTask.updateAllAppStatus(true);
    }

    @Override
    public void removeAllFromAppWhitelist(List<AppRecord> toRemoveAppList) {
        for (AppRecord app : toRemoveAppList) {
            if (app.getStatus() != AppRecord.STATUS_DISABLED) {
                app.setStatus(AppRecord.STATUS_DISABLED);
                DatabaseManager.getInstance(mContext).modify(app.getPkgName(), app.getCategory()
                                .toString(),
                        AppRecord.STATUS_DISABLED);
            }
        }
        setAppWhitelist();
        mViewTask.updateAllAppStatus(false);
    }

    @Override
    public HashSet<AppShowedCategory> getAllAppCategory() {
        return DatabaseManager.getInstance(mContext).getCategories();
    }

    @Override
    public void startCategoryList(boolean checked) {
        // firstly setPolicy to FWK
        setAppWhitelist();
        if (checked) {
            HashSet<AppShowedCategory> categoryList = getAllAppCategory();
            mViewTask.showCategoryList(categoryList);
        } else {
            mViewTask.showEmptyView();
        }
    }
}

