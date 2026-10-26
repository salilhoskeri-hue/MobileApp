package com.supplytrack.app

import android.app.Application
import com.supplytrack.app.data.AppDatabase
import com.supplytrack.app.data.SupplyRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SupplyTrackApp : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var repository: SupplyRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = SupplyRepository(AppDatabase.build(this))
        appScope.launch { repository.seedIfEmpty() }
    }
}
