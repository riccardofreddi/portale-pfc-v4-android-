package com.example.data.remote

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class PfcApiClient(private val context: Context) {

    private val TAG = "PfcApiClient"

    private val prefs: SharedPreferences =
        context.getSharedPreferences("pfc_auth_prefs", Context.MODE_PRIVATE)

    // In-memory cookie store: host -> (cookieName -> Cookie)
    private val cookieStore = ConcurrentHashMap<String, ConcurrentHashMap<String, Cookie>>()

    init {
        loadPersistedCookies()
    }

    private fun loadPersistedCookies() {
        try {
            val savedCookieStrings = prefs.getStringSet("persisted_cookies_v2", null)
            if (!savedCookieStrings.isNullOrEmpty()) {
                for (encoded in savedCookieStrings) {
                    val parts = encoded.split("||")
                    if (parts.size >= 3) {
                        val host = parts[0]
                        val name = parts[1]
                        val raw = parts[2]
                        val dummyUrl = HttpUrl.Builder()
                            .scheme("https")
                            .host(host)
                            .build()
                        val parsed = Cookie.parse(dummyUrl, raw)
                        if (parsed != null && (parsed.expiresAt > System.currentTimeMillis() || parsed.persistent.not())) {
                            cookieStore.getOrPut(host) { ConcurrentHashMap() }[name] = parsed
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading persisted cookies: ${e.message}")
        }
    }

    private fun persistCookies() {
        try {
            val set = mutableSetOf<String>()
            val now = System.currentTimeMillis()
            for ((host, cookies) in cookieStore) {
                for ((name, cookie) in cookies) {
                    // Only keep non-expired or session cookies
                    if (cookie.expiresAt > now || !cookie.persistent) {
                        set.add("$host||$name||${cookie}")
                    }
                }
            }
            prefs.edit().putStringSet("persisted_cookies_v2", set).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error persisting cookies: ${e.message}")
        }
    }

    private val customCookieJar = object : CookieJar {
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            if (cookies.isEmpty()) return
            val host = url.host
            val hostMap = cookieStore.getOrPut(host) { ConcurrentHashMap() }
            for (cookie in cookies) {
                // If max-age=0 or expires in the past or value is deleted, remove it
                if (cookie.expiresAt <= System.currentTimeMillis() || cookie.value == "deleted") {
                    hostMap.remove(cookie.name)
                } else {
                    hostMap[cookie.name] = cookie
                }
            }
            persistCookies()
            Log.d(TAG, "Saved ${cookies.size} cookies for $host. Active count: ${hostMap.size}")
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            val now = System.currentTimeMillis()
            val host = url.host
            val result = mutableListOf<Cookie>()

            // 1. Direct host match
            cookieStore[host]?.values?.forEach { cookie ->
                if (cookie.expiresAt > now || !cookie.persistent) {
                    result.add(cookie)
                }
            }

            // 2. Domain match (e.g. .vercel.app)
            cookieStore.forEach { (savedHost, map) ->
                if (savedHost != host && (host.endsWith(savedHost) || savedHost.endsWith(host))) {
                    map.values.forEach { cookie ->
                        if (cookie.matches(url) && (cookie.expiresAt > now || !cookie.persistent)) {
                            if (result.none { it.name == cookie.name }) {
                                result.add(cookie)
                            }
                        }
                    }
                }
            }

            return result
        }
    }

    // Safety interceptor: guarantees headers and logs session state
    private val sessionHeaderInterceptor = Interceptor { chain ->
        val original = chain.request()
        val builder = original.newBuilder()

        // Ensure user-agent is explicit and mobile-friendly
        builder.header("User-Agent", "PortalePfcAndroid/2.0")

        val request = builder.build()
        val response = chain.proceed(request)

        if (response.code == 401) {
            Log.w(TAG, "Received 401 Unauthorized for ${request.url}")
        }

        response
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(customCookieJar)
        .addInterceptor(sessionHeaderInterceptor)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi: Moshi = Moshi.Builder()
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
        get() = Retrofit.Builder()
            .baseUrl(getBaseUrl())
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(PfcApiService::class.java)

    fun clearSession() {
        cookieStore.clear()
        prefs.edit()
            .remove("persisted_cookies_v2")
            .remove("raw_cookie_header")
            .remove("saved_user_json")
            .apply()
        Log.d(TAG, "Session cleared")
    }

    fun hasValidSession(): Boolean {
        val now = System.currentTimeMillis()
        for ((_, cookies) in cookieStore) {
            for ((_, cookie) in cookies) {
                if (cookie.expiresAt > now || !cookie.persistent) {
                    return true
                }
            }
        }
        val saved = prefs.getStringSet("persisted_cookies_v2", null)
        return !saved.isNullOrEmpty()
    }
}
