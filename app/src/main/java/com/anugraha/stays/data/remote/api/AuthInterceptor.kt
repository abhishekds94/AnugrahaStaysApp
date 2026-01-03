package com.anugraha.stays.data.remote.api

import com.anugraha.stays.data.local.preferences.UserPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val userPreferences: UserPreferences
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking {
            userPreferences.getAuthToken().first()
        }

        val request = chain.request().newBuilder()

        token?.let {
            request.addHeader("Authorization", "Bearer $it")
        }

        request.addHeader("Content-Type", "application/json")
        request.addHeader("Accept", "application/json")

        return chain.proceed(request.build())
    }
}