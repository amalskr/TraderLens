package com.ceylonapz.traderlens

import android.app.Application
import com.ceylonapz.traderlens.di.appModule
import com.ceylonapz.traderlens.di.sharedModule
import com.ceylonapz.traderlens.presentation.widget.WidgetRefresher
import kotlinx.coroutines.MainScope
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class TraderLensApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@TraderLensApplication)
            modules(sharedModule(enableLogging = BuildConfig.DEBUG), appModule)
        }
        get<WidgetRefresher>().start(MainScope())
    }
}
