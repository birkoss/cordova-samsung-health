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
                    // https://developer.samsung.com/forum/thread/retrieve-how-many-steps-were-walked-per-day-for-the-past-7-days/201/279929?boardName=SDK&startId=zzzzz~&searchSubId=0000000026
                  

                    mDebug += "Get the current step count and display it : " + HealthConstants.StepCount.COUNT + "\n";
                }


                mReporter = new StepCountReporter(mStore, mShealth);

                mReporter.start(mStepCountObserver);
                mDebug += "Reporter started...\n";

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


    public StepCountReporter.StepCountObserver mStepCountObserver = new StepCountReporter.StepCountObserver() {
        @Override
        public void onChanged(String json) {
            //mDebug += "Step reported : " + count + "\n";

            JsonObject jo = Json.createObjectBuilder()
              .add("employees", Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                  .add("firstName", "John")
                  .add("lastName", "Doe")))
              .build();

            mCallbackContext.success(json.toString());
        }
    };

    private final HealthResultHolder.ResultListener<PermissionResult> mPermissionListener = new HealthResultHolder.ResultListener<PermissionResult>() {
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