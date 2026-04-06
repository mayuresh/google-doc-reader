package com.docreader.app

import android.app.Application
import com.docreader.app.session.SessionManager

class DocReaderApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // No persistent state initialised — fully incognito
    }

    override fun onTerminate() {
        super.onTerminate()
        SessionManager.logout()
    }
}
