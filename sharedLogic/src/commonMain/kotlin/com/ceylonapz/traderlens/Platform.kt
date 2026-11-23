package com.ceylonapz.traderlens

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform