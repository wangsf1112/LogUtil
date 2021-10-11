package com.wsf.logutil;

import android.app.Application;

import com.wsf.logutil.utils.LogUtil;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        LogUtil.init(this);
        LogUtil.setSaveToLocal(true);
    }
}
