package com.ceylonapz.traderlens

import android.app.Application
import com.ceylonapz.traderlens.di.sharedModule
import org.koin.core.context.startKoin

class TraderLensApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            modules(sharedModule(enableLogging = BuildConfig.DEBUG))
        }
    }
}
