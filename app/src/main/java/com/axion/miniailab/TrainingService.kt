package com.axion.miniailab

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

class TrainingService : Service() {
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Default)
    private var job: Job?=null; private val stop=AtomicBoolean(false); private var lock:PowerManager.WakeLock?=null
    override fun onCreate(){super.onCreate();createChannel()}
    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int {
        when(intent?.action) {
            ACTION_STOP -> { stop.set(true); updateNotification("Menghentikan training…",0); return START_NOT_STICKY }
            ACTION_START -> startTraining(intent.getStringExtra(EXTRA_TEXT).orEmpty(),intent.getIntExtra(EXTRA_STEPS,1000))
        }
        return START_NOT_STICKY
    }
    private fun startTraining(text:String,steps:Int) {
        if(job?.isActive==true)return
        stop.set(false)
        startForeground(NOTIFICATION_ID,notification("Menyiapkan training…",0))
        val pm=getSystemService(Context.POWER_SERVICE) as PowerManager
        lock=pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"MiniAILab:Training").apply{setReferenceCounted(false);acquire(4*60*60*1000L)}
        job=scope.launch {
            try {
                val out=File(filesDir,"models/model-${System.currentTimeMillis()}.bin")
                val result=ModelTrainer.train(text,out,steps,{done,total,loss->updateNotification("Training $done/$total · loss ${"%.4f".format(loss)}",(done*100/total).coerceIn(0,100));sendBroadcast(Intent(ACTION_PROGRESS).setPackage(packageName).putExtra("done",done).putExtra("total",total).putExtra("loss",loss))},{stop.get()})
                getSharedPreferences("training",MODE_PRIVATE).edit().putString("last_model",out.absolutePath).putInt("last_steps",result.steps).putFloat("last_loss",result.loss.toFloat()).apply()
                DatasetDb(this@TrainingService).saveModel(UUID.randomUUID().toString(), "model-${System.currentTimeMillis()}", out, result.steps, result.loss)
                updateNotification(if(stop.get())"Training dihentikan · ${result.steps} langkah" else "Training selesai · ${result.steps} langkah",100)
                sendBroadcast(Intent(ACTION_FINISHED).setPackage(packageName).putExtra("path",out.absolutePath).putInt("steps",result.steps).putFloat("loss",result.loss.toFloat()).putBoolean("stopped",stop.get()))
            } catch(e:Exception) { updateNotification("Training gagal: ${e.message ?: "error"}",0);sendBroadcast(Intent(ACTION_FINISHED).setPackage(packageName).putExtra("error",e.message ?: "Training gagal")) }
            finally { try{lock?.let{if(it.isHeld)it.release()}}catch(_:Exception){};lock=null;stopSelf() }
        }
    }
    private fun createChannel(){if(Build.VERSION.SDK_INT>=26){val nm=getSystemService(NotificationManager::class.java);nm.createNotificationChannel(NotificationChannel(CHANNEL,"Training AI",NotificationManager.IMPORTANCE_LOW).apply{description="Progres training model"})}}
    private fun notification(msg:String,progress:Int):Notification {
        val stopIntent=PendingIntent.getService(this,2,Intent(this,TrainingService::class.java).setAction(ACTION_STOP),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.stat_notify_sync).setContentTitle("Mini AI Lab").setContentText(msg).setOnlyAlertOnce(true).setOngoing(progress in 1..99).setProgress(100,progress,false).addAction(0,"Hentikan",stopIntent).build()
    }
    private fun updateNotification(msg:String,progress:Int){getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID,notification(msg,progress))}
    override fun onBind(intent:Intent?):IBinder?=null
    override fun onDestroy(){scope.cancel();try{lock?.let{if(it.isHeld)it.release()}}catch(_:Exception){};super.onDestroy()}
    companion object { const val ACTION_START="com.axion.miniailab.START";const val ACTION_STOP="com.axion.miniailab.STOP";const val ACTION_PROGRESS="com.axion.miniailab.PROGRESS";const val ACTION_FINISHED="com.axion.miniailab.FINISHED";const val EXTRA_TEXT="training_text";const val EXTRA_STEPS="steps";private const val CHANNEL="training";private const val NOTIFICATION_ID=42 }
}
