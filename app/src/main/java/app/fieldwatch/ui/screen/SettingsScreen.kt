package app.fieldwatch.ui.screen

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import app.fieldwatch.ui.component.FieldwatchFilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import app.fieldwatch.ui.component.FieldwatchActionButton
import app.fieldwatch.ui.component.FieldwatchOutlinedField
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import app.fieldwatch.ui.component.FieldwatchSlider
import androidx.compose.material3.Surface
import app.fieldwatch.ui.component.FieldwatchSwitch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.fieldwatch.R
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.fieldwatch.domain.AlertVoiceWhat
import app.fieldwatch.domain.AppSettings
import app.fieldwatch.domain.GnssAlertFloor
import app.fieldwatch.domain.GnssSensitivity
import app.fieldwatch.domain.ScanIntensity
import app.fieldwatch.domain.TakDefaults
import app.fieldwatch.domain.TakFeedStatus
import app.fieldwatch.domain.TakFloodSend
import app.fieldwatch.domain.TakGnssHold
import app.fieldwatch.domain.TakGnssSend
import app.fieldwatch.domain.TakPublish
import app.fieldwatch.domain.TakUdpPreset
import app.fieldwatch.radio.WifiRadio
import app.fieldwatch.ui.NestedTabInsets
import app.fieldwatch.ui.NestedTopBar
import app.fieldwatch.ui.FieldwatchUi
import app.fieldwatch.ui.FieldwatchViewModel
import app.fieldwatch.ui.component.SectionCard
import app.fieldwatch.ui.component.FieldwatchFilterChip
import app.fieldwatch.ui.component.StableCaption
import app.fieldwatch.ui.component.StickyHeight
import java.net.Inet4Address
import java.net.NetworkInterface
import app.fieldwatch.l10n.L10n
import app.fieldwatch.l10n.L10nLang
import app.fieldwatch.l10n.l10n

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    state: FieldwatchUi,
    vm: FieldwatchViewModel,
    onRadioBookmarks: () -> Unit,
    onScanStatus: () -> Unit,
    onShowLiveTour: () -> Unit = {},
) {
    val context = LocalContext.current
    val settings = state.settings
    val saveSignatures = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> uri?.let(vm::saveSignaturesToUri) }
    val importSignatures = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(vm::importSignaturesFromUri) }
    val saveSettings = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> uri?.let(vm::saveSettingsToUri) }
    val importSettings = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(vm::importSettingsFromUri) }
    var confirmRestore by remember { mutableStateOf(false) }
    val pendingSettingsImport by vm.pendingSettingsImport.collectAsStateWithLifecycle()
    val gnssProfile by vm.gnssProfile.collectAsStateWithLifecycle()
    val gnssCal by vm.gnssCalibration.collectAsStateWithLifecycle()
    val gnssReady = gnssProfile != null
    Scaffold(
        contentWindowInsets = NestedTabInsets,
        topBar = { NestedTopBar(l10n("Settings")) },
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionCard(l10n("Language")) {
                Text(
                    l10n("App language"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FieldwatchFilterChip(
                        selected = L10n.lang.value == L10nLang.SYSTEM,
                        onClick = { L10n.set(L10nLang.SYSTEM, context) },
                        label = { Text(l10n("System default")) },
                    )
                    FieldwatchFilterChip(
                        selected = L10n.lang.value == L10nLang.EN,
                        onClick = { L10n.set(L10nLang.EN, context) },
                        label = { Text("English") },
                    )
                    FieldwatchFilterChip(
                        selected = L10n.lang.value == L10nLang.ZH,
                        onClick = { L10n.set(L10nLang.ZH, context) },
                        label = { Text("中文") },
                    )
                }
            }
            SectionCard(l10n("Appearance")) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(l10n("Night mode"), Modifier.weight(1f))
                FieldwatchSwitch(settings.nightMode, { on -> vm.updateSettings { it.copy(nightMode = on) } })
            }
            Text(
                l10n("Red on black so a dark sit stays dim. Phone brightness does not change."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(l10n("Keep screen on"), Modifier.weight(1f))
                FieldwatchSwitch(settings.keepScreenOn, { on -> vm.updateSettings { it.copy(keepScreenOn = on) } })
            }
            Text(
                l10n("The screen stays awake while Fieldwatch is open, so Bluetooth keeps scanning. The scan still runs from the notification if you leave the app. Turn this off when you pocket the phone."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(l10n("Privacy mode"), Modifier.weight(1f))
                FieldwatchSwitch(settings.demoMode, { on -> vm.updateSettings { it.copy(demoMode = on) } })
            }
            Text(
                l10n("Hides the last half of each MAC on screen and in sit reports. Coordinates show as masked, and street names are left out. The vendor prefix stays. Logs, matching, and Hunt still use the real address and position. The TAK feed pauses so those are not sent. The Path map still loads if Online place names and maps is on. Turn this off when you need the full address on screen."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            }

            SectionCard(l10n("Scanning")) {
            val label = when (settings.intensity) {
                ScanIntensity.SAVER -> l10n("Battery saver")
                ScanIntensity.BALANCED -> l10n("Balanced")
                ScanIntensity.PERFORMANCE -> l10n("High performance")
            }
            Text("Scan intensity  ·  $label")
            FieldwatchSlider(
                value = settings.intensity.ordinal.toFloat(),
                onValueChange = { v ->
                    val next = ScanIntensity.entries[v.toInt().coerceIn(0, 2)]
                    vm.updateSettings { it.copy(intensity = next) }
                },
                valueRange = 0f..2f,
                steps = 1,
            )
            Text(
                l10n("Wi-Fi reads every access point at once, then has to wait. High performance asks about every 30 seconds, which is as fast as Android allows. Bluetooth keeps listening between those scans."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            StableCaption(
                state.throttleHint.ifBlank { " " },
                l10n("Wi-Fi waiting on OS"),
                l10n("Wi-Fi scanning"),
                "Wi-Fi next 99s",
                " ",
            )

            val lifecycleOwner = LocalLifecycleOwner.current
            var osThrottled by remember { mutableStateOf(WifiRadio.osScanThrottled(context)) }
            var backgroundAllowed by remember { mutableStateOf(isBackgroundUsageAllowed(context)) }
            var unrestricted by remember { mutableStateOf(isIgnoringBatteryOptimizations(context)) }
            var needDevOptions by remember { mutableStateOf(false) }
            var batteryGate by remember { mutableStateOf<BatteryAndroidGate?>(null) }
            DisposableEffect(lifecycleOwner) {
                val obs = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        osThrottled = WifiRadio.osScanThrottled(context)
                        backgroundAllowed = isBackgroundUsageAllowed(context)
                        unrestricted = isIgnoringBatteryOptimizations(context)
                    }
                }
                lifecycleOwner.lifecycle.addObserver(obs)
                onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(l10n("Faster Wi-Fi AP scans"), Modifier.weight(1f))
                FieldwatchSwitch(
                    checked = !osThrottled,
                    onCheckedChange = { needDevOptions = true },
                )
            }
            StableCaption(
                when {
                    Build.VERSION.SDK_INT < 30 ->
                        l10n("This phone is older than Android 11, so Fieldwatch cannot tell if Wi-Fi scan throttling is on. The switch stays off.")
                    !osThrottled ->
                        l10n("On. A new access-point list about every 8 seconds. Uses more battery. This matches Wi-Fi scan throttling in Developer options.")
                    else ->
                        l10n("Off. Android is limiting Wi-Fi scans to about four per two minutes. Tap the switch to open Developer options.")
                },
                l10n("This phone is older than Android 11, so Fieldwatch cannot tell if Wi-Fi scan throttling is on. The switch stays off."),
                l10n("On. A new access-point list about every 8 seconds. Uses more battery. This matches Wi-Fi scan throttling in Developer options."),
                l10n("Off. Android is limiting Wi-Fi scans to about four per two minutes. Tap the switch to open Developer options."),
            )
            if (needDevOptions) {
                AlertDialog(
                    onDismissRequest = { needDevOptions = false },
                    title = { Text(l10n("Faster Wi-Fi AP scans")) },
                    text = {
                        Text(
                            if (Build.VERSION.SDK_INT < 30) {
                                l10n("This phone is older than Android 11, so Fieldwatch cannot tell if Wi-Fi scan throttling is on. Faster scanning stays off.")
                            } else if (osThrottled) {
                                l10n("This opens Developer options. Turn Wi-Fi scan throttling off. Fieldwatch matches that switch when you come back.")
                            } else {
                                l10n("This opens Developer options. Turn Wi-Fi scan throttling on to use the normal wait. Fieldwatch matches that switch when you come back.")
                            },
                        )
                    },
                    confirmButton = {
                        if (Build.VERSION.SDK_INT >= 30) {
                            TextButton(
                                onClick = {
                                    needDevOptions = false
                                    runCatching {
                                        context.startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
                                    }
                                },
                            ) { Text(l10n("Open developer options")) }
                        } else {
                            TextButton(onClick = { needDevOptions = false }) { Text(l10n("OK")) }
                        }
                    },
                    dismissButton = {
                        if (Build.VERSION.SDK_INT >= 30) {
                            TextButton(onClick = { needDevOptions = false }) { Text(l10n("Not now")) }
                        }
                    },
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(l10n("Allow background usage"), Modifier.weight(1f))
                FieldwatchSwitch(
                    checked = backgroundAllowed,
                    onCheckedChange = { batteryGate = BatteryAndroidGate.BACKGROUND },
                )
            }
            Text(
                l10n("Opens this app’s battery page. Turn on Allow background usage there. If it is off, Android can stop the scan when you leave."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(l10n("Unrestricted battery"), Modifier.weight(1f))
                FieldwatchSwitch(
                    checked = unrestricted,
                    onCheckedChange = { batteryGate = BatteryAndroidGate.UNRESTRICTED },
                )
            }
            Text(
                l10n("Opens the same battery page. Choose Unrestricted. On some phones, including Samsung, tap Allow background usage first, then choose Unrestricted."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (batteryGate != null) {
                val background = batteryGate == BatteryAndroidGate.BACKGROUND
                AlertDialog(
                    onDismissRequest = { batteryGate = null },
                    title = {
                        Text(if (background) l10n("Allow background usage") else l10n("Unrestricted battery"))
                    },
                    text = {
                        Text(
                            if (background) {
                                l10n("This opens Fieldwatch’s battery page. Turn on Allow background usage. Fieldwatch matches it when you come back.")
                            } else {
                                l10n("Choose Unrestricted. On some phones, including Samsung, tap the words Allow background usage, then choose Unrestricted. Fieldwatch matches it when you come back.")
                            },
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val gate = batteryGate
                                batteryGate = null
                                openAppBatteryPage(
                                    context,
                                    highlightBackground = gate == BatteryAndroidGate.BACKGROUND,
                                )
                            },
                        ) { Text(l10n("Open Android settings")) }
                    },
                    dismissButton = {
                        TextButton(onClick = { batteryGate = null }) { Text(l10n("Not now")) }
                    },
                )
            }
            }

            SectionCard(l10n("Watchlist")) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(l10n("Watchlist alerts"), Modifier.weight(1f))
                FieldwatchSwitch(settings.alertsEnabled, { on -> vm.updateSettings { it.copy(alertsEnabled = on) } })
            }
            Text(
                l10n("Beep, voice, flash, and the jump when a bookmarked signature or named radio appears. If this is off, bookmarks stay, and Fieldwatch stays quiet."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val radioWatchN = state.watchlist.count { it.deviceKey != null }
            FieldwatchActionButton(
                onClick = onRadioBookmarks,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Named radios ($radioWatchN)") }
            Text(
                l10n("A name for one MAC. Alert is optional. Filters → Named radios shows them on Live. Signature watches stay on the Signatures tab."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(l10n("Beep on watched signature"), Modifier.weight(1f))
                FieldwatchSwitch(
                    settings.alertBeep,
                    { on -> vm.updateSettings { it.copy(alertBeep = on) } },
                    enabled = settings.alertsEnabled,
                )
            }
            Text(
                l10n("A double pip on the media volume when a watched radio first appears, or comes back after leaving. It does not repeat while that radio stays in view. You can use the beep, the voice, or both. Turn the media volume up if you hear nothing, then tap Test alert."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(l10n("Voice on watched signature"), Modifier.weight(1f))
                FieldwatchSwitch(
                    settings.alertVoice,
                    { on -> vm.updateSettings { it.copy(alertVoice = on) } },
                    enabled = settings.alertsEnabled,
                )
            }
            Text(
                l10n("Speaks the watch on the same volume as the beep. You can use the beep, the voice, or both. A second hit is skipped if Fieldwatch is already speaking. With no speech engine, the beep still plays if Beep is on."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(l10n("What to say"), style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AlertVoiceWhat.entries.forEach { item ->
                    FieldwatchFilterChip(
                        selected = settings.alertVoiceWhat == item,
                        onClick = { vm.updateSettings { it.copy(alertVoiceWhat = item) } },
                        enabled = settings.alertsEnabled && settings.alertVoice,
                        label = { Text(item.label()) },
                    )
                }
            }
            Text(
                l10n("Class is the group on Live, such as finder tags or audio. Signature is the catalog name, such as Apple AirTags. Class + signature says both. A named radio with Alert on always says the name you gave it."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FieldwatchActionButton(
                onClick = vm::testWatchBeep,
                modifier = Modifier.fillMaxWidth(),
                enabled = settings.alertsEnabled && (settings.alertBeep || settings.alertVoice),
            ) { Text(l10n("Test alert")) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(l10n("Jump to new watched detection"), Modifier.weight(1f))
                FieldwatchSwitch(
                    settings.snapToBeep,
                    { on -> vm.updateSettings { it.copy(snapToBeep = on) } },
                    enabled = settings.alertsEnabled && (settings.alertBeep || settings.alertVoice),
                )
            }
            Text(
                l10n("Live scrolls to a new watched radio so you can see the flash. Turn this off if you do not want the list to move."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(l10n("System notification"), Modifier.weight(1f))
                FieldwatchSwitch(
                    settings.alertShade,
                    { on -> vm.updateSettings { it.copy(alertShade = on) } },
                    enabled = settings.alertsEnabled,
                )
            }
            Text(
                l10n("A silent notification when a watched radio appears. The beep and the flash are enough for most sits."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            }

            SectionCard(l10n("Location")) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(l10n("Tag detections with GPS"), Modifier.weight(1f))
                FieldwatchSwitch(settings.tagLocation, { on -> vm.updateSettings { it.copy(tagLocation = on) } })
            }
            Text(
                "Stamps this phone’s position on each detection: Live detail, Moving with you, Debrief, and new log rows. " +
                    "That is where you were, not a location from the other radio. " +
                    "Use high-accuracy location, or the path stays empty. " +
                    "Heard-here TAK pins need this. Remote ID uses the coordinates in the advertisement. " +
                    l10n("Turn this off to keep your position out of the logs."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(l10n("Online place names and maps"), Modifier.weight(1f))
                FieldwatchSwitch(settings.onlineLookup, { on -> vm.updateSettings { it.copy(onlineLookup = on) } })
            }
            Text(
                "With internet, Debrief turns coordinates into a street and city, and Reports → Path loads a map under the trace. " +
                    "Nothing is sent to a Fieldwatch server. " +
                    "Offline, Debrief keeps the coordinates and Path stays a plain trace. " +
                    l10n("Turn this off to leave streets and the map out."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            }

            SectionCard(l10n("GNSS interference")) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(l10n("GNSS interference check"), Modifier.weight(1f))
                FieldwatchSwitch(
                    checked = settings.gnssMonitor && gnssReady,
                    onCheckedChange = { on ->
                        if (gnssReady) vm.updateSettings { it.copy(gnssMonitor = on) }
                    },
                    enabled = gnssReady && !gnssCal.running,
                )
            }
            Text(
                "Detects interference and spoofing of GNSS (GPS) satellites. " +
                    "Interference can also be caused by other nearby electronics, such as Wi-Fi routers. " +
                    "Fieldwatch cannot tell the source or the distance. " +
                    "A hit already under way when the check starts can be missed. " +
                    l10n("A hit opens a popup. After you dismiss it, a red line stays on Live until it ends."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!gnssReady) {
                Text(
                    l10n("Calibrate this phone before turning the check on."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            FieldwatchActionButton(
                onClick = { if (gnssCal.running) vm.stopGnssCalibration() else vm.startGnssCalibration() },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (gnssCal.running) l10n("Stop calibration") else l10n("Calibrate this phone")) }
            Text(
                "Stand outside, hold still, and stay away from a router. This takes about a minute. " +
                    l10n("The result stays on this phone until you calibrate again. It is not part of Export settings."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (gnssCal.running) {
                Text(
                    "Calibrating… ${gnssCal.leftSec} s left",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (gnssCal.rejection.isNotBlank()) {
                Text(
                    gnssCal.rejection,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            gnssProfile?.let { profile ->
                Text(
                    profile.summary(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "Calibrated ${profile.calibratedLabel()}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(l10n("Sensitivity"), style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GnssSensitivity.entries.forEach { item ->
                    FieldwatchFilterChip(
                        selected = settings.gnssSensitivity == item,
                        onClick = { vm.updateSettings { it.copy(gnssSensitivity = item) } },
                        label = { Text(item.label()) },
                    )
                }
            }
            Text(
                l10n("Low waits for a deeper drop. High alerts on a smaller drop, including some routers."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(l10n("Alert from"), style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GnssAlertFloor.entries.forEach { item ->
                    FieldwatchFilterChip(
                        selected = settings.gnssAlertFloor == item,
                        onClick = { vm.updateSettings { it.copy(gnssAlertFloor = item) } },
                        label = { Text(item.label()) },
                    )
                }
            }
            Text(
                l10n("The popup, red line, beep, and voice start at this level. A weaker hit still shows in Diagnostics and in the sit report."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(l10n("Spoofing checks"), Modifier.weight(1f))
                FieldwatchSwitch(settings.gnssSpoofChecks, { on -> vm.updateSettings { it.copy(gnssSpoofChecks = on) } })
            }
            Text(
                l10n("Watches for gain falling while the signal holds or rises. A clock or position mismatch counts with that, or with another mismatch. One mismatch alone does not warn. A careful spoofer can still get past these checks."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(l10n("Beep"), Modifier.weight(1f))
                FieldwatchSwitch(settings.gnssBeep, { on -> vm.updateSettings { it.copy(gnssBeep = on) } })
            }
            Text(
                l10n("The same pip as a watchlist hit, on the media volume. The popup and the red line still show if this is off."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(l10n("Voice"), Modifier.weight(1f))
                FieldwatchSwitch(settings.gnssVoice, { on -> vm.updateSettings { it.copy(gnssVoice = on) } })
            }
            Text(
                l10n("Says possible GPS interference, or possible GPS spoofing. It speaks again only when the level goes up. Uses the media volume. Separate from the watchlist Voice switch."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(l10n("System notification"), Modifier.weight(1f))
                FieldwatchSwitch(settings.gnssShade, { on -> vm.updateSettings { it.copy(gnssShade = on) } })
            }
            Text(
                l10n("A silent notification for this warning. The popup and the red line still show if this is off. Separate from the watchlist switch."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(l10n("Full tracking"), Modifier.weight(1f))
                FieldwatchSwitch(settings.gnssFullTracking, { on -> vm.updateSettings { it.copy(gnssFullTracking = on) } })
            }
            Text(
                l10n("Keeps the GPS receiver awake instead of letting it rest. Uses more battery. Android 12 and newer. Older phones ignore this."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(l10n("Fading-together check"), Modifier.weight(1f))
                FieldwatchSwitch(settings.gnssCorrelation, { on -> vm.updateSettings { it.copy(gnssCorrelation = on) } })
            }
            Text(
                l10n("Watches for satellites fading at the same time. A hand, a pocket, or getting in a car can do that. Leave this off unless you are testing it."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(l10n("Invert gain"), Modifier.weight(1f))
                FieldwatchSwitch(settings.gnssInvertAgc, { on -> vm.updateSettings { it.copy(gnssInvertAgc = on) } })
            }
            Text(
                l10n("A few phones report gain backwards. Turn this on only if a Wi-Fi router makes the AGC number in Diagnostics go up."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            }

            SectionCard("TAK / CoT") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("TAK / CoT feed", Modifier.weight(1f))
                FieldwatchSwitch(settings.takEnabled, { on -> vm.updateSettings { it.copy(takEnabled = on) } })
            }
            Text(
                "Sends map markers to ATAK, WinTAK, or iTAK. " +
                    "Radios you hear are pinned at this phone’s GPS, at the loudest point, and labeled (here). " +
                    "Walking away does not move the pin. A louder detection does. " +
                    "Remote ID aircraft use the coordinates in the advertisement, and a pilot location is a second pin. " +
                    "A radio that leaves keeps its name for How long a spot stays. Tap a marker for the name, MAC, and signal. " +
                    l10n("This is not direction finding. Privacy mode pauses the feed."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (settings.takEnabled && settings.demoMode) {
                Text(
                    l10n("Privacy mode is on, so the feed is paused. Turn Privacy mode off to send markers."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (settings.takEnabled) {
                TakFeedSettings(settings, vm, state.takStatus)
            }
            }

            SectionCard(l10n("Logging")) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(l10n("Write detections to disk"), Modifier.weight(1f))
                FieldwatchSwitch(settings.loggingEnabled, { on -> vm.updateSettings { it.copy(loggingEnabled = on) } })
            }
            StableCaption(
                if (settings.loggingEnabled) {
                    l10n("Logging is on. New detections are added to the file.")
                } else {
                    l10n("Logging is off. Scanning still runs. Nothing new is written until you turn this on.")
                },
                l10n("Logging is on. New detections are added to the file."),
                l10n("Logging is off. Scanning still runs. Nothing new is written until you turn this on."),
            )
            Text(
                l10n("The file on disk is one detection per line. Reports → Log can share or save CSV, JSON, GPX, KML, or WiGLE."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            var rotateDrag by remember { mutableIntStateOf(settings.logRotateKb) }
            var rotateDragging by remember { mutableStateOf(false) }
            LaunchedEffect(settings.logRotateKb) {
                if (!rotateDragging) rotateDrag = settings.logRotateKb
            }
            Text("Rotate at $rotateDrag KB")
            FieldwatchSlider(
                value = rotateDrag.toFloat(),
                onValueChange = {
                    rotateDragging = true
                    rotateDrag = it.toInt().coerceIn(128, 4096)
                },
                onValueChangeFinished = {
                    vm.updateSettings { s -> s.copy(logRotateKb = rotateDrag) }
                    rotateDragging = false
                },
                valueRange = 128f..4096f,
            )
            var staleDrag by remember { mutableIntStateOf(settings.staleSec) }
            var staleDragging by remember { mutableStateOf(false) }
            LaunchedEffect(settings.staleSec) {
                if (!staleDragging) staleDrag = settings.staleSec
            }
            Text("Stale after ${staleDrag}s")
            FieldwatchSlider(
                value = staleDrag.toFloat(),
                onValueChange = {
                    staleDragging = true
                    staleDrag = it.toInt().coerceIn(15, 180)
                },
                onValueChangeFinished = {
                    vm.updateSettings { s -> s.copy(staleSec = staleDrag) }
                    staleDragging = false
                },
                valueRange = 15f..180f,
            )
            StickyHeight("log-stats") {
                Text(
                    "${state.logLines} lines this session  ·  ${vm.logBytes() / 1024} KB on disk. " +
                        l10n("Share, Save, and Reset / clear log are on the Reports tab."),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            }

            SectionCard(l10n("Signatures")) {
            Text(
                l10n("Export shares the catalog, including signatures you added. Import adds rows and does not delete any. Update from GitHub refreshes the stock signatures and leaves yours, your bookmarks, and your settings. It needs internet. Restore defaults deletes the signatures you added."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FieldwatchActionButton(
                onClick = vm::startSignatureShare,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(l10n("Export signatures")) }
            FieldwatchActionButton(
                onClick = { saveSignatures.launch(vm.suggestedSignaturesName()) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(l10n("Save signatures to SD card / storage…")) }
            FieldwatchActionButton(
                onClick = {
                    importSignatures.launch(arrayOf("application/json", "text/plain", "*/*"))
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Import signatures…") }
            FieldwatchActionButton(
                onClick = vm::updateStockCatalogFromGitHub,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Update stock catalog from GitHub") }

            FieldwatchActionButton(
                onClick = { confirmRestore = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Restore default signatures & presets")
            }
            }

            SectionCard("Settings backup") {
            Text(
                "Saves switches, filters, presets, named radios, and signature watches. Not the catalog, and not logs. " +
                    "The catalog is Export signatures, above. Import replaces those items on this phone and leaves the catalog. " +
                    "Fieldwatch asks before a file turns the TAK feed on or changes where it sends.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FieldwatchActionButton(
                onClick = vm::startSettingsShare,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Export settings") }
            FieldwatchActionButton(
                onClick = { saveSettings.launch(vm.suggestedSettingsName()) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save settings to SD card / storage…") }
            FieldwatchActionButton(
                onClick = {
                    importSettings.launch(arrayOf("application/json", "text/plain", "*/*"))
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(l10n("Import settings…")) }
            }

            FieldwatchActionButton(
                onClick = onShowLiveTour,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(l10n("Show Live tour")) }
            Text(
                l10n("Shows the Live tour again: display, pause, filters, signatures, reports, and settings."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FieldwatchActionButton(
                onClick = onScanStatus,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(l10n("Diagnostics")) }
            Text(
                l10n("Phone, Android version, the scan switches, and whether Wi-Fi, Bluetooth, and Location are on. Copy pastes that into an issue. Network names and GPS coordinates are left out."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                "Fieldwatch ${app.fieldwatch.BuildConfig.VERSION_NAME}  ·  Catalog ${state.catalogVersion}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                l10n("Wi-Fi and Bluetooth only. Android does not show phones that are only connected to an access point. Fieldwatch sees access points and Bluetooth advertisers."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val footerLifecycle = LocalLifecycleOwner.current
            var ipv4 by remember { mutableStateOf(localIpv4Addresses()) }
            DisposableEffect(footerLifecycle) {
                val obs = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) ipv4 = localIpv4Addresses()
                }
                footerLifecycle.lifecycle.addObserver(obs)
                onDispose { footerLifecycle.lifecycle.removeObserver(obs) }
            }
            Text(
                if (ipv4.isEmpty()) {
                    l10n("This phone’s IPv4  ·  none")
                } else {
                    "This phone’s IPv4  ·  ${ipv4.joinToString("  ·  ")}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
            CreditFooter()
        }
    }
    pendingSettingsImport?.let { pending ->
        AlertDialog(
            onDismissRequest = vm::dismissPendingSettingsImport,
            title = { Text(l10n("Import settings?")) },
            text = { Text(pending.message) },
            confirmButton = {
                TextButton(onClick = vm::confirmPendingSettingsImport) { Text(l10n("Import")) }
            },
            dismissButton = {
                TextButton(onClick = vm::dismissPendingSettingsImport) { Text(l10n("Cancel")) }
            },
        )
    }
    if (confirmRestore) {
        AlertDialog(
            onDismissRequest = { confirmRestore = false },
            title = { Text(l10n("Restore defaults?")) },
            text = {
                Text(
                    "Replaces the stock catalog, colors, stock bookmarks, stock filters, and the default switches. " +
                        "Signatures and filters you added are deleted. Export those first if you want a backup. " +
                        l10n("This cannot be undone."),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmRestore = false
                        vm.restoreDefaults()
                    },
                ) { Text(l10n("Restore")) }
            },
            dismissButton = {
                TextButton(onClick = { confirmRestore = false }) { Text(l10n("Cancel")) }
            },
        )
    }
}

@Composable
private fun CreditFooter() {
    val context = LocalContext.current
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "Copyright (c) 2026 Off Grid Pete LLC. All rights reserved.",
            style = MaterialTheme.typography.labelSmall,
            color = muted,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SocialChip(
                icon = R.drawable.ic_instagram,
                label = "@OffGridPete",
                tint = muted,
                onClick = { openUrl(context, "https://instagram.com/OffGridPete") },
            )
            SocialChip(
                icon = R.drawable.ic_x,
                label = "@OGridPete",
                tint = muted,
                onClick = { openUrl(context, "https://x.com/OGridPete") },
            )
        }
    }
}

@Composable
private fun SocialChip(
    icon: Int,
    label: String,
    tint: Color,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(99.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = tint)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TakFeedSettings(settings: AppSettings, vm: FieldwatchViewModel, status: TakFeedStatus) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    var hostText by remember { mutableStateOf(settings.takHost) }
    var portText by remember { mutableStateOf(settings.takPort.toString()) }
    LaunchedEffect(settings.takHost) { hostText = settings.takHost }
    LaunchedEffect(settings.takPort) { portText = settings.takPort.toString() }
    val preset = TakPublish.udpPreset(settings.takHost, settings.takPort)
    Text(l10n("Destination"), style = MaterialTheme.typography.labelLarge)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldwatchFilterChip(
            selected = preset == TakUdpPreset.THIS_PHONE,
            onClick = {
                val (host, port) = TakPublish.applyPreset(TakUdpPreset.THIS_PHONE)
                vm.updateSettings { it.copy(takHost = host, takPort = port) }
            },
            enabled = !settings.demoMode,
            label = { Text(l10n("This phone")) },
        )
        FieldwatchFilterChip(
            selected = preset == TakUdpPreset.LAN_MULTICAST,
            onClick = {
                val (host, port) = TakPublish.applyPreset(TakUdpPreset.LAN_MULTICAST)
                vm.updateSettings { it.copy(takHost = host, takPort = port) }
            },
            enabled = !settings.demoMode,
            label = { Text(l10n("LAN multicast")) },
        )
        FieldwatchFilterChip(
            selected = preset == TakUdpPreset.CUSTOM,
            onClick = {
                if (preset != TakUdpPreset.CUSTOM) {
                    val (host, port) = TakPublish.applyPreset(TakUdpPreset.CUSTOM)
                    vm.updateSettings { it.copy(takHost = host, takPort = port) }
                }
            },
            enabled = !settings.demoMode,
            label = { Text(l10n("Custom")) },
        )
    }
    Text(
        "This phone is ATAK on this handset, at ${TakDefaults.LOOPBACK}:${TakDefaults.PORT}. " +
            "LAN multicast is ${TakDefaults.SA_HOST}:${TakDefaults.SA_PORT}, for other ATAK apps on this Wi-Fi. " +
            "Custom is one address. UDP only. A TAK server on TCP 8087 is not this feed. " +
            "If This phone shows no pins, use Custom with the Wi-Fi address at the bottom of Settings and port ${TakDefaults.PORT}.",
        style = MaterialTheme.typography.bodySmall,
        color = muted,
    )
    FieldwatchOutlinedField(
        value = hostText,
        onValueChange = { value ->
            hostText = value
            val trimmed = value.trim()
            if (trimmed.isNotEmpty()) {
                vm.updateSettings { it.copy(takHost = trimmed) }
            }
        },
        label = l10n("Host"),
        placeholder = TakDefaults.HOST,
        enabled = !settings.demoMode,
    )
    FieldwatchOutlinedField(
        value = portText,
        onValueChange = { value ->
            val filtered = value.filter { it.isDigit() }.take(5)
            portText = filtered
            filtered.toIntOrNull()?.let { n ->
                if (n in 1..65_535) {
                    vm.updateSettings { it.copy(takPort = n) }
                }
            }
        },
        label = l10n("Port"),
        placeholder = TakDefaults.PORT.toString(),
        supportingText = "UDP. This phone uses ${TakDefaults.PORT}. LAN multicast uses ${TakDefaults.SA_PORT}. Not TCP 8087.",
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        enabled = !settings.demoMode,
    )
    Text(takStatusLine(status), style = MaterialTheme.typography.bodySmall, color = muted)
    Text(l10n("What to send"), style = MaterialTheme.typography.labelLarge)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldwatchFilterChip(
            selected = settings.takAttention,
            onClick = { vm.updateSettings { it.copy(takAttention = !it.takAttention) } },
            enabled = !settings.demoMode,
            label = { Text("Extra attention") },
        )
        FieldwatchFilterChip(
            selected = settings.takPayloadFix,
            onClick = { vm.updateSettings { it.copy(takPayloadFix = !it.takPayloadFix) } },
            enabled = !settings.demoMode,
            label = { Text(l10n("Payload location")) },
        )
        FieldwatchFilterChip(
            selected = settings.takWatchlist,
            onClick = { vm.updateSettings { it.copy(takWatchlist = !it.takWatchlist) } },
            enabled = !settings.demoMode,
            label = { Text(l10n("Watchlist")) },
        )
        FieldwatchFilterChip(
            selected = settings.takAllSignatures,
            onClick = { vm.updateSettings { it.copy(takAllSignatures = !it.takAllSignatures) } },
            enabled = !settings.demoMode,
            label = { Text(l10n("All signatures")) },
        )
    }
    Text(
        "Turn on any combination. Extra attention sends body cameras, glasses, recorders, pentest gear, and public-safety access points. " +
            "Payload location sends Remote ID and any radio that advertises its own coordinates. " +
            "Watchlist sends bookmarked signatures and named radios with Alert on. " +
            "All signatures sends every labeled radio, which is noisy in a crowd. " +
            l10n("Unmatched radios are never sent. A pin needs coordinates, from the advertisement or from Tag detections with GPS."),
        style = MaterialTheme.typography.bodySmall,
        color = muted,
    )
    Text(l10n("How long a spot stays"), style = MaterialTheme.typography.labelLarge)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TakGnssHold.entries.forEach { item ->
            FieldwatchFilterChip(
                selected = settings.takRadioHold == item,
                onClick = { vm.updateSettings { it.copy(takRadioHold = item) } },
                enabled = !settings.demoMode,
                label = { Text(item.label()) },
            )
        }
    }
    Text(
        "After a radio leaves, the spot keeps its name for this long. The time starts when it leaves. " +
            "Until deleted stays until the feed is turned off. " +
            l10n("This phone’s Fieldwatch marker stays for about 2 minutes."),
        style = MaterialTheme.typography.bodySmall,
        color = muted,
    )
    Text("GNSS", style = MaterialTheme.typography.labelLarge)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TakGnssSend.entries.forEach { item ->
            FieldwatchFilterChip(
                selected = settings.takGnss == item,
                onClick = { vm.updateSettings { it.copy(takGnss = item) } },
                enabled = !settings.demoMode,
                label = { Text(item.label()) },
            )
        }
    }
    Text(
        "A mark on the map where this phone was, and a line on this phone’s Fieldwatch marker. " +
            "A short hit is a pin. A hit that continues while the phone moves becomes an oval from the start to the last place it was heard, half as wide as it is long. " +
            "The mark does not say where the cause is. " +
            "Off sends nothing. While alerting sends during a hit at or above Alert from. " +
            "Red line also keeps the line for the 10 minutes after. The oval stops growing when the hit ends. " +
            "Any hit also sends a Low detection that never shows the red line. " +
            l10n("This phone needs a GPS fix."),
        style = MaterialTheme.typography.bodySmall,
        color = muted,
    )
    Text(l10n("How long the mark stays"), style = MaterialTheme.typography.labelLarge)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TakGnssHold.entries.forEach { item ->
            FieldwatchFilterChip(
                selected = settings.takGnssHold == item,
                onClick = { vm.updateSettings { it.copy(takGnssHold = item) } },
                enabled = !settings.demoMode && settings.takGnss != TakGnssSend.OFF,
                label = { Text(item.label()) },
            )
        }
    }
    Text(
        l10n("The time starts at the last update. Until deleted stays until GNSS is set to Off."),
        style = MaterialTheme.typography.bodySmall,
        color = muted,
    )
    Text(l10n("Floods"), style = MaterialTheme.typography.labelLarge)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TakFloodSend.entries.forEach { item ->
            FieldwatchFilterChip(
                selected = settings.takFlood == item,
                onClick = { vm.updateSettings { it.copy(takFlood = item) } },
                enabled = !settings.demoMode,
                label = { Text(item.label()) },
            )
        }
    }
    Text(
        "Adds a note to this phone’s Fieldwatch marker when a flood is detected, until the flood ends. Not its own pin. " +
            "This does not change the red line on Live. A store full of radios advertising pairing can cause this. " +
            l10n("The note does not say it was one radio, and it does not name a tool. This phone needs a GPS fix."),
        style = MaterialTheme.typography.bodySmall,
        color = muted,
    )
}

private fun takStatusLine(status: TakFeedStatus): String {
    if (status.paused) return l10n("Feed status  ·  paused (Privacy mode)")
    if (status.error != null) {
        val whenAt = takStatusWhen(status.at)
        return "Feed status  ·  error: ${status.error}" + if (whenAt.isNotEmpty()) "  ·  $whenAt" else ""
    }
    if (status.at <= 0L) {
        return l10n("Feed status  ·  no send yet this session")
    }
    val bits = ArrayList<String>(5)
    bits += "on the feed ${status.onFeed}"
    bits += "sent ${status.sent}"
    if (status.gone > 0) {
        bits += if (status.gone == 1) l10n("1 gone") else "${status.gone} gone"
    }
    if (status.dest.isNotBlank()) bits += status.dest
    val whenAt = takStatusWhen(status.at)
    if (whenAt.isNotEmpty()) bits += whenAt
    val head = "Feed status  ·  ${bits.joinToString("  ·  ")}"
    return if (status.detail.isNotBlank() && status.sent == 0 && status.gone == 0) {
        "$head  ·  ${status.detail}"
    } else {
        head
    }
}

private fun takStatusWhen(at: Long): String {
    if (at <= 0L) return ""
    return java.time.Instant.ofEpochMilli(at)
        .atZone(java.time.ZoneId.systemDefault())
        .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))
}

private fun localIpv4Addresses(): List<String> {
    val found = LinkedHashSet<String>()
    val nifs = runCatching {
        java.util.Collections.list(NetworkInterface.getNetworkInterfaces())
    }.getOrDefault(emptyList())
    for (nif in nifs) {
        if (!nif.isUp || nif.isLoopback) continue
        for (addr in java.util.Collections.list(nif.inetAddresses)) {
            if (addr is Inet4Address && !addr.isLoopbackAddress && !addr.isLinkLocalAddress) {
                addr.hostAddress?.let { found += it }
            }
        }
    }
    return found.toList()
}

internal fun isIgnoringBatteryOptimizations(context: Context): Boolean =
    context.getSystemService(PowerManager::class.java)
        ?.isIgnoringBatteryOptimizations(context.packageName) == true

internal fun isBackgroundUsageAllowed(context: Context): Boolean =
    context.getSystemService(ActivityManager::class.java)?.isBackgroundRestricted != true

private enum class BatteryAndroidGate { BACKGROUND, UNRESTRICTED }

/**
 * Fieldwatch’s per-app Battery page. Samsung keeps Allow background usage and
 * Unrestricted on this same screen. [highlightBackground] asks Settings to
 * focus the background-usage switch when the OEM supports it.
 */
private fun openAppBatteryPage(context: Context, highlightBackground: Boolean) {
    val pkgUri = Uri.fromParts("package", context.packageName, null)
    val attempts = listOf(
        Intent("android.settings.VIEW_ADVANCED_POWER_USAGE_DETAIL").apply {
            data = pkgUri
            addCategory(Intent.CATEGORY_DEFAULT)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("request_ignore_background_restriction", highlightBackground)
            if (!highlightBackground) {
                putExtra(":settings:fragment_args_key", "unrestricted_pref")
            }
        },
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = pkgUri
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        },
    )
    for (intent in attempts) {
        if (intent.resolveActivity(context.packageManager) == null) continue
        if (runCatching { context.startActivity(intent) }.isSuccess) return
    }
}

private fun openUrl(context: android.content.Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}
