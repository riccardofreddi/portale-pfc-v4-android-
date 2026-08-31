package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.*
import com.example.data.local.entity.*

@Database(
    entities = [
        CachedDocumentEntity::class,
        CachedMessaggioEntity::class,
        CachedCassettoEntity::class,
        CachedNotificaEntity::class,
        CachedAuditEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class PfcDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao
    abstract fun messaggioDao(): MessaggioDao
    abstract fun cassettoDao(): CassettoDao
    abstract fun notificaDao(): NotificaDao
    abstract fun auditDao(): AuditDao

    companion object {
        @Volatile
        private var INSTANCE: PfcDatabase? = null

        fun getInstance(context: Context): PfcDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PfcDatabase::class.java,
                    "pfc_portal.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
