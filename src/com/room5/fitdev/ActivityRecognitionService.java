package com.room5.fitdev;

import java.util.List;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import android.app.IntentService;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

public class ActivityRecognitionService extends IntentService {

    private String TAG = this.getClass().getSimpleName();

    public ActivityRecognitionService() {
        super("My Activity Recognition Service");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (ActivityRecognitionResult.hasResult(intent)) {
            ActivityRecognitionResult result = ActivityRecognitionResult
                    .extractResult(intent);
            Log.i(TAG, getType(result.getMostProbableActivity().getType())
                    + "\t" + result.getMostProbableActivity().getConfidence());
            Intent i = new Intent(
                    "com.room5.fitdev.ACTIVITY_RECOGNITION_DATA");

            // Get the most probable activity from the list of activities in the
            // update
            DetectedActivity mostProbableActivity = result
                    .getMostProbableActivity();

            // Get the type of activity
            int activityType = mostProbableActivity.getType();

            // If the activity is "on foot", determine if it is walking or running
            if (activityType == DetectedActivity.ON_FOOT) {
                DetectedActivity betterActivity = walkingOrRunning(result
                        .getProbableActivities());
                if (null != betterActivity)
                    mostProbableActivity = betterActivity;
            }

            i.putExtra("Activity", getType(mostProbableActivity.getType()));
            i.putExtra("Confidence", result.getMostProbableActivity()
                    .getConfidence());
            sendBroadcast(i);
        }
    }

    private String getType(int type) {
        if (type == DetectedActivity.UNKNOWN)
            return "Unknown";
        else if (type == DetectedActivity.IN_VEHICLE)
            return "In Vehicle";
        else if (type == DetectedActivity.ON_BICYCLE)
            return "On Bicycle";
        else if (type == DetectedActivity.STILL) {
            // PackageManager pm = getPackageManager();
            // try {
            // String packageName = "com.fitnesskeeper.runkeeper.pro";
            // Intent launchIntent = pm.getLaunchIntentForPackage(packageName);
            // launchIntent.addCategory(Intent.ACTION_MAIN);
            // startActivity(launchIntent);
            // } catch (Exception e1) {
            // Log.e("ecooksey", "Exception starting intent: " +
            // e1.getMessage());
            // }
            return "Still";
        } else if (type == DetectedActivity.TILTING)
            return "Tilting";
        else if (type == DetectedActivity.WALKING) {
            // PackageManager pm = getPackageManager();
            // try {
            // String packageName = "com.pandora.android";
            // Intent launchIntent = pm.getLaunchIntentForPackage(packageName);
            // startActivity(launchIntent);
            // } catch (Exception e1) {
            // }
            return "Walking";
        } else if (type == DetectedActivity.RUNNING)
            return "Running";
        else if (type == DetectedActivity.ON_FOOT)
            return "On Foot";
        else
            return "";
    }

    /**
     * Private helper method to determine if, when on foot, the user is walking
     * or running.
     * 
     * @param probableActivities The list returned by getProbableActivities
     * @return The activity "walking" or "running", depending on which is more likely.
     */
    private DetectedActivity walkingOrRunning(
            List<DetectedActivity> probableActivities) {
        DetectedActivity myActivity = null;
        int confidence = 0;
        for (DetectedActivity activity : probableActivities) {
            if (activity.getType() != DetectedActivity.RUNNING
                    && activity.getType() != DetectedActivity.WALKING)
                continue;

            if (activity.getConfidence() > confidence)
                myActivity = activity;
        }

        return myActivity;
    }

}
