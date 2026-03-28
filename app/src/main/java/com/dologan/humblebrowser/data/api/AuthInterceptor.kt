package com.dologan.humblebrowser.data.api

import com.dologan.humblebrowser.data.prefs.AuthPreferences
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val authPreferences: AuthPreferences,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val cookie = authPreferences.getSessionCookie() ?: return chain.proceed(original)

        val request = original.newBuilder()
            .header("Cookie", "_simpleauth_sess=$cookie")
            .build()

        return chain.proceed(request)
    }
}
