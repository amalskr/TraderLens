package com.ceylonapz.traderlens.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.ceylonapz.traderlens.data.local.TraderLensDatabase
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val platformModule: Module = module {
    single<SqlDriver> { NativeSqliteDriver(TraderLensDatabase.Schema, DATABASE_NAME) }
}
