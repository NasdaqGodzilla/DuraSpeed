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
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.TextView;

import com.mediatek.duraspeed.R;
import com.mediatek.duraspeed.presenter.AppRecord;
import com.mediatek.duraspeed.presenter.AppShowedCategory;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class WhiteListFragment extends BaseFragment {
    private static final String TAG = "WhiteListFragment";

    // menu list
    private MenuItem mCategoryMenuItem;
    private MenuItem mTurnAllOnMenuItem;
    private MenuItem mTurnAllOffMenuItem;
    private boolean mIsFromCategoryList = true;
    private String mFromCategory = null;

    private ArrayList<AppRecord> mAllCurrentShowedApp = new ArrayList<AppRecord>();

    private PackageComparator mPkgComp;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mPkgComp = new PackageComparator();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
        Bundle bundle = getArguments();
        Log.d(TAG, "bundle = " + bundle);
        //switch to here from category fragment
        if (bundle != null) {
            String str = bundle.getString(ViewUtils.KEY_FROM_CATEGORY);
            if (str != null) {
                mIsFromCategoryList = false;
            }
            Log.d(TAG, "bundle = " + str);
            mFromCategory = str;
            this.getActivity().setTitle(ViewUtils.getCategoryName(AppShowedCategory.valueOf
                    (mFromCategory), this.getResources()));
        } else {
            mIsFromCategoryList = true;
            mFromCategory = null;
            this.getActivity().setTitle(R.string.app_name);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mIsFromCategoryList) {
            // UI show the feature on/off
            if (mSwitchEnabler != null) {
                mSwitchEnabler.setupSwitchBar();
            }
        } else {
            // UI don't show the feature on/off
            if (mSwitchEnabler != null) {
                mSwitchEnabler.teardownSwitchBar();
            }
        }
        // Reload data
        refreshData();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void showControlAppList(ArrayList<AppRecord> appList) {
        if (appList == null || appList.size() == 0) {
            this.mActivity.getFragmentManager().popBackStackImmediate();
            showBlankView();
            Log.d(TAG, "show control app list is empty");
            return;
        }
        super.showControlAppList(appList);
        mAllCurrentShowedApp = appList;
        Log.d(TAG, "showControlAppList");
        //add description preference
        addDesPreference(R.string.fun_desc);
        getAppLabels(mAllCurrentShowedApp);
        // sort app
        Collections.sort(mAllCurrentShowedApp, mPkgComp);
        // add app list preference
        addAllAppPreference(mAllCurrentShowedApp);
    }

    public void showBlankView() {
        mPreferenceScreen.removeAll();
        TextView emptyView = (TextView) getView().findViewById(android.R.id.empty);
        emptyView.setText("");
        ListView list = (ListView) getView().findViewById(android.R.id.list);
        list.setEmptyView(emptyView);
    }

    @Override
    public void showEmptyView() {
        super.showEmptyView();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.by_category_menu, menu);
        Log.d(TAG, "onCreateOptionsMenu()");
        mTurnAllOnMenuItem = menu.findItem(R.id.turn_all_on);
        mTurnAllOffMenuItem = menu.findItem(R.id.turn_all_off);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        updateMenu(ViewUtils.getDuraSpeedStatus(mActivity));
    }

    public void updateMenu(boolean status) {
        if (mCategoryMenuItem != null) {
            mCategoryMenuItem.setEnabled(status);
            // if from category list, don't show menu "By category"
            mCategoryMenuItem.setVisible(mIsFromCategoryList);
        }
        if (mTurnAllOnMenuItem != null) mTurnAllOnMenuItem.setVisible(status);
        if (mTurnAllOffMenuItem != null) mTurnAllOffMenuItem.setVisible(status);
        Log.d(TAG, "status = " + status + " when update prepare option menu");
        if (status) {
            int onCount = 0;
            int offCount = 0;
            for (AppRecord appRecord : mAllCurrentShowedApp) {
                if (!TextUtils.isEmpty(appRecord.getLabel())) {
                    int i = (appRecord.getStatus() == AppRecord.STATUS_ENABLED) ? onCount++ :
                            offCount++;
                } else {
                    Log.w(TAG, "empty label for pkg: " + appRecord.getPkgName());
                }
            }
            Log.d(TAG, "onCount = " + onCount + " offCount = " + offCount);
            if (onCount == 0 && offCount == 0) {
                if (mTurnAllOnMenuItem != null) mTurnAllOnMenuItem.setVisible(false);
                if (mTurnAllOffMenuItem != null) mTurnAllOffMenuItem.setVisible(false);
            } else if (onCount == 0 && mTurnAllOffMenuItem != null) {
                mTurnAllOffMenuItem.setVisible(false);
            } else if (offCount == 0 && mTurnAllOnMenuItem != null) {
                mTurnAllOnMenuItem.setVisible(false);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, "onOptionItemSelected = " + item.getTitle());
        switch (item.getItemId()) {
            case R.id.turn_all_on:
                mIViewPresenter.addAllToAppWhitelist(mAllCurrentShowedApp);
                return true;
            case R.id.turn_all_off:
                mIViewPresenter.removeAllFromAppWhitelist(mAllCurrentShowedApp);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void updateAllAppStatus(boolean status) {
        //add description preference
        addDesPreference(R.string.fun_desc);
        getAppLabels(mAllCurrentShowedApp);
        Collections.sort(mAllCurrentShowedApp, mPkgComp);
        // add app list preference
        addAllAppPreference(mAllCurrentShowedApp);
    }

    private void addAllAppPreference(List<AppRecord> appList) {
        for (AppRecord appRecord : appList) {
            if (!TextUtils.isEmpty(appRecord.getLabel())) {
                AppListPreference prf = new AppListPreference(mActivity,
                        appRecord, mIViewPresenter);
                mPreferenceScreen.addPreference(prf);
            } else {
                Log.w(TAG, "empty label for pkg: " + appRecord.getPkgName());
            }
        }
    }

    // comparator for sort the app list by A,B,C....
    public static final class PackageComparator implements Comparator<AppRecord> {
        private final Collator mCollator = Collator.getInstance();

        @Override
        public int compare(AppRecord pkg1, AppRecord pkg2) {
            if (pkg1.getStatus() == pkg2.getStatus()) {
                return mCollator.compare(pkg1.getLabel(), pkg2.getLabel());
            } else {
                return pkg1.getStatus() == AppRecord.STATUS_ENABLED ? -1 : 1;
            }
        }
    }

    @Override
    public void onPackageUpdated(Intent intent) {
        Log.d(TAG, "package updated");
        refreshData();
    }

    @Override
    public void refreshData() {
        if (mIsFromCategoryList) {
            mIViewPresenter.startAppList(ViewUtils.getDuraSpeedStatus(mActivity));
        } else {
            if (mFromCategory != null) {
                mIViewPresenter.restartAppList(mFromCategory);
            } else {
                Log.d(TAG, "refreshData, but from category is NULL");
            }
        }
    }

    private void getAppLabels(ArrayList<AppRecord> appList) {
        Iterator<AppRecord> iterator = appList.iterator();
        while (iterator.hasNext()) {
            AppRecord item = iterator.next();
            item.setLabel(ViewUtils.getAppLabel(mActivity, item.getPkgName()));
        }
    }
}

