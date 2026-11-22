package com.example.data.db

enum class MediaType {
    PHOTO,
    VIDEO
}

enum class ScheduleStatus {
    SCHEDULED,
    AWAITING_CONFIRMATION,
    FAILED
}

enum class HistoryOutcome {
    DELETED,
    FAILED,
    CANCELLED
}
