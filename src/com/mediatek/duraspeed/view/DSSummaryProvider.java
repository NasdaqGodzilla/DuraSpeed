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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.android.settings.dashboard.SummaryLoader;

import com.mediatek.duraspeed.presenter.ICallback;
import com.mediatek.duraspeed.presenter.IRemoteService;
import com.mediatek.duraspeed.R;

public class DSSummaryProvider {
    private static final String TAG = "DSSummaryProvider";

    private static class SummaryProvider implements SummaryLoader.SummaryProvider {
        private final Context mContext;
        private final SummaryLoader mSummaryLoader;
        private Context mDesContext;
        private IRemoteService mRemoteService;
        private boolean mListening = false;

        private ICallback.Stub mCallback = new ICallback.Stub() {

            @Override
            public void onStateChanged(int state) {
                Log.d(TAG, "on state changed: " + state + " mListening ? " + mListening);
                if (mListening) {
                    updateSummary(state);
                }
            }
        };

        private IBinder.DeathRecipient mDeadthCallback = new IBinder.DeathRecipient() {

            @Override
            public void binderDied() {
                Log.d(TAG, "remote service disconnected");
                releaseService();
            }
        };

        private ServiceConnection mSerConn = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                mRemoteService = IRemoteService.Stub.asInterface(service);
                try {
                    mRemoteService.asBinder().linkToDeath(mDeadthCallback, 0);
                    mRemoteService.registerCallback(mCallback);
                } catch (RemoteException e) {
                    Log.d(TAG, "remote exception", e);
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                // Most times this method will not be called
                releaseService();
            }
        };

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
            mListening = listening;
            if (listening) {
                Intent intent = new Intent();
                intent.setClass(mDesContext, RemoteService.class);
                mDesContext.bindService(intent, mSerConn, Context.BIND_AUTO_CREATE);
            } else {
                mDesContext.unbindService(mSerConn);
                releaseService();
            }
        }

        private void releaseService() {
            if (mRemoteService != null) {
                try {
                    mRemoteService.unregisterCallback(mCallback);
                    mRemoteService.asBinder().unlinkToDeath(mDeadthCallback, 0);
                } catch (RemoteException e) {
                    Log.d(TAG, "remote exception", e);
                }
                mRemoteService = null;
            }
        }

        private void updateSummary(int state) {
            mSummaryLoader.setSummary(this, getSummary(state));
        }

        private String getSummary(int state) {
            int resId = R.string.tile_summary_disable;
            switch (state) {
                case ViewUtils.FUNCTION_STATE_DISABLE:
                    resId = R.string.tile_summary_disable;
                    break;
                case ViewUtils.FUNCTION_STATE_ON:
                    resId = R.string.tile_summary_on;
                    break;
                case ViewUtils.FUNCTION_STATE_OFF:
                    resId = R.string.tile_summary_off;
                    break;
                default:
                    resId = R.string.tile_summary_disable;
            }
            return mDesContext.getResources().getString(resId);
        }

    }

    public static final SummaryLoader.SummaryProviderFactory SUMMARY_PROVIDER_FACTORY =
            new SummaryLoader.SummaryProviderFactory() {
                @Override
                public SummaryLoader.SummaryProvider createSummaryProvider(
                        Activity activity, SummaryLoader summaryLoader) {
                    return new SummaryProvider(activity, summaryLoader);
                }
            };
}
