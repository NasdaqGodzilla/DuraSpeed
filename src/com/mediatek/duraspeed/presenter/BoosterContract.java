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

import android.content.Intent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public interface BoosterContract {
    interface View {
        // show apps list
        void showControlAppList(ArrayList<AppRecord> appList);

        void showEmptyView();

        // update all app list status
        void updateAllAppStatus(boolean status);

        // show all the controlled installed apps categories
        void showCategoryList(HashSet<AppShowedCategory> categoryList);

        // refresh data
        void refreshData();

        // show disclaimer dialog
        void showDisclaimerDlg();
    }

    interface IViewPresenter {
        // get UI showed app list
        ArrayList<AppRecord> getControlAppList();

        // start to load app list
        void startAppList(boolean checked);

        // restart the app list as switch from category list
        void restartAppList(String category);

        // get category list of all UI controlled installed apps
        HashSet<AppShowedCategory> getAllAppCategory();

        // start to load category list
        void startCategoryList(boolean checked);

        // this app's status: is in current AppWhitelist or not
        boolean isInAppWhitelist(String pkg);

        // set the app in AppWhitelist
        void setInAppWhitelist(AppRecord appRecord, int status);

        // turn all on, put all apps to AppWhitelist
        void addAllToAppWhitelist(List<AppRecord> toAddAppList);

        // turn all off, remove all apps from AppWhitelist
        void removeAllFromAppWhitelist(List<AppRecord> toRemoveAppList);

        /*
         * get all the apps list belong to this type
         */
        ArrayList<AppRecord> getAppListOfCategory(String type);

        // start do show disclaimer dialog
        void startShowDisclaimerDlg();
    }

    interface IPolicyPresenter {
        void setAppWhitelist();
    }

    interface IPackageUpdate {
        void onPackageUpdated(Intent intent);
    }
}
