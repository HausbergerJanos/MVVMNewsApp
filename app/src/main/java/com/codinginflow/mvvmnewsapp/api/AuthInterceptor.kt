package com.codinginflow.mvvmnewsapp.api

import com.codinginflow.mvvmnewsapp.BuildConfig
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor : Interceptor {

    private var token: String = ""

    init {
        GlobalScope.launch {
            // Get data for example from cache
            token = BuildConfig.NEWS_API_ACCESS_KEY
        }
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()

        if (request.header("No-Authentication") == null) {
            //val token = getTokenFromSharedPreference();
            //or use Token Function
            if (token.isNotEmpty()) {
                request = request.newBuilder()
                    .addHeader("X-Api-Key", token)
                    .build()
            }

        }

        return chain.proceed(request)
    }
}