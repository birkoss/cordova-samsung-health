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

import javax.json.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
       // long startTime = getStartTimeOfToday();
       // long endTime = startTime + ONE_DAY_IN_MILLIS;



        HealthDataResolver.ReadRequest request = new ReadRequest.Builder()
                    .setDataType("com.samsung.shealth.step_daily_trend")
                    //.setProperties(new String[] {"com.samsung.shealth.step_daily_trend"})
                    //.setLocalTimeRange(HealthConstants.StepCount.START_TIME, HealthConstants.StepCount.TIME_OFFSET, startTime, endTime)
                    //.setFilter(filter)
                    .setSort("day_time", HealthDataResolver.SortOrder.DESC)
                    .build();

        try {
            mShealth.mDebug += "Trying to resolve...\n";
            resolver.read(request).setResultListener(mRdResult);
        } catch (Exception e) {
            mShealth.mDebug += "Getting step count fails: " + e.getMessage() + "\n";
        }
    }

    private final HealthResultHolder.ResultListener<ReadResult> mRdResult = new HealthResultHolder.ResultListener<ReadResult>(){

        @Override
        public void onResult(HealthDataResolver.ReadResult result) {

            JSONArray jsonSteps = new JSONArray();

            try {
                for (HealthData data : result) {
                    String device_uuid = data.getString(HealthConstants.StepCount.DEVICE_UUID);
                    int count = data.getInt(HealthConstants.StepCount.COUNT);
                    int source_type = data.getInt("source_type");
                    long time = data.getLong("create_time");

                    if (source_type == -2) {
                        continue;
                    }

                    try {
                        JSONObject device = null;

                        for (int i = 0; i < jsonSteps.length(); i++) {
                            JSONObject tmp = jsonSteps.getJSONObject(i);
                            if (tmp.getString("uuid").equals(device_uuid)) {
                                device = tmp;
                                break;
                            }
                        }

                        if (device == null) {
                            device = new JSONObject();
                            device.put("uuid", device_uuid);
                            device.put("steps", new JSONArray());
                            device.put("type", source_type);

                            jsonSteps.put(device);
                        }

                        JSONObject step = new JSONObject();
                        step.put("time", time);
                        step.put("count", count);

                        device.getJSONArray("steps").put(step);
                    } catch (JSONException e1) {
                        // TODO Auto-generated catch block
                      e1.printStackTrace();
                    }

                    if (data.getString(HealthConstants.StepCount.DEVICE_UUID).equals("ALV976sVxD") || 1 == 1) {
                        count += data.getInt(HealthConstants.StepCount.COUNT);
                        mShealth.mDebug += "UUID: (" + data.getString(HealthConstants.StepCount.DEVICE_UUID) + ")\n";
                        mShealth.mDebug += "Count: " + data.getInt(HealthConstants.StepCount.COUNT) + "\n";
                        mShealth.mDebug += "TIME: " + data.getString(HealthConstants.StepCount.CREATE_TIME) + "\n";
                        mShealth.mDebug += "Source Type PKG: " + data.getInt("source_pkg_name") + "\n";
                        mShealth.mDebug += "Source Type: " + data.getInt("source_type") + "\n";
                        //mShealth.mDebug += "DAY TIME: " + data.getLong(c.getColumnIndex("day_time")) + "\n";
                        mShealth.mDebug += "CCOUNT: " + data.getString("com.samsung.shealth.step_daily_trend.datauuid") + "\n";
                        mShealth.mDebug += "-------------------------\n";
                    }
                }
            } finally {
                result.close();
            }


            if (mStepCountObserver != null) {
                mStepCountObserver.onChanged(jsonSteps.toString());
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
        void onChanged(String json);
    }
}
