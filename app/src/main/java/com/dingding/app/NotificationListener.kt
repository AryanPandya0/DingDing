package com.dingding.app

import android.media.RingtoneManager
import android.net.Uri
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class NotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {

        val notification = sbn.notification
        val extras = notification.extras

        val data = NotificationData(
            packageName = sbn.packageName,
            title = extras.getString("android.title"),
            message = extras.getCharSequence("android.text")?.toString(),
            timestamp = sbn.postTime
        )

        val personName = data.title ?: ""

        val rule = RuleRepository.findRule(
            packageName = data.packageName,
            personName = personName
        )

        if (rule != null) {

            Log.d(
                "DINGDING_RULE",
                """
                🔥 RULE MATCHED

                App: ${data.packageName}
                Person: $personName
                Message: ${data.message}
                Sound: ${rule.soundName}
                """.trimIndent()
            )

            playCustomSound(rule.soundName)

        } else {

            Log.d(
                "DINGDING_RULE",
                """
                No rule found

                App: ${data.packageName}
                Person: $personName
                """.trimIndent()
            )
        }
    }

    private fun playCustomSound(soundName: String) {
        try {
            val alertUri: Uri = when (soundName) {
                "Alarm" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                else -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }
            val ringtone = RingtoneManager.getRingtone(applicationContext, alertUri)
            ringtone?.play()
        } catch (e: Exception) {
            Log.e("DingDing", "Error playing sound", e)
        }
    }
}
