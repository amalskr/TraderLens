package com.ceylonapz.traderlens.di

import com.ceylonapz.traderlens.presentation.chart.ChartViewModel
import com.ceylonapz.traderlens.presentation.history.HistoryViewModel
import com.ceylonapz.traderlens.presentation.widget.WidgetRefresher
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    viewModelOf(::ChartViewModel)
    viewModelOf(::HistoryViewModel)
    single { WidgetRefresher(androidContext(), get()) }
}
