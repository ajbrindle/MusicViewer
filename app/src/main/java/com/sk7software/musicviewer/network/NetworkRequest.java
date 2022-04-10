package com.sk7software.musicviewer.network;

import android.content.Context;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sk7software.musicviewer.ApplicationContextProvider;
import com.sk7software.musicviewer.BuildConfig;
import com.sk7software.musicviewer.R;
import com.sk7software.musicviewer.model.MusicFile;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NetworkRequest {

    private static RequestQueue queue;

    private static final String FILE_LIST_URL = "http://www.sk7software.co.uk/sheetmusic/musiclist.php?key=" + BuildConfig.API_KEY;
    private static final String FILE_UPD_URL = "http://www.sk7software.co.uk/sheetmusic/musicupd.php?key=" + BuildConfig.API_KEY;
    private static final String TAG = NetworkRequest.class.getSimpleName();

    public interface NetworkCallback {
        public void onRequestCompleted(List<MusicFile> callbackData);

        public void onError(Exception e);
    }

    private synchronized static RequestQueue getQueue(Context context) {
        if (queue == null) {
            queue = Volley.newRequestQueue(context);
        }
        return queue;
    }

    public static void fetchFiles(final Context context, final NetworkCallback callback) {
        Log.d(TAG, "Fetching PDF file list");
        try {
            JsonArrayRequest jsObjRequest = new JsonArrayRequest
                    (Request.Method.GET, FILE_LIST_URL,
                            null,
                            new Response.Listener<JSONArray>() {
                                @Override
                                public void onResponse(JSONArray response) {
                                    try {
                                        ObjectMapper mapper = new ObjectMapper();
                                        List<MusicFile> files = new ArrayList<>();

                                        for (int i = 0; i < response.length(); i++) {
                                            try {
                                                JSONObject o = response.getJSONObject(i);
                                                MusicFile mf = mapper.readValue(o.toString(), MusicFile.class);
                                                files.add(mf);
                                            } catch (JSONException e) {
                                                Log.e(TAG, "Error getting JSON Object: " + e);
                                            }
                                        }

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
                                    callback.onError(error);
                                }
                            }
                    );
            jsObjRequest.setRetryPolicy(new DefaultRetryPolicy(5000, 1, 1));
            getQueue(context).add(jsObjRequest);
        } catch (Exception e) {
            Log.d(TAG, "Error fetching music files: " + e.getMessage());
        }
    }

    public static void updateFile(final Context context, final MusicFile file, final NetworkCallback callback) {
        Log.d(TAG, "Updating file");
        try {
            Gson gson = new GsonBuilder()
                    .create();
            String json = gson.toJson(file);
            Log.d(TAG, "JSON: " + json);
            JSONObject jsonData = new JSONObject(json);
            JsonObjectRequest jsonRequest = new JsonObjectRequest
                    (Request.Method.POST, FILE_UPD_URL, jsonData,
                            new Response.Listener<JSONObject>() {
                                @Override
                                public void onResponse(JSONObject response) {
                                    callback.onRequestCompleted(null);
                                }
                            },
                            new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {
                                    Log.d(TAG, "Error => " + error.toString());
                                    callback.onError(error);
                                }
                            }
                    );
            jsonRequest.setRetryPolicy(new DefaultRetryPolicy(5000, 1, 1));
            getQueue(context).add(jsonRequest);
        } catch (Exception e) {
            Log.d(TAG, "Error updating music file: " + e.getMessage());
        }
    }
}