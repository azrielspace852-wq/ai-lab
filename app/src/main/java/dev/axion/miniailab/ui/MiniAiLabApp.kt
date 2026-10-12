package dev.axion.miniailab.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.axion.miniailab.data.DatasetEntity
import dev.axion.miniailab.data.DatasetRepository
import dev.axion.miniailab.model.ModelBin
import dev.axion.miniailab.training.TrainingService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.*
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject

private val navItems = listOf("Dataset", "Konfigurasi", "Pre-Train", "Fine-Tune", "Model", "Chat", "Panduan", "Pengaturan", "SDK", "Privasi")

@HiltViewModel
class LabViewModel @Inject constructor(private val repo: DatasetRepository) : ViewModel() {
    val datasets = repo.observe().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    var selectedId by mutableStateOf<String?>(null); private set
    var status by mutableStateOf("Siap")
    var language by mutableStateOf("Indonesia")
    var dark by mutableStateOf(true)
    var cw by mutableIntStateOf(4); var dim by mutableIntStateOf(32); var hidden by mutableIntStateOf(96)
    var vocab by mutableIntStateOf(300); var batch by mutableIntStateOf(16); var lr by mutableStateOf("0.01"); var steps by mutableIntStateOf(2000); var seed by mutableIntStateOf(42)
    var modelName by mutableStateOf("model-baru")
    var prompt by mutableStateOf(""); var answer by mutableStateOf("Belum ada model aktif. Latih di tab Pre-Train atau pilih model di tab Model.")
    fun select(id: String?) { selectedId = id }
    fun save(item: DatasetEntity) { viewModelScope.launch { repo.save(item); selectedId = item.id; status = "Dataset disimpan" } }
    fun create(name: String, folder: Boolean=false) { viewModelScope.launch { repo.create(name, folder); status = "Item dibuat" } }
    fun delete(item: DatasetEntity) { viewModelScope.launch { repo.delete(item.id); if(selectedId==item.id) selectedId=null; status="Item dihapus" } }
    fun selected(): DatasetEntity? = datasets.value.firstOrNull { it.id == selectedId }
    fun runChat() { answer = "Runtime model belum dimuat. Impor model .bin v1 pada halaman Model untuk menjalankan inferensi." }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)\nfun MiniAiLabApp(vm: LabViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val data by vm.datasets.collectAsState()
    var page by remember { mutableStateOf("Dataset") }
    var editor by remember { mutableStateOf(false) }
    var editText by remember { mutableStateOf("") }
    var editName by remember { mutableStateOf("") }
    var newName by remember { mutableStateOf("") }
    var showCreate by remember { mutableStateOf(false) }
    var folderCreate by remember { mutableStateOf(false) }
    var importedModel by remember { mutableStateOf("Belum ada model dimuat") }
    var modelInfo by remember { mutableStateOf("Format yang didukung: Mini AI Lab .bin v1 (MALAB001)") }
    val zipImport = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    ZipInputStream(BufferedInputStream(stream)).use { zis ->
                        val items = mutableListOf<DatasetEntity>()
                        while (true) {
                            val entry = zis.nextEntry ?: break
                            val safe = entry.name.replace('\\','/')
                            if (safe.startsWith("/") || safe.split('/').any { it == ".." } || entry.isDirectory) { zis.closeEntry(); continue }
                            val bytes = zis.readBytes()
                            val text = bytes.toString(Charsets.UTF_8)
                            val parts = safe.split('/').filter { it.isNotBlank() }
                            if (parts.isNotEmpty() && !safe.endsWith(".attachments/")) {
                                items += DatasetEntity(UUID.randomUUID().toString(), parts.last(), content=text)
                            }
                            zis.closeEntry()
                        }
                        items.forEach { vm.save(it) }
                        Toast.makeText(context, "Impor ZIP: ${items.size} item teks", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) { Toast.makeText(context, "Impor gagal: ${e.message}", Toast.LENGTH_LONG).show() }
        }
    }
    val zipExport = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri: Uri? ->
        if (uri != null) try {
            context.contentResolver.openOutputStream(uri)?.use { output ->
                ZipOutputStream(BufferedOutputStream(output)).use { zos ->
                    data.filter { !it.isFolder }.forEach { item ->
                        zos.putNextEntry(ZipEntry(item.name.replace("/", "_")))
                        zos.write(item.content.toByteArray(Charsets.UTF_8)); zos.closeEntry()
                    }
                }
            }
            Toast.makeText(context, "ZIP berhasil diekspor", Toast.LENGTH_SHORT).show()
        } catch(e: Exception) { Toast.makeText(context, "Ekspor gagal: ${e.message}", Toast.LENGTH_LONG).show() }
    }
    val modelImport = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) try {
            val bytes = context.contentResolver.openInputStream(uri)!!.use { it.readBytes() }
            val parsed = ModelBin.read(bytes)
            importedModel = parsed.json.optString("name", "model")
            modelInfo = "Model valid: $importedModel · V=${parsed.json.optInt("V")} · tensors=${parsed.tensors.keys.joinToString()}"
            Toast.makeText(context, "Header .bin valid; bobot berhasil dibaca.", Toast.LENGTH_LONG).show()
        } catch(e: Exception) { modelInfo = "Gagal membaca model: ${e.message}" }
    }

    MaterialTheme(colorScheme = if (vm.dark) darkColorScheme() else lightColorScheme()) {
        Scaffold(topBar = {
            TopAppBar(title = { Column { Text("Mini AI Lab", fontWeight=FontWeight.Bold); Text("Native Android · ${vm.status}", style=MaterialTheme.typography.labelSmall) } },
                navigationIcon = { IconButton(onClick={ page = if(page=="Dataset") "Panduan" else "Dataset" }) { Icon(Icons.Default.Menu, "Menu") } },
                actions = { IconButton(onClick={vm.dark=!vm.dark}) { Icon(if(vm.dark) Icons.Default.LightMode else Icons.Default.DarkMode, "Tema") } })
        }) { padding ->
            Row(Modifier.fillMaxSize().padding(padding)) {
                NavigationRail(containerColor=MaterialTheme.colorScheme.surface) {
                    navItems.forEach { item ->
                        NavigationRailItem(selected=page==item, onClick={page=item;editor=false}, icon={
                            Icon(when(item) {
                                "Dataset" -> Icons.Default.FolderOpen; "Konfigurasi" -> Icons.Default.Tune
                                "Pre-Train" -> Icons.Default.PlayArrow; "Fine-Tune" -> Icons.Default.Build
                                "Model" -> Icons.Default.Memory; "Chat" -> Icons.Default.Chat
                                "Panduan" -> Icons.Default.MenuBook; "Pengaturan" -> Icons.Default.Settings
                                "SDK" -> Icons.Default.Code; else -> Icons.Default.Policy
                            }, contentDescription=item)
                        }, label={Text(item, style=MaterialTheme.typography.labelSmall)})
                    }
                }
                Column(Modifier.fillMaxSize().padding(horizontal=14.dp, vertical=10.dp)) {
                    when(page) {
                        "Dataset" -> {
                            Text("Explorer", style=MaterialTheme.typography.headlineSmall)
                            Text("Dataset files and nested folders · ${data.size} item", style=MaterialTheme.typography.bodySmall)
                            Row(horizontalArrangement=Arrangement.spacedBy(8.dp), modifier=Modifier.padding(vertical=8.dp)) {
                                Button(onClick={folderCreate=true;showCreate=true}) { Text("＋ Folder") }
                                Button(onClick={folderCreate=false;showCreate=true}) { Text("＋ Dataset") }
                                OutlinedButton(onClick={zipImport.launch(arrayOf("application/zip","application/x-zip-compressed"))}) { Text("Import ZIP") }
                                OutlinedButton(onClick={zipExport.launch("mini-ai-lab-datasets.zip")}) { Text("Export ZIP") }
                            }
                            LazyColumn(Modifier.weight(1f)) {
                                items(data, key={it.id}) { item ->
                                    Card(Modifier.fillMaxWidth().padding(vertical=3.dp), onClick={
                                        vm.select(item.id); editName=item.name; editText=item.content; editor=true
                                    }) {
                                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment=Alignment.CenterVertically) {
                                            Icon(if(item.isFolder) Icons.Default.Folder else Icons.Default.Description, null)
                                            Column(Modifier.weight(1f).padding(start=10.dp)) {
                                                Text(item.name, fontWeight=FontWeight.SemiBold)
                                                Text(if(item.isFolder) "Folder" else "${item.content.length} karakter · ${item.content.lines().size} baris", style=MaterialTheme.typography.bodySmall)
                                            }
                                            IconButton(onClick={vm.delete(item)}) { Icon(Icons.Default.Delete, "Hapus") }
                                        }
                                    }
                                }
                            }
                            if(editor) {
                                val item=vm.selected()
                                HorizontalDivider(Modifier.padding(vertical=8.dp))
                                Text("Editor Dataset", style=MaterialTheme.typography.titleLarge)
                                OutlinedTextField(editName, {editName=it}, label={Text("Nama file")}, modifier=Modifier.fillMaxWidth())
                                OutlinedTextField(editText, {editText=it}, label={Text("Konten dataset")}, modifier=Modifier.fillMaxWidth().weight(1f, fill=false).heightIn(min=120.dp, max=300.dp))
                                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                                    Button(onClick={ if(item!=null) vm.save(item.copy(name=editName,content=editText)) }) { Text("Simpan") }
                                    OutlinedButton(onClick={editor=false}) { Text("Tutup editor") }
                                }
                            }
                        }
                        "Konfigurasi" -> ConfigScreen(vm)
                        "Pre-Train" -> {
                            Text("Pre-Training (dari nol)", style=MaterialTheme.typography.headlineSmall)
                            Text("Konfigurasi jaringan kecil: embedding + tanh hidden layer + softmax output. Format bobot kompatibel dengan Mini AI Lab .bin v1.")
                            OutlinedTextField(vm.modelName,{vm.modelName=it},label={Text("Nama model")},modifier=Modifier.fillMaxWidth())
                            Text("Dataset siap: ${data.count{!it.isFolder && it.content.isNotBlank()}}")
                            Button(onClick={
                                val intent=Intent(context,TrainingService::class.java)
                                androidx.core.content.ContextCompat.startForegroundService(context,intent)
                                vm.status="Training service aktif"
                                Toast.makeText(context,"Service aktif. Engine training penuh perlu dijalankan sebelum model dapat dibuat.",Toast.LENGTH_LONG).show()
                            }, modifier=Modifier.padding(top=8.dp)) { Text("▶ Run Pre-Train") }
                            OutlinedButton(onClick={context.stopService(Intent(context,TrainingService::class.java))}) { Text("■ Stop") }
                            Text("Catatan: proses training native lengkap belum diaktifkan dalam build ini; tombol di atas menguji lifecycle service saja.", color=MaterialTheme.colorScheme.error)
                        }
                        "Fine-Tune" -> {
                            Text("Fine-Tune Model", style=MaterialTheme.typography.headlineSmall)
                            OutlinedTextField(vm.modelName,{vm.modelName=it},label={Text("Nama model hasil")},modifier=Modifier.fillMaxWidth())
                            OutlinedTextField(vm.lr,{vm.lr=it},label={Text("Learning rate")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),modifier=Modifier.fillMaxWidth())
                            OutlinedTextField(vm.steps.toString(),{vm.steps=it.toIntOrNull()?:vm.steps},label={Text("Steps")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),modifier=Modifier.fillMaxWidth())
                            Text("Pilih model dasar pada halaman Model. Bobot dasar harus kompatibel dengan format Mini AI Lab.")
                            Button(onClick={Toast.makeText(context,"Fine-tuning belum dieksekusi: engine optimisasi native belum dipasang.",Toast.LENGTH_LONG).show()}) { Text("▶ Mulai Fine-Tune") }
                        }
                        "Model" -> {
                            Text("Daftar Model", style=MaterialTheme.typography.headlineSmall)
                            Text(modelInfo)
                            Text("Model aktif: $importedModel", modifier=Modifier.padding(vertical=8.dp))
                            Button(onClick={modelImport.launch(arrayOf("application/octet-stream","application/x-binary","*/*"))}) { Text("📥 Import Model .bin") }
                            OutlinedButton(onClick={importedModel="Belum ada model dimuat";modelInfo="Format yang didukung: Mini AI Lab .bin v1 (MALAB001)")}) { Text("Unload") }
                            Text("Validasi file .bin membaca header dan seluruh tensor; inferensi/generasi native belum diaktifkan.", color=MaterialTheme.colorScheme.error)
                        }
                        "Chat" -> {
                            Text("Chat", style=MaterialTheme.typography.headlineSmall)
                            Card(Modifier.fillMaxWidth().weight(1f)) { Column(Modifier.verticalScroll(rememberScrollState()).padding(12.dp)) {
                                Text("Sistem: ${vm.answer}")
                                if(vm.prompt.isNotBlank()) Text("Anda: ${vm.prompt}", modifier=Modifier.padding(top=12.dp))
                            } }
                            OutlinedTextField(vm.prompt,{vm.prompt=it},label={Text("Tulis prompt…")},modifier=Modifier.fillMaxWidth())
                            Button(onClick={vm.runChat(); vm.prompt=""},modifier=Modifier.padding(top=8.dp)) { Text("Kirim") }
                        }
                        "Panduan" -> GuideScreen()
                        "Pengaturan" -> {
                            Text("Pengaturan",style=MaterialTheme.typography.headlineSmall)
                            Row(verticalAlignment=Alignment.CenterVertically) { Text("Tema gelap",Modifier.weight(1f)); Switch(vm.dark,{vm.dark=it}) }
                            Text("Bahasa")
                            Row { FilterChip(selected=vm.language=="Indonesia",onClick={vm.language="Indonesia"},label={Text("Indonesia")}); Spacer(Modifier.width(8.dp)); FilterChip(selected=vm.language=="English",onClick={vm.language="English"},label={Text("English")}) }
                            Button(onClick={Toast.makeText(context,"Pengaturan diterapkan.",Toast.LENGTH_SHORT).show()}) { Text("Simpan pengaturan") }
                        }
                        "SDK" -> {
                            Text("Mini AI Lab SDK",style=MaterialTheme.typography.headlineSmall)
                            Text("Native SDK · API Kotlin", style=MaterialTheme.typography.titleMedium)
                            Text("ModelBin.read(bytes) memvalidasi format Mini AI Lab .bin v1. Model native sebaiknya dipakai melalui repository model dan runtime Kotlin, bukan WebView atau JavaScript.")
                            CodePanel("val model = ModelBin.read(file.readBytes())\nval header = model.json\nval tensors = model.tensors")
                            Text("Status",style=MaterialTheme.typography.titleMedium); Text("Parser .bin tersedia; runtime generate native belum lengkap.")
                        }
                        else -> {
                            Text("Kebijakan Privasi",style=MaterialTheme.typography.headlineSmall)
                            Text("1. Penyimpanan data\nDataset disimpan lokal di SQLite (Room).\n\n2. File yang Anda impor\nFile ZIP dan model diproses di perangkat.\n\n3. Koneksi pihak ketiga\nAplikasi tidak mengunggah dataset secara otomatis.\n\n4. Berbagi dan keamanan\nJangan menyimpan rahasia sensitif di dataset.\n\n5. Retensi dan penghapusan\nDataset dapat dihapus dari halaman Dataset.")
                        }
                    }
                }
            }
        }
    }
    if(showCreate) AlertDialog(onDismissRequest={showCreate=false},title={Text(if(folderCreate) "Folder baru" else "Dataset baru")},text={
        OutlinedTextField(newName,{newName=it},label={Text("Nama")})
    },confirmButton={TextButton(onClick={vm.create(newName,folderCreate);newName="";showCreate=false}){Text("Buat")}},dismissButton={TextButton(onClick={showCreate=false}){Text("Batal")}})
}

@Composable private fun ConfigScreen(vm: LabViewModel) {
    Text("Arsitektur & Konfigurasi",style=MaterialTheme.typography.headlineSmall)
    NumberSetting("Context window (cw)",vm.cw,{vm.cw=it},1,16)
    NumberSetting("Embedding dimension (D)",vm.dim,{vm.dim=it},4,128)
    NumberSetting("Hidden dimension (H)",vm.hidden,{vm.hidden=it},4,512)
    NumberSetting("Vocabulary size",vm.vocab,{vm.vocab=it},20,5000)
    NumberSetting("Batch size",vm.batch,{vm.batch=it},1,128)
    OutlinedTextField(vm.lr,{vm.lr=it},label={Text("Learning rate")},modifier=Modifier.fillMaxWidth())
    NumberSetting("Training steps",vm.steps,{vm.steps=it},10,200000)
    NumberSetting("Seed",vm.seed,{vm.seed=it},0,Int.MAX_VALUE)
    Text("Parameter estimate: ${vm.vocab*vm.dim + vm.cw*vm.dim*vm.hidden + vm.hidden + vm.hidden*vm.vocab + vm.vocab}")
}
@Composable private fun NumberSetting(label:String,value:Int,onChange:(Int)->Unit,min:Int,max:Int) {
    OutlinedTextField(value.toString(),{ it.toIntOrNull()?.let { n -> onChange(n.coerceIn(min,max)) } },label={Text(label)},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),modifier=Modifier.fillMaxWidth().padding(vertical=3.dp),singleLine=true)
}
@Composable private fun GuideScreen() {
    Column(Modifier.verticalScroll(rememberScrollState())) {
        Text("Panduan Mini AI Lab",style=MaterialTheme.typography.headlineSmall)
        listOf("1. Dataset Multiformat" to "Kelola dataset teks dan folder. Format konten dapat berupa teks bebas atau JSONL; sanitasi memisahkan metadata dari konten aktual.",
            "2. Mask JSON — Kontrol Tokenisasi Granular" to "Gunakan mask untuk memilih field yang menjadi konten training. Jangan melatih metadata/struktur serialisasi sebagai target bahasa.",
            "3. Media Attachments" to "Lampiran media perlu disimpan sebagai file lokal; versi port ini belum mengaktifkan pratinjau/tokenisasi media.",
            "4. Folder & Subfolder" to "Dataset memiliki parentId untuk struktur folder.",
            "5. Export & Import" to "Import dan export ZIP tersedia untuk teks. Implementasi lanjutan perlu memulihkan hierarchy dan asosiasi attachments.",
            "6. Output AI Multiformat" to "Runtime native perlu menghubungkan token output dengan renderer teks/media.",
            "7. Tips" to "Mulai dari dataset kecil dan konfigurasi dimensi yang sesuai RAM perangkat.").forEach { (h,b) ->
            Text(h,style=MaterialTheme.typography.titleMedium,modifier=Modifier.padding(top=12.dp)); Text(b)
        }
    }
}
@Composable private fun CodePanel(code:String) {
    Surface(color=MaterialTheme.colorScheme.surfaceVariant,shape=MaterialTheme.shapes.medium,modifier=Modifier.fillMaxWidth().padding(vertical=8.dp)) {
        Text(code,modifier=Modifier.horizontalScroll(rememberScrollState()).padding(12.dp),style=MaterialTheme.typography.bodyMedium)
    }
}
