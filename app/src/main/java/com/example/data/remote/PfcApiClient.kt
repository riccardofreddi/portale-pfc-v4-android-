package com.example.data.remote

import android.content.Context
import android.content.SharedPreferences
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class PfcApiClient(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("pfc_auth_prefs", Context.MODE_PRIVATE)

    private val cookieMap = ConcurrentHashMap<String, String>()

    init {
        // Load persisted cookies into memory
        val savedCookieHeader = prefs.getString("raw_cookie_header", "") ?: ""
        if (savedCookieHeader.isNotBlank()) {
            savedCookieHeader.split(";").forEach { pair ->
                val trimmed = pair.trim()
                val eqIdx = trimmed.indexOf('=')
                if (eqIdx > 0) {
                    val k = trimmed.substring(0, eqIdx).trim()
                    val v = trimmed.substring(eqIdx + 1).trim()
                    if (k.isNotEmpty() && v.isNotEmpty()) {
                        cookieMap[k] = v
                    }
                }
            }
        }
    }

    private fun persistCookies() {
        val headerVal = cookieMap.entries.joinToString("; ") { "${it.key}=${it.value}" }
        prefs.edit().putString("raw_cookie_header", headerVal).apply()
    }

    private val cookieInterceptor = Interceptor { chain ->
        val original = chain.request()
        val builder = original.newBuilder()

        // Attach Cookie header if cookies exist
        if (cookieMap.isNotEmpty()) {
            val cookieHeader = cookieMap.entries.joinToString("; ") { "${it.key}=${it.value}" }
            builder.header("Cookie", cookieHeader)
        }

        val response = chain.proceed(builder.build())

        // Save Set-Cookie from response
        val setCookieHeaders = response.headers("Set-Cookie")
        if (setCookieHeaders.isNotEmpty()) {
            var updated = false
            for (header in setCookieHeaders) {
                val parts = header.split(";")
                if (parts.isNotEmpty()) {
                    val pair = parts[0].trim()
                    val eqIdx = pair.indexOf('=')
                    if (eqIdx > 0) {
                        val name = pair.substring(0, eqIdx).trim()
                        val value = pair.substring(eqIdx + 1).trim()
                        if (name.isNotEmpty()) {
                            cookieMap[name] = value
                            updated = true
                        }
                    }
                }
            }
            if (updated) {
                persistCookies()
            }
        }

        response
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(cookieInterceptor)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
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
        cookieMap.clear()
        prefs.edit()
            .remove("raw_cookie_header")
            .remove("all_cookies")
            .remove("saved_cookie")
            .apply()
    }

    fun hasValidSession(): Boolean {
        return cookieMap.isNotEmpty() || (prefs.getString("raw_cookie_header", "")?.isNotBlank() == true)
    }
}

