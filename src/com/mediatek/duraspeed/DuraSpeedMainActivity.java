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

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Log;
import android.widget.Switch;
import android.widget.TextView;

import com.mediatek.duraspeed.SwitchBar.OnSwitchChangeListener;

/**
 * Main activity of Dura Speed App in system setting.
 *
 */
public class DuraSpeedMainActivity extends Activity {
    private static final String TAG = "DuraSpeedMainActivity";

    private static final String SETTING_DURASPEED_ENABLED = "setting.duraspeed.enabled";
    private static final String SHARED_PREFERENCE_RB = "RBSharedPreference";
    private static final String SHARED_PREFERENCE_KEY_STATUS = "status";
    private static final int DURASPEED_DEFAULT_VALUE =
            SystemProperties.getInt("persist.vendor.duraspeed.app.on", 0);

    private int mMainContentId = R.id.main_content;
    private SwitchBar mSwitchBar;
    private TextView mCommentView;
    @Override
    protected void onCreate(Bundle savedState) {
        Log.d(TAG, "[onCreate]");
        super.onCreate(savedState);
        // If start by monkey, finish it.
        if (isMonkeyRunning()) {
           finish();
        }

        // set up empty UI
        setContentView(R.layout.main_layout);
        mSwitchBar = (SwitchBar) findViewById(R.id.switch_bar);
        mCommentView = (TextView) findViewById(R.id.comment);

        SharedPreferences sharedPrf = getSharedPreferences(
                SHARED_PREFERENCE_RB, Context.MODE_PRIVATE);
        int value = sharedPrf.getInt(SHARED_PREFERENCE_KEY_STATUS, -1);
        if (value == -1) {
            value = Settings.System.getInt(getContentResolver(),
                    SETTING_DURASPEED_ENABLED, DURASPEED_DEFAULT_VALUE);
        }
        Log.d(TAG, "[onCreate], value:" + value);

        boolean checked = (value == 1);
        mSwitchBar.setChecked(checked);
        mCommentView.setText(R.string.empty_desc);

        mSwitchBar.addOnSwitchChangeListener(new OnSwitchChangeListener() {

            @Override
            public void onSwitchChanged(Switch switchView, boolean isChecked) {
                Log.d(TAG, "[onSwitchChanged] isChecked = " + isChecked);
                if (isChecked) {

                } else {
                    mCommentView.setText(R.string.empty_desc);
                }
            }
        });
    }

    /**
     * Returns true if Monkey is running.
     *
     * @return True when is in monkey running.
     */
    public boolean isMonkeyRunning() {
        return ActivityManager.isUserAMonkey();
    }
}
