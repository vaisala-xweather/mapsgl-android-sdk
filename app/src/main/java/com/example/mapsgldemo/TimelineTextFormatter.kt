package com.example.mapsgldemo

import com.xweather.mapsgl.anim.Timeline
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Class used to format the start, end, and current times into user-readable strings  **/
class TimelineTextFormatter {
    companion object {

        private val formatter = SimpleDateFormat("MM/dd/yyyy h:mma", Locale.US)
        private val formatterCurrentTime = SimpleDateFormat("h:mm a", Locale.US)
        private val formatterCurrentDate = SimpleDateFormat("EEE, MMM d", Locale.US)

        /** Sets the Start, End, and Current time TextViews.
         *
         * Position should be between 0f and 1f. If position is excluded or less than 0,
         * the current position of the seekbar is used **/
        fun setTimeTextViews(
            view: com.example.mapsgldemo.databinding.TimelineBinding,
            timeline: Timeline,
            position: Double = -1.0
        ) {
            view.startTimeTextView.text = formatter.format(timeline.start)
            view.endTimeTextview.text = formatter.format(timeline.end)
            setCurrentTimeTextView(view, timeline)
        }

        /** Sets the TextView that displays the current time for the timeline.
         *
         * Position should be between 0f and 1f
         * **/
        fun setCurrentTimeTextView(
            view: com.example.mapsgldemo.databinding.TimelineBinding,
            timeline: Timeline,
        ) {
            val midDate = timeline.currentDate
            view.currentTimeTextView.text = formatterCurrentTime.format(midDate)
            view.currentDateTextView.text = formatterCurrentDate.format(midDate)
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