package br.com.mazim.financasmodernas.data

import android.content.Context
import br.com.mazim.financasmodernas.model.FinanceEntry
import org.json.JSONArray
import java.io.File

class FinanceStore(context: Context) {
    private val dataFile = File(context.filesDir, "finance_entries.json")

    fun load(): List<FinanceEntry> {
        if (!dataFile.exists()) return emptyList()
        return runCatching {
            val raw = dataFile.readText(Charsets.UTF_8)
            if (raw.isBlank()) {
                emptyList()
            } else {
                val array = JSONArray(raw)
                buildList {
                    for (index in 0 until array.length()) {
                        add(FinanceEntry.fromJson(array.getJSONObject(index)))
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    fun save(entries: List<FinanceEntry>) {
        val array = JSONArray()
        entries.forEach { array.put(it.toJson()) }
        dataFile.writeText(array.toString(), Charsets.UTF_8)
    }
}
