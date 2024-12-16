package com.debanshu.neatpic.di

import com.debanshu.neatpic.ImageSource
import org.koin.core.module.Module
import org.koin.dsl.module

actual val targetModule: Module = module {
    single{ImageSource()}
}