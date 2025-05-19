package com.example.servicios;

import android.content.Context;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

public class ConexionHelper {

    public static final String BASE_URL = "http://172.100.8.99";

    // Cola de peticiones Volley (singleton)
    private static RequestQueue requestQueue;

    public static RequestQueue getInstance(Context context) {
        if (requestQueue == null) {
            requestQueue = Volley.newRequestQueue(context.getApplicationContext());
        }
        return requestQueue;
    }
}
