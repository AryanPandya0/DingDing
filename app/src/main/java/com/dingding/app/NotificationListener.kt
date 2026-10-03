package com.dingding.app

import android.app.Notification
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.dingding.app.identity.AppIdentityResolverRegistry
import java.util.concurrent.ConcurrentHashMap

class NotificationListener : NotificationListenerService() {

    private val processedNotifications = ConcurrentHashMap<String, Long>()

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        val resolver = AppIdentityResolverRegistry.getResolver(packageName) ?: return

        if (!resolver.isRelevantNotification(sbn)) {
            return
        }

        val parsed = resolver.processNotification(applicationContext, sbn) ?: return

        val deduplicationKey = "${sbn.packageName}_${sbn.id}_${sbn.tag ?: ""}_${sbn.postTime}_${parsed.senderId}"

        val currentTime = System.currentTimeMillis()
        processedNotifications.entries.removeIf { currentTime - it.value > 3000 }

        if (processedNotifications.containsKey(deduplicationKey)) {
            Log.d("DingDing", "Skipping duplicate notification event: $deduplicationKey")
            return
        }
        processedNotifications[deduplicationKey] = currentTime

        val messageText = sbn.notification.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()

        val rule = RuleRepository.findRule(applicationContext, packageName, parsed.senderId) 
            ?: RuleRepository.findRule(applicationContext, packageName, parsed.senderName)

        if (rule != null && rule.enabled) {
            Log.d(
                "DINGDING_RULE",
                """
                🔥 RULE MATCHED
                App: $packageName
                Identity: ${parsed.senderName} (ID: ${parsed.senderId})
                Message: $messageText
                Sound: ${rule.soundName} (URI: ${rule.soundUri})
                """.trimIndent()
            )

            playCustomSound(rule.soundUri, rule.soundName)
        } else {
            Log.d(
                "DINGDING_RULE",
                """
                No active rule found
                App: $packageName
                Identity: ${parsed.senderName} (ID: ${parsed.senderId})
                """.trimIndent()
            )
        }
    }

    private fun playCustomSound(soundUriStr: String?, fallbackSoundName: String) {
        try {
            val mediaPlayer = MediaPlayer()
            if (!soundUriStr.isNullOrBlank()) {
                val uri = Uri.parse(soundUriStr)
                mediaPlayer.setDataSource(applicationContext, uri)
            } else {
                val alertUri: Uri = when (fallbackSoundName) {
                    "Alarm" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    else -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                }
                mediaPlayer.setDataSource(applicationContext, alertUri)
            }

            mediaPlayer.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            mediaPlayer.prepare()
            mediaPlayer.start()
            mediaPlayer.setOnCompletionListener {
                it.release()
            }
        } catch (e: Exception) {
            Log.e("DingDing", "Error playing custom sound", e)
        }
    }
}
