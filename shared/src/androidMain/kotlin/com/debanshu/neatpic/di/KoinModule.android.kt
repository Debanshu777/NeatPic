package com.debanshu.neatpic.di

import com.debanshu.neatpic.ImageSource
import org.koin.dsl.module

actual val targetModule = module {
    single { ImageSource(get()) }
}