package com.emberr.presentation.people

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import com.emberr.domain.people.ContactImportProgress
import com.emberr.domain.people.ContactImporter
import com.emberr.domain.util.system.showNativeToast
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import org.koin.compose.koinInject

private val openPeopleScreenCount = MutableStateFlow(0)

@Composable
internal fun CountPeopleScreenAsOpen() {
    DisposableEffect(Unit) {
        openPeopleScreenCount.update { count -> count + 1 }
        onDispose { openPeopleScreenCount.update { count -> count - 1 } }
    }
}

@Composable
fun ContactImportResultToasts() {
    val contactImporter = koinInject<ContactImporter>()
    LaunchedEffect(contactImporter) {
        combine(contactImporter.progress, openPeopleScreenCount) { progress, openCount ->
            progress is ContactImportProgress.Finished && openCount == 0
        }.collect { isWaitingWithNoPeopleScreen ->
            if (!isWaitingWithNoPeopleScreen) return@collect
            val result = contactImporter.takeFinishedResult() ?: return@collect
            showNativeToast(contactImportMessage(result))
        }
    }
}
