package com.dingding.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.dingding.app.identity.AppIdentity
import com.dingding.app.identity.AppIdentityResolverRegistry
import com.dingding.app.ui.theme.DingDingTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

fun isNotificationServiceEnabled(context: Context): Boolean {
    val pkgName = context.packageName
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    return flat != null && flat.contains(pkgName)
}

sealed class Screen {
    object Home : Screen()
    object Settings : Screen()
    data class ContactSelection(val app: SupportedApp) : Screen()
    data class AddIdentity(val app: SupportedApp, val isGroup: Boolean) : Screen()
    data class SoundSelection(val app: SupportedApp, val contactName: String, val isGroup: Boolean, val existingRule: NotificationRule? = null) : Screen()
    data class EditRule(val rule: NotificationRule) : Screen()
}

@Composable
fun DingDingApp() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0 && (currentScreen is Screen.Home || currentScreen is Screen.ContactSelection || currentScreen is Screen.AddIdentity || currentScreen is Screen.SoundSelection || currentScreen is Screen.EditRule),
                    onClick = {
                        selectedTab = 0
                        currentScreen = Screen.Home
                    },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1 || currentScreen is Screen.Settings,
                    onClick = {
                        selectedTab = 1
                        currentScreen = Screen.Settings
                    },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") }
                )
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            when (val screen = currentScreen) {
                is Screen.Home -> {
                    HomeScreen(
                        onSelectApp = { app ->
                            currentScreen = Screen.ContactSelection(app)
                        },
                        onEditRule = { rule ->
                            currentScreen = Screen.EditRule(rule)
                        }
                    )
                }
                is Screen.Settings -> {
                    SettingsScreen(onBack = { currentScreen = Screen.Home })
                }
                is Screen.ContactSelection -> {
                    ContactSelectionScreen(
                        app = screen.app,
                        onBack = { currentScreen = Screen.Home },
                        onAddContact = { currentScreen = Screen.AddIdentity(screen.app, false) },
                        onAddGroup = { currentScreen = Screen.AddIdentity(screen.app, true) },
                        onSelectIdentityDirect = { identityName, isGroup ->
                            currentScreen = Screen.SoundSelection(screen.app, identityName, isGroup)
                        }
                    )
                }
                is Screen.AddIdentity -> {
                    AddIdentityScreen(
                        app = screen.app,
                        isGroup = screen.isGroup,
                        onBack = { currentScreen = Screen.ContactSelection(screen.app) },
                        onSelectIdentity = { identityName ->
                            currentScreen = Screen.SoundSelection(screen.app, identityName, screen.isGroup)
                        }
                    )
                }
                is Screen.SoundSelection -> {
                    SoundSelectionScreen(
                        app = screen.app,
                        contactName = screen.contactName,
                        isGroup = screen.isGroup,
                        existingRule = screen.existingRule,
                        onBack = {
                            if (screen.existingRule != null) {
                                currentScreen = Screen.Home
                            } else {
                                currentScreen = Screen.ContactSelection(screen.app)
                            }
                        },
                        onSaved = { currentScreen = Screen.Home }
                    )
                }
                is Screen.EditRule -> {
                    EditRuleScreen(
                        rule = screen.rule,
                        onBack = { currentScreen = Screen.Home },
                        onChangeSound = {
                            val app = SupportedApps.apps.find { it.packageName == screen.rule.packageName } ?: SupportedApps.apps.first()
                            currentScreen = Screen.SoundSelection(app, screen.rule.personName, screen.rule.isGroup, screen.rule)
                        },
                        onSaved = { currentScreen = Screen.Home }
                    )
                }
            }
        }
    }
}

@Composable
fun DingDingHeader(title: String, onBack: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Text(text = "←", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun HomeScreen(
    onSelectApp: (SupportedApp) -> Unit,
    onEditRule: (NotificationRule) -> Unit
) {
    val context = LocalContext.current
    var rules by remember { mutableStateOf<List<NotificationRule>>(emptyList()) }
    var isNotifActive by remember { mutableStateOf(isNotificationServiceEnabled(context)) }

    LaunchedEffect(Unit) {
        rules = RuleRepository.getRules(context)
        isNotifActive = isNotificationServiceEnabled(context)
    }

    val activeCount = rules.count { it.enabled }
    val peopleCount = rules.count { !it.isGroup }
    val groupCount = rules.count { it.isGroup }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // DingDing Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🔔", fontSize = 24.sp)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "DingDing",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Personal notification sounds, made personal.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Notification Access Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "Notification Access",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isNotifActive) Color(0xFF22C55E) else Color(0xFFEF4444))
                            )
                            Text(
                                text = if (isNotifActive) "Active" else "Inactive",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isNotifActive) Color(0xFF22C55E) else Color(0xFFEF4444)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "DingDing can now detect supported notifications.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Manage Access")
                    }
                }
            }
        }

        // Stats Row
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    StatItem(label = "Active Rules", value = activeCount.toString())
                    StatItem(label = "People", value = peopleCount.toString())
                    StatItem(label = "Groups", value = groupCount.toString())
                }
            }
        }

        // Supported Apps Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Supported Apps",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        items(SupportedApps.apps) { app ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                    .clickable { onSelectApp(app) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = app.iconEmoji,
                        fontSize = 28.sp
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = app.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = app.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = { onSelectApp(app) },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Text("Configure ›", color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
        }

        // Customized People & Groups
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Customized People & Groups",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (rules.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No custom rules configured yet.",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap 'Configure' on any app above to add people or groups.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(rules) { rule ->
                RuleCardMockup(
                    rule = rule,
                    onToggleEnabled = { enabled ->
                        CoroutineScope(Dispatchers.IO).launch {
                            RuleRepository.addRule(context, rule.copy(enabled = enabled))
                            rules = RuleRepository.getRules(context)
                        }
                    },
                    onEdit = { onEditRule(rule) },
                    onTest = { playRuleSoundPreview(context, rule) },
                    onDelete = {
                        CoroutineScope(Dispatchers.IO).launch {
                            RuleRepository.deleteRule(context, rule.id)
                            rules = RuleRepository.getRules(context)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun RuleCardMockup(
    rule: NotificationRule,
    onToggleEnabled: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onTest: () -> Unit,
    onDelete: () -> Unit
) {
    val appEmoji = when (rule.packageName) {
        "com.whatsapp" -> "🟢"
        "org.telegram.messenger" -> "✈️"
        "com.instagram.android" -> "📸"
        else -> "🎮"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = appEmoji, fontSize = 28.sp)
                    Column {
                        Text(
                            text = rule.personName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${rule.appName} • ${if (rule.isGroup) "Group" else "Contact"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "🎵 ${rule.soundName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Switch(
                    checked = rule.enabled,
                    onCheckedChange = onToggleEnabled
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onTest,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Test")
                }

                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit")
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

fun playRuleSoundPreview(context: Context, rule: NotificationRule) {
    try {
        val mediaPlayer = MediaPlayer()
        if (!rule.soundUri.isNullOrBlank()) {
            mediaPlayer.setDataSource(context, Uri.parse(rule.soundUri))
        } else {
            val alertUri: Uri = when (rule.soundName) {
                "Chime" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                "Alarm" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                else -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }
            mediaPlayer.setDataSource(context, alertUri)
        }
        mediaPlayer.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        )
        mediaPlayer.prepare()
        mediaPlayer.start()
        mediaPlayer.setOnCompletionListener {
            it.release()
        }
    } catch (e: Exception) {
        // ignore
    }
}

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            DingDingHeader(title = "Settings", onBack = onBack)
        }

        item {
            SettingCard(
                title = "Notification Access",
                subtitle = "Manage system notification permissions.",
                onClick = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
            )
        }

        item {
            SettingCard(
                title = "Supported Apps",
                subtitle = "WhatsApp, Telegram, Instagram, Discord",
                onClick = { }
            )
        }

        item {
            SettingCard(
                title = "Sound Settings",
                subtitle = "Manage default and custom sounds.",
                onClick = { }
            )
        }

        item {
            SettingCard(
                title = "Privacy Information",
                subtitle = "All processing happens locally on your device.",
                onClick = { }
            )
        }

        item {
            SettingCard(
                title = "About DingDing",
                subtitle = "v1.0 • Personal notifications, made personal.",
                onClick = { }
            )
        }
    }
}

@Composable
fun SettingCard(title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(text = "›", fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun ContactSelectionScreen(
    app: SupportedApp,
    onBack: () -> Unit,
    onAddContact: () -> Unit,
    onAddGroup: () -> Unit,
    onSelectIdentityDirect: (String, Boolean) -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val resolver = remember(app.packageName) {
        AppIdentityResolverRegistry.getResolver(app.packageName)
    }

    var appRules by remember { mutableStateOf<List<NotificationRule>>(emptyList()) }

    LaunchedEffect(app.packageName) {
        appRules = RuleRepository.getRulesForApp(context, app.packageName)
    }

    val addedPeople = appRules.filter { !it.isGroup }
    val addedGroups = appRules.filter { it.isGroup }

    var observedPeople by remember { mutableStateOf<List<AppIdentity>>(emptyList()) }
    var observedGroupsList by remember { mutableStateOf<List<AppIdentity>>(emptyList()) }

    LaunchedEffect(app.packageName) {
        if (resolver != null) {
            observedPeople = resolver.getObservedPeople(context)
            observedGroupsList = resolver.getGroups(context)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        DingDingHeader(title = app.name, onBack = onBack)

        Text(
            text = if (resolver?.supportsDeviceContacts == true)
                "Select contacts to customize notification sounds."
            else
                "Add users from your notifications. No contacts needed.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (resolver?.supportsDeviceContacts == true) {
            Button(
                onClick = onAddContact,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Contact")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // Configured People
            item {
                Text(
                    text = "Configured People",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (addedPeople.isEmpty()) {
                    Text(
                        text = "No contacts added yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(addedPeople) { rule ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = rule.personName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(text = "🔊 ${rule.soundName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                        OutlinedButton(
                            onClick = {
                                CoroutineScope(Dispatchers.IO).launch {
                                    RuleRepository.deleteRule(context, rule.id)
                                    val updated = RuleRepository.getRulesForApp(context, app.packageName)
                                    withContext(Dispatchers.Main) {
                                        appRules = updated
                                    }
                                }
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Remove")
                        }
                    }
                }
            }

            // Observed People for Instagram / Discord
            if (resolver?.supportsDeviceContacts == false) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Recent Notifications",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Add users/groups from recent notifications.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (observedPeople.isEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "When a notification arrives from ${app.name}, you can add that user directly from the notification.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                items(observedPeople) { person ->
                    val isAlreadyAdded = addedPeople.any { it.personName.equals(person.displayName, ignoreCase = true) }
                    if (!isAlreadyAdded) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(text = person.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(text = app.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Button(
                                    onClick = { onSelectIdentityDirect(person.displayName, false) },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Add")
                                }
                            }
                        }
                    }
                }
            }

            // Configured Groups
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Configured Groups",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (addedGroups.isEmpty()) {
                    Text(
                        text = "No groups added yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(addedGroups) { rule ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = rule.personName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(text = "🔊 ${rule.soundName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                        OutlinedButton(
                            onClick = {
                                CoroutineScope(Dispatchers.IO).launch {
                                    RuleRepository.deleteRule(context, rule.id)
                                    val updated = RuleRepository.getRulesForApp(context, app.packageName)
                                    withContext(Dispatchers.Main) {
                                        appRules = updated
                                    }
                                }
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Remove")
                        }
                    }
                }
            }

            // Observed Groups
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Observed Groups",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (observedGroupsList.isEmpty()) {
                    Text(
                        text = "No group notifications detected yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(observedGroupsList) { group ->
                val isAlreadyAdded = addedGroups.any { it.personName.equals(group.displayName, ignoreCase = true) }
                if (!isAlreadyAdded) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = group.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Button(
                                onClick = { onSelectIdentityDirect(group.displayName, true) },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Add")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddIdentityScreen(
    app: SupportedApp,
    isGroup: Boolean,
    onBack: () -> Unit,
    onSelectIdentity: (String) -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<AppIdentity>>(emptyList()) }

    val resolver = remember(app.packageName) {
        AppIdentityResolverRegistry.getResolver(app.packageName)
    }

    var hasContactsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasContactsPermission = granted
        if (granted && resolver != null && !isGroup) {
            searchResults = resolver.searchPeople(context, searchQuery)
        }
    }

    val updateSearch: (String) -> Unit = { query ->
        searchQuery = query
        if (resolver != null) {
            if (isGroup) {
                searchResults = resolver.getGroups(context).filter { it.displayName.contains(query, ignoreCase = true) }
            } else {
                if (resolver.supportsDeviceContacts && hasContactsPermission) {
                    searchResults = resolver.searchPeople(context, query)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        DingDingHeader(title = if (isGroup) "Add Group" else "Add Contact", onBack = onBack)

        Text(
            text = if (isGroup) "Search observed notification groups:" else "Search device contacts as you type:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (!isGroup && resolver?.supportsDeviceContacts == true && !hasContactsPermission) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Contacts Permission Required", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "DingDing needs contacts permission to search your address book.", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.READ_CONTACTS) },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Grant Permission")
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = updateSearch,
            label = { Text("Search contacts...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (searchQuery.isBlank() && !isGroup && resolver?.supportsDeviceContacts == true) {
                item {
                    Text(
                        text = "Type to search contacts...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (searchResults.isEmpty()) {
                item {
                    Text(
                        text = "No matching results found.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(searchResults) { identity ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = identity.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Button(
                                onClick = { onSelectIdentity(identity.displayName) },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Add")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SoundSelectionScreen(
    app: SupportedApp,
    contactName: String,
    isGroup: Boolean,
    existingRule: NotificationRule? = null,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val presetSounds = listOf("Chime", "Alarm", "Soft Bell", "Forest Chime", "Game Alert")
    var selectedSound by remember { mutableStateOf(existingRule?.soundName ?: "Chime") }
    var selectedUriString by remember { mutableStateOf<String?>(existingRule?.soundUri) }
    var selectedFileName by remember { mutableStateOf<String?>(if (existingRule?.soundUri != null) "Custom Audio File" else null) }

    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var soundTab by remember { mutableStateOf(0) } // 0: My Files, 1: Default Sounds

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
        }
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                selectedUriString = uri.toString()
                selectedFileName = uri.lastPathSegment ?: "Custom Audio File"
            } catch (e: Exception) {
                selectedUriString = uri.toString()
                selectedFileName = "Custom Audio File"
            }
        }
    }

    fun togglePreview(uriStr: String?, fallbackName: String) {
        try {
            if (isPlaying) {
                mediaPlayer?.stop()
                mediaPlayer?.release()
                mediaPlayer = null
                isPlaying = false
            } else {
                mediaPlayer = MediaPlayer().apply {
                    if (!uriStr.isNullOrBlank()) {
                        setDataSource(context, Uri.parse(uriStr))
                    } else {
                        val alertUri: Uri = when (fallbackName) {
                            "Chime", "Soft Bell", "Forest Chime", "Game Alert" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                            "Alarm" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                            else -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                        }
                        setDataSource(context, alertUri)
                    }
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    prepare()
                    start()
                    setOnCompletionListener {
                        isPlaying = false
                        release()
                        mediaPlayer = null
                    }
                }
                isPlaying = true
            }
        } catch (e: Exception) {
            isPlaying = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        DingDingHeader(title = "Choose Sound", onBack = onBack)

        Text(
            text = "Select custom or default notification sound for $contactName.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Sound source buttons (My Files / Default Sounds)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { soundTab = 0 },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (soundTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text("My Files")
            }
            Button(
                onClick = { soundTab = 1 },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (soundTab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text("Default Sounds")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (soundTab == 0) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Pick from device", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Select any audio file (MP3, WAV, OGG)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { audioPickerLauncher.launch(arrayOf("audio/*")) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(selectedFileName ?: "Browse Audio File")
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(presetSounds) { sound ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                if (selectedUriString == null && selectedSound == sound) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.08f),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                selectedSound = sound
                                selectedUriString = null
                                selectedFileName = null
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                IconButton(
                                    onClick = { togglePreview(null, sound) },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Preview", tint = MaterialTheme.colorScheme.primary)
                                }
                                Column {
                                    Text(text = sound, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(text = "00:05", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            if (selectedUriString == null && selectedSound == sound) {
                                Text(text = "✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                        }
                    }
                }
            }
        }

        if (soundTab == 0) {
            Spacer(modifier = Modifier.weight(1f))
        }

        if (selectedUriString != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(
                            onClick = { togglePreview(selectedUriString, selectedSound) },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(if (isPlaying) Icons.Default.Close else Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                        }
                        Column {
                            Text(text = selectedFileName ?: "Custom Audio", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(text = "Custom File", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    IconButton(onClick = {
                        selectedUriString = null
                        selectedFileName = null
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Button(
            onClick = {
                mediaPlayer?.release()
                CoroutineScope(Dispatchers.IO).launch {
                    RuleRepository.addRule(
                        context,
                        NotificationRule(
                            id = existingRule?.id ?: System.currentTimeMillis(),
                            appName = app.name,
                            packageName = app.packageName,
                            personId = contactName.trim().lowercase(),
                            personName = contactName,
                            soundName = selectedFileName ?: selectedSound,
                            soundUri = selectedUriString,
                            isGroup = isGroup,
                            enabled = existingRule?.enabled ?: true
                        )
                    )
                    withContext(Dispatchers.Main) {
                        onSaved()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Save Sound")
        }
    }
}

@Composable
fun EditRuleScreen(
    rule: NotificationRule,
    onBack: () -> Unit,
    onChangeSound: () -> Unit,
    onSaved: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    var nameText by remember { mutableStateOf(rule.personName) }
    var isEnabled by remember { mutableStateOf(rule.enabled) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        DingDingHeader(title = "Edit Rule", onBack = onBack)
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "👤", fontSize = 24.sp)
                }
                Column {
                    Text(text = rule.personName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = "${rule.appName} • ${if (rule.isGroup) "Group" else "Contact"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(text = "Name", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = nameText,
            onValueChange = { nameText = it },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(text = "Sound", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "🔊", fontSize = 20.sp)
                    Text(text = rule.soundName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                }
                Button(
                    onClick = onChangeSound,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Change")
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Enabled", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Switch(checked = isEnabled, onCheckedChange = { isEnabled = it })
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                CoroutineScope(Dispatchers.IO).launch {
                    RuleRepository.deleteRule(context, rule.id)
                    withContext(Dispatchers.Main) {
                        onSaved()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Delete Rule", color = Color.White)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                CoroutineScope(Dispatchers.IO).launch {
                    RuleRepository.addRule(
                        context,
                        rule.copy(
                            personName = nameText,
                            enabled = isEnabled
                        )
                    )
                    withContext(Dispatchers.Main) {
                        onSaved()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Save Changes")
        }
    }
}
