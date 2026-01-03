package com.anugraha.stays.util

object Constants {
    const val BASE_URL = "https://anugrahastays.co.in/api/"
    const val DATABASE_NAME = "anugraha_stays_db"
    const val PREFS_NAME = "anugraha_prefs"
    const val AUTH_TOKEN_KEY = "auth_token"
    const val USER_EMAIL_KEY = "user_email"

    // Date formats
    const val API_DATE_FORMAT = "yyyy-MM-dd"
    const val DISPLAY_DATE_FORMAT = "MMM dd, yyyy"
    const val TIME_FORMAT = "HH:mm"

    // Allowed emails
    val ALLOWED_EMAILS = listOf(
        "dksheshagiri@gmail.com",
        "abhishekds94@gmail.com"
    )
}