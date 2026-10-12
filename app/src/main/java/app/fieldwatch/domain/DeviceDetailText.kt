package app.fieldwatch.domain

import app.fieldwatch.radio.BleAdParser
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import app.fieldwatch.l10n.l10n

/**
 * Plain-text dump of the device-detail screen. Same fields, no sparkline/presence art.
 * Not a legal identity.
 */
object DeviceDetailText {
    fun build(
        device: Sighting,
        signatureNames: List<String>,
        now: Long = System.currentTimeMillis(),
        attentionNotes: List<Pair<String, String>> = emptyList(),
        signatureNotes: List<Pair<String, String>> = emptyList(),
        fleets: List<Fleet> = emptyList(),
        mine: Boolean = false,
    ): String {
        val fmt = SimpleDateFormat("HH:mm:ss", Locale.US)
        val iso = SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US)
        val facts = device.facts
        val title = device.listTitle(signatureNames)
        val guess = DeviceExplain.guess(device, signatureNames)
        val out = StringBuilder()

        fun line(label: String, value: String) {
            out.append(label).append(": ").append(value.trim()).append('\n')
        }
        fun section(title: String) {
            out.append('\n').append("## ").append(title).append('\n')
        }

        out.append("Fieldwatch device detail\n")
        out.append(iso.format(Date(now))).append('\n')
        out.append(
            "Experimental. Not a legal identity. Stock Android radios — this is what the OS " +
                "exposed, not a guarantee a tracker or camera is present.\n",
        )
        out.append('\n')
        out.append(title).append('\n')
        line("MAC", device.mac)
        if (device.name.isNotBlank()) line(l10n("Advertised name"), device.name)
        if (mine) line("Marked mine", "No beep while this is on. Still listed.")

        out.append('\n')
        out.append("What this looks like: ").append(guess.headline).append('\n')
        out.append(guess.because).append('\n')
        if (attentionNotes.isNotEmpty()) {
            section("Extra attention")
            attentionNotes.forEach { (name, note) ->
                out.append("EXTRA ATTENTION ($name): ").append(note.trim()).append('\n')
            }
            out.append("Pattern match, not identity. Not a safety finding.\n")
        }
        if (signatureNotes.isNotEmpty()) {
            section(l10n("Notes"))
            signatureNotes.forEach { (name, note) ->
                out.append(name).append(": ").append(note.trim()).append('\n')
            }
        }

        section(l10n("Identity"))
        line(
            l10n("Radio"),
            if (device.kind == RadioKind.WIFI) {
                l10n("Wi-Fi access point (beaconing a network)")
            } else {
                l10n("Bluetooth Low Energy advertiser")
            },
        )
        line(l10n("Address"), DeviceExplain.addressExplain(device))
        vendorLine(device)?.let { line(l10n("Who made it"), it.replace('\n', ' ')) }
            ?: line(l10n("OUI (vendor prefix)"), "${device.oui} — no IEEE match; randomized addresses usually have none")
        if (device.hiddenSsid) {
            line(l10n("Network name (SSID)"), l10n("Hidden — the AP is beaconing but not publishing a name"))
        }

        section(l10n("Signal"))
        if (device.gone) {
            line(l10n("How loud here (RSSI)"), l10n("Not available"))
            val last = Rssi.lastMeasured(device.rssi, device.rssiHistory)
            line(l10n("Last heard"), last?.let { "$it dBm" } ?: l10n("Not available"))
        } else {
            line(l10n("How loud here (RSSI)"), DeviceExplain.rssiExplain(device.rssi))
            out.append("Closer to 0 dBm is louder here, not a distance.\n")
        }
        line(l10n("Heard range this session"), Rssi.sessionRange(device.rssiMin, device.rssiMax, device.rssiHistory))
        facts.txPowerDbm?.let {
            line(l10n("Claimed transmit power"), "$it dBm — how loud it says it transmits, not a distance")
        }
        if (device.channel != 0 || device.frequencyMhz != 0) {
            line(
                l10n("Channel / frequency"),
                buildString {
                    if (device.channel != 0) append("channel ${device.channel}")
                    if (device.frequencyMhz != 0) {
                        if (isNotEmpty()) append("  ·  ")
                        append("${device.frequencyMhz} MHz")
                    }
                    facts.channelWidth?.let { append("  ·  $it wide") }
                },
            )
        }
        facts.wifiStandard?.let { line(l10n("Wi-Fi generation"), it) }
        if (facts.centerFreq0 != null || facts.centerFreq1 != null) {
            line(
                l10n("Center frequencies"),
                listOfNotNull(
                    facts.centerFreq0?.let { "$it MHz" },
                    facts.centerFreq1?.let { "$it MHz" },
                ).joinToString("  ·  "),
            )
        }
        val rssiTail = device.rssiHistory.filter { Rssi.measured(it.rssi) }.takeLast(24)
        if (rssiTail.isNotEmpty()) {
            line(
                "Recent RSSI (oldest → newest)",
                rssiTail.joinToString(", ") { it.rssi.toString() },
            )
        }

        if (device.kind == RadioKind.BLE) {
            section(l10n("Bluetooth advertisement"))
            facts.primaryPhy?.let {
                val phys = listOfNotNull(it, facts.secondaryPhy).distinct()
                line(l10n("Radio PHY"), phys.joinToString(" / ") { phy -> DeviceExplain.phyExplain(phy) })
            }
            facts.connectable?.let {
                line(
                    l10n("Connectable"),
                    if (it) l10n("Yes — a phone could open a BLE connection")
                    else l10n("No — broadcast-only (you can hear it, not join it from this scan)"),
                )
            }
            facts.advertisingIntervalMs?.let {
                line(l10n("How often it advertises"), l10n("%.0f ms between bursts (smaller = chattier on the air)").format(it))
            }
            facts.periodicIntervalMs?.let { line(l10n("Periodic advertising"), "%.0f ms".format(it)) }
            facts.advFlags?.let { flags ->
                line(l10n("Discoverability"), DeviceExplain.flagsExplain(flags))
                line(l10n("Flags (raw)"), "0x%02X".format(flags))
            }
            facts.appearance?.let { value ->
                val name = RadioDb.appearance(value)
                line(
                    l10n("What it says it is (Appearance)"),
                    name ?: l10n("Unlisted Appearance 0x%04X").format(value),
                )
                line(l10n("Appearance code"), "0x%04X".format(value))
            }
            CodDecoder.decodeOrNull(facts.deviceClass)?.let { cod ->
                line(
                    l10n("Classic Bluetooth class"),
                    buildString {
                        append(cod.major)
                        if (cod.minor.isNotBlank()) append(" / ").append(cod.minor)
                        if (cod.services.isNotEmpty()) {
                            append(". Also offers: ")
                            append(cod.services.joinToString(", "))
                        }
                    },
                )
            }
        }

        if (device.kind == RadioKind.WIFI) {
            section("Wi-Fi access point")
            facts.security?.let {
                line(l10n("Encryption / login"), DeviceExplain.wifiSecurityExplain(it))
                if (it.isNotBlank()) line(l10n("Security string"), it)
            }
            facts.supportedRates?.let { line(l10n("Supported rates"), "$it Mbps  (* = required basic rate)") }
            facts.capabilities?.takeIf { it.isNotBlank() && it != facts.security }?.let {
                line(l10n("Capability string"), it)
            }
        }

        device.payloadAircraft?.trim()?.takeIf { it.isNotEmpty() }?.let { line(l10n("Aircraft"), it) }

        if (fleets.isNotEmpty() && (device.kind == RadioKind.BLE || device.kind == RadioKind.WIFI)) {
            val decoded = SignatureFieldDecoder.decodeSighting(device, fleets)
            if (decoded.isNotEmpty()) {
                section(l10n("Decoded fields"))
                decoded.forEach { row ->
                    line(row.label, row.display)
                    if (row.note.isNotBlank()) line(l10n("Note"), row.note)
                }
            }
        }

        if (device.serviceUuids.isNotEmpty()) {
            section(l10n("Services it offers"))
            line(
                l10n("Service IDs"),
                device.serviceUuids.joinToString("; ") { uuid ->
                    DeviceExplain.uuidGloss(uuid)?.let { "$uuid  ·  $it" } ?: uuid
                },
            )
        }
        if (facts.serviceData.isNotEmpty()) {
            facts.serviceData.forEach { sd ->
                val named = RadioDb.serviceUuid(sd.uuid)?.let { " ($it)" } ?: ""
                AdvPayloadDecoder.decodeService(sd).forEach { field -> line(field.label, field.value) }
                line(
                    "Service data ${uuidShort(sd.uuid)}$named",
                    sd.dataHex.hexSpaced().ifBlank { l10n("(empty)") },
                )
            }
        }

        val mfg = facts.mfgRecords.ifEmpty {
            device.manufacturerId?.let {
                listOf(MfgRecord(it, device.manufacturerDataHex))
            } ?: emptyList()
        }
        if (mfg.isNotEmpty()) {
            section(l10n("Maker data inside the ad"))
            mfg.forEach { rec ->
                val company = RadioDb.company(rec.companyId) ?: l10n("Not in the Bluetooth company list")
                line(l10n("Bluetooth company 0x%04X").format(rec.companyId), company)
                BleAdParser.mfgDecodedFields(rec).forEach { (k, v) -> line(k, v) }
                if (rec.dataHex.isNotBlank()) {
                    line("Raw payload (${rec.dataHex.length / 2} bytes)", rec.dataHex.hexSpaced())
                }
            }
        }

        if (facts.vendorIes.isNotEmpty() || device.vendorIeOuis.isNotEmpty()) {
            section(l10n("Wi-Fi vendor tags"))
            val rows = facts.vendorIes.ifEmpty {
                device.vendorIeOuis.map { VendorIeRecord(it, -1, "") }
            }
            rows.forEach { ie ->
                val org = RadioDb.vendorForOui24(ie.oui)
                val type = if (ie.type >= 0) " type %d".format(ie.type) else ""
                line(
                    "Vendor OUI ${ie.oui}$type",
                    buildString {
                        append(org ?: l10n("Unknown IEEE OUI"))
                        append(" — extra AP information element, not the SSID.")
                        if (ie.dataHex.isNotBlank()) {
                            append(" ")
                            append(ie.dataHex.hexSpaced())
                        }
                    },
                )
            }
        }

        section(l10n("Session"))
        line(l10n("First seen"), fmt.format(Date(device.firstSeen)))
        line(l10n("Last seen"), fmt.format(Date(device.lastSeen)))
        line(l10n("Hits"), device.hitCount.toString())
        Geo.screenCoord(device.latitude, device.longitude, false)?.let {
            line(l10n("Last fix"), it)
            out.append("Last fix is the phone’s GPS at hear-time, not a fix on this radio.\n")
        }
        if (device.fleetIds.isNotEmpty()) {
            line(l10n("Matched signatures"), signatureNames.joinToString("; ").ifBlank {
                device.fleetIds.joinToString("; ")
            })
        }
        if (device.rawHex.isNotBlank() && device.kind == RadioKind.BLE) {
            line(l10n("Raw advertisement"), device.rawHex.hexSpaced())
        }
        presenceLine(device, now, fmt)?.let { line(l10n("Presence (15 min)"), it) }
        return out.toString().trimEnd() + "\n"
    }

    private fun vendorLine(device: Sighting): String? {
        val parts = ArrayList<String>(3)
        device.vendor?.let {
            parts += "IEEE board/chip vendor: $it (${device.oui}). This is who owns the MAC prefix, not always the product brand."
        }
        val mfgId = device.facts.mfgRecords.firstOrNull()?.companyId ?: device.manufacturerId
        if (mfgId != null) {
            val company = RadioDb.company(mfgId)
            parts += "Bluetooth company in the ad: ${company ?: "unlisted"} (0x%04X).".format(mfgId)
        }
        return parts.joinToString(" ").ifBlank { null }
    }

    private fun uuidShort(uuid: String): String {
        val hex = uuid.filter { it.isLetterOrDigit() }.uppercase()
        return if (hex.length >= 8 && hex.startsWith("0000")) hex.substring(4, 8) else uuid.take(8)
    }

    private fun presenceLine(device: Sighting, now: Long, fmt: SimpleDateFormat): String? {
        if (device.presence.isEmpty()) return null
        val from = now - 15 * 60 * 1000L
        val spans = device.presence.filter { (it.end ?: now) >= from }
        if (spans.isEmpty()) return null
        return spans.joinToString("; ") { span ->
            val start = fmt.format(Date(span.start.coerceAtLeast(from)))
            val end = span.end?.let { fmt.format(Date(it)) } ?: l10n("now")
            "$start–$end"
        }
    }
}
