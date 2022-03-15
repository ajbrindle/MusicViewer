package com.sk7software.musicviewer.network;

import android.content.Context;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NetworkRequest {

    private static RequestQueue queue;

    private static final String FILE_LIST_URL = "http://www.sk7software.co.uk/sheetmusic/musiclist.php";
    private static final String TAG = NetworkRequest.class.getSimpleName();

    public interface NetworkCallback {
        public void onRequestCompleted(List<String> callbackData);
        public void onError(Exception e);
    }

    private synchronized static RequestQueue getQueue(Context context) {
        if (queue == null) {
            queue = Volley.newRequestQueue(context);
        }
        return queue;
    }

    public static void fetchFiles(final Context context, final NetworkCallback callback) {
        Log.d(TAG, "Fetching GPX file list");
        try {
//            uiUpdate.setProgress(true, "Fetching file list");
            JsonArrayRequest jsObjRequest = new JsonArrayRequest
                    (Request.Method.GET, FILE_LIST_URL,
                            null,
                            new Response.Listener<JSONArray>() {
                                @Override
                                public void onResponse(JSONArray response) {
                                    try {
                                        ObjectMapper mapper = new ObjectMapper();
                                        List<String> files = new ArrayList<>();
                                        files = mapper.readValue(response.toString(), ArrayList.class);

//                                        uiUpdate.setProgress(false, null);
                                        callback.onRequestCompleted(files);
                                    } catch (JsonProcessingException e) {
                                        Log.d(TAG, "Error getting dev messages: " + e.getMessage());
                                    }
                                }
                            },
                            new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {
                                    Log.d(TAG, "Error => " + error.toString());
//                                    uiUpdate.setProgress(false, null);
                                    callback.onError(error);
                                }
                            }
                    );
            jsObjRequest.setRetryPolicy(new DefaultRetryPolicy(5000, 1, 1));
            getQueue(context).add(jsObjRequest);
        } catch (Exception e) {
            Log.d(TAG, "Error fetching GPX route: " + e.getMessage());
        }
    }
}
