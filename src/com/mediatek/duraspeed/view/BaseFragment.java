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

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.widget.ListView;
import android.widget.TextView;

import com.mediatek.duraspeed.R;
import com.mediatek.duraspeed.presenter.AppRecord;
import com.mediatek.duraspeed.presenter.AppShowedCategory;
import com.mediatek.duraspeed.presenter.BoosterContract;
import com.mediatek.duraspeed.presenter.BoosterPresenter;

import java.util.ArrayList;
import java.util.HashSet;

public class BaseFragment extends PreferenceFragment implements BoosterContract.View,
        BoosterContract.IPackageUpdate {
    private static final String TAG = "BaseFragment";

    public Activity mActivity;
    public Resources mRes;
    public PreferenceScreen mPreferenceScreen;
    public SwitchEnabler mSwitchEnabler;
    private Preference mDesPrf;
    private TextView mEmptyView;

    public BoosterContract.IViewPresenter mIViewPresenter;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.whitelist_fragment);
        mActivity = getActivity();
        mPreferenceScreen = getPreferenceScreen();
        mRes = mActivity.getResources();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
        mIViewPresenter = new BoosterPresenter(mActivity, this);
        mSwitchEnabler = createSwitchEnabler();
        // DuraSpeed is on
        // && "Report Sharing" is not switched by user, is default status
        // && the dialog's OK is not clicked by user
        boolean isFeatureOn = ViewUtils.getDuraSpeedStatus(mActivity);
        boolean isDisClaimerNotOK = !ViewUtils.getDisclaimerStatus(mActivity);
        Log.d(TAG, "ViewUtils.isFeatureOn(this) = " + isFeatureOn
                + ", isDisClaimerNotOK = " + isDisClaimerNotOK);
        if (isFeatureOn && ViewUtils.isWebQueryEnable(mActivity) && isDisClaimerNotOK) {
            // show disclaimer dialog
            mIViewPresenter.startShowDisclaimerDlg();
            return;
        } else {
            ViewUtils.initNormalCreateFollow(mActivity);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mSwitchEnabler != null) {
            mSwitchEnabler.teardownSwitchBar();
        }
    }

    SwitchEnabler createSwitchEnabler() {
        final DuraSpeedMainActivity activity = (DuraSpeedMainActivity) mActivity;
        return new SwitchEnabler(this, activity.getSwitchBar(), mIViewPresenter);
    }

    @Override
    public void showControlAppList(ArrayList<AppRecord> appList) {
        mPreferenceScreen.removeAll();
    }

    @Override
    public void showEmptyView() {
        getPreferenceScreen().removeAll();
        mEmptyView = (TextView) getView().findViewById(android.R.id.empty);
        mEmptyView.setText(R.string.empty_desc);
        ListView list = (ListView) getView().findViewById(android.R.id.list);
        list.setEmptyView(mEmptyView);
    }

    @Override
    public void showCategoryList(HashSet<AppShowedCategory> categoryList) {

    }

    @Override
    public void refreshData() {

    }

    @Override
    public void showDisclaimerDlg() {
        Log.d(TAG, "Show Disclaimer dialog");
        DisclaimerDialog.show(this, mIViewPresenter);
    }

    @Override
    public void updateAllAppStatus(boolean status) {
    }

    public void addDesPreference(int summaryId) {
        mPreferenceScreen.removeAll();
        // add description preference
        if (mDesPrf == null) {
            mDesPrf = new Preference(mActivity);
        }
        mDesPrf.setSummary(getResources().getString(summaryId));
        mDesPrf.setSelectable(false);
        mPreferenceScreen.addPreference(mDesPrf);
    }

    @Override
    public void onPackageUpdated(Intent intent) {
        Log.d(TAG, "base fragment onPackageUpdated");
    }
}