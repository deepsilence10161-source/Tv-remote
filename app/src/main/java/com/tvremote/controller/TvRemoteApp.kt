package com.tvremote.controller

import android.app.Application

class TvRemoteApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Firebase is initialised automatically from google-services.json.
        // TODO: FirebaseAuth sign-in, Firestore offline cache enable karna.
    }
}
