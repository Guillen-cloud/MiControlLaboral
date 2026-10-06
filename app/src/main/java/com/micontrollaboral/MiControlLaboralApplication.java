package com.micontrollaboral;

import android.app.Application;

import com.micontrollaboral.database.AppDatabase;

public class MiControlLaboralApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
    }

    public AppDatabase getDatabase(String ownerUid) {
        return AppDatabase.getInstance(this, ownerUid);
    }
}