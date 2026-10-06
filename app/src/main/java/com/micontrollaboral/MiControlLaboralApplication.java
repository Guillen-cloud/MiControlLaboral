package com.micontrollaboral;

import android.app.Application;

import com.micontrollaboral.database.AppDatabase;

public class MiControlLaboralApplication extends Application {
    private AppDatabase database;

    @Override
    public void onCreate() {
        super.onCreate();
        database = AppDatabase.getInstance(this);
    }

    public AppDatabase getDatabase() {
        return database;
    }
}