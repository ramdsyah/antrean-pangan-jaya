package id.pasarjaya.antreanpangan

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.util.UUID

private fun isValidDob(dob: String): Boolean {
    if (!dob.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) return false
    return runCatching { LocalDate.parse(dob) }.isSuccess
}

@Composable
fun ProfileEditor(initial: Profile?, cancel: () -> Unit, save: (Profile) -> Unit) {
    var label by remember { mutableStateOf(initial?.label ?: "") }
    var kk by remember { mutableStateOf(initial?.kk ?: "") }
    var nik by remember { mutableStateOf(initial?.nik ?: "") }
    var atm by remember { mutableStateOf(initial?.atm ?: "") }
    var dob by remember { mutableStateOf(initial?.dob ?: "") }

    val valid = label.isNotBlank() &&
        kk.length == 16 &&
        nik.length == 16 &&
        atm.isNotBlank() &&
        isValidDob(dob)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text(if (initial == null) "Tambah Profil" else "Edit Profil", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(
            value = label,
            onValueChange = { label = it },
            label = { Text("Nama profil (mis. Ibu)") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = kk,
            onValueChange = { kk = it.filter(Char::isDigit).take(16) },
            label = { Text("Nomor KK (16 digit)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = nik,
            onValueChange = { nik = it.filter(Char::isDigit).take(16) },
            label = { Text("NIK (16 digit)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = atm,
            onValueChange = { atm = it.filter(Char::isDigit).take(25) },
            label = { Text("Nomor Kartu ATM") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = dob,
            onValueChange = { dob = it },
            label = { Text("Tanggal lahir (YYYY-MM-DD)") },
            placeholder = { Text("1980-05-17") },
            isError = dob.isNotBlank() && !isValidDob(dob),
            supportingText = {
                if (dob.isNotBlank() && !isValidDob(dob)) Text("Format tanggal tidak valid") else Text("Gunakan format YYYY-MM-DD")
            },
            modifier = Modifier.fillMaxWidth()
        )
        Text("Data disimpan terenkripsi di perangkat. Jangan bagikan KK, NIK, atau nomor ATM.")
        Button(
            onClick = { save(Profile(initial?.id ?: UUID.randomUUID().toString(), label.trim(), kk, nik, atm, dob)) },
            enabled = valid,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Simpan Profil") }
        OutlinedButton(onClick = cancel, modifier = Modifier.fillMaxWidth()) { Text("Batal") }
    }
}
