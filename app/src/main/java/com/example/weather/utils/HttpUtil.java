package com.example.weather.utils;

import okhttp3.OkHttpClient;
import okhttp3.Request;

public class HttpUtil {

    /*
    发起HTTP请求，传入请求地址，注册回调
     */
    public static void sendOkHttpRequest(String address, okhttp3.Callback callback) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(address).build();
        client.newCall(request).enqueue(callback);
    }
}
