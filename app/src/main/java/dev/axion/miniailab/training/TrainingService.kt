package dev.axion.miniailab.training

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import dev.axion.miniailab.MainActivity
import dev.axion.miniailab.R
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class TrainingService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var wakeLock: PowerManager.WakeLock? = null
    override fun onCreate() {
        super.onCreate()
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MiniAILab:Training").apply { setReferenceCounted(false); acquire(4 * 60 * 60 * 1000L) }
        if (Build.VERSION.SDK_INT >= 26) getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "Training", NotificationManager.IMPORTANCE_LOW))
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, notification("Training sedang berjalan", 0))
        // Training engine posts progress through updateProgress; service lifecycle is independent of the UI.
        return START_NOT_STICKY
    }
    fun updateProgress(done: Int, total: Int) {
        val p = if (total <= 0) 0 else (done * 100 / total).coerceIn(0,100)
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification("Training: $p%", p))
    }
    private fun notification(message: String, progress: Int) = NotificationCompat.Builder(this, CHANNEL)
        .setSmallIcon(android.R.drawable.stat_notify_sync).setContentTitle("Mini AI Lab").setContentText(message)
        .setProgress(100, progress, false).setOngoing(progress < 100).build()
    override fun onDestroy() { scope.cancel(); wakeLock?.let { if(it.isHeld) it.release() }; wakeLock = null; super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null
    companion object { const val CHANNEL = "training"; const val NOTIFICATION_ID = 4102 }
}
