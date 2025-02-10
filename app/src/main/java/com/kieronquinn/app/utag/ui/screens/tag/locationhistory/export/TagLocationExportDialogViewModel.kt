package com.kieronquinn.app.utag.ui.screens.tag.locationhistory.export

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.utag.model.DeviceType
import com.kieronquinn.app.utag.repositories.ApiRepository
import com.kieronquinn.app.utag.repositories.AuthRepository
import com.kieronquinn.app.utag.repositories.LocationHistoryRepository.ExportLocation
import com.kieronquinn.app.utag.repositories.LocationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter

abstract class TagLocationExportDialogViewModel: ViewModel() {

    abstract val state: StateFlow<State>

    sealed class State {
        data object Exporting: State()
        data class Finished(val success: Boolean): State()
    }

}

class TagLocationExportDialogViewModelImpl(
    context: Context,
    apiRepository: ApiRepository,
    authRepository: AuthRepository,
    locationRepository: LocationRepository,
    uri: Uri,
    locations: List<ExportLocation>
): TagLocationExportDialogViewModel() {

    private val export = flow {
        val output = context.contentResolver.openOutputStream(uri) ?: run {
            emit(false)
            return@flow
        }
        val outputWriter = output.writer()
        val format = CSVFormat.DEFAULT.builder()
            .setHeader(
                "timestamp",
                "latitude",
                "longitude",
                "address",
                "nearby",
                "on_demand",
                "find_host",
                "user",
                "device",
                "method",
                "accuracy",
                "speed",
                "rssi",
                "battery",
                "d2d_status",
                "was_encrypted"
            ).build()
        val printer = CSVPrinter(outputWriter, format)
        //Get all known users from the account, since other users may have sent
        val users = locationRepository.getAllUsers()
        //Get all known FME devices from the account, since other devices may have sent
        val devices = apiRepository.getDevices()?.devices?.mapNotNull {
            Pair(
                it.saGuid ?: return@mapNotNull null,
                it.stDevName ?: return@mapNotNull null
            )
        }.let {
            //Add our own ID, since we may have sent
            (it ?: emptyList()) + Pair(authRepository.getDeviceId(), Build.MODEL)
        }.toMap()
        locations.forEach {
            val user = it.connectedUserId?.let { id -> users?.get(id) }
            val device = it.connectedDeviceId?.let { id -> devices[id] }
            val findHost = it.findHost ?: DeviceType.NONE_GALAXY_PHONE
            printer.printRecord(
                it.time.toString(),
                it.location.latitude.toString(),
                it.location.longitude.toString(),
                it.address ?: "",
                it.nearby ?: false,
                it.onDemand ?: false,
                context.getString(findHost.label),
                user ?: "",
                device ?: "",
                it.method,
                it.accuracy.toString(),
                it.speed?.toString() ?: "",
                it.rssi ?: "",
                it.battery?.let { battery -> context.getString(battery.labelRaw) } ?: "",
                it.d2dStatus?.let { status -> context.getString(status.label) } ?: "",
                it.wasEncrypted
            )
        }
        printer.close(true)
        outputWriter.close()
        output.close()
        emit(true)
    }.flowOn(Dispatchers.IO)

    override val state = export.mapLatest {
        State.Finished(it)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Exporting)

}