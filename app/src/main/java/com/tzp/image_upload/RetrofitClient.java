package com.tzp.image_upload;


import com.google.gson.Gson;

import java.util.concurrent.TimeUnit;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static RetrofitClient myClient;
    private Retrofit retrofit;


    public static synchronized RetrofitClient getInstance() {
        if (myClient == null) {
            myClient = new RetrofitClient();
        }
        return myClient;
    }
    public RetrofitApiService getApi() {
        if (retrofit==null){
            retrofit = new Retrofit.Builder()
                    .baseUrl("https://www.cookiebytes.ca/")
                    .client(getOkHttpClient())
                    .addConverterFactory(GsonConverterFactory.create(new Gson()))
                    .build();
        }
        return retrofit.create(RetrofitApiService.class);
    }
    private static OkHttpClient getOkHttpClient() {
        return new OkHttpClient()
                .newBuilder()
                .readTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .addInterceptor(getHttpLoginInterceptor())
                .build();
    }
    private static Interceptor getHttpLoginInterceptor() {
        return new HttpLoggingInterceptor()
                .setLevel(HttpLoggingInterceptor
                        .Level
                        .BODY);
    }

}
