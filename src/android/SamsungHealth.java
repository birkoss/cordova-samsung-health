package com.birkoss.plugin;

import org.apache.cordova.*;
import org.json.JSONArray;
import org.json.JSONException;
import android.app.Activity;

import com.samsung.android.sdk.*;
import com.samsung.android.sdk.healthdata.*;
import com.samsung.android.sdk.healthdata.HealthPermissionManager.*;    

import java.util.Set;
import java.util.HashSet;
import java.util.Map;

import android.util.Log;
import javax.json.*;


public class SamsungHealth extends CordovaPlugin {

    private Activity mActivity = null;

    private HealthDataStore mStore;
    private HealthConnectionErrorResult mConnError;

    private Set<PermissionKey> mKeySet;

    private StepCountReporter mReporter;

    private SamsungHealth mShealth;
    public String mDebug = "";

    private CallbackContext mCallbackContext;

    @Override
    public boolean execute(String action, JSONArray data, CallbackContext callbackContext) throws JSONException {

        if (mShealth == null) {
            mShealth = this;
        }

        if (mActivity == null) {
            mActivity = this.cordova.getActivity();
        }

        mCallbackContext = callbackContext;

        if (action.equals("debug")) {
            callbackContext.success("Hello " + data.getString(0) + "!\n\n" + mDebug);

            return true;
        } else if (action.equals("askPermissions")) {

            healthServiceSetup();

            healthStoreSetup();

            return true;
        } else if (action.equals("getData")) {

            mDebug += "Init the connection...\n";

            mKeySet = new HashSet<PermissionKey>();
            mKeySet.add(new PermissionKey(HealthConstants.StepCount.HEALTH_DATA_TYPE, PermissionType.READ));
            mKeySet.add(new PermissionKey("com.samsung.shealth.step_daily_trend", PermissionType.READ));

            HealthDataService healthDataService = new HealthDataService();
            try {
                healthDataService.initialize(mActivity.getApplicationContext());
                mDebug += "Is initialized\n";
            } catch (Exception e) {
                mDebug += e.toString() + "\n";
            }

            mDebug += "Requesting the connection to the health data store\n";    
            try {
                mStore = new HealthDataStore(mActivity.getApplicationContext(), mConnectionListener);
                mStore.connectService();
            } catch (Exception e) {
                mDebug += e.toString() + "\n";
            }

            mDebug += "Connection requested\n";

            return true;
        } else if (action.equals("getSteps")) {

            getSteps();

            return true;
        } else {
            callbackContext.success("Action Not Found!");

            return false;
        }
    }


    private final HealthDataStore.ConnectionListener mConnectionListener = new HealthDataStore.ConnectionListener() {

        @Override
        public void onConnected() {
            //mCallbackContext.success("Health data service is connected.");
            checkPermissions();
        }

        @Override
        public void onConnectionFailed(HealthConnectionErrorResult error) {
            String message = "Connection with Samsung Health is not available";

            if (error.hasResolution()) {
                switch(error.getErrorCode()) {
                    case HealthConnectionErrorResult.PLATFORM_NOT_INSTALLED:
                        message = "Please install Samsung Health";
                        break;
                    case HealthConnectionErrorResult.OLD_VERSION_PLATFORM:
                        message = "Please upgrade Samsung Health";
                        break;
                    case HealthConnectionErrorResult.PLATFORM_DISABLED:
                        message = "Please enable Samsung Health";
                        break;
                    case HealthConnectionErrorResult.USER_AGREEMENT_NEEDED:
                        message = "Please agree with Samsung Health policy";
                        break;
                    default:
                        message = "Please make Samsung Health available";
                        break;
                }
            }

            mCallbackContext.error(message);
        }

        @Override
        public void onDisconnected() {
            mCallbackContext.error("Health data service is disconnected.");
        }
    };


    public StepCountReporter.StepCountObserver mStepCountObserver = new StepCountReporter.StepCountObserver() {
        @Override
        public void onChanged(String json) {
            mCallbackContext.success(json.toString());
        }
    };

    private final HealthResultHolder.ResultListener<PermissionResult> mPermissionListener = new HealthResultHolder.ResultListener<PermissionResult>() {
        @Override
        public void onResult(PermissionResult result) {
            Map<PermissionKey, Boolean> resultMap = result.getResultMap();

            if (resultMap.containsValue(Boolean.FALSE)) {
                mCallbackContext.error("Requesting permission fails...");
            } else {
                mCallbackContext.success("Get steps...");
            }
        }
    };


    private void initPermissions() {
        mKeySet = new HashSet<PermissionKey>();
        mKeySet.add(new PermissionKey(HealthConstants.StepCount.HEALTH_DATA_TYPE, PermissionType.READ));
        mKeySet.add(new PermissionKey("com.samsung.shealth.step_daily_trend", PermissionType.READ));
    }

    private void healthServiceSetup() {
        try {
            HealthDataService healthDataService = new HealthDataService();
            healthDataService.initialize(mActivity.getApplicationContext());
        } catch (Exception e) {
            mCallbackContext.error(e.toString());
        }
    }

    private void healthStoreSetup() {
        try {
            mStore = new HealthDataStore(mActivity.getApplicationContext(), mConnectionListener);
            mStore.connectService();
        } catch (Exception e) {
            mCallbackContext.error(e.toString());
        }
    }

    private void getSteps() {
        mReporter = new StepCountReporter(mStore, mShealth);
        mReporter.start(mStepCountObserver);
    }

    private void checkPermissions() {
        try {
            initPermissions();

            HealthPermissionManager pmsManager = new HealthPermissionManager(mStore);

            Map<PermissionKey, Boolean> resultMap = pmsManager.isPermissionAcquired(mKeySet);

            if (resultMap.containsValue(Boolean.FALSE)) {
                pmsManager.requestPermissions(mKeySet, mActivity).setResultListener(mPermissionListener);
            } else {
                mCallbackContext.success("Get the current step count and display it");
            }
        } catch (Exception e) {
            mCallbackContext.error("Permission setting fails: " + e.getClass().getName() + " - " + e.getMessage());
        }
    }
}