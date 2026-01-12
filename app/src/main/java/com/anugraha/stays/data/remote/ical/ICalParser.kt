package com.anugraha.stays.data.remote.ical

import android.util.Log
import com.anugraha.stays.domain.model.ICalSource
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class ICalEvent(
    val uid: String,
    val summary: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val source: ICalSource
)

class ICalParser {

    fun parseICalString(icalContent: String, source: ICalSource): List<ICalEvent> {
        return try {
            Log.d("ICalParser", "")
            Log.d("ICalParser", "🔍 PARSING iCAL for ${source.name}")
            Log.d("ICalParser", "   Content length: ${icalContent.length} characters")
            Log.d("ICalParser", "   First 200 chars: ${icalContent.take(200)}")

            val events = mutableListOf<ICalEvent>()
            val lines = icalContent.lines()

            Log.d("ICalParser", "   Total lines: ${lines.size}")

            var currentEvent: MutableMap<String, String>? = null
            var currentKey = ""
            var eventCount = 0

            for (line in lines) {
                val trimmedLine = line.trim()

                when {
                    trimmedLine == "BEGIN:VEVENT" -> {
                        eventCount++
                        currentEvent = mutableMapOf()
                        Log.d("ICalParser", "   ▶️ Found VEVENT #$eventCount")
                    }
                    trimmedLine == "END:VEVENT" && currentEvent != null -> {
                        Log.d("ICalParser", "      Parsing event data...")
                        parseEvent(currentEvent, source)?.let {
                            events.add(it)
                            Log.d("ICalParser", "      ✅ Event parsed successfully")
                        } ?: Log.w("ICalParser", "      ⚠️ Event parsing returned null")
                        currentEvent = null
                    }
                    currentEvent != null && trimmedLine.isNotEmpty() -> {
                        if (trimmedLine.startsWith(" ")) {
                            currentEvent[currentKey] = currentEvent[currentKey] + trimmedLine.substring(1)
                        } else {
                            val colonIndex = trimmedLine.indexOf(':')
                            if (colonIndex > 0) {
                                currentKey = trimmedLine.substring(0, colonIndex).split(';')[0]
                                val value = trimmedLine.substring(colonIndex + 1)
                                currentEvent[currentKey] = value

                                // Log important fields
                                when (currentKey) {
                                    "UID" -> Log.d("ICalParser", "         UID: $value")
                                    "SUMMARY" -> Log.d("ICalParser", "         SUMMARY: $value")
                                    "DTSTART" -> Log.d("ICalParser", "         DTSTART: $value")
                                    "DTEND" -> Log.d("ICalParser", "         DTEND: $value")
                                }
                            }
                        }
                    }
                }
            }

            Log.d("ICalParser", "")
            Log.d("ICalParser", "   ✅ Parsing complete for ${source.name}")
            Log.d("ICalParser", "   Found ${events.size} valid events out of $eventCount VEVENT blocks")
            Log.d("ICalParser", "")

            events
        } catch (e: Exception) {
            Log.e("ICalParser", "❌ Error parsing iCal for ${source.name}: ${e.message}", e)
            emptyList()
        }
    }

    private fun parseEvent(eventData: Map<String, String>, source: ICalSource): ICalEvent? {
        return try {
            val uid = eventData["UID"] ?: return null
            val summary = eventData["SUMMARY"] ?: source.getDisplayName()

            val dtStart = eventData["DTSTART"] ?: return null
            val dtEnd = eventData["DTEND"] ?: return null

            val startDate = parseDate(dtStart)
            val endDate = parseDate(dtEnd)

            if (startDate == null || endDate == null) return null

            ICalEvent(
                uid = uid,
                summary = summary,
                startDate = startDate,
                endDate = endDate,
                source = source
            )
        } catch (e: Exception) {
            Log.e("ICalParser", "Error parsing event: ${e.message}")
            null
        }
    }

    private fun parseDate(dateString: String): LocalDate? {
        return try {
            val cleanDate = dateString.replace("VALUE=DATE:", "")

            val date = when {
                cleanDate.contains('T') -> {
                    // Has time component - parse as date-time in IST
                    val dateTimeStr = cleanDate.take(15) // YYYYMMDDTHHmmss
                    java.time.LocalDateTime.parse(dateTimeStr, java.time.format.DateTimeFormatter.BASIC_ISO_DATE)
                        .atZone(com.anugraha.stays.util.DateUtils.IST_ZONE)
                        .toLocalDate()
                }
                cleanDate.length == 8 -> {
                    // YYYYMMDD format
                    java.time.LocalDate.parse(cleanDate, java.time.format.DateTimeFormatter.BASIC_ISO_DATE)
                }
                cleanDate.contains('-') -> {
                    // YYYY-MM-DD format
                    java.time.LocalDate.parse(cleanDate.take(10))
                }
                else -> null
            }

            Log.d("ICalParser", "Parsed date: $dateString → $date (IST)")
            date

        } catch (e: Exception) {
            Log.e("ICalParser", "Error parsing date '$dateString': ${e.message}")
            null
        }
    }
}