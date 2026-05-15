package com.sixblock.app

import android.app.Application
import com.google.firebase.FirebaseApp
import com.sixblock.app.core.di.AppContainer
import com.sixblock.app.core.di.DemoAppContainer
import com.sixblock.app.core.util.AppSettings
import com.sixblock.app.data.remote.messaging.SixBlockReminderReceiver

class SixBlockApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        AppSettings.applyThemeMode(this)
        val firebaseApp = FirebaseApp.initializeApp(this)
        container = if (firebaseApp != null) {
            AppContainer.createFirebase(this)
        } else {
            DemoAppContainer(this)
        }
        SixBlockReminderReceiver.schedule(this)
    }
}
