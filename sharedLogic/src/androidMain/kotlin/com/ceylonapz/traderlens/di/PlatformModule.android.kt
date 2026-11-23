package com.ceylonapz.traderlens.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.ceylonapz.traderlens.data.local.TraderLensDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val platformModule: Module = module {
    single<SqlDriver> { AndroidSqliteDriver(TraderLensDatabase.Schema, androidContext(), DATABASE_NAME) }
}
