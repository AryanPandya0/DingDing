package com.dingding.app

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dingding.app.ui.theme.DingDingTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            DingDingTheme {
                DingDingApp()
            }
        }
    }
}

@Composable
fun DingDingApp() {
    var showAddPerson by remember { mutableStateOf(false) }

    if (showAddPerson) {
        AddPersonScreen(
            onBack = { showAddPerson = false }
        )
    } else {
        HomeScreen(
            onAddPerson = { showAddPerson = true }
        )
    }
}

@Composable
fun HomeScreen(
    onAddPerson: () -> Unit
) {
    val context = LocalContext.current
    var rules by remember { mutableStateOf(RuleRepository.getRules()) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = "🔔 DingDing",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Personal notifications, made personal.",
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Notification Access")
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Your Rules",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (rules.isEmpty()) {
                Text(
                    text = "No rules added yet.",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(rules) { rule ->
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = rule.personName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = rule.appName,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "🔊 ${rule.soundName}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        RuleRepository.deleteRule(rule.id)
                                        rules = RuleRepository.getRules()
                                    }
                                ) {
                                    Text("Delete")
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onAddPerson,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("+ Add Rule")
            }
        }
    }
}

@Composable
fun AddPersonScreen(
    onBack: () -> Unit
) {
    var personName by remember { mutableStateOf("") }
    var selectedApp by remember { mutableStateOf(SupportedApps.apps.first()) }
    var appMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = "Add Rule",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "App",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box {
            OutlinedButton(
                onClick = { appMenuExpanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(selectedApp.name)
            }

            DropdownMenu(
                expanded = appMenuExpanded,
                onDismissRequest = { appMenuExpanded = false }
            ) {
                SupportedApps.apps.forEach { app ->
                    DropdownMenuItem(
                        text = { Text(app.name) },
                        onClick = {
                            selectedApp = app
                            appMenuExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Person / Sender Name",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = personName,
            onValueChange = { personName = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }

            Button(
                onClick = {
                    if (personName.isNotBlank()) {
                        RuleRepository.addRule(
                            NotificationRule(
                                appName = selectedApp.name,
                                packageName = selectedApp.packageName,
                                personName = personName.trim(),
                                soundName = "Default"
                            )
                        )
                        onBack()
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Save")
            }
        }
    }
}
