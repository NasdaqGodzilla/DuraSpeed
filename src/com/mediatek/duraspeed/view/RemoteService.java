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

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.mediatek.duraspeed.presenter.ICallback;
import com.mediatek.duraspeed.presenter.IRemoteService;

public class RemoteService extends Service {
    private static final String TAG = "RemoteService";
    private Context mContext;
    private ICallback mCallback;
    private int mState = ViewUtils.FUNCTION_STATE_DISABLE;

    public RemoteService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        mContext = this.getApplicationContext();
        return mBinder;
    }

    private final SharedPreferences.OnSharedPreferenceChangeListener mPrefListener = new
            SharedPreferences.OnSharedPreferenceChangeListener() {

                @Override
                public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String
                        key) {
                    boolean state = ViewUtils.getDuraSpeedStatus(mContext);
                    mState = state ? ViewUtils.FUNCTION_STATE_ON : ViewUtils.FUNCTION_STATE_OFF;
                    if (mCallback != null) {
                        try {
                            mCallback.onStateChanged(mState);
                        } catch (RemoteException e) {
                            Log.d(TAG, "remote exception", e);
                        }
                    }
                }
            };

    private final IBinder.DeathRecipient mDeadthCallback = new IBinder.DeathRecipient() {

        @Override
        public void binderDied() {
            Log.d(TAG, "binderDied");
            mCallback = null;
            try {
                mBinder.unregisterCallback(mCallback);
            } catch (RemoteException e) {
                Log.d(TAG, "binderDied RemoteException", e);
            }
        }
    };

    private final IRemoteService.Stub mBinder = new IRemoteService.Stub() {

        @Override
        public boolean registerCallback(ICallback callback) throws RemoteException {
            mCallback = callback;
            if (mCallback != null) {
                mCallback.asBinder().linkToDeath(mDeadthCallback, 0);
                mState = ViewUtils.getFunctionState(mContext);
                if (mState != ViewUtils.FUNCTION_STATE_DISABLE) {
                    ViewUtils.registerSharedPreferenceListener(mContext, mPrefListener);
                }
                // Check mCallback again to avoid NPE
                if (mCallback != null) {
                    mCallback.onStateChanged(mState);
                }
            }
            return true;
        }

        @Override
        public boolean unregisterCallback(ICallback callback) throws RemoteException {
            if (mState != ViewUtils.FUNCTION_STATE_DISABLE) {
                ViewUtils.unregisterSharedPreferenceListener(mContext, mPrefListener);
            }
            if (mCallback != null) {
                mCallback.asBinder().unlinkToDeath(mDeadthCallback, 0);
            }
            mCallback = null;
            return true;
        }
    };
}
