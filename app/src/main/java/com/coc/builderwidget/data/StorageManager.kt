package com.coc.builderwidget.data

import android.content.Context
import com.coc.builderwidget.model.AccountSummary
import com.coc.builderwidget.model.BuilderTask
import com.coc.builderwidget.model.UpgradeCategory
import org.json.JSONArray
import org.json.JSONObject

class StorageManager(context: Context) {
    private val prefs = context.getSharedPreferences("coc_builder_prefs", Context.MODE_PRIVATE)

    fun saveAccount(summary: AccountSummary) {
        val arr = JSONArray()
        summary.tasks.forEach { task ->
            arr.put(JSONObject().apply {
                put("accountSlot", task.accountSlot)
                put("accountName", task.accountName)
                put("itemId", task.itemId)
                put("itemName", task.itemName)
                put("currentLevel", task.currentLevel)
                put("category", task.category.name)
                put("finishTimeMillis", task.finishTimeMillis)
            })
        }
        val obj = JSONObject().apply {
            put("slot", summary.slot)
            put("name", summary.name)
            put("tag", summary.tag)
            put("lastUpdatedMillis", summary.lastUpdatedMillis)
            put("tasks", arr)
        }
        prefs.edit().putString("account_${summary.slot}", obj.toString()).apply()
    }

    fun getAccount(slot: Int): AccountSummary {
        val defaultName = "Akun $slot"
        val raw = prefs.getString("account_$slot", null)
            ?: return AccountSummary(slot, defaultName, "", 0L, emptyList())

        return runCatching {
            val obj = JSONObject(raw)
            val name = obj.optString("name", defaultName)
            val tag = obj.optString("tag", "")
            val updated = obj.optLong("lastUpdatedMillis", 0L)
            val arr = obj.optJSONArray("tasks") ?: JSONArray()
            val tasks = mutableListOf<BuilderTask>()
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                tasks.add(
                    BuilderTask(
                        accountSlot = item.optInt("accountSlot", slot),
                        accountName = name,
                        itemId = item.optInt("itemId", 0),
                        itemName = item.optString("itemName", "Upgrade"),
                        currentLevel = item.optInt("currentLevel", 0),
                        category = UpgradeCategory.valueOf(
                            item.optString("category", UpgradeCategory.BUILDER.name)
                        ),
                        finishTimeMillis = item.optLong("finishTimeMillis", 0L)
                    )
                )
            }
            AccountSummary(slot, name, tag, updated, tasks.sortedBy { it.finishTimeMillis })
        }.getOrElse {
            AccountSummary(slot, defaultName, "", 0L, emptyList())
        }
    }

    fun getAllBuilderTasksSorted(onlyHomeBuilders: Boolean = false): List<BuilderTask> {
        val all = getAccount(1).tasks + getAccount(2).tasks
        val filtered = if (onlyHomeBuilders) {
            all.filter { it.category == UpgradeCategory.BUILDER }
        } else {
            all
        }
        return filtered.sortedBy { it.finishTimeMillis }
    }

    fun getOnlyHomeBuilders(): Boolean = prefs.getBoolean("only_home_builders", true)

    fun setOnlyHomeBuilders(value: Boolean) {
        prefs.edit().putBoolean("only_home_builders", value).apply()
    }
}
