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

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.mediatek.duraspeed.R;
import com.mediatek.duraspeed.presenter.AppRecord;
import com.mediatek.duraspeed.presenter.AppShowedCategory;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

public class CategoryFragment extends BaseFragment {

    private static final String TAG = "CategoryFragment";
    private List<AppShowedCategory> mCategoryList = new ArrayList<AppShowedCategory>();
    private MenuItem mAppListMenuItem;
    private CategoryComparator mCateComp;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mCateComp = new CategoryComparator(mRes);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        this.getActivity().setTitle(R.string.app_name);
    }
    @Override
    public void onStart() {
        super.onStart();
        // Reload data
        refreshData();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.by_applist_menu, menu);
        mAppListMenuItem = menu.findItem(R.id.by_app_list);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        boolean status = ViewUtils.getDuraSpeedStatus(mActivity);
        if (mAppListMenuItem != null) {
            mAppListMenuItem.setEnabled(status);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.by_app_list:
                ((DuraSpeedMainActivity) mActivity).switchToFragment(
                        WhiteListFragment.class.getName(), null, false);
                return true;
            default:
                return super.onOptionsItemSelected(item);

        }
    }

    @Override
    public void showCategoryList(HashSet<AppShowedCategory> categoryList) {
        addDesPreference(R.string.fun_desc_category);
        if (categoryList == null) {
            return;
        }
        mCategoryList.clear();
        mCategoryList = new ArrayList(categoryList);
        Collections.sort(mCategoryList, mCateComp);

        for (AppShowedCategory category : mCategoryList) {
            Preference prf = new Preference(mActivity);
            prf.setKey(category.toString());
            prf.setTitle(ViewUtils.getCategoryName((category), mRes));
            prf.setIcon(ViewUtils.getCategoryIcon(category));
            List<AppRecord> allAppsByCategoryList = mIViewPresenter
                    .getAppListOfCategory(category.toString());
            if (allAppsByCategoryList != null) {
                prf.setSummary(mRes.getString(R.string.category_summary,
                        getAppOnCount(allAppsByCategoryList),
                        allAppsByCategoryList.size()));
            }
            mPreferenceScreen.addPreference(prf);
        }
    }

    // comparator for sort the category name by A,B,C....
    private static class CategoryComparator implements Comparator<AppShowedCategory> {
        private final Collator mCollator = Collator.getInstance();
        private Resources mRes;

        public CategoryComparator(Resources res) {
            this.mRes = res;
        }

        @Override
        public int compare(AppShowedCategory category1,
                           AppShowedCategory category2) {
            return mCollator.compare(
                    ViewUtils.getCategoryName((category1), mRes),
                    ViewUtils.getCategoryName((category2), mRes));
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
                                         Preference preference) {
        Bundle bundle = new Bundle();
        bundle.putString(ViewUtils.KEY_FROM_CATEGORY, preference.getKey());
        ((DuraSpeedMainActivity) mActivity).switchToFragment(
                WhiteListFragment.class.getName(), bundle, true);
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private int getAppOnCount(List<AppRecord> allAppsByCategoryList) {
        int onCount = 0;
        if (allAppsByCategoryList != null) {
            for (AppRecord appRecord : allAppsByCategoryList) {
                if (appRecord.getStatus() == AppRecord.STATUS_ENABLED) {
                    onCount++;
                }
            }
        }
        return onCount;
    }

    @Override
    public void onPackageUpdated(Intent intent) {
        refreshData();
    }

    @Override
    public void refreshData() {
        mIViewPresenter.startCategoryList(ViewUtils.getDuraSpeedStatus(mActivity));
    }
}