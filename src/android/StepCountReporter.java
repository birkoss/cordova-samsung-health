/**
 * Copyright (C) 2014 Samsung Electronics Co., Ltd. All rights reserved.
 *
 * Mobile Communication Division,
 * Digital Media & Communications Business, Samsung Electronics Co., Ltd.
 *
 * This software and its documentation are confidential and proprietary
 * information of Samsung Electronics Co., Ltd.  No part of the software and
 * documents may be copied, reproduced, transmitted, translated, or reduced to
 * any electronic medium or machine-readable form without the prior written
 * consent of Samsung Electronics.
 *
 * Samsung Electronics makes no representations with respect to the contents,
 * and assumes no responsibility for any errors that might appear in the
 * software and documents. This publication and the contents hereof are subject
 * to change without notice.
 */

package com.birkoss.plugin;

import com.samsung.android.sdk.healthdata.HealthConstants;
import com.samsung.android.sdk.healthdata.HealthData;
import com.samsung.android.sdk.healthdata.HealthDataObserver;
import com.samsung.android.sdk.healthdata.HealthDataResolver;
import com.samsung.android.sdk.healthdata.HealthDataResolver.ReadRequest;
import com.samsung.android.sdk.healthdata.HealthDataResolver.ReadResult;
import com.samsung.android.sdk.healthdata.HealthDataStore;
import com.samsung.android.sdk.healthdata.HealthResultHolder;

import android.util.Log;

import java.util.Calendar;
import java.util.TimeZone;

public class StepCountReporter {
    private final HealthDataStore mStore;
    private StepCountObserver mStepCountObserver;
    private static final long ONE_DAY_IN_MILLIS = 24 * 60 * 60 * 1000L;

    private SamsungHealth mShealth;

    public StepCountReporter(HealthDataStore store, SamsungHealth shealth) {
        mStore = store;
        mShealth = shealth;
    }

    public void start(StepCountObserver listener) {
        mShealth.mDebug += "start()...\n";
        mStepCountObserver = listener;
        // Register an observer to listen changes of step count and get today step count
        HealthDataObserver.addObserver(mStore, HealthConstants.StepCount.HEALTH_DATA_TYPE, mObserver);
        readTodayStepCount();
    }

    // Read the today's step count on demand
    private void readTodayStepCount() {
        HealthDataResolver resolver = new HealthDataResolver(mStore, null);

        // Set time range from start time of today to the current time
        long startTime = getStartTimeOfToday();
        long endTime = startTime + ONE_DAY_IN_MILLIS;

         mShealth.mDebug += startTime + " AND " + endTime + "\n";

        HealthDataResolver.ReadRequest request = new ReadRequest.Builder()
                    .setDataType(HealthConstants.StepCount.HEALTH_DATA_TYPE)
                    .setProperties(new String[] {HealthConstants.StepCount.COUNT})
                    .setLocalTimeRange(HealthConstants.StepCount.START_TIME, HealthConstants.StepCount.TIME_OFFSET,
                            startTime, endTime)
                    .build();

        try {
            mShealth.mDebug += "Trying to resolve...\n";
            resolver.read(request).setResultListener(mRdResult);
        } catch (Exception e) {
            mShealth.mDebug += "Getting step count fails: " + e.getMessage() + "\n";
        }
    }

    private long getStartTimeOfToday() {
        Calendar today = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        return today.getTimeInMillis();
    }

    private final HealthResultHolder.ResultListener<ReadResult> mRdResult = new HealthResultHolder.ResultListener<ReadResult>(){

        @Override
        public void onResult(HealthDataResolver.ReadResult result) {
            int count = 0;

            try {
                for (HealthData data : result) {
                    count += data.getInt(HealthConstants.StepCount.COUNT);
                }
            } finally {
                result.close();
            }

            if (mStepCountObserver != null) {
                mStepCountObserver.onChanged(count);
            }
        }
    };

    private final HealthDataObserver mObserver = new HealthDataObserver(null) {

        // Update the step count when a change event is received
        @Override
        public void onChange(String dataTypeName) {
            //Log.d(MainActivity.APP_TAG, "Observer receives a data changed event");
            readTodayStepCount();
        }
    };

    public interface StepCountObserver {
        void onChanged(int count);
    }
}
