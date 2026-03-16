package com.afternote.app.di

import android.content.Context
import androidx.room.Room
import com.afternote.app.data.db.NoteDatabase
import com.afternote.app.data.db.NoteDao
import com.afternote.app.data.db.VaultNoteDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): NoteDatabase =
        Room.databaseBuilder(ctx, NoteDatabase::class.java, "afternote.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideNoteDao(db: NoteDatabase): NoteDao = db.noteDao()

    @Provides
    fun provideVaultNoteDao(db: NoteDatabase): VaultNoteDao = db.vaultNoteDao()
}
