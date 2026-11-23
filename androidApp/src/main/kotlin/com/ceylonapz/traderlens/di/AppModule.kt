package com.ceylonapz.traderlens.di

import com.ceylonapz.traderlens.presentation.chart.ChartViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    viewModelOf(::ChartViewModel)
}
