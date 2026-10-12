package app.fieldwatch.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import app.fieldwatch.ui.component.FieldwatchActionButton
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import app.fieldwatch.ui.component.FieldwatchOutlinedField
import androidx.compose.material3.Scaffold
import app.fieldwatch.ui.component.FieldwatchSlider
import androidx.compose.material3.Surface
import app.fieldwatch.ui.component.FieldwatchSwitch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.fieldwatch.domain.FilterLogic
import app.fieldwatch.domain.FilterPreset
import app.fieldwatch.domain.Fleet
import app.fieldwatch.domain.SignatureClass
import app.fieldwatch.ui.ClassGlyphs
import app.fieldwatch.ui.NestedTabInsets
import app.fieldwatch.ui.NestedTopBar
import app.fieldwatch.ui.FieldwatchUi
import app.fieldwatch.ui.FieldwatchViewModel
import app.fieldwatch.ui.component.SectionCard
import app.fieldwatch.ui.component.FieldwatchFilterChip
import app.fieldwatch.ui.component.spectreSectionFill
import app.fieldwatch.ui.component.spectreTileEdge
import app.fieldwatch.ui.component.spectreTileFill
import app.fieldwatch.ui.theme.LocalNightMode
import app.fieldwatch.ui.theme.PhosphorActive
import app.fieldwatch.ui.theme.nightIf
import app.fieldwatch.l10n.l10n

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiltersScreen(state: FieldwatchUi, vm: FieldwatchViewModel) {
    var presetName by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<FilterPreset?>(null) }
    var confirmReset by remember { mutableStateOf(false) }
    val filter = state.filter
    Scaffold(
        contentWindowInsets = NestedTabInsets,
        topBar = { NestedTopBar(l10n("Filters")) },
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionCard(l10n("Presets")) {
            Text(
                "Tap a preset to replace the whole filter. Long-press one to delete it. " +
                    "Save current as… keeps the filter you have now. " +
                    l10n("Settings → Restore default signatures & presets brings deleted stock presets back."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    state.presets.chunked(2).forEach { row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            row.forEach { preset ->
                                PresetChip(
                                    name = preset.name,
                                    selected = preset.filter == filter,
                                    onApply = { vm.applyPreset(preset) },
                                    onLongPress = { pendingDelete = preset },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                FieldwatchOutlinedField(
                    presetName,
                    { presetName = it },
                    l10n("Save current as…"),
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = {
                    if (presetName.isNotBlank()) {
                        vm.savePreset(presetName.trim())
                        presetName = ""
                    }
                }) { Text(l10n("Save")) }
            }
            }

            SectionCard(l10n("Radios")) {
            Text(
                if (filter.movingWithYou) {
                    l10n("Moving with you uses BLE only. Both and Wi-Fi only stay unavailable until you turn that switch off.")
                } else {
                    l10n("Pick which radios Live shows. One radio is Wi-Fi or BLE, not both.")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FieldwatchFilterChip(
                    selected = !filter.movingWithYou && filter.showWifi && filter.showBle,
                    onClick = { vm.updateFilter { it.copy(showWifi = true, showBle = true) } },
                    enabled = !filter.movingWithYou,
                    label = { Text(l10n("Both")) },
                )
                FieldwatchFilterChip(
                    selected = !filter.movingWithYou && filter.showWifi && !filter.showBle,
                    onClick = { vm.updateFilter { it.copy(showWifi = true, showBle = false) } },
                    enabled = !filter.movingWithYou,
                    label = { Text(l10n("Wi-Fi only")) },
                )
                FieldwatchFilterChip(
                    selected = filter.movingWithYou || (filter.showBle && !filter.showWifi),
                    onClick = { vm.updateFilter { it.copy(showWifi = false, showBle = true) } },
                    label = { Text(l10n("BLE only")) },
                )
            }
            }

            SectionCard(l10n("Moving with you")) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(l10n("Moving with you"), Modifier.weight(1f))
                FieldwatchSwitch(
                    filter.movingWithYou,
                    { on ->
                        vm.updateFilter { current ->
                            if (!on) current.copy(movingWithYou = false)
                            else {
                                // Follow test is BLE. Leftover Trackers / Show only hides
                                // unmatched rows; AirTags rotate, so Live looks empty.
                                val hiding = current.useClassFilter && current.excludeClasses
                                current.copy(
                                    movingWithYou = true,
                                    showWifi = false,
                                    showBle = true,
                                    namedOnly = false,
                                    customNamesOnly = false,
                                    watchedOnly = false,
                                    useClassFilter = hiding,
                                    excludeClasses = hiding,
                                    classes = if (hiding) current.classes else emptySet(),
                                    includeSignatures = false,
                                )
                            }
                        }
                    },
                )
            }
            Text(
                when {
                    !state.settings.tagLocation ->
                        "Turn on Tag detections with GPS, then walk or drive. " +
                            "This shows loud Bluetooth radios that stay with you. Wi-Fi access points stay off. " +
                            l10n("Turning this on clears the other “only” filters, except Hide these.")
                    state.operatorSpanM < 45.0 ->
                        "GPS path so far ${state.operatorSpanM.toInt()} m. Keep moving until about 50 m. " +
                            "If this stays at 0 while you drive, set Location to high accuracy. " +
                            l10n("A phone that keeps changing its Bluetooth address will not match.") +
                            when {
                                filter.customNamesOnly ->
                                    " Named radios only is also on, so unlabeled radios stay hidden."
                                filter.watchedOnly ->
                                    " Watched only is also on, so unwatched radios stay hidden."
                                filter.namedOnly || filter.namedOnlyImplied() ->
                                    " Signatures only or Show only is also on, so unmatched radios stay hidden."
                                else -> ""
                            } +
                            if (filter.hideMine) " Hide my radios is on, so those stay off this list." else ""
                    filter.customNamesOnly || filter.watchedOnly || filter.namedOnly || filter.namedOnlyImplied() ->
                        "GPS path ${state.operatorSpanM.toInt()} m. Another “only” filter is on, so only those radios can match. " +
                            l10n("A tag in a bag or a car should show. Wi-Fi stays hidden.") +
                            if (filter.hideMine) " Hide my radios is on, so those stay off this list." else ""
                    else ->
                        "GPS path ${state.operatorSpanM.toInt()} m. Loud Bluetooth heard along that path at a steady level. " +
                            (if (filter.hideMine) {
                                "Hide my radios is on, so those stay off this list. "
                            } else {
                                "A tag in a bag or a car should show. "
                            }) +
                            "Wi-Fi stays hidden, because a strong access point can look like it is traveling with you. " +
                            "A phone that keeps changing its Bluetooth address will not show as one follower. " +
                            l10n("Live → Start over clears the path so you can test again.")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            }

            SectionCard(l10n("New detections")) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (state.arrivalsLearning) l10n("New detections only  ·  learning") else l10n("New detections only"),
                    Modifier.weight(1f),
                )
                FieldwatchSwitch(
                    filter.arrivalsOnly,
                    { on -> vm.updateFilter { it.copy(arrivalsOnly = on) } },
                )
            }
            Text(
                if (filter.arrivalsOnly) {
                    "Mark seen and Reset seen are on Live, above the tabs. " +
                        when {
                            state.arrivalsLearning ->
                                l10n("Learning the Wi-Fi that is already here.")
                            state.hiddenKnown > 0 ->
                                "${state.hiddenKnown} already-seen radios are hidden."
                            else ->
                                l10n("Nothing is marked seen yet.")
                        }
                } else {
                    "Hides radios already here so Live shows only new ones. " +
                        "Mark seen and Reset seen appear on Live while this is on. " +
                        "Brief hold still sets how long a new radio stays after the last packet. " +
                        l10n("A Bluetooth radio that changes its address looks new.")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            }

            SectionCard(l10n("Who stays")) {
            val namedImplied = filter.namedOnlyImplied()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    l10n("Signatures only (hide unmatched)"),
                    Modifier.weight(1f),
                    color = if (namedImplied) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
                FieldwatchSwitch(
                    checked = filter.namedOnly || namedImplied,
                    onCheckedChange = { on ->
                        if (!namedImplied) vm.updateFilter { it.copy(namedOnly = on) }
                    },
                    enabled = !namedImplied,
                )
            }
            if (namedImplied) {
                Text(
                    l10n("Show only is already hiding unmatched radios. Turn Show only off to use this switch."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(l10n("Watched only"), Modifier.weight(1f))
                FieldwatchSwitch(
                    filter.watchedOnly,
                    { on -> vm.updateFilter { it.copy(watchedOnly = on) } },
                )
            }
            Text(
                l10n("Live shows bookmarked signatures, and named radios with Alert on. Hide these still applies. A name with no bookmark stays under Named radios only."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(l10n("Named radios only"), Modifier.weight(1f))
                FieldwatchSwitch(
                    filter.customNamesOnly,
                    { on -> vm.updateFilter { it.copy(customNamesOnly = on) } },
                )
            }
            Text(
                l10n("Live shows radios you named. Alert can be off. A radio that changes its address will not keep that name."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(l10n("Hide my radios"), Modifier.weight(1f))
                FieldwatchSwitch(
                    filter.hideMine,
                    { on -> vm.updateFilter { it.copy(hideMine = on) } },
                )
            }
            Text(
                l10n("Radios marked Mine stay off Live. The sit and Debrief still have them. Moving with you leaves this on."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(l10n("Hide Fast Pair account-key"), Modifier.weight(1f))
                FieldwatchSwitch(
                    filter.hideFastPairAccountKey,
                    { on -> vm.updateFilter { it.copy(hideFastPairAccountKey = on) } },
                )
            }
            Text(
                l10n("Hides already-paired Fast Pair radios that match no other signature. Radios still in pairing mode stay. Hide selected signatures can hide those too."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            }

            SectionCard(l10n("Signature classes")) {
            Text(
                l10n("Live only. Signatures still label, log, and can beep. Pick Show only or Hide these, then the classes. Show only with nothing picked leaves Live unchanged. Save current as… keeps this as a preset."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FieldwatchFilterChip(
                    selected = filter.useClassFilter && !filter.excludeClasses,
                    onClick = {
                        vm.updateFilter {
                            val on = !(it.useClassFilter && !it.excludeClasses)
                            it.copy(useClassFilter = on, excludeClasses = false)
                        }
                    },
                    label = { Text(l10n("Show only")) },
                )
                FieldwatchFilterChip(
                    selected = filter.useClassFilter && filter.excludeClasses,
                    onClick = {
                        vm.updateFilter {
                            val on = !(it.useClassFilter && it.excludeClasses)
                            it.copy(useClassFilter = on, excludeClasses = on)
                        }
                    },
                    label = { Text(l10n("Hide these")) },
                )
            }
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SignatureClass.visible.sortedBy { it.label().lowercase() }.chunked(2).forEach { row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            row.forEach { kind ->
                                val on = kind in filter.classes
                                FieldwatchFilterChip(
                                    selected = on,
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(max = 32.dp),
                                    onClick = {
                                        vm.updateFilter { current ->
                                            val next = current.classes.toMutableSet()
                                            if (on) next.remove(kind) else next.add(kind)
                                            current.copy(classes = next)
                                        }
                                    },
                                    leadingIcon = {
                                        Icon(
                                            ClassGlyphs.of(kind),
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                        )
                                    },
                                    label = {
                                        Text(
                                            kind.label(),
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    },
                                )
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
            }

            SectionCard(l10n("Selected signatures")) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(l10n("Show only selected signatures"), Modifier.weight(1f))
                FieldwatchSwitch(
                    filter.includeSignatures,
                    { on -> vm.updateFilter { it.copy(includeSignatures = on) } },
                )
            }
            if (filter.includeSignatures) {
                SignaturePickList(
                    fleets = state.fleets,
                    selected = filter.includeFleetIds,
                    help = l10n("Tap a class to open it. Live keeps radios that match a signature you turn on. An empty list leaves Live unchanged. Your picks stay if you turn this off and back on."),
                    onToggle = { id, checked ->
                        vm.updateFilter { current ->
                            val next = current.includeFleetIds.toMutableSet()
                            if (checked) next.add(id) else next.remove(id)
                            current.copy(includeFleetIds = next)
                        }
                    },
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(l10n("Hide selected signatures"), Modifier.weight(1f))
                FieldwatchSwitch(
                    filter.excludeSignatures,
                    { on -> vm.updateFilter { it.copy(excludeSignatures = on) } },
                )
            }
            if (filter.excludeSignatures) {
                SignaturePickList(
                    fleets = state.fleets,
                    selected = filter.fleetIds,
                    help = l10n("Tap a class to open it. Radios that match a signature you turn on stay off Live. Your picks stay if you turn this off and back on."),
                    onToggle = { id, checked ->
                        vm.updateFilter { current ->
                            val next = current.fleetIds.toMutableSet()
                            if (checked) next.add(id) else next.remove(id)
                            current.copy(fleetIds = next)
                        }
                    },
                )
            }
            }

            SectionCard(l10n("Fine filter")) {
            var rssiDrag by remember { mutableIntStateOf(filter.rssiMin) }
            var rssiDragging by remember { mutableStateOf(false) }
            LaunchedEffect(filter.rssiMin) {
                if (!rssiDragging) rssiDrag = filter.rssiMin
            }
            Text("Minimum RSSI  $rssiDrag dBm", style = MaterialTheme.typography.labelLarge)
            FieldwatchSlider(
                value = rssiDrag.toFloat(),
                onValueChange = { v ->
                    rssiDragging = true
                    rssiDrag = v.toInt()
                },
                onValueChangeFinished = {
                    vm.updateFilter { it.copy(rssiMin = rssiDrag) }
                    rssiDragging = false
                },
                valueRange = -100f..-30f,
            )

            FieldwatchOutlinedField(
                filter.nameQuery,
                { value -> vm.updateFilter { it.copy(nameQuery = value) } },
                l10n("Name / MAC contains"),
            )
            FieldwatchOutlinedField(
                filter.ouiQuery,
                { value -> vm.updateFilter { it.copy(ouiQuery = value) } },
                l10n("Vendor contains"),
            )

            Text(l10n("Name, vendor, signal, and class"), style = MaterialTheme.typography.labelLarge)
            Text(
                l10n("AND means all of these match. OR means any one of them. This does not change the radio type, the “only” switches, Hide my radios, Hide Fast Pair, or a hide list."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FieldwatchFilterChip(
                    selected = filter.logic == FilterLogic.AND,
                    onClick = { vm.updateFilter { it.copy(logic = FilterLogic.AND) } },
                    label = { Text("AND") },
                )
                FieldwatchFilterChip(
                    selected = filter.logic == FilterLogic.OR,
                    onClick = { vm.updateFilter { it.copy(logic = FilterLogic.OR) } },
                    label = { Text("OR") },
                )
            }

            FieldwatchActionButton(onClick = { confirmReset = true }) {
                Text(l10n("Reset filter"))
            }
            }
        }
    }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(l10n("Reset filter?")) },
            text = {
                Text(
                    "Clears the radios, classes, selected signatures, signal strength, and name and vendor fields. " +
                        l10n("Presets you saved stay. Live shows the radios again. This cannot be undone."),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmReset = false
                        vm.updateFilter { app.fieldwatch.domain.FilterState() }
                    },
                ) { Text(l10n("Reset")) }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text(l10n("Cancel")) }
            },
        )
    }
    pendingDelete?.let { preset ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(l10n("Delete preset?")) },
            text = {
                Text(
                    if (preset.isBuiltIn()) {
                        "Remove “${preset.name}” from this list? A catalog update will not put it back. Settings → Restore default signatures & presets brings the stock presets back. Live keeps this filter until you pick another preset or tap Reset filter."
                    } else {
                        "Delete “${preset.name}”? This cannot be undone. Live keeps this filter until you pick another preset or tap Reset filter."
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.deletePreset(preset.id)
                    pendingDelete = null
                }) { Text(l10n("Delete")) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(l10n("Cancel")) }
            },
        )
    }
}

@Composable
private fun SignaturePickList(
    fleets: List<Fleet>,
    selected: Set<String>,
    help: String,
    onToggle: (id: String, checked: Boolean) -> Unit,
) {
    val groups = remember(fleets) {
        fleets.groupBy { it.kind.folded() }
            .toList()
            .sortedBy { it.first.label().lowercase() }
            .map { (kind, rows) -> kind to rows.sortedBy { it.name.lowercase() } }
    }
    var open by remember {
        mutableStateOf(
            groups.filter { (_, rows) -> rows.any { it.id in selected } }
                .map { it.first.name }
                .toSet(),
        )
    }
    Column(
        modifier = Modifier.padding(start = 24.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            help,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        groups.forEach { (kind, rows) ->
            val classId = kind.name
            val expanded = classId in open
            val picked = rows.count { it.id in selected }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        open = if (expanded) open - classId else open + classId
                    }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    ClassGlyphs.of(kind),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    kind.label(),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    buildString {
                        append(if (expanded) "▾  " else "▸  ")
                        if (picked > 0) append("$picked/")
                        append(rows.size)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (picked > 0) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            if (expanded) {
                rows.forEach { fleet ->
                    val on = fleet.id in selected
                    Row(
                        modifier = Modifier.padding(start = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(fleet.name, Modifier.weight(1f))
                        FieldwatchSwitch(on, { checked -> onToggle(fleet.id, checked) })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PresetChip(
    name: String,
    selected: Boolean = false,
    onApply: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = FilterChipDefaults.shape,
        color = if (selected) spectreSectionFill() else spectreTileFill(),
        border = BorderStroke(
            1.dp,
            if (selected) PhosphorActive.nightIf(LocalNightMode.current) else spectreTileEdge(),
        ),
        modifier = modifier
            .heightIn(max = 32.dp)
            .combinedClickable(
                onClick = onApply,
                onLongClick = onLongPress,
            ),
    ) {
        Text(
            name,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
