package com.debanshu.neatpic.android

import android.app.Application
import com.debanshu.neatpic.di.initialiseKoin
import org.koin.android.ext.koin.androidContext

class NeatPicApplication:Application() {
    override fun onCreate() {
        super.onCreate()
        initialiseKoin {
            androidContext(this@NeatPicApplication)
        }
    }
}