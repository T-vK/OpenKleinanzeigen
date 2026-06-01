package de.openkleinanzeigen.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.openkleinanzeigen.R
import de.openkleinanzeigen.core.data.AppRepositories

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(repos: AppRepositories, onLogin: () -> Unit, onConversation: (String) -> Unit) {
    val session by repos.auth.observeSession().collectAsState(initial = null)
    val conversations by repos.messages.observeConversations().collectAsState(initial = emptyList())

    LaunchedEffect(session) {
        if (session != null) repos.messages.refreshConversations()
    }

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.messages_title)) }) }) { padding ->
        if (session == null) {
            Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                Text(stringResource(R.string.messages_login_required))
                Button(onClick = onLogin, modifier = Modifier.padding(top = 16.dp)) {
                    Text(stringResource(R.string.login_button))
                }
            }
        } else if (conversations.isEmpty()) {
            Text(stringResource(R.string.messages_empty), Modifier.padding(padding).padding(16.dp))
        } else {
            LazyColumn(Modifier.padding(padding)) {
                items(conversations, key = { it.id }) { convo ->
                    Card(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                            .clickable { onConversation(convo.id) },
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(convo.title, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                            convo.preview?.let { Text(it, maxLines = 1) }
                        }
                    }
                }
            }
        }
    }
}
