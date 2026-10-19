package com.feryaeljustice.docmask

import android.app.Application
import com.feryaeljustice.docmask.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class DocMaskApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@DocMaskApplication)
            modules(appModules)
        }
    }
}
