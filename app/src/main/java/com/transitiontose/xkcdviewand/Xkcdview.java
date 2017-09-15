package com.transitiontose.xkcdviewand;

import android.app.Application;
import com.squareup.leakcanary.LeakCanary;

public class Xkcdview extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        LeakCanary.install(this);
    }
}