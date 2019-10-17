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

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.mediatek.duraspeed.R;
import com.mediatek.duraspeed.model.DatabaseManager;
import com.mediatek.duraspeed.presenter.AppShowedCategory;
import com.mediatek.duraspeed.presenter.BoosterContract;
import com.mediatek.duraspeed.presenter.BoosterPresenter;

import java.util.ArrayList;

public class DuraSpeedAppService extends IntentService {
    private static final String TAG = "DuraSpeedAppService";
    public static final String ACTION_START_DURASPEED_APP =
            "mediatek.intent.action.ACTION_START_DURASPEED_APP";
    protected static final String ACTION_PKG_UPDATE = "com.mediatek.action.ACTION_RB_PK_UPDATE";
    protected static final String ACTION_EXTRA_PKG_NAMES = "pkgnames";
    private BoosterContract.IPolicyPresenter mIPolicyPresenter;

    public DuraSpeedAppService() {
        super(DuraSpeedAppService.class.getSimpleName());
    }

    protected void onHandleIntent(Intent intent) {
        final String action = intent.getAction();
        if (!ViewUtils.ACTION_START_DURASPEED_APP_SERVICE.equals(action)) {
            return;
        }
        Bundle extras = intent.getExtras();
        if (extras != null) {
            Intent extraIntent = (Intent) extras.get(Intent.EXTRA_INTENT);
            String extraAction = extraIntent.getAction();
            Log.d(TAG, "onHandleIntent, action = " + extraAction);
            // init DB if necessary
            DatabaseManager dbManager = DatabaseManager.getInstance(this.getApplicationContext());
            if (ACTION_START_DURASPEED_APP.equals(extraAction) ||
                    Intent.ACTION_BOOT_COMPLETED.equals(extraAction)) {
                Context context = getApplicationContext();
                if (ViewUtils.getDuraSpeedStatus(context)) {
                    ViewUtils.setAppInitialStatus(context, ViewUtils.INITIAL_STATUS_STARTUP);
                    ViewUtils.showNotify(this);
                }
                mIPolicyPresenter = new BoosterPresenter(getApplicationContext(), null);
                mIPolicyPresenter.setAppWhitelist();
            } else if (Intent.ACTION_PACKAGE_ADDED.equals(extraAction)
                    || Intent.ACTION_PACKAGE_REMOVED.equals(extraAction)) {
                String pkgName = extraIntent.getData().getSchemeSpecificPart();
                if (Intent.ACTION_PACKAGE_ADDED.equals(extraAction)) {
                    // Add package info to DB and cache
                    if (dbManager.insert(pkgName)) {
                        // Set free policy when it need add into white list
                        if (ViewUtils.getDuraSpeedStatus(this.getApplicationContext())) {
                            mIPolicyPresenter = new BoosterPresenter(getApplicationContext(), null);
                            mIPolicyPresenter.setAppWhitelist();
                        }
                        notifyPackageUpdated(pkgName);
                        if (dbManager.getCategory(pkgName).equals(AppShowedCategory.OTHERS)) {
                            // Query category from Web
                            ArrayList<String> pkgList = new ArrayList<String>();
                            pkgList.add(pkgName);
                            QueryCategoryJobService.scheduleQueryCategoryTask(this
                                    .getApplicationContext(), pkgList);
                        }
                    }
                } else if (Intent.ACTION_PACKAGE_REMOVED.equals(extraAction)) {
                    // Remove package info from DB and cache and update UI if necessary
                    if (dbManager.delete(pkgName)) {
                        notifyPackageUpdated(pkgName);
                    }
                }
            } else {
                Log.e(TAG, "Unknown extra intent action = " + extraAction);
            }
        }

    }

    private void notifyPackageUpdated(String pkgName) {
        Intent intent = new Intent(ACTION_PKG_UPDATE);
        intent.putExtra(ACTION_EXTRA_PKG_NAMES, new String[]{pkgName});
        this.getApplicationContext().sendBroadcast(intent);
    }
}
