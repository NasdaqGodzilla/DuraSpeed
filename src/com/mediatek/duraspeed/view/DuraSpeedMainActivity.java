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
import android.app.ActivityManager;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Process;
import android.util.Log;

import com.mediatek.duraspeed.R;
import com.mediatek.duraspeed.model.DatabaseManager;
import com.mediatek.duraspeed.presenter.AppShowedCategory;
import com.mediatek.duraspeed.presenter.BoosterContract;
import com.mediatek.duraspeed.presenter.BoosterPresenter;

import java.util.ArrayList;

public class DuraSpeedMainActivity extends Activity {
    private static final String TAG = "DuraSpeedMainActivity";
    protected static final String ACTION_PKG_UPDATE = "com.mediatek.action.ACTION_RB_PK_UPDATE";
    private int mMainContentId = R.id.main_content;
    private SwitchBar mSwitchBar;
    private PackageReceiver mPackageReceiver = new PackageReceiver();
    private boolean mRegistered = false;

    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        // If start by monkey, finish it
        if (isMonkeyRunning()) {
            finish();
        }
        // set up empty UI
        setContentView(R.layout.whitelist);
        mSwitchBar = (SwitchBar) findViewById(R.id.switch_bar);
        switchToFragment(WhiteListFragment.class.getName(), null, false);
        ViewUtils.sStarted = true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter1 = new IntentFilter(ACTION_PKG_UPDATE);
        registerReceiver(mPackageReceiver, filter1);

        IntentFilter filter2 = new IntentFilter();
        filter2.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter2.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter2.addDataScheme("package");
        registerReceiver(mPackageReceiver, filter2);
        mRegistered = true;
    }

    @Override
    protected void onStop() {
        if (mRegistered) {
            unregisterReceiver(mPackageReceiver);
            mRegistered = false;
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ViewUtils.sLowRamDevice && ViewUtils.sStarted) {
            ViewUtils.sStarted = false;
            Process.killProcessQuiet(Process.myPid());
        }
    }

    /**
     * switch To Fragment.
     *
     * @param fragmentName switch to fragment class name
     * @param args         Bundle , can be null
     * @param backStack    put it to back stack or not
     * @return fragment will switch to
     */
    public Fragment switchToFragment(String fragmentName, Bundle args, boolean backStack) {
        Log.d(TAG, "switchToFragment = " + fragmentName);
        Fragment f = Fragment.instantiate(this, fragmentName, args);
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(mMainContentId, f);
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        if (backStack) {
            transaction.addToBackStack("RunningBooster");
        }
        transaction.commitAllowingStateLoss();
        return f;
    }

    public SwitchBar getSwitchBar() {
        return mSwitchBar;
    }

    private class PackageReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                String action = intent.getAction();
                Log.d(TAG, "onReceive, action: " + action);
                if (ACTION_PKG_UPDATE.equals(action)) {
                    if (context instanceof Activity) {
                        Log.d(TAG, "package receiver: " + intent.toString());
                        FragmentManager manager = ((Activity) context).getFragmentManager();
                        Fragment frag = manager.findFragmentById(mMainContentId);
                        if (frag instanceof BaseFragment) {
                            ((BaseFragment) frag).onPackageUpdated(intent);
                        } else {
                            Log.d(TAG, "not find base fragment: " + frag);
                        }
                    }
                } else if (Intent.ACTION_PACKAGE_ADDED.equals(action)
                    || Intent.ACTION_PACKAGE_REMOVED.equals(action)) {
                    String packageName = intent.getData().getSchemeSpecificPart();
                    DatabaseManager databaseManager = DatabaseManager.getInstance(context);
                    if (Intent.ACTION_PACKAGE_ADDED.equals(action)) {
                        // Add package info to DB and cache
                        if (databaseManager.insert(packageName)) {
                            // Set free policy when it need add into white list
                            if (ViewUtils.getDuraSpeedStatus(context)) {
                                BoosterContract.IPolicyPresenter mIPolicyPresenter =
                                        new BoosterPresenter(context, null);
                                mIPolicyPresenter.setAppWhitelist();
                            }
                            notifyPackageUpdated(context, packageName);
                            if (databaseManager.getCategory(packageName).
                                    equals(AppShowedCategory.OTHERS)) {
                                // Query category from Web
                                ArrayList<String> pkgList = new ArrayList<String>();
                                pkgList.add(packageName);
                                QueryCategoryJobService.scheduleQueryCategoryTask(context, pkgList);
                            }
                        }
                    } else if (Intent.ACTION_PACKAGE_REMOVED.equals(action)) {
                        // Remove package info from DB and cache and update UI if necessary
                        if (databaseManager.delete(packageName)) {
                            notifyPackageUpdated(context, packageName);
                        }
                    }
                }
            }
        }

        private void notifyPackageUpdated(Context context, String packageName) {
            Intent intent = new Intent(ACTION_PKG_UPDATE);
            intent.putExtra("pkgnames", new String[]{ packageName });
            context.sendBroadcast(intent);
        }
    }

    /**
     * Returns true if Monkey is running.
     */
    public boolean isMonkeyRunning() {
        return ActivityManager.isUserAMonkey();
    }
}
