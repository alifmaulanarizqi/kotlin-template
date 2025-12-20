package com.example.kotlintemplate.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.kotlintemplate.data.local.dao.UserDao
import com.example.kotlintemplate.data.local.entity.UserEntity

@Database(
    entities = [UserEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
}
