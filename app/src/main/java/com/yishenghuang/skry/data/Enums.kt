package com.yishenghuang.skry.data

enum class ScanStatus {
    PENDING,
    DONE,
    ERROR
}

enum class VaultStatus {
    NONE,
    MOVED,
    REDACTED
}

/** Manual user review of an AI privacy finding. */
enum class UserReviewStatus {
    NONE,
    CONFIRMED_LEAK,
    DISMISSED
}
