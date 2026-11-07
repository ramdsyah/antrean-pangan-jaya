package id.pasarjaya.antreanpangan

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceRequest
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import android.util.Base64

private const val OFFICIAL_URL = "https://antrianpanganbersubsidi.pasarjaya.co.id/"
private val Green = Color(0xFF087A48)
private val PaleGreen = Color(0xFFEAF6EF)

data class Profile(
    val id: String,
    val label: String,
    val name: String,
    val nik: String,
    val kk: String,
    val phone: String,
    val address: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = SecureProfileStore(this)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Green,
                    secondary = Color(0xFF2C8A62),
                    background = Color(0xFFF7FAF8),
                    surface = Color.White
                )
            ) {
                AntreanApp(store)
            }
        }
    }
}

@Composable
private fun AntreanApp(store: SecureProfileStore) {
    val context = LocalContext.current
    var profiles by remember { mutableStateOf(store.load()) }
    var selectedId by remember { mutableStateOf(profiles.firstOrNull()?.id) }
    var editor by remember { mutableStateOf<Profile?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var showSite by remember { mutableStateOf(false) }
    val selected = profiles.firstOrNull { it.id == selectedId }

    if (showSite) {
        SiteScreen(
            profile = selected,
            onBack = { showSite = false },
            onOpenBrowser = {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(OFFICIAL_URL)))
            }
        )
        return
    }

    Scaffold(
        topBar = {
            Column(Modifier.fillMaxWidth().background(Green).padding(horizontal = 20.dp, vertical = 18.dp)) {
                Text("ANTREAN PANGAN JAYA", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text("Asisten pendaftaran pangan bersubsidi", color = Color.White.copy(alpha = .88f), fontSize = 12.sp)
            }
        },
        containerColor = Color(0xFFF7FAF8)
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = PaleGreen), shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Pendaftaran lebih praktis", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Green)
                        Text("Simpan data beberapa anggota keluarga. Pilih satu profil untuk membantu mengisi kolom formulir yang cocok.")
                        Text("Pastikan data benar sebelum melanjutkan ke situs resmi.", fontSize = 12.sp, color = Color.DarkGray)
                    }
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Profil tersimpan", fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Button(onClick = { editor = null; showEditor = true }) { Text("+ Tambah") }
                }
            }
            if (profiles.isEmpty()) {
                item {
                    Card {
                        Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Belum ada profil")
                            Text("Tambahkan profil pertama Anda.", color = Color.Gray, fontSize = 13.sp)
                        }
                    }
                }
            }
            items(profiles, key = { it.id }) { p ->
                ProfileCard(
                    profile = p,
                    selected = p.id == selectedId,
                    onSelect = { selectedId = p.id },
                    onEdit = { editor = p; showEditor = true },
                    onDelete = {
                        profiles = profiles.filterNot { it.id == p.id }
                        if (selectedId == p.id) selectedId = profiles.firstOrNull()?.id
                        store.save(profiles)
                    }
                )
            }
            item {
                Button(
                    onClick = { showSite = true },
                    enabled = selected != null,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Lanjut ke pendaftaran resmi", fontSize = 15.sp) }
            }
            item {
                Text(
                    "Catatan: aplikasi ini bukan layanan resmi Pasar Jaya. Kuota, verifikasi, pengiriman pendaftaran, dan nomor antrean hanya ditentukan oleh situs resmi.",
                    fontSize = 11.sp, color = Color.Gray
                )
            }
        }
    }

    if (showEditor) {
        ProfileEditor(
            initial = editor,
            onDismiss = { showEditor = false },
            onSave = { p ->
                profiles = if (editor == null) profiles + p else profiles.map { if (it.id == p.id) p else it }
                store.save(profiles)
                if (selectedId == null) selectedId = p.id
                showEditor = false
            }
        )
    }
}

@Composable
private fun ProfileCard(
    profile: Profile,
    selected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = if (selected) PaleGreen else Color.White),
        border = if (selected) androidx.compose.foundation.BorderStroke(1.5.dp, Green) else null,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(profile.label, fontWeight = FontWeight.Bold, color = Green, modifier = Modifier.weight(1f))
                if (selected) Text("DIPILIH", color = Green, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Text(profile.name.ifBlank { "Nama belum diisi" }, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text("NIK: " + mask(profile.nik), color = Color.Gray, fontSize = 12.sp)
            Text("No. HP: " + maskPhone(profile.phone), color = Color.Gray, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onSelect, modifier = Modifier.weight(1f)) { Text(if (selected) "Terpilih" else "Pilih profil") }
                OutlinedButton(onClick = onEdit) { Text("Edit") }
                TextButton(onClick = onDelete) { Text("Hapus", color = Color(0xFFB3261E)) }
            }
        }
    }
}

private fun mask(s: String): String = if (s.length < 5) "—" else "••••••••" + s.takeLast(4)
private fun maskPhone(s: String): String = if (s.length < 5) "—" else "••••" + s.takeLast(4)

@Composable
private fun ProfileEditor(initial: Profile?, onDismiss: () -> Unit, onSave: (Profile) -> Unit) {
    var label by remember { mutableStateOf(initial?.label ?: "Profil A") }
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var nik by remember { mutableStateOf(initial?.nik ?: "") }
    var kk by remember { mutableStateOf(initial?.kk ?: "") }
    var phone by remember { mutableStateOf(initial?.phone ?: "") }
    var address by remember { mutableStateOf(initial?.address ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Tambah profil" else "Edit profil") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.heightIn(max = 480.dp)) {
                item { OutlinedTextField(label, { label = it }, label = { Text("Nama profil (mis. A / Ibu)") }, singleLine = true) }
                item { OutlinedTextField(name, { name = it }, label = { Text("Nama lengkap") }, singleLine = true) }
                item { OutlinedTextField(nik, { nik = it.filter(Char::isDigit).take(16) }, label = { Text("NIK (16 digit)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true) }
                item { OutlinedTextField(kk, { kk = it.filter(Char::isDigit).take(16) }, label = { Text("Nomor KK (16 digit)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true) }
                item { OutlinedTextField(phone, { phone = it.filter { c -> c.isDigit() || c == '+' }.take(16) }, label = { Text("Nomor HP") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true) }
                item { OutlinedTextField(address, { address = it }, label = { Text("Alamat") }, minLines = 2) }
                item { Text("Data disimpan terenkripsi di perangkat ini. Jangan bagikan tangkapan layar yang menampilkan data pribadi.", fontSize = 11.sp, color = Color.Gray) }
            }
        },
        confirmButton = {
            Button(
                enabled = label.isNotBlank() && name.isNotBlank(),
                onClick = {
                    onSave(Profile(initial?.id ?: java.util.UUID.randomUUID().toString(), label.trim(), name.trim(), nik, kk, phone, address))
                }
            ) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun SiteScreen(profile: Profile?, onBack: () -> Unit, onOpenBrowser: () -> Unit) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var status by remember { mutableStateOf("Membuka situs resmi…") }
    Column(Modifier.fillMaxSize().background(Color.White)) {
        Row(
            Modifier.fillMaxWidth().background(Green).padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) { Text("← Kembali", color = Color.White) }
            Text("Situs Pasar Jaya", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        }
        if (profile != null) {
            Card(Modifier.fillMaxWidth().padding(10.dp), colors = CardDefaults.cardColors(containerColor = PaleGreen)) {
                Column(Modifier.padding(10.dp)) {
                    Text("Profil aktif: ${profile.label} — ${profile.name}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text("Tekan tombol Isi data setelah formulir tampil. Periksa hasilnya sebelum mengirim.", fontSize = 11.sp)
                    Button(
                        onClick = { webView?.evaluateJavascript(buildAutofillScript(profile), null) },
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                    ) { Text("Isi otomatis dari profil") }
                }
            }
        }
        Text(status, fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
        AndroidView(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            factory = { ctx ->
                WebView(ctx).apply {
                    webView = this
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.javaScriptCanOpenWindowsAutomatically = false
                    webChromeClient = WebChromeClient()
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            status = "Situs terbuka. Pilih formulir, lalu tekan Isi otomatis."
                        }
                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            val uri = request?.url ?: return false
                            val host = uri.host.orEmpty()
                            return !(host == "antrianpanganbersubsidi.pasarjaya.co.id" || host.endsWith(".pasarjaya.co.id"))
                        }
                    }
                    loadUrl(OFFICIAL_URL)
                }
            },
            update = { webView = it }
        )
        Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onOpenBrowser, modifier = Modifier.weight(1f)) { Text("Buka di browser") }
            OutlinedButton(onClick = { webView?.reload() }, modifier = Modifier.weight(1f)) { Text("Muat ulang") }
        }
    }
}

private fun buildAutofillScript(p: Profile): String {
    val data = JSONObject().apply {
        put("name", p.name)
        put("nik", p.nik)
        put("kk", p.kk)
        put("phone", p.phone)
        put("address", p.address)
    }
    return """
        (function() {
          const data = ${data};
          const aliases = {
            name: ['nama','name','nama lengkap','pemohon'],
            nik: ['nik','nomor induk kependudukan','no ktp','nomor ktp'],
            kk: ['no kk','nomor kk','kartu keluarga','kk'],
            phone: ['no hp','nomor hp','telepon','telephone','phone','whatsapp','wa'],
            address: ['alamat','address']
          };
          const norm = s => (s || '').toString().toLowerCase().replace(/[^a-z0-9 ]/g,' ').replace(/\s+/g,' ').trim();
          const fields = Array.from(document.querySelectorAll('input:not([type=hidden]), textarea'));
          let filled = 0;
          for (const el of fields) {
            const labelText = el.labels ? Array.from(el.labels).map(x => x.innerText).join(' ') : '';
            const hay = norm([labelText,el.getAttribute('aria-label'),el.placeholder,el.name,el.id].join(' '));
            for (const key of Object.keys(aliases)) {
              if (!data[key]) continue;
              if (aliases[key].some(a => hay.includes(norm(a)))) {
                const proto = el.tagName === 'TEXTAREA' ? HTMLTextAreaElement.prototype : HTMLInputElement.prototype;
                const setter = Object.getOwnPropertyDescriptor(proto,'value')?.set;
                if (setter) setter.call(el,data[key]); else el.value = data[key];
                el.dispatchEvent(new Event('input',{bubbles:true}));
                el.dispatchEvent(new Event('change',{bubbles:true}));
                filled++;
                break;
              }
            }
          }
          alert(filled ? ('Berhasil mengisi ' + filled + ' kolom yang cocok. Periksa kembali data.') : 'Belum menemukan kolom yang cocok. Silakan isi manual.');
        })();
    """.trimIndent()
}

private class SecureProfileStore(context: android.content.Context) {
    private val prefs = context.getSharedPreferences("secure_profiles", android.content.Context.MODE_PRIVATE)
    private val alias = "antrean_pangan_aes_key"

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = ks.getKey(alias, null) as? SecretKey
        if (existing != null) return existing
        val generator = KeyGenerator.getInstance("AES", "AndroidKeyStore")
        val spec = android.security.keystore.KeyGenParameterSpec.Builder(
            alias,
            android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or android.security.keystore.KeyProperties.PURPOSE_DECRYPT
        ).setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        generator.init(spec)
        return generator.generateKey()
    }

    fun load(): List<Profile> {
        val blob = prefs.getString("blob", null) ?: return emptyList()
        return try {
            val packed = Base64.decode(blob, Base64.DEFAULT)
            val iv = packed.copyOfRange(0, 12)
            val cipherText = packed.copyOfRange(12, packed.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
            val json = JSONArray(String(cipher.doFinal(cipherText), Charsets.UTF_8))
            (0 until json.length()).map { i ->
                val o = json.getJSONObject(i)
                Profile(o.getString("id"), o.getString("label"), o.getString("name"), o.optString("nik"), o.optString("kk"), o.optString("phone"), o.optString("address"))
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun save(profiles: List<Profile>) {
        val json = JSONArray()
        profiles.forEach { p ->
            json.put(JSONObject().apply {
                put("id", p.id); put("label", p.label); put("name", p.name)
                put("nik", p.nik); put("kk", p.kk); put("phone", p.phone); put("address", p.address)
            })
        }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.iv + cipher.doFinal(json.toString().toByteArray(Charsets.UTF_8))
        prefs.edit().putString("blob", Base64.encodeToString(encrypted, Base64.NO_WRAP)).apply()
    }
}
