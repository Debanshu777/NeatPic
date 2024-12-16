package com.debanshu.neatpic

import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class KoinHelper: KoinComponent {
    fun getAppViewModel() = get<AppViewModel>()
}