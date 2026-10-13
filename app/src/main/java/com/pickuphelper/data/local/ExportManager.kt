package com.pickuphelper.data.local

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportManager {

    private const val CSV_HEADER = "waybill_no,shipper_code,shipper_name,platform,status,pickup_code,station_name,created_at,arrived_at,signed_at"

    /**
     * Export all parcels to a CSV file under the app's cache directory,
     * then return its content Uri via FileProvider for sharing.
     * Uses Storage Access Framework (no WRITE_EXTERNAL_STORAGE permission needed).
     *
     * @return Uri of the created file, or null on failure.
     */
    suspend fun exportToCsv(context: Context, parcels: List<ParcelEntity>): Uri? = withContext(Dispatchers.IO) {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "pickup_parcels_$timestamp.csv"
            val cacheDir = File(context.cacheDir, "exports")
            if (!cacheDir.exists()) cacheDir.mkdirs()
            val file = File(cacheDir, fileName)

            FileWriter(file).use { writer ->
                writer.appendLine(CSV_HEADER)
                parcels.forEach { p ->
                    val line = listOf(
                        escapeCsv(p.waybillNo),
                        escapeCsv(p.shipperCode),
                        escapeCsv(p.shipperName),
                        escapeCsv(p.platform ?: ""),
                        escapeCsv(p.status.toString()),
                        escapeCsv(p.pickupCode ?: ""),
                        escapeCsv(p.stationName ?: ""),
                        escapeCsv(formatTs(p.createdAt)),
                        escapeCsv(formatTs(p.arrivedAt)),
                        escapeCsv(formatTs(p.signedAt))
                    ).joinToString(",")
                    writer.appendLine(line)
                }
            }

            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    private fun formatTs(ts: Long?): String {
        return ts?.let {
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(it))
        } ?: ""
    }
}
