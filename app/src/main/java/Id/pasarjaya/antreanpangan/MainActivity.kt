package id.pasarjaya.antreanpangan

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private const val SITE = "https://antrianpanganbersubsidi.pasarjaya.co.id/"

data class Profile(
    val id: String,
    val label: String,
    val kk: String,
    val nik: String,
    val atm: String,
    val dob: String
)

class Store(context: android.content.Context) {
    private val appContext = context.applicationContext
    private val pref = EncryptedSharedPreferences.create(
        appContext,
        "profiles",
        MasterKey.Builder(appContext).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun load(): List<Profile> = try {
        val raw = pref.getString("data", "[]") ?: "[]"
        val arr = JSONArray(raw)
        buildList {
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                add(
                    Profile(
                        id = obj.optString("id").ifBlank { UUID.randomUUID().toString() },
                        label = obj.optString("label").ifBlank { "Profil ${i + 1}" },
                        kk = obj.optString("kk"),
                        nik = obj.optString("nik"),
                        atm = obj.optString("atm"),
                        dob = obj.optString("dob")
                    )
                )
            }
        }
    } catch (_: Exception) {
        emptyList()
    }

    fun save(list: List<Profile>) {
        val arr = JSONArray()
        list.forEach { p ->
            arr.put(
                JSONObject()
                    .put("id", p.id)
                    .put("label", p.label)
                    .put("kk", p.kk)
                    .put("nik", p.nik)
                    .put("atm", p.atm)
                    .put("dob", p.dob)
            )
        }
        pref.edit().putString("data", arr.toString()).apply()
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = Store(this)
        setContent { MaterialTheme { MainScreen(store) } }
    }
}

@Composable
private fun MainScreen(store: Store) {
    var list by remember { mutableStateOf(store.load()) }
    var chosen by remember { mutableStateOf(list.firstOrNull()?.id) }
    var editing by remember { mutableStateOf<Profile?>(null) }
    var editMode by remember { mutableStateOf(false) }
    var opened by remember { mutableStateOf(false) }
    var browser by remember { mutableStateOf<WebView?>(null) }
    val active = list.firstOrNull { it.id == chosen }

    if (editMode) {
        ProfileEditor(
            initial = editing,
            cancel = {
                editMode = false
                editing = null
            },
            save = { p ->
                list = if (list.any { it.id == p.id }) {
                    list.map { if (it.id == p.id) p else it }
                } else {
                    list + p
                }
                store.save(list)
                chosen = p.id
                editMode = false
                editing = null
            }
        )
        return
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            "ANTREAN PANGAN JAYA",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )
        if (!opened) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Profil penerima manfaat", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = {
                    editing = null
                    editMode = true
                }) { Text("+ Tambah") }
            }
            LazyColumn(Modifier.weight(1f)) {
                items(list, key = { it.id }) { p ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(Modifier.padding(10.dp)) {
                            Row { RadioButton(chosen == p.id, { chosen = p.id }); Text(p.label, Modifier.padding(top = 12.dp)) }
                            Text("KK: ${mask(p.kk)}  NIK: ${mask(p.nik)}")
                            Text("ATM: ${mask(p.atm)}  Lahir: ${p.dob}")
                            Row {
                                TextButton(onClick = {
                                    editing = p
                                    editMode = true
                                }) { Text("Edit") }
                                TextButton(onClick = {
                                    list = list.filterNot { it.id == p.id }
                                    store.save(list)
                                    if (chosen == p.id) chosen = list.firstOrNull()?.id
                                }) { Text("Hapus") }
                            }
                        }
                    }
                }
            }
            Button(onClick = { opened = true }, enabled = active != null, modifier = Modifier.fillMaxWidth()) {
                Text("Buka Website Resmi")
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { opened = false }) { Text("← Profil") }
                TextButton(onClick = { browser?.reload() }) { Text("Muat ulang") }
            }
            Text("Profil: ${active?.label ?: ""}")
            Button(
                onClick = { active?.let { browser?.evaluateJavascript(fillJs(it), null) } },
                enabled = active != null,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Isi KK, NIK, ATM & Tanggal Lahir") }
            Text("Wilayah, gerai, CAPTCHA, dan disclaimer tetap dilakukan manual. Periksa data sebelum mengirim.")
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        webViewClient = WebViewClient()
                        loadUrl(SITE)
                        browser = this
                    }
                },
                update = { view -> browser = view },
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
        }
    }
}

private fun mask(s: String): String = if (s.length <= 4) "••••" else "••••••${s.takeLast(4)}"

private fun fillJs(p: Profile): String {
    fun q(s: String) = JSONObject.quote(s)
    return r"""(function(){
      const d={kk:${q(p.kk)},nik:${q(p.nik)},atm:${q(p.atm)},dob:${q(p.dob)}};
      const norm=s=>(s||'').toLowerCase().replace(/[^a-z0-9]/g,'');
      const els=Array.from(document.querySelectorAll('input:not([type=hidden]):not([type=checkbox]),textarea'));
      const rules=[[['nomorkartukeluarga','kartukeluarga','nomorkk'],d.kk],[['nomorindukkependudukan','nik'],d.nik],[['nomorkartuatm','kartuatm','atm'],d.atm],[['tanggallahirpenerimaanmanfaat','tanggallahir','lahir'],d.dob]];
      let n=0; for(const [keys,val] of rules){const el=els.find(e=>{const s=norm([e.placeholder,e.name,e.id,e.getAttribute('aria-label'),e.parentElement?.innerText].join(' '));return keys.some(k=>s.includes(norm(k)))});if(el){const set=Object.getOwnPropertyDescriptor(Object.getPrototypeOf(el),'value')?.set;if(set)set.call(el,val);else el.value=val;el.dispatchEvent(new Event('input',{bubbles:true}));el.dispatchEvent(new Event('change',{bubbles:true}));n++}}
      alert('Kolom terdeteksi: '+n+'/4. Periksa ulang secara manual.');
    })();"""
}
