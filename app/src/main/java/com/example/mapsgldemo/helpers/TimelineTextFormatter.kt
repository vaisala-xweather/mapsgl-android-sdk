package com.example.mapsgldemo.helpers

import com.example.mapsgldemo.databinding.TimelineBinding
import com.example.mapsgldemo.databinding.TimelineSettingsPanelBinding
import com.xweather.mapsgl.anim.Timeline
import com.xweather.mapsgl.anim.TimelineDisplayFormatter
import java.util.Date

/** Formats timeline labels (device local time, `en` locale — aligned with MapsGL JS). */
class TimelineTextFormatter {
    companion object {

        /**
         * Sets the Start, End, and Current time TextViews.
         *
         * [startEndLabels]: when set (activity-level overlay), start/end strings go there; when null,
         * uses [TimelineBinding.settingsCS] embedded in the timeline strip.
         *
         * Position should be between 0f and 1f. If position is excluded or less than 0,
         * the current position of the seekbar is used
         */
        fun setTimeTextViews(
            view: TimelineBinding,
            timeline: Timeline,
            position: Double = -1.0,
            startEndLabels: TimelineSettingsPanelBinding? = null,
        ) {
            val labels = startEndLabels ?: view.settingsCS
            labels.startTimeTextView.text = TimelineDisplayFormatter.formatStartEnd(timeline.start)
            labels.endTimeTextview.text = TimelineDisplayFormatter.formatStartEnd(timeline.end)

            if (position >= 0) {
                setCurrentTimeTextView(view, timeline, position)
            } else {
                setCurrentTimeTextView(view, timeline, timeline.position)
            }
            view.timelineControls.setNowMarkerFromTimeline(timeline.start, timeline.end)
        }

        /** Sets the TextView that displays the current time for the timeline.
         *
         * Position should be between 0f and 1f
         * **/
        fun setCurrentTimeTextView(view: TimelineBinding, timeline: Timeline, position: Double) {
            val midDate = if (position in 0.0..1.0) {
                calculateMidpointDate(timeline.start, timeline.end, position)
            } else {
                timeline.currentDate
            }
            view.currentTimeTextView.text = TimelineDisplayFormatter.formatCurrentTime(midDate)
            view.currentDateTextView.text = TimelineDisplayFormatter.formatCurrentDate(midDate)
        }

        /** Given the startDate, endDate, and a location between 0f and 1f,
         *  returns a midpoint Date
         *  **/
        private fun calculateMidpointDate(startDate: Date, endDate: Date, position: Double): Date {
            require(position in 0.0..1.0) { "Progress must be between 0 and 1 (inclusive)" }
            val duration = endDate.time - startDate.time
            val durationMult = duration * position
            return Date(startDate.time + durationMult.toLong())
        }
    }
}
