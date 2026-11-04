package com.example.chabi.maplibre

import android.content.Context
import okhttp3.Cache
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import org.maplibre.android.MapLibre
import org.maplibre.android.module.http.HttpRequestUtil
import java.io.File
import java.util.concurrent.TimeUnit

internal object MapRuntime {
    private var initialized = false
    @Synchronized fun initialize(context: Context) {
        if (initialized) return
        MapLibre.getInstance(context.applicationContext)
        val dispatcher = Dispatcher().apply { maxRequestsPerHost = 4 }
        HttpRequestUtil.setOkHttpClient(OkHttpClient.Builder()
            .dispatcher(dispatcher)
            .cache(Cache(File(context.cacheDir, "map-http"), 50L * 1024 * 1024))
            .connectTimeout(15, TimeUnit.SECONDS).readTimeout(25, TimeUnit.SECONDS)
            .addInterceptor { chain -> chain.proceed(chain.request().newBuilder()
                .header("User-Agent", "Chabi/0.6 (com.example.chabi; Android Automotive)").build()) }
            .build())
        initialized = true
    }
}
