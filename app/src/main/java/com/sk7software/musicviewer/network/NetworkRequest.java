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
import com.sk7software.musicviewer.model.MusicAnnotation;
import com.sk7software.musicviewer.model.MusicFile;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NetworkRequest {

    private static RequestQueue queue;

    private static final String FILE_LIST_URL = "http://www.sk7software.co.uk/sheetmusic/musiclist.php?key=" + BuildConfig.API_KEY;
    private static final String FILE_UPD_URL = "http://www.sk7software.co.uk/sheetmusic/musicupd.php?key=" + BuildConfig.API_KEY;
    private static final String FILE_INFO_UPD_URL = "http://www.sk7software.co.uk/sheetmusic/setdesc.php?key=" + BuildConfig.API_KEY;
    private static final String GET_ANNOTATIONS_URL = "http://www.sk7software.co.uk/sheetmusic/annotations.php";
    private static final String IMAGE_URL = "https://www.googleapis.com/customsearch/v1?key=" + BuildConfig.GOOGLE_API_KEY + "&cx=" + BuildConfig.SEARCH_ENGINE_ID +
            "&searchType=image&imgSize=xlarge";
    private static final String TAG = NetworkRequest.class.getSimpleName();

    public interface NetworkCallback {
        public void onRequestCompleted(Object callbackData);

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

    public static void updateFileInfo(final Context context, final int id, final String artist, final String title, final NetworkCallback callback) {
        Log.d(TAG, "Updating file info");
        try {
            String json = "{\"id\":" + id + ",\"artist\":\"" + artist + "\",\"title\":\"" + title + "\"}";
            JSONObject jsonData = new JSONObject(json);
            JsonObjectRequest jsonRequest = new JsonObjectRequest
                    (Request.Method.POST, FILE_INFO_UPD_URL, jsonData,
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

    public static void fetchAnnotations(final Context context, final int id, final NetworkCallback callback) {
        Log.d(TAG, "Fetching annotations for id " + id);
        try {
            JsonArrayRequest jsObjRequest = new JsonArrayRequest
                    (Request.Method.GET, GET_ANNOTATIONS_URL + "?id=" + id,
                            null,
                            new Response.Listener<JSONArray>() {
                                @Override
                                public void onResponse(JSONArray response) {
                                    List<MusicAnnotation> annotations = new ArrayList<>();

                                    for (int i = 0; i < response.length(); i++) {
                                        try {
                                            MusicAnnotation annotation = MusicAnnotation.fromJson(response.getJSONObject(i));
                                            if (!annotation.getPoints().isEmpty()) {
                                                annotations.add(annotation);
                                            }
                                        } catch (JSONException e) {
                                            Log.e(TAG, "Error getting JSON Object: " + e);
                                        }
                                    }
                                    callback.onRequestCompleted(annotations);
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

    public static void findImage(final Context context, final String title, final String artist, final NetworkCallback callback) {
        Log.d(TAG, "Finding random image");
        try {
            JsonObjectRequest jsObjRequest = new JsonObjectRequest
                    (Request.Method.GET, IMAGE_URL +
                            "&q=" + URLEncoder.encode(title + " " + artist, "UTF-8") +
                            "&exactTerms=" + URLEncoder.encode(artist, "UTF-8") +
                            "&excludeTerms=" + URLEncoder.encode("sheet music", "UTF-8") +
                            "&filter=1&num=10",
                            null,
                            new Response.Listener<JSONObject>() {
                                @Override
                                public void onResponse(JSONObject response) {
                                    try {
                                        JSONArray items = response.getJSONArray("items");
                                        for (int i=0; i<10; i++) {
                                            int random = (int)(Math.random() * items.length());
                                            String mimeType = items.getJSONObject(random).getString("mime");
                                            if (mimeType.endsWith("jpeg") || mimeType.endsWith("jpg") || mimeType.endsWith("png")) {
                                                callback.onRequestCompleted(items.getJSONObject(random).getString("link"));
                                                return;
                                            }
                                        }
                                        callback.onError(new Exception("No JPEG image found"));
                                    } catch (JSONException e) {
                                        Log.d(TAG, "Error getting images: " + e.getMessage());
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
            Log.d(TAG, "Error fetching images: " + e.getMessage());
        }
    }

}