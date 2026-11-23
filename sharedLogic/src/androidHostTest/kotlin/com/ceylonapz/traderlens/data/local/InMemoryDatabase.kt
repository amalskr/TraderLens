package com.ceylonapz.traderlens.data.local

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver

fun inMemoryDatabase(): TraderLensDatabase {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    TraderLensDatabase.Schema.create(driver)
    return TraderLensDatabase(driver)
}
