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
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnKeyListener;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;

import com.mediatek.duraspeed.R;
import com.mediatek.duraspeed.presenter.BoosterContract;

/**
 * add the disclaimer dialog , show when turn on the feature from off status.
 */
public  class DisclaimerDialog extends DialogFragment implements OnClickListener , OnKeyListener {
    private static final String TAG = "DisclaimerDialog";
    private Activity mActivity;
    static Fragment mFragment;
    private static BoosterContract.IViewPresenter mIViewPresenter;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mActivity = getActivity();
        AlertDialog dialog = new AlertDialog.Builder(mActivity)
        .setTitle(R.string.dlg_disclaimer_title)
                .setMessage(R.string.dlg_disclaimer_msg)
        .setPositiveButton(R.string.dlg_notify_ok, this)
                .setCancelable(false)
                .show();
        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnKeyListener(this);
        return dialog;
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        if (i == DialogInterface.BUTTON_POSITIVE) {
            Log.d(TAG, "Click on OK as Disclaimer dialog");
            ViewUtils.setDisclaimerStatus(mActivity, true);
            // init the normal activity created follow even if there is no disclaimer dialog
            ViewUtils.initNormalCreateFollow(mActivity);
            // update the UI follow after action bar switch changed
            ViewUtils.updateSwitchChangeUI(mActivity, true, mFragment, mIViewPresenter);
        }
    }

    public static void show(Fragment parent, BoosterContract.IViewPresenter viewPresenter) {
        if (!parent.isAdded()) return;
        mFragment = parent;
        mIViewPresenter = viewPresenter;
        final DisclaimerDialog dialog = new DisclaimerDialog();
        dialog.setTargetFragment(parent, 0);
        dialog.show(parent.getFragmentManager(), "disclaimerDialog");
        Log.d(TAG, "show()");
    }

    @Override
    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Log.d(TAG, "finish Activity when press back key in Disclaimer dialog");
            mActivity.finish();
            return true;
        }
        return false;
    }
}
