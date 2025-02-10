package com.kieronquinn.app.utag.repositories

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.IInterface
import android.os.RemoteException
import com.kieronquinn.app.utag.repositories.BaseServiceRepository.ServiceResponse
import com.kieronquinn.app.utag.repositories.BaseServiceRepository.ServiceResponse.FailureReason
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import org.koin.core.component.KoinComponent
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

interface BaseServiceRepository<I: IInterface> {

    sealed class ServiceResponse<T> {
        data class Success<T>(val result: T): ServiceResponse<T>()
        data class Failed<T>(val reason: FailureReason): ServiceResponse<T>()

        enum class FailureReason {
            /**
             *  Couldn't bind to the service, is Xposed working?
             */
            BIND_ERROR,

            /**
             *  The service is not available
             */
            NOT_AVAILABLE
        }

        /**
         *  Unwraps a result into either its value or null if it failed
         */
        fun unwrap(): T? {
            return (this as? Success)?.result
        }
    }

    val service: StateFlow<I?>

    suspend fun assertReady(): Boolean
    suspend fun <T> runWithService(block: suspend (I) -> T): ServiceResponse<T>
    fun <T> runWithServiceIfAvailable(block: (I) -> T): ServiceResponse<T>
    suspend fun disconnect()

}

abstract class BaseServiceRepositoryImpl<I: IInterface>(
    private val context: Context
): BaseServiceRepository<I>, KoinComponent {

    companion object {
        private const val TIMEOUT = 10_000L
    }

    private var serviceConnection: ServiceConnection? = null
    private val serviceLock = Mutex()
    private val runLock = Mutex()
    private val scope = MainScope()

    override val service = MutableStateFlow<I?>(null)

    override suspend fun assertReady(): Boolean {
        val rawResult = runWithService {
            it.ping()
        }
        val result = rawResult.unwrap()
        return result == true
    }

    override suspend fun <T> runWithService(
        block: suspend (I) -> T
    ): ServiceResponse<T> = runLock.withLock {
        return runWithServiceLocked(block)
    }

    private suspend fun <T> runWithServiceLocked(
        block: suspend (I) -> T
    ): ServiceResponse<T> {
        service.value?.let {
            if(!it.ping()){
                //Service has disconnected or died
                service.emit(null)
                serviceConnection = null
                return@let
            }
            return ServiceResponse.Success(block(it))
        }
        val service  = withTimeout(TIMEOUT) {
            getService()
        } ?: return ServiceResponse.Failed(FailureReason.BIND_ERROR)
        return try {
            ServiceResponse.Success(block(service))
        }catch (e: Exception) {
            ServiceResponse.Failed(FailureReason.NOT_AVAILABLE)
        }
    }

    override fun <T> runWithServiceIfAvailable(
        block: (I) -> T
    ): ServiceResponse<T> {
        return try {
            service.value?.let {
                ServiceResponse.Success(block(it))
            } ?: ServiceResponse.Failed(FailureReason.NOT_AVAILABLE)
        }catch (e: Exception){
            ServiceResponse.Failed(FailureReason.NOT_AVAILABLE)
        }
    }

    override suspend fun disconnect() {
        serviceConnection?.let {
            context.unbindService(it)
        }
    }

    private suspend fun getService() = serviceLock.withLock {
        suspendCoroutine {
            var hasResumed = false
            val serviceConnection = object: ServiceConnection {
                override fun onServiceConnected(
                    componentName: ComponentName?,
                    binder: IBinder
                ) {
                    serviceConnection = this
                    val service = binder.asInterface()
                    scope.launch {
                        this@BaseServiceRepositoryImpl.service.emit(service)
                        if(!hasResumed){
                            hasResumed = true
                            try {
                                it.resume(service)
                            }catch (e: IllegalStateException) {
                                //Already resumed
                            }
                        }
                    }
                }

                override fun onServiceDisconnected(component: ComponentName) {
                    serviceConnection = null
                    scope.launch {
                        service.emit(null)
                    }
                }
            }
            try {
                if(!context.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)) {
                    it.resume(null)
                }
            }catch (e: RemoteException) {
                it.resume(null)
            }
        }
    }

    private fun I.ping(): Boolean {
        return asBinder().pingBinder()
    }

    abstract val serviceIntent: Intent
    abstract fun IBinder.asInterface(): I

}