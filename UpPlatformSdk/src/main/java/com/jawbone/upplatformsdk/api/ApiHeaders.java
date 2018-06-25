/**
 * @author Omer Muhammed
 * Copyright 2014 (c) Jawbone. All rights reserved.
 */
package com.jawbone.upplatformsdk.api;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Small class to dynamically add the required headers to the API calls.
 */
public class ApiHeaders implements Interceptor {
    private String accessToken;

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public void clearAccessToken() {
        accessToken = null;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        if (accessToken != null) {
            Request newRequest = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .addHeader("Accept", "application/json")
                    .build();
            return chain.proceed(newRequest);
        } else throw new IOException("no access token");
    }
}