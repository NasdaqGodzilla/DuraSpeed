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
import android.content.Context;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.util.Log;

import com.android.settings.dashboard.SummaryLoader;

/**
 * Summary provider for dura speed in setting.
 *
 */
public class DSSummaryProvider {
    private static final String TAG = "DSSummaryProvider";

    private static final String SETTING_DURASPEED_ENABLED = "setting.duraspeed.enabled";

    private static final int FUNCTION_STATE_DISABLE = 2;
    private static final int FUNCTION_STATE_ON = 1;
    private static final int FUNCTION_STATE_OFF = 0;

    /**
     * Summary provider for system setting.
     *
     */
    private static class SummaryProvider implements SummaryLoader.SummaryProvider {
        private final Context mContext;
        private final SummaryLoader mSummaryLoader;
        private Context mDesContext;
        private boolean mListening = false;

        public SummaryProvider(Context context, SummaryLoader summaryLoader) {
            mContext = context;
            mSummaryLoader = summaryLoader;
            try {
                mDesContext = mContext.createPackageContext("com.mediatek.duraspeed", Context
                        .CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
            } catch (PackageManager.NameNotFoundException e) {
                Log.d(TAG, "NameNotFoundException", e);
            }
        }

        @Override
        public void setListening(boolean listening) {
            Log.d(TAG, "[setListening], listening:" + listening);
            mListening = listening;
            if (listening) {
                int value = Settings.System.getInt(mDesContext.getContentResolver(),
                        SETTING_DURASPEED_ENABLED, 0);
                Log.d(TAG, "[setListening], value:" + value);
                updateSummary(value);
            }
        }

        private void updateSummary(int state) {
            Log.d(TAG, "updateSummary: " + getSummary(state));
            mSummaryLoader.setSummary(this, getSummary(state));
        }

        private String getSummary(int state) {
            int resId = R.string.tile_summary_disable;
            switch (state) {
                case FUNCTION_STATE_DISABLE:
                    resId = R.string.tile_summary_disable;
                    break;
                case FUNCTION_STATE_ON:
                    resId = R.string.tile_summary_on;
                    break;
                case FUNCTION_STATE_OFF:
                    resId = R.string.tile_summary_off;
                    break;
                default:
                    resId = R.string.tile_summary_disable;
            }
            return mDesContext.getResources().getString(resId);
        }
    }

    public static final SummaryLoader.SummaryProviderFactory SUMMARY_PROVIDER_FACTORY = new
            SummaryLoader.SummaryProviderFactory() {
                @Override
                public SummaryLoader.SummaryProvider createSummaryProvider(
                        Activity activity, SummaryLoader summaryLoader) {
                    Log.d(TAG, "create summary loader");
                    return new SummaryProvider(activity, summaryLoader);
                }
            };
}
