package com.debanshu.neatpic.di

import com.debanshu.neatpic.AppViewModel
import com.debanshu.neatpic.MediaPagingSource
import com.debanshu.neatpic.MediaRepository
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.module

expect val targetModule: Module

val dataModule = module {
    single { MediaPagingSource(get()) }
    single { MediaRepository(imageSource = get()) }
    single { AppViewModel(get()) }
}

fun initialiseKoin(
    config: (KoinApplication.() -> Unit)? = null
) {
    startKoin {
        config?.invoke(this)
        modules(targetModule, dataModule)
    }
}