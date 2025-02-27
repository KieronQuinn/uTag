package com.kieronquinn.app.utag.components.navigation

import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.annotation.IdRes
import androidx.browser.customtabs.CustomTabsIntent
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.fragment.FragmentNavigator
import androidx.navigation.fragment.NavHostFragment
import com.jakewharton.processphoenix.ProcessPhoenix
import com.kieronquinn.app.utag.ui.activities.MainActivity
import com.kieronquinn.app.utag.utils.extensions.sendSafely
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

interface BaseNavigation {

    val navigationBus: Flow<NavigationEvent>

    suspend fun navigate(navDirections: NavDirections, extras: FragmentNavigator.Extras? = null)
    suspend fun navigate(@IdRes id: Int, arguments: Bundle? = null)
    suspend fun navigate(intent: Intent, errorCallback: suspend () -> Unit = {})
    suspend fun navigate(intent: PendingIntent, errorCallback: suspend () -> Unit = {})
    suspend fun navigate(event: NavigationEvent)
    suspend fun navigate(uri: Uri)
    suspend fun navigate(url: String)
    suspend fun navigateUpTo(@IdRes id: Int, popInclusive: Boolean = false)
    suspend fun navigateBack()
    suspend fun navigateWithContext(method: (Context) -> Unit)
    suspend fun restartActivity()
    suspend fun finish()
    suspend fun finishAndRemoveTask()
    suspend fun phoenix()

}

sealed class NavigationEvent {
    data class Directions(val directions: NavDirections, val extras: FragmentNavigator.Extras?): NavigationEvent()
    data class Id(@IdRes val id: Int, val arguments: Bundle? = null): NavigationEvent()
    data class PopupTo(@IdRes val id: Int, val popInclusive: Boolean): NavigationEvent()
    data class Intent(val intent: android.content.Intent, val onActivityNotFound: (suspend () -> Unit)? = null): NavigationEvent()
    data class PendingIntent(val intent: android.app.PendingIntent, val onError: (suspend () -> Unit)? = null): NavigationEvent()
    data class ContextInjectedMethod(val method: (Context) -> Unit): NavigationEvent()
    data class Uri(val uri: android.net.Uri): NavigationEvent()
    data class CustomTab(val url: String): NavigationEvent()
    data object Back: NavigationEvent()
    data object RestartActivity: NavigationEvent()
    data object Finish: NavigationEvent()
    data object FinishAndRemoveTask: NavigationEvent()
    data object Phoenix: NavigationEvent()
}

open class NavigationImpl: BaseNavigation {

    private val _navigationBus = MutableSharedFlow<NavigationEvent>()
    override val navigationBus = _navigationBus

    override suspend fun navigate(id: Int, arguments: Bundle?) {
        _navigationBus.emit(NavigationEvent.Id(id, arguments))
    }

    override suspend fun navigate(navDirections: NavDirections, extras: FragmentNavigator.Extras?) {
        _navigationBus.emit(NavigationEvent.Directions(navDirections, extras))
    }

    override suspend fun navigateBack() {
        _navigationBus.emit(NavigationEvent.Back)
    }

    override suspend fun navigateUpTo(id: Int, popInclusive: Boolean) {
        _navigationBus.emit(NavigationEvent.PopupTo(id, popInclusive))
    }

    override suspend fun navigate(intent: Intent, errorCallback: suspend () -> Unit) {
        _navigationBus.emit(NavigationEvent.Intent(intent, errorCallback))
    }

    override suspend fun navigate(intent: PendingIntent, errorCallback: suspend () -> Unit) {
        _navigationBus.emit(NavigationEvent.PendingIntent(intent, errorCallback))
    }

    override suspend fun navigate(event: NavigationEvent) {
        _navigationBus.emit(event)
    }

    override suspend fun navigate(uri: Uri) {
        _navigationBus.emit(NavigationEvent.Uri(uri))
    }

    override suspend fun navigate(url: String) {
        _navigationBus.emit(NavigationEvent.CustomTab(url))
    }

    override suspend fun navigateWithContext(method: (Context) -> Unit) {
        _navigationBus.emit(NavigationEvent.ContextInjectedMethod(method))
    }

    override suspend fun restartActivity() {
        _navigationBus.emit(NavigationEvent.RestartActivity)
    }

    override suspend fun finish() {
        _navigationBus.emit(NavigationEvent.Finish)
    }

    override suspend fun finishAndRemoveTask() {
        _navigationBus.emit(NavigationEvent.FinishAndRemoveTask)
    }

    override suspend fun phoenix() {
        _navigationBus.emit(NavigationEvent.Phoenix)
    }

}

class DummyNavigation: BaseNavigation {

    override val navigationBus = MutableSharedFlow<NavigationEvent>()

    override suspend fun navigate(id: Int, arguments: Bundle?) {
        //No-op
    }

    override suspend fun navigate(navDirections: NavDirections, extras: FragmentNavigator.Extras?) {
        //No-op
    }

    override suspend fun navigateBack() {
        //No-op
    }

    override suspend fun navigateUpTo(id: Int, popInclusive: Boolean) {
        //No-op
    }

    override suspend fun navigate(intent: Intent, errorCallback: suspend () -> Unit) {
        //No-op
    }

    override suspend fun navigate(intent: PendingIntent, errorCallback: suspend () -> Unit) {
        //No-op
    }

    override suspend fun navigate(event: NavigationEvent) {
        //No-op
    }

    override suspend fun navigate(uri: Uri) {
        //No-op
    }

    override suspend fun navigate(url: String) {
        //No-op
    }

    override suspend fun navigateWithContext(method: (Context) -> Unit) {
        //No-op
    }

    override suspend fun restartActivity() {
        //No-op
    }

    override suspend fun finish() {
        //No-op
    }

    override suspend fun finishAndRemoveTask() {
        //No-op
    }

    override suspend fun phoenix() {
        //No-op
    }

}

//Root level navigation for full screen changes
interface RootNavigation: BaseNavigation
class RootNavigationImpl: NavigationImpl(), RootNavigation

//Container level navigation for inner changes
interface SettingsNavigation: BaseNavigation
class SettingsNavigationImpl: NavigationImpl(), SettingsNavigation

//Setup is a whole different flow
interface SetupNavigation: BaseNavigation
class SetupNavigationImpl: NavigationImpl(), SetupNavigation

//Root level navigation for full screen changes
interface TagRootNavigation: BaseNavigation
class TagRootNavigationImpl: NavigationImpl(), TagRootNavigation

//Container level navigation for inner changes
interface TagContainerNavigation: BaseNavigation
class TagContainerNavigationImpl: NavigationImpl(), TagContainerNavigation

//More level navigation for inner-inner changes
interface TagMoreNavigation: BaseNavigation
class TagMoreNavigationImpl: NavigationImpl(), TagMoreNavigation

//Tag picker level navigation for inner-inner changes
interface TagPickerNavigation: BaseNavigation
class TagPickerNavigationImpl: NavigationImpl(), TagPickerNavigation

//Container level navigation for inner changes
interface WidgetContainerNavigation: BaseNavigation
class WidgetContainerNavigationImpl: NavigationImpl(), WidgetContainerNavigation

//Container level navigation for inner changes
interface UnknownTagNavigation: BaseNavigation
class UnknownTagNavigationImpl: NavigationImpl(), UnknownTagNavigation

suspend fun NavHostFragment.setupWithNavigation(navigation: BaseNavigation) {
    navigation.navigationBus.collect {
        when(it){
            is NavigationEvent.Directions -> navController.navigateSafely(it.directions, it.extras)
            is NavigationEvent.Id -> navController.navigateSafely(it.id, it.arguments ?: Bundle.EMPTY)
            is NavigationEvent.Back -> navController.navigateUp()
            is NavigationEvent.PopupTo -> navController.popBackStack(it.id, it.popInclusive)
            is NavigationEvent.Uri -> {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = it.uri
                    putExtra(MainActivity.EXTRA_SKIP_SPLASH, true)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                startActivity(intent)
            }
            is NavigationEvent.CustomTab -> {
                CustomTabsIntent.Builder().apply {
                    setShowTitle(true)
                }.build().intent.apply {
                    data = Uri.parse(it.url)
                }.let {
                    startActivity(it)
                }
            }
            is NavigationEvent.Intent -> try {
                startActivity(it.intent)
            }catch (e: ActivityNotFoundException){
                it.onActivityNotFound?.invoke()
            }
            is NavigationEvent.PendingIntent -> try {
                it.intent.sendSafely()
            }catch (e: Exception) {
                it.onError?.invoke()
            }
            is NavigationEvent.RestartActivity -> {
                requireActivity().let { activity ->
                    activity.finish()
                    activity.startActivity(activity.intent)
                }
            }
            is NavigationEvent.Finish -> {
                requireActivity().finish()
            }
            is NavigationEvent.FinishAndRemoveTask -> {
                requireActivity().finishAndRemoveTask()
            }
            is NavigationEvent.Phoenix -> {
                val mainIntent = Intent(requireContext(), MainActivity::class.java).apply {
                    putExtra(MainActivity.EXTRA_SKIP_SPLASH, true)
                }
                ProcessPhoenix.triggerRebirth(requireContext(), mainIntent)
            }
            is NavigationEvent.ContextInjectedMethod -> {
                it.method.invoke(requireContext())
            }
        }
    }
}

private fun NavController.navigateSafely(directions: NavDirections, extras: FragmentNavigator.Extras?){
    currentDestination?.getAction(directions.actionId)?.let {
        if(it.destinationId != currentDestination?.id) {
            if(extras != null) {
                navigate(directions, extras)
            }else{
                navigate(directions)
            }
        }
    }
}

private fun NavController.navigateSafely(@IdRes action: Int, arguments: Bundle){
    currentDestination?.getAction(action)?.let {
        if(it.destinationId != currentDestination?.id) {
            navigate(action, arguments)
        }
    }
}