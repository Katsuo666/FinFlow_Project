package com.example.api

import android.content.Context
import com.example.utils.TokenManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = "https://finflow-backend-production-5d94.up.railway.app/"

    private var retrofit: Retrofit? = null

    fun reset() {
        retrofit = null
    }

    fun getApiService(context: Context): ApiService {
        if (retrofit == null) {
            val tokenManager = TokenManager(context.applicationContext)

            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .addInterceptor { chain ->
                    val request = chain.request()
                    val requestBuilder = request.newBuilder()
                    requestBuilder.addHeader("Content-Type", "application/json")
                    requestBuilder.addHeader("Accept", "application/json")
                    
                    val isAuthRoute = request.url.encodedPath.contains("/api/auth")
                    val token = tokenManager.getToken()
                    
                    if (!token.isNullOrEmpty() && !isAuthRoute) {
                        requestBuilder.addHeader("Authorization", "Bearer $token")
                    }
                    chain.proceed(requestBuilder.build())
                }
                .build()

            retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return retrofit!!.create(ApiService::class.java)
    }
}
