package com.coc.builderwidget

import android.Manifest
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.coc.builderwidget.data.CoCJsonParser
import com.coc.builderwidget.data.StorageManager
import com.coc.builderwidget.data.TimeFormatter
import com.coc.builderwidget.model.AccountSummary
import com.coc.builderwidget.model.BuilderTask
import com.coc.builderwidget.widget.BuilderWidgetProvider
import com.coc.builderwidget.worker.WidgetUpdateWorker
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WidgetUpdateWorker.schedule(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFFF59E0B),
                    background = Color(0xFF0F172A),
                    surface = Color(0xFF1E293B),
                    onBackground = Color(0xFFF8FAFC),
                    onSurface = Color(0xFFF8FAFC)
                )
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CoCTrackerScreen()
                }
            }
        }
    }
}

@Composable
fun CoCTrackerScreen() {
    val context = LocalContext.current
    val storage = remember { StorageManager(context) }

    var acc1 by remember { mutableStateOf(storage.getAccount(1)) }
    var acc2 by remember { mutableStateOf(storage.getAccount(2)) }
    var onlyHomeBuilders by remember { mutableStateOf(storage.getOnlyHomeBuilders()) }
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(15_000L)
            nowMillis = System.currentTimeMillis()
        }
    }

    val combinedTasks = remember(acc1, acc2, onlyHomeBuilders) {
        storage.getAllBuilderTasksSorted(onlyHomeBuilders = onlyHomeBuilders)
    }

    fun pasteJsonForAccount(slot: Int, customName: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipText = clipboard.primaryClip?.getItemAt(0)?.text?.toString().orEmpty()
        if (clipText.isBlank()) {
            Toast.makeText(context, "Clipboard kosong! Copy Village JSON di CoC dulu.", Toast.LENGTH_SHORT).show()
            return
        }
        CoCJsonParser.parse(clipText, slot, customName).onSuccess { summary ->
            storage.saveAccount(summary)
            if (slot == 1) acc1 = summary else acc2 = summary
            BuilderWidgetProvider.triggerUpdate(context)
            Toast.makeText(
                context,
                "$customName berhasil di-update (${summary.tasks.size} upgrade aktif)",
                Toast.LENGTH_SHORT
            ).show()
        }.onFailure {
            Toast.makeText(
                context,
                "Format JSON tidak valid. Pastikan copy dari Settings > More Settings > Data Export di CoC.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "CoC Builder Tracker",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF59E0B)
            )
            Text(
                text = "Pantau tukang 2 akun langsung dari Home Screen Widget",
                fontSize = 13.sp,
                color = Color(0xFF94A3B8)
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Hanya Tukang Desa Utama", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(
                        "Matikan untuk tampilkan Lab, Pet & Builder Base",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
                Switch(
                    checked = onlyHomeBuilders,
                    onCheckedChange = {
                        onlyHomeBuilders = it
                        storage.setOnlyHomeBuilders(it)
                        BuilderWidgetProvider.triggerUpdate(context)
                    }
                )
            }
        }

        item {
            AccountInputCard(
                account = acc1,
                badgeColor = Color(0xFF3B82F6),
                onPasteClick = { name -> pasteJsonForAccount(1, name) }
            )
        }

        item {
            AccountInputCard(
                account = acc2,
                badgeColor = Color(0xFFF97316),
                onPasteClick = { name -> pasteJsonForAccount(2, name) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Urutan Tukang Selesai Terdekat",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (combinedTasks.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Belum ada data upgrade. Copy Village JSON di CoC lalu tekan Paste di atas.",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            items(combinedTasks) { task ->
                BuilderTaskRow(task = task, nowMillis = nowMillis)
            }
        }
    }
}

@Composable
fun AccountInputCard(
    account: AccountSummary,
    badgeColor: Color,
    onPasteClick: (String) -> Unit
) {
    var nameInput by remember(account.name) { mutableStateOf(account.name) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(badgeColor, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("Slot ${account.slot}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            if (account.tag.isNotBlank()) {
                Text(account.tag, fontSize = 12.sp, color = Color(0xFF94A3B8))
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                label = { Text("Nama Akun") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = { onPasteClick(nameInput.ifBlank { "Akun ${account.slot}" }) },
                colors = ButtonDefaults.buttonColors(containerColor = badgeColor)
            ) {
                Text("Paste JSON", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun BuilderTaskRow(task: BuilderTask, nowMillis: Long) {
    val remaining = task.remainingMillis(nowMillis)
    val badgeColor = if (task.accountSlot == 1) Color(0xFF3B82F6) else Color(0xFFF97316)
    val statusColor = when {
        remaining <= 0L -> Color(0xFF4ADE80)
        remaining <= 3_600_000L -> Color(0xFFFACC15)
        else -> Color(0xFFE2E8F0)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1E293B), RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(badgeColor, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(task.accountName, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Text(
                    text = "[${task.category.label}]",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
            }
            Text(
                text = "${task.itemName} (Lv.${task.currentLevel + 1})",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = TimeFormatter.formatRemaining(remaining),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = statusColor
            )
            if (remaining > 0L) {
                Text(
                    text = TimeFormatter.formatFinishClock(task.finishTimeMillis),
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }
    }
}
