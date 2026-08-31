package com.example.data.remote

import android.content.Context
import android.content.SharedPreferences
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class PfcApiClient(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("pfc_auth_prefs", Context.MODE_PRIVATE)

    private val cookieJar = object : CookieJar {
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            val sessionCookie = cookies.firstOrNull { it.name == "pfc_session_cookie" || it.name.contains("session") }
            if (sessionCookie != null) {
                prefs.edit().putString("saved_cookie", sessionCookie.toString()).apply()
            }
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            val saved = prefs.getString("saved_cookie", null) ?: return emptyList()
            val parsed = Cookie.parse(url, saved)
            return if (parsed != null) listOf(parsed) else emptyList()
        }
    }

    private val okHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    val apiService: PfcApiService = Retrofit.Builder()
        .baseUrl("https://portale-pfc-v2.vercel.app/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(PfcApiService::class.java)

    fun clearSession() {
        prefs.edit().remove("saved_cookie").apply()
    }

    fun getSavedCookie(): String? = prefs.getString("saved_cookie", null)
}
