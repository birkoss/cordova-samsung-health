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

public class SamsungHealth extends CordovaPlugin {

    String APP_TAG = "CordovaSamsungHealthPlugin";

    private Activity mActivity = null;

    private HealthDataStore mStore;
    private HealthConnectionErrorResult mConnError;
    private Set<PermissionKey> mKeySet;

    private String mDebug = "";

    @Override
    public boolean execute(String action, JSONArray data, CallbackContext callbackContext) throws JSONException {

        if (mActivity == null) {
            mActivity = this.cordova.getActivity();
        }

        if (action.equals("greet")) {

            String name = data.getString(0);
            String message = "Hello " + name + "!\n\n" + mDebug;
            callbackContext.success(message);

            return true;

        } else if (action.equals("connect")) {
            mDebug += "Init the connection...\n";
            mKeySet = new HashSet<PermissionKey>();
            mKeySet.add(new PermissionKey(HealthConstants.StepCount.HEALTH_DATA_TYPE, PermissionType.READ));
            HealthDataService healthDataService = new HealthDataService();
            try {
                healthDataService.initialize(mActivity.getApplicationContext());
                mDebug += "Is initialized\n";
            } catch (Exception e) {
                mDebug += e.toString() + "\n";
            }

            mDebug += "Starting the connection...\n";    

            try {
                mStore = new HealthDataStore(mActivity.getApplicationContext(), mConnectionListener);
                // Request the connection to the health data store
                mStore.connectService();
            } catch (Exception e) {
                mDebug += e.toString() + "\n";
            }

            mDebug += "After connection...\n";

            return true;
        } else {
            callbackContext.success("Action Not Found!");

            return false;
        }
    }


    private final HealthDataStore.ConnectionListener mConnectionListener = new HealthDataStore.ConnectionListener() {

        @Override
        public void onConnected() {
            mDebug += "Health data service is connected.\n";
            HealthPermissionManager pmsManager = new HealthPermissionManager(mStore);

            try {
                // Check whether the permissions that this application needs are acquired
                mDebug += "Testing permissions\n";
                Map<PermissionKey, Boolean> resultMap = pmsManager.isPermissionAcquired(mKeySet);
                mDebug += "IsPermissionAcquired.\n";

                if (resultMap.containsValue(Boolean.FALSE)) {
                    mDebug += "requestPermissions start\n";
                    // Request the permission for reading step counts if it is not acquired
                    pmsManager.requestPermissions(mKeySet, mActivity).setResultListener(mPermissionListener);
                    mDebug += "requestPermissions end\n";
                } else {
                    // Get the current step count and display it
                    // ...
                    mDebug += "Get the current step count and display it";
                }
            } catch (Exception e) {
                mDebug += e.getClass().getName() + " - " + e.getMessage() + "\n";
                mDebug += "Permission setting fails.\n";
            }
        }

        @Override
        public void onConnectionFailed(HealthConnectionErrorResult error) {
            mDebug += "Health data service is not available.\n";
            showConnectionFailureDialog(error);
        }

        @Override
        public void onDisconnected() {
            mDebug += "Health data service is disconnected.\n";
        }
    };


    private final HealthResultHolder.ResultListener<PermissionResult> mPermissionListener =
            new HealthResultHolder.ResultListener<PermissionResult>() {

            @Override
            public void onResult(PermissionResult result) {
                mDebug += "Permission callback is received.\n";
                Map<PermissionKey, Boolean> resultMap = result.getResultMap();

                if (resultMap.containsValue(Boolean.FALSE)) {
                    // Requesting permission fails
                    mDebug += "Requesting permission fails...\n";
                } else {
                    // Get the current step count and display it
                    mDebug += "get steps...\n";
                }
            }
        };
    


    private void showConnectionFailureDialog(HealthConnectionErrorResult error) {

        mConnError = error;
        String message = "Connection with Samsung Health is not available";

        if (mConnError.hasResolution()) {
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

        mDebug += message + "\n";
    }


}