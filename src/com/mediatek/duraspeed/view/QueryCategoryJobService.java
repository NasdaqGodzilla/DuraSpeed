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

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.PersistableBundle;
import android.util.Log;

import com.mediatek.duraspeed.categoryDB.QueryCategoryTask;
import com.mediatek.duraspeed.presenter.APPCategory;
import com.mediatek.duraspeed.model.DatabaseManager;
import com.mediatek.duraspeed.model.ModelUtils;
import com.mediatek.duraspeed.presenter.AppShowedCategory;
import com.mediatek.duraspeed.presenter.BoosterPresenter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

public class QueryCategoryJobService extends JobService {
    private static final String TAG = "QueryCategoryJobService";
    private static final int JOB_ID_START = 5500;
    private static final int JOB_MAX_NUM = 90;
    private static final int JOB_GROUP_NUM = 20;
    private static final String JOB_BUNDLE_KEY_PKGS = "pkgNames";
    private ArrayList<AsyncTask> mQueryTasks = null;
    private JobParameters mJobParams = null;

    private static final Comparator<JobInfo> JOB_COMPARATOR = new Comparator<JobInfo>() {
        @Override
        public int compare(JobInfo jobInfo1, JobInfo jobInfo2) {
            return jobInfo1.getId() - jobInfo2.getId();
        }
    };

    public QueryCategoryJobService() {
        super();
        mQueryTasks = new ArrayList<AsyncTask>();
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public static boolean scheduleQueryCategoryTask(Context context, ArrayList<String> pkgList) {
        boolean ret = false;
        if (!ViewUtils.isAppStartup(context) ||
                !ViewUtils.isWebQueryEnable(context)) {
            Log.d(TAG, "App not start up yet, do not run Web query");
            return ret;
        }
        //if disclaimer dialog don't show once , cannot query from Web query
        if (!ViewUtils.getDisclaimerStatus(context)) {
            Log.d(TAG, "disclaimer dialog don't show once,, do not run Web query");
            return ret;
        }
        if (pkgList != null && pkgList.size() > 0) {
            JobScheduler scheduler = (JobScheduler) context.getSystemService(Context
                    .JOB_SCHEDULER_SERVICE);
            int jobId = JOB_ID_START;
            ArrayList<JobInfo> pendingJobs = new ArrayList<JobInfo>(scheduler.getAllPendingJobs());
            int pendingJobSize = pendingJobs.size();
            Log.d(TAG, "pending job size: " + pendingJobSize);
            if (pendingJobSize >= JOB_MAX_NUM || (pkgList.size() < JOB_GROUP_NUM &&
                    pendingJobSize > 0)) {
                // Merge pkgList to old jobs
                JobInfo temp = null;
                String[] pkgNames = null;
                for (JobInfo jobInfo : pendingJobs) {
                    PersistableBundle extras = jobInfo.getExtras();
                    if (extras.containsKey(JOB_BUNDLE_KEY_PKGS)) {
                        pkgNames = extras.getStringArray(JOB_BUNDLE_KEY_PKGS);
                        if (pkgNames.length < JOB_GROUP_NUM) {
                            temp = jobInfo;
                            break;
                        }
                    }
                }
                if (temp == null) {
                    temp = pendingJobs.get(0);
                    if (temp.getExtras().containsKey(JOB_BUNDLE_KEY_PKGS)) {
                        pkgNames = temp.getExtras().getStringArray(JOB_BUNDLE_KEY_PKGS);
                    } else {
                        pkgNames = null;
                    }
                }
                Log.d(TAG, "re-use jobId: " + temp.getId());
                scheduler.cancel(temp.getId());
                if (pkgNames != null) {
                    for (String pkgName : pkgNames) {
                        if (!pkgList.contains(pkgName)) {
                            pkgList.add(pkgName);
                        }
                    }
                }
            }
            if (pendingJobs.size() > 0) {
                Collections.sort(pendingJobs, JOB_COMPARATOR);
                jobId = pendingJobs.get(pendingJobs.size() - 1).getId() + 1;
            }
            PersistableBundle extra = new PersistableBundle();
            extra.putStringArray(JOB_BUNDLE_KEY_PKGS, pkgList.toArray(new String[]{}));
            JobInfo jobInfo = new JobInfo.Builder(jobId, new ComponentName(context,
                    QueryCategoryJobService.class))
                    .setPersisted(true)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setExtras(extra)
                    .build();
            ret = scheduler.schedule(jobInfo) == JobScheduler.RESULT_SUCCESS;
        }
        return ret;
    }

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        mJobParams = jobParameters;
        int jobId = jobParameters.getJobId();
        Log.d(TAG, "onStartJob jobId = " + jobId);
        PersistableBundle extras = jobParameters.getExtras();
        if (extras.containsKey(JOB_BUNDLE_KEY_PKGS)) {
            String[] pkgNames = extras.getStringArray(JOB_BUNDLE_KEY_PKGS);
            Log.d(TAG, "onStartJob pkgNames = " + Arrays.toString(pkgNames));
            AsyncTask task = new QueryCategoryBatchTasks(mJobParams);
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, pkgNames);
            synchronized (mQueryTasks) {
                mQueryTasks.add(task);
            }
            return true;
        } else {
            Log.d(TAG, "no available pkgNames");
            jobFinished(mJobParams, false);
        }
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        int jobId = jobParameters.getJobId();
        Log.d(TAG, "onStopJob jobId = " + jobId);
        PersistableBundle extras = jobParameters.getExtras();
        if (extras.containsKey(JOB_BUNDLE_KEY_PKGS)) {
            String[] pkgNames = extras.getStringArray(JOB_BUNDLE_KEY_PKGS);
            Log.d(TAG, "onStopJob pkgNames = " + Arrays.toString(pkgNames));
        } else {
            Log.d(TAG, "no available pkgNames");
        }
        synchronized (mQueryTasks) {
            Log.d(TAG, "mQueryTasks size: " + mQueryTasks.size());
            for (int i = 0; i < mQueryTasks.size(); i++) {
                QueryCategoryBatchTasks task = (QueryCategoryBatchTasks) mQueryTasks.get(i);
                if (!task.isCancelled()) {
                    task.cancel(true);
                }
            }
        }
        return true;
    }

    private class QueryCategoryBatchTasks extends AsyncTask<String, Integer,
            HashMap<String, String>> {

        private DatabaseManager mDataManager = null;
        private JobParameters mJobParameters = null;
        private boolean mDataUpdated = false;

        public QueryCategoryBatchTasks(JobParameters jobParameters) {
            mDataManager = DatabaseManager.getInstance
                    (QueryCategoryJobService.this.getApplicationContext());
            mJobParameters = jobParameters;
        }

        private void removeFromTaskList() {
            Log.d(TAG, "removeFromTaskList");
            synchronized (mQueryTasks) {
                mQueryTasks.remove(this);
            }
        }

        private void notifyUpdateUI(HashMap<String, String> hashMap) {
            if (hashMap != null) {
                Intent intent = new Intent(DuraSpeedMainActivity.ACTION_PKG_UPDATE);
                intent.putExtra("pkgnames", hashMap.keySet().toArray(new String[]{}));
                mDataManager.getAppContext().sendBroadcast(intent);
            }
        }

        private void onTaskDone(HashMap<String, String> hashMap, boolean completed) {
            if (mDataUpdated) {
                // Re-set free policy when data updated
                if (ViewUtils.getDuraSpeedStatus(mDataManager.getAppContext())) {
                    BoosterPresenter presenter = new BoosterPresenter(mDataManager.getAppContext(),
                            null);
                    presenter.setAppWhitelist();
                }
                notifyUpdateUI(hashMap);
            }
            removeFromTaskList();
            jobFinished(mJobParameters, !completed);
        }

        @Override
        protected void onPostExecute(HashMap<String, String> hashMap) {
            onTaskDone(hashMap, true);
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
        }

        @Override
        protected void onCancelled(HashMap<String, String> hashMap) {
            if (hashMap != null) {
                Log.d(TAG, "onCancelled: hashMap size: " + hashMap.size());
            } else {
                Log.e(TAG, "onCancelled: hashMap is null");
            }
            onTaskDone(hashMap, false);
        }

        @Override
        protected HashMap<String, String> doInBackground(String... strings) {
            HashMap<String, String> retMap = new HashMap<String, String>();
            ArrayList<String> pkgNameList = new ArrayList<String>();
            for (int i = 0; i < strings.length; i++) {
                pkgNameList.add(strings[i]);
            }
            for (int i = 0; i < pkgNameList.size(); i++) {
                if (isCancelled()) {
                    Log.w(TAG, "query async task cancelled!");
                    break;
                }
                String pkgName = pkgNameList.get(i);
                if (!mDataManager.isInvisibleWhitelist(pkgName) && AppShowedCategory.OTHERS.equals(
                        mDataManager.getCategory(pkgName))) {
                    APPCategory appCate;
                    try {
                        QueryCategoryTask queryCategoryTask = new QueryCategoryTask(mDataManager
                                .getAppContext());
                        appCate = queryCategoryTask.queryFromWeb(pkgName);
                    } catch (Exception e) {
                        Log.e(TAG, "query pkg exception: " + pkgName, e);
                        this.publishProgress(i + 1, pkgNameList.size());
                        break;
                    }
                    AppShowedCategory showedCate = ModelUtils.convertAppCategory(appCate);
                    Log.d(TAG, "query pkgName:" + pkgName + " to appCate:" + appCate.toString() +
                            " showedCate:" + showedCate.toString());
                    if (showedCate != AppShowedCategory.OTHERS) {
                        // Update DB and UI if necessary
                        if (mDataManager.modifyCategory(pkgName, showedCate.toString())) {
                            mDataUpdated = true;
                            retMap.put(pkgName, showedCate.toString());
                        } else {
                            Log.d(TAG, "update category fail for pkgName = " + pkgName);
                        }
                    }
                }
                this.publishProgress(i + 1, pkgNameList.size());
            }
            return retMap;
        }
    }
}
