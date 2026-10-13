package com.pickuphelper.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.pickuphelper.domain.model.Status
import com.pickuphelper.domain.model.Trace

@Entity(
    tableName = "traces",
    indices = [Index(value = ["waybillNo"])],
    foreignKeys = [
        ForeignKey(
            entity = ParcelEntity::class,
            parentColumns = ["waybillNo"],
            childColumns = ["waybillNo"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class TraceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val waybillNo: String,
    val traceTime: String,
    val traceStation: String,
    val traceAction: Int
) {
    fun toDomain(): Trace = Trace(
        waybillNo = waybillNo,
        time = traceTime,
        station = traceStation,
        action = Status.fromValue(traceAction)
    )

    companion object {
        fun fromDomain(t: Trace): TraceEntity = TraceEntity(
            waybillNo = t.waybillNo,
            traceTime = t.time,
            traceStation = t.station,
            traceAction = t.action.value
        )
    }
}
