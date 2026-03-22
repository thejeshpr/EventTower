package com.prtlabs.eventtower

import android.app.Application
import com.prtlabs.eventtower.data.AppDatabase

class EventApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
}
