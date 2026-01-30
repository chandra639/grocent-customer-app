package com.codewithchandra.grocent.database

import android.content.Context
import androidx.room.Room

object DatabaseProvider {
    @Volatile
    private var INSTANCE: GrocentDatabase? = null

    fun getDatabase(context: Context): GrocentDatabase {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                GrocentDatabase::class.java,
                "grocent_database"
            )
                .fallbackToDestructiveMigration() // For development - remove in production
                .build()
            INSTANCE = instance
            instance
        }
    }
}

