package com.transitiontose.xkcdviewand

import android.app.Application
import com.squareup.leakcanary.LeakCanary

class XkcdApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        LeakCanary.install(this)
    }
}