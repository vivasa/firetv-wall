package com.clock.firetv

import android.view.View
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class ClockPresenter(
    private val primaryTime: TextView,
    private val primarySeconds: TextView,
    private val primaryAmPm: TextView,
    private val primaryLabel: TextView,
    private val primaryDate: TextView,
    private val secondaryTime: TextView,
    private val secondarySeconds: TextView,
    private val secondaryAmPm: TextView,
    private val secondaryLabel: TextView,
    private val secondaryDate: TextView
) {
    fun update(primaryTz: String, secondaryTz: String, timeFormat: Int) {
        val now = Date()
        val is24h = timeFormat == SettingsManager.FORMAT_24H

        updateClock(now, primaryTz, is24h, primaryTime, primarySeconds, primaryAmPm, primaryLabel, primaryDate)
        updateClock(now, secondaryTz, is24h, secondaryTime, secondarySeconds, secondaryAmPm, secondaryLabel, secondaryDate)
    }

    private fun updateClock(
        now: Date,
        timezoneId: String,
        is24h: Boolean,
        timeView: TextView,
        secondsView: TextView,
        amPmView: TextView,
        labelView: TextView,
        dateView: TextView
    ) {
        val tz = TimeZone.getTimeZone(timezoneId)

        val timePattern = if (is24h) "HH:mm" else "hh:mm"
        val timeFmt = SimpleDateFormat(timePattern, Locale.US).apply { timeZone = tz }
        val secFmt = SimpleDateFormat("ss", Locale.US).apply { timeZone = tz }
        val dateFmt = SimpleDateFormat("EEE, MMM d, yyyy", Locale.US).apply { timeZone = tz }

        timeView.text = timeFmt.format(now)
        secondsView.text = secFmt.format(now)

        if (is24h) {
            amPmView.visibility = View.GONE
        } else {
            amPmView.visibility = View.VISIBLE
            val amPmFmt = SimpleDateFormat("a", Locale.US).apply { timeZone = tz }
            amPmView.text = amPmFmt.format(now)
        }

        val abbrev = tz.getDisplayName(tz.inDaylightTime(now), TimeZone.SHORT, Locale.US)
        labelView.text = abbrev.uppercase()

        dateView.text = dateFmt.format(now)
    }
}
