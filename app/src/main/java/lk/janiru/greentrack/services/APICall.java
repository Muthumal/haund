package lk.janiru.greentrack.services;

/*
 *
 * Project Name : ${PROJECT}
 * Created by Janiru on 4/3/2019 7:46 PM.
 *
 */

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONObject;

import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import lk.janiru.greentrack.main.ui.MainActivity;

public class APICall extends AsyncTask<String, String, String> {

    private String TAG = "API CALL";

    @Override
    protected String doInBackground(String... strings) {
        try {
            int i = sendPost();
            if (i == 400) {
                sendPost();
            } else if (i == 200) {
                Log.d(TAG, "doInBackground: Sucessfull ***************************************");
            }
        } catch (Exception e) {

        }


        return null;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        MainActivity.notificationStatus = false;
        Log.d(TAG, "onPreExecute: Called");
    }




    public int sendPost() throws Exception {
        URL url = new URL("https://fcm.googleapis.com/fcm/send");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        try {

            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            conn.setRequestProperty("Authorization", "key=AAAAs45Fskk:APA91bGcxsyhbCXFkd5Myo_vTFdM9jUxg5MO6SFhbLGb8kPx_vbBR2ulIxiNvCtnLgV3wXZMj-gJLS1eOK-adH-eFVMqRSCdVatpFg2mHRTJZ_QiVsBo3-md7HRWmO4CspwNZLDsrl9G");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);
            conn.setDoInput(true);

            JSONObject jsonObj = new JSONObject();
            jsonObj.put("title", "Hi there");
            jsonObj.put("text", "test");
            jsonObj.put("click_action", "android.intent.action.MAIN");

            JSONObject jsonParam = new JSONObject();
            jsonParam.put("to", "/topics/BUS_IS_NEAR_BY");
            jsonParam.put("data", "");
            jsonParam.put("notification", jsonObj);


            Log.i("JSON", jsonParam.toString());
            DataOutputStream os = new DataOutputStream(conn.getOutputStream());
            //os.writeBytes(URLEncoder.encode(jsonParam.toString(), "UTF-8"));
            os.writeBytes(jsonParam.toString());

            os.flush();
            os.close();

            Log.i("STATUS", String.valueOf(conn.getResponseCode()));
            Log.i("MSG", conn.getResponseMessage());

            MainActivity.notificationStatus = false;

            LocationServices.threadStatus = false;


//                    conn.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }


        return conn.getResponseCode();

    }


}
