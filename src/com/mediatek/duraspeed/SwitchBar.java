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

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Dura speed switch bar view.
 *
 */
public class SwitchBar extends LinearLayout implements CompoundButton.OnCheckedChangeListener,
        View.OnClickListener {
    private static final String TAG = "DuraSpeed/SwitchBar";
    private static final String SETTING_DURASPEED_ENABLED = "setting.duraspeed.enabled";

    /**
     * Interface to callback when switch bar status changed.
     *
     */
    public interface OnSwitchChangeListener {
        /**
         * Called when the checked state of the Switch has changed.
         *
         * @param switchView The Switch view whose state has changed.
         * @param isChecked  The new checked state of switchView.
         */
        void onSwitchChanged(Switch switchView, boolean isChecked);
    }

    private Context mContext;
    private Switch mSwitch;
    private TextView mTextView;
    private String mLabel;
    private boolean mDisabledByAdmin = false;
    private ArrayList<OnSwitchChangeListener> mSwitchChangeListeners =
            new ArrayList<OnSwitchChangeListener>();
    private static final int[] XML_ATTRIBUTES = {
            R.attr.switchBarMarginStart,
            R.attr.switchBarMarginEnd,
            R.attr.switchBarBackgroundColor};

    /**
     * Switch bar constructor.
     *
     * @param context App context.
     */
    public SwitchBar(Context context) {
        this(context, null);
    }

    /**
     * Switch bar constructor.
     *
     * @param context App context.
     * @param attrs The attributes of the XML tag that is inflating the view.
     */
    public SwitchBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Switch bar constructor.
     *
     * @param context App context.
     * @param attrs The attributes of the XML tag that is inflating the view.
     * @param defStyleAttr An attribute in the current theme that contains a
     *        reference to a style resource that supplies default values for
     *        the view. Can be 0 to not look for defaults.
     */
    public SwitchBar(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    /**
     * Switch bar constructor.
     *
     * @param context App context.
     * @param attrs The attributes of the XML tag that is inflating the view.
     * @param defStyleAttr An attribute in the current theme that contains a
     *        reference to a style resource that supplies default values for
     *        the view. Can be 0 to not look for defaults.
     * @param defStyleRes A resource identifier of a style resource that
     *        supplies default values for the view, used only if
     *        defStyleAttr is 0 or can not be found in the theme. Can be 0
     *        to not look for defaults.
     */
    public SwitchBar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        mContext = context;
        LayoutInflater.from(context).inflate(R.layout.switch_bar, this);

        final TypedArray a = context.obtainStyledAttributes(attrs, XML_ATTRIBUTES);
        int switchBarMarginStart = (int) a.getDimension(0, 0);
        int switchBarMarginEnd = (int) a.getDimension(1, 0);
        int switchBarBackgroundColor = (int) a.getColor(2, 0);
        a.recycle();

        mTextView = (TextView) findViewById(R.id.switch_text);
        mTextView.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        mLabel = getResources().getString(R.string.switch_off_text);
        updateText();
        ViewGroup.MarginLayoutParams lp = (MarginLayoutParams) mTextView.getLayoutParams();
        lp.setMarginStart(switchBarMarginStart);

        mSwitch = (Switch) findViewById(R.id.switch_widget);
        // Prevent onSaveInstanceState() to be called as we are managing the state of the Switch
        // on our own
        mSwitch.setSaveEnabled(false);
        mSwitch.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        lp = (MarginLayoutParams) mSwitch.getLayoutParams();
        lp.setMarginEnd(switchBarMarginEnd);
        setBackgroundColor(switchBarBackgroundColor);
        mSwitch.setBackgroundColor(switchBarBackgroundColor);

        addOnSwitchChangeListener(new OnSwitchChangeListener() {
            @Override
            public void onSwitchChanged(Switch switchView, boolean isChecked) {
                setTextViewLabel(isChecked);
            }
        });

        setOnClickListener(this);
        mSwitch.setOnCheckedChangeListener(this);
    }

    /**
     * Set checked or not.
     *
     * @param checked Switch bar checked value.
     */
    public void setChecked(boolean checked) {
        setTextViewLabel(checked);
        mSwitch.setChecked(checked);
    }

    /**
     * Whether this switch bar is checked or not.
     *
     * @return True when switch bar is checked.
     */
    public boolean isChecked() {
        return mSwitch.isChecked();
    }

    /**
     * Set checked or not.
     *
     * @param enabled Switch bar enabled value.
     */
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        mTextView.setEnabled(enabled);
        mSwitch.setEnabled(enabled);
    }

    @Override
    public void onClick(View v) {
        final boolean isChecked = !mSwitch.isChecked();
        setChecked(isChecked);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        Log.d(TAG, "[onCheckedChanged], isChecked:" + isChecked);
        Settings.System.putInt(mContext.getContentResolver(),
                SETTING_DURASPEED_ENABLED, isChecked ? 1 : 0);
        Settings.Global.putInt(mContext.getContentResolver(),
                SETTING_DURASPEED_ENABLED, isChecked ? 1 : 0);
        propagateChecked(isChecked);
    }

    /**
     * Add switch change listener.
     *
     * @param listener The instance of {@link OnSwitchChangeListener}.
     */
    public void addOnSwitchChangeListener(OnSwitchChangeListener listener) {
        if (mSwitchChangeListeners.contains(listener)) {
            throw new IllegalStateException("Cannot add twice the same OnSwitchChangeListener");
        }
        mSwitchChangeListeners.add(listener);
    }

    /**
     * Remove switch change listener.
     *
     * @param listener The instance of {@link OnSwitchChangeListener}.
     */
    public void removeOnSwitchChangeListener(OnSwitchChangeListener listener) {
        if (!mSwitchChangeListeners.contains(listener)) {
            throw new IllegalStateException("Cannot remove OnSwitchChangeListener");
        }
        mSwitchChangeListeners.remove(listener);
    }

    private void setTextViewLabel(boolean isChecked) {
        mLabel = getResources()
                .getString(isChecked ? R.string.switch_on_text : R.string.switch_off_text);
        updateText();
    }

    private void updateText() {
        mTextView.setText(mLabel);
    }

    private void propagateChecked(boolean isChecked) {
        final int count = mSwitchChangeListeners.size();
        for (int n = 0; n < count; n++) {
            mSwitchChangeListeners.get(n).onSwitchChanged(mSwitch, isChecked);
        }
    }

    /**
     * Class to saved state.
     *
     */
    static class SavedState extends BaseSavedState {
        public boolean checked;
        public boolean visible;

        SavedState(Parcelable superState) {
            super(superState);
        }

        /**
         * Constructor called from {@link #CREATOR}.
         */
        private SavedState(Parcel in) {
            super(in);
            checked = (Boolean) in.readValue(null);
            visible = (Boolean) in.readValue(null);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeValue(checked);
            out.writeValue(visible);
        }

        @Override
        public String toString() {
            return "SwitchBar.SavedState{"
                    + Integer.toHexString(System.identityHashCode(this))
                    + " checked=" + checked
                    + " visible=" + visible + "}";
        }

        public static final Parcelable.Creator<SavedState> CREATOR
                = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();

        SavedState ss = new SavedState(superState);
        ss.checked = mSwitch.isChecked();
        ss.visible = (getVisibility() == View.VISIBLE);
        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;

        super.onRestoreInstanceState(ss.getSuperState());

        mSwitch.setChecked(ss.checked);
        setTextViewLabel(ss.checked);
        setVisibility(ss.visible ? View.VISIBLE : View.GONE);
        mSwitch.setOnCheckedChangeListener(ss.visible ? this : null);

        requestLayout();
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return Switch.class.getName();
    }
}
