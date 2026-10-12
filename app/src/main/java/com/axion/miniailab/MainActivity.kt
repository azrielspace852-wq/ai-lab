package com.axion.miniailab

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.util.zip.ZipInputStream

class MainActivity : ComponentActivity() {
    private lateinit var db: DatasetDb
    private val receiver=object:BroadcastReceiver(){override fun onReceive(c:Context?,i:Intent?){ if(i?.action==TrainingService.ACTION_PROGRESS) runOnUiThread { trainingProgress="${i.getIntExtra("done",0)} / ${i.getIntExtra("total",0)} · loss ${"%.4f".format(i.getFloatExtra("loss",0f))}" }; if(i?.action==TrainingService.ACTION_FINISHED)runOnUiThread{trainingStatus=i.getStringExtra("error")?.let{"Gagal: $it"}?:"Selesai · ${i.getIntExtra("steps",0)} langkah"} }}
    companion object { @Volatile var trainingProgress="Belum berjalan";@Volatile var trainingStatus="Siap" }
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);db=DatasetDb(this)
        val filter=IntentFilter().apply{addAction(TrainingService.ACTION_PROGRESS);addAction(TrainingService.ACTION_FINISHED)}
        ContextCompat.registerReceiver(this,receiver,filter,ContextCompat.RECEIVER_NOT_EXPORTED)
        if(Build.VERSION.SDK_INT>=33)requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS),11)
        setContent { MiniAiApp(db) }
    }
    override fun onDestroy(){try{unregisterReceiver(receiver)}catch(_:Exception){};super.onDestroy()}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun MiniAiApp(db:DatasetDb) {
    val ctx= LocalContext.current; val scope= rememberCoroutineScope(); var tab by remember{mutableStateOf(0)}
    var datasets by remember{mutableStateOf(db.list())}; var selected by remember{mutableStateOf<String?>(null)}
    var editorName by remember{mutableStateOf("")};var editorText by remember{mutableStateOf("")};var steps by remember{mutableStateOf("1000")};var progress by remember{mutableStateOf(MainActivity.trainingProgress)};var status by remember{mutableStateOf(MainActivity.trainingStatus)}
    val importLauncher= rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){ uri:Uri? -> if(uri!=null) scope.launch(Dispatchers.IO){try{val name=uri.lastPathSegment?.substringAfterLast(':')?.substringAfterLast('/')?:"dataset.txt";val bytes=ctx.contentResolver.openInputStream(uri)?.use{it.readBytes()}?:byteArrayOf();val text=bytes.toString(Charsets.UTF_8);val created= if(name.endsWith(".zip",true)) importZip(ctx,db,bytes) else {val s=text;db.add(name,"",s,if(bytes.size>2*1024*1024)storeObject(ctx,name,bytes) else null);1};withContext(Dispatchers.Main){datasets=db.list();Toast.makeText(ctx,"Impor selesai: $created file",Toast.LENGTH_LONG).show()}}catch(e:Exception){withContext(Dispatchers.Main){Toast.makeText(ctx,"Import gagal: ${e.message}",Toast.LENGTH_LONG).show()}}} }
    MaterialTheme(colorScheme=darkColorScheme(primary=androidx.compose.ui.graphics.Color(0xFF7DD3FC),background=androidx.compose.ui.graphics.Color(0xFF0B1220),surface=androidx.compose.ui.graphics.Color(0xFF121C2D))) {
        Scaffold(topBar={TopAppBar(title={Column{Text("MINI AI LAB",fontWeight=FontWeight.Bold);Text("Native Android · API 30+",style=MaterialTheme.typography.labelSmall)}},colors=TopAppBarDefaults.topAppBarColors(containerColor=MaterialTheme.colorScheme.surface))},bottomBar={NavigationBar { listOf("Dataset","Training","Model").forEachIndexed{idx,label->NavigationBarItem(selected=tab==idx,onClick={tab=idx},icon={},label={Text(label)})} }}) { pad ->
            Column(Modifier.fillMaxSize().padding(pad).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                when(tab){
                    0 -> {
                        Row(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically){Text("Dataset",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Spacer(Modifier.weight(1f));Button(onClick={importLauncher.launch(arrayOf("application/zip","text/*","application/json","application/octet-stream"))}){Text("Import")};Button(onClick={selected=null;editorName="dataset-${datasets.size+1}.txt";editorText=""}){Text("Baru")}}
                        OutlinedTextField(value=editorName,onValueChange={editorName=it},label={Text("Nama dataset")},modifier=Modifier.fillMaxWidth(),singleLine=true)
                        OutlinedTextField(value=editorText,onValueChange={editorText=it},label={Text("Isi dataset / JSONL")},modifier=Modifier.fillMaxWidth().weight(1f),minLines=5)
                        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button(onClick={if(editorName.isBlank()||editorText.isBlank())Toast.makeText(ctx,"Nama dan isi wajib diisi",Toast.LENGTH_SHORT).show() else {db.add(editorName.trim(),"",editorText);datasets=db.list();selected=null;editorText="";Toast.makeText(ctx,"Dataset disimpan di SQLite",Toast.LENGTH_SHORT).show()}},modifier=Modifier.weight(1f)){Text("Simpan sebagai baru")};OutlinedButton(onClick={datasets=db.list();editorText="";editorName=""}){Text("Bersihkan")}}
                        HorizontalDivider();Text("Tersimpan (${datasets.size})",fontWeight=FontWeight.SemiBold)
                        LazyColumn(verticalArrangement=Arrangement.spacedBy(6.dp),modifier=Modifier.weight(1f)){items(datasets,key={it.id}){d->Card(onClick={selected=d.id;editorName=d.name;editorText=d.content?:"[File besar disimpan di object storage internal; editor belum memuat isi file.]"},modifier=Modifier.fillMaxWidth()){Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(d.name,fontWeight=FontWeight.SemiBold);Text("${d.size/1024} KB · ${d.path.ifBlank{"/"}}",style=MaterialTheme.typography.bodySmall)};TextButton(onClick={db.delete(d.id);datasets=db.list();if(selected==d.id){selected=null;editorText=""}}){Text("Hapus")}}}}}
                    }
                    1 -> {Text("Training",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold);Text("Training berjalan di foreground service agar dapat berlanjut saat aplikasi tidak di depan.");OutlinedTextField(value=steps,onValueChange={steps=it.filter(Char::isDigit).take(7)},label={Text("Langkah training")},singleLine=true);Text("Status: $status");Text("Progres: $progress");Button(onClick={val text=db.trainingText();if(text.isBlank())Toast.makeText(ctx,"Tambahkan dataset terlebih dahulu",Toast.LENGTH_LONG).show() else {val n=(steps.toIntOrNull()?:1000).coerceIn(1,500000);val i=Intent(ctx,TrainingService::class.java).setAction(TrainingService.ACTION_START).putExtra(TrainingService.EXTRA_TEXT,text).putExtra(TrainingService.EXTRA_STEPS,n);ContextCompat.startForegroundService(ctx,i);status="Training dimulai";tab=1}},modifier=Modifier.fillMaxWidth()){Text("Mulai training")};OutlinedButton(onClick={ctx.startService(Intent(ctx,TrainingService::class.java).setAction(TrainingService.ACTION_STOP));status="Permintaan berhenti dikirim"},modifier=Modifier.fillMaxWidth()){Text("Hentikan training")};Text("Dataset JSONL diparsing menjadi isi teks user/assistant; key JSON dan tanda struktur tidak dilatih.") }
                    else -> {Text("Model",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold);val models=db.models();if(models.isEmpty())Text("Belum ada model. Jalankan training untuk menghasilkan file .bin.") else LazyColumn{items(models){m->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp)){Text(m.second,fontWeight=FontWeight.SemiBold);Text(m.third,style=MaterialTheme.typography.bodySmall);TextButton(onClick={val f=File(m.third);if(f.exists())Toast.makeText(ctx,"Model ${f.name} · ${f.length()/1024} KB",Toast.LENGTH_LONG).show()}){Text("Info model")}}}}};Text("Format .bin milik Mini AI Lab (MAIB v1). Tidak kompatibel dengan PyTorch atau model eksternal.") }
                }
            }
        }
    }
}

private fun storeObject(ctx:Context,name:String,bytes:ByteArray):File { val dir=File(ctx.filesDir,"object-storage/media").apply{mkdirs()};return File(dir,"${System.currentTimeMillis()}-${name.replace(Regex("[^A-Za-z0-9._-]"),"_")}").apply{writeBytes(bytes)} }
private fun importZip(ctx:Context,db:DatasetDb,bytes:ByteArray):Int {
    var count=0
    ZipInputStream(BufferedInputStream(bytes.inputStream())).use { zip -> var e=zip.nextEntry;while(e!=null){if(!e.isDirectory && !e.name.startsWith("__MACOSX/") && !e.name.substringAfterLast('/').startsWith(".")) {val name=e.name.replace('\\','/');val data=zip.readBytes();if(name.endsWith(".png",true)||name.endsWith(".jpg",true)||name.endsWith(".jpeg",true)||name.endsWith(".webp",true)||name.endsWith(".mp4",true)||name.endsWith(".wav",true)||name.endsWith(".mp3",true)){storeObject(ctx,name,data)}else if(name.endsWith(".jsonl",true)||name.endsWith(".json",true)||name.endsWith(".txt",true)||name.endsWith(".md",true)||name.endsWith(".csv",true)){val text=data.toString(Charsets.UTF_8);val parent=name.substringBeforeLast('/',"");db.add(name.substringAfterLast('/'),parent,text,if(data.size>2*1024*1024)storeObject(ctx,name,data) else null);count++}};zip.closeEntry();e=zip.nextEntry}}
    return count
}
