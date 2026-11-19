package com.coc.builderwidget.model

enum class UpgradeCategory(val label: String) {
    BUILDER("Tukang"),
    LAB("Lab"),
    PET("Pet"),
    BUILDER_BASE("BB")
}

data class BuilderTask(
    val accountSlot: Int, // 1 or 2
    val accountName: String,
    val itemId: Int,
    val itemName: String,
    val currentLevel: Int,
    val category: UpgradeCategory,
    val finishTimeMillis: Long
) {
    fun remainingMillis(nowMillis: Long = System.currentTimeMillis()): Long {
        return (finishTimeMillis - nowMillis).coerceAtLeast(0L)
    }

    fun isFinished(nowMillis: Long = System.currentTimeMillis()): Boolean {
        return finishTimeMillis <= nowMillis
    }
}

data class AccountSummary(
    val slot: Int,
    val name: String,
    val tag: String,
    val lastUpdatedMillis: Long,
    val tasks: List<BuilderTask>
)
