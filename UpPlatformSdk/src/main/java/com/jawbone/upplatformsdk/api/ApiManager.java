/**
 * @author Omer Muhammed
 * Copyright 2014 (c) Jawbone. All rights reserved.
 */
package com.jawbone.upplatformsdk.api;

import com.jawbone.upplatformsdk.utils.UpPlatformSdkConstants;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;

/**
 * Main class that handles all the API calls
 */
public class ApiManager {

    private static Retrofit restAdapter;
    private static RestApiInterface restApiInterface;
    private static ApiHeaders restApiHeaders;

    private static Retrofit getRestAdapter() {
        if (restAdapter == null) {
            OkHttpClient httpClient = new OkHttpClient.Builder().addInterceptor(getRequestInterceptor()).build();

            restAdapter = new Retrofit.Builder()
                    .baseUrl(UpPlatformSdkConstants.API_URL)
                    .client(httpClient)
                    //.setErrorHandler(new CustomErrorHandler())
                    .build();
        }
        return restAdapter;
    }

    public static RestApiInterface getRestApiInterface() {
        restAdapter = getRestAdapter();
        if (restApiInterface == null) {
            restApiInterface = restAdapter.create(RestApiInterface.class);
        }
        return restApiInterface;
    }

    public static ApiHeaders getRequestInterceptor() {
        if (restApiHeaders == null) {
            restApiHeaders = new ApiHeaders();
        }
        return restApiHeaders;
    }

    /*
    //TODO make this more robust
    private static class CustomErrorHandler implements ErrorHandler {
        @Override
        public Throwable handleError(RetrofitError cause) {
            Response r = cause.getResponse();
            if (r != null && r.getStatus() == 401) {
                return cause.getCause();
            }
            return cause;
        }
    }*/
}