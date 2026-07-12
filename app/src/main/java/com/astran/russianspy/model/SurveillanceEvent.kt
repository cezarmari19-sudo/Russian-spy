package com.astran.russianspy.model

data class SurveillanceEvent(
    val id: String,
    val type: SurveillanceEventType,
    val timestampMillis: Long,
    val relatedRoomId: String = ""
)

enum class SurveillanceEventType {
    SPY_SENDING_INTEL
}
