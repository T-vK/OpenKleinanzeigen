package de.openkleinanzeigen.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import de.openkleinanzeigen.R
import de.openkleinanzeigen.core.common.AppLogger

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogViewerScreen(onBack: () -> Unit) {
    var logs by remember { mutableStateOf(AppLogger.getLogs()) }
    var filter by remember { mutableStateOf("") }
    val context = LocalContext.current
    val scroll = rememberScrollState()

    DisposableEffect(Unit) {
        val listener: (String) -> Unit = {
            logs = AppLogger.getLogs()
        }
        AppLogger.addListener(listener)
        onDispose { AppLogger.removeListener(listener) }
    }

    val displayed = remember(logs, filter) {
        if (filter.isBlank()) logs
        else logs.lineSequence().filter { it.contains(filter, ignoreCase = true) }.joinToString("\n")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.logs_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            OutlinedTextField(
                value = filter,
                onValueChange = { filter = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.logs_filter)) },
                singleLine = true,
            )
            Row(
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(onClick = {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("logs", displayed))
                }) { Text(stringResource(R.string.logs_copy)) }
                Button(onClick = {
                    AppLogger.clear()
                    logs = ""
                }) { Text(stringResource(R.string.logs_clear)) }
                Button(onClick = { logs = AppLogger.getLogs() }) { Text(stringResource(R.string.logs_refresh)) }
            }
            Text(
                displayed.ifBlank { stringResource(R.string.logs_empty) },
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scroll)
                    .padding(top = 4.dp),
                fontFamily = FontFamily.Monospace,
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            )
        }
    }
}
