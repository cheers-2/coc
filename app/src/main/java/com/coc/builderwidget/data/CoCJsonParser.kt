package com.coc.builderwidget.data

import com.coc.builderwidget.model.AccountSummary
import com.coc.builderwidget.model.BuilderTask
import com.coc.builderwidget.model.UpgradeCategory
import org.json.JSONArray
import org.json.JSONObject

object CoCJsonParser {

    fun parse(
        rawJson: String,
        accountSlot: Int,
        accountName: String,
        fallbackNowMillis: Long = System.currentTimeMillis()
    ): Result<AccountSummary> {
        return runCatching {
            val root = JSONObject(rawJson.trim())
            val tag = root.optString("tag", "")
            val jsonTimestampSec = root.optLong("timestamp", 0L)
            val baseTimeMillis = if (jsonTimestampSec > 1_600_000_000L) {
                jsonTimestampSec * 1000L
            } else {
                fallbackNowMillis
            }

            val tasks = mutableListOf<BuilderTask>()

            fun extractArray(key: String, category: UpgradeCategory) {
                val arr: JSONArray = root.optJSONArray(key) ?: return
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val timerSec = obj.optLong("timer", 0L)
                    if (timerSec > 0L) {
                        val id = obj.optInt("data", 0)
                        val lvl = obj.optInt("lvl", 0)
                        tasks.add(
                            BuilderTask(
                                accountSlot = accountSlot,
                                accountName = accountName,
                                itemId = id,
                                itemName = CoCItemNames.resolve(id, category),
                                currentLevel = lvl,
                                category = category,
                                finishTimeMillis = baseTimeMillis + (timerSec * 1000L)
                            )
                        )
                    }
                }
            }

            // Home Village Builders
            extractArray("buildings", UpgradeCategory.BUILDER)
            extractArray("traps", UpgradeCategory.BUILDER)
            extractArray("heroes", UpgradeCategory.BUILDER)

            // Laboratory & Pet House
            extractArray("units", UpgradeCategory.LAB)
            extractArray("spells", UpgradeCategory.LAB)
            extractArray("siege_machines", UpgradeCategory.LAB)
            extractArray("pets", UpgradeCategory.PET)

            // Builder Base
            extractArray("buildings2", UpgradeCategory.BUILDER_BASE)
            extractArray("traps2", UpgradeCategory.BUILDER_BASE)
            extractArray("heroes2", UpgradeCategory.BUILDER_BASE)
            extractArray("units2", UpgradeCategory.BUILDER_BASE)

            AccountSummary(
                slot = accountSlot,
                name = accountName,
                tag = tag,
                lastUpdatedMillis = System.currentTimeMillis(),
                tasks = tasks.sortedBy { it.finishTimeMillis }
            )
        }
    }
}
