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

class PfcApiClient(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("pfc_auth_prefs", Context.MODE_PRIVATE)

    private val cookieJar = object : CookieJar {
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            val savedSet = prefs.getStringSet("all_cookies", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
            cookies.forEach { cookie ->
                // Remove older version of same cookie name on host
                savedSet.removeAll { oldStr ->
                    val oldCookie = Cookie.parse(url, oldStr)
                    oldCookie?.name == cookie.name
                }
                savedSet.add(cookie.toString())
            }
            prefs.edit().putStringSet("all_cookies", savedSet).apply()
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            val savedSet = prefs.getStringSet("all_cookies", emptySet()) ?: emptySet()
            return savedSet.mapNotNull { Cookie.parse(url, it) }
        }
    }

    private val okHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    fun getBaseUrl(): String {
        var url = prefs.getString("backend_base_url", "https://portale-pfc-v2.vercel.app/") ?: "https://portale-pfc-v2.vercel.app/"
        if (!url.endsWith("/")) url += "/"
        return url
    }

    fun setBaseUrl(newUrl: String) {
        var url = newUrl.trim()
        if (!url.endsWith("/")) url += "/"
        prefs.edit().putString("backend_base_url", url).apply()
    }

    val apiService: PfcApiService
        get() {
            return Retrofit.Builder()
                .baseUrl(getBaseUrl())
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(PfcApiService::class.java)
        }

    fun clearSession() {
        prefs.edit().remove("all_cookies").remove("saved_cookie").apply()
    }

    fun hasValidSession(): Boolean {
        val cookies = prefs.getStringSet("all_cookies", emptySet()) ?: emptySet()
        return cookies.isNotEmpty()
    }
}
