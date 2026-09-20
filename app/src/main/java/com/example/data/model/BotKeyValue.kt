package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "bot_key_values",
    foreignKeys = [
        ForeignKey(
            entity = BotProject::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["projectId", "storageKey"], unique = true)
    ]
)
data class BotKeyValue(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long,
    val storageKey: String,
    val storageValue: String,
    val valueType: String = "STRING", // STRING, JSON, NUMBER, BOOLEAN
    val updatedAt: Long = System.currentTimeMillis()
)
