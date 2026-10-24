package com.reayt.youtube.di

import androidx.room.Room
import com.reayt.youtube.data.UserDataRepository
import com.reayt.youtube.data.YouTubeRepository
import com.reayt.youtube.data.db.AppDatabase
import com.reayt.youtube.player.PlayerStateHolder
import com.reayt.youtube.ui.viewmodel.*
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "reayt_youtube.db"
        ).fallbackToDestructiveMigration().build()
    }

    single { get<AppDatabase>().watchHistoryDao() }
    single { get<AppDatabase>().likedVideoDao() }
    single { get<AppDatabase>().watchLaterDao() }
    single { get<AppDatabase>().subscriptionDao() }
    single { get<AppDatabase>().searchHistoryDao() }
    single { get<AppDatabase>().downloadDao() }
    single { get<AppDatabase>().appSettingsDao() }

    single { YouTubeRepository(androidContext(), get()) }
    single { UserDataRepository(get()) }
    single { PlayerStateHolder(androidContext()) }

    viewModel { HomeViewModel(get()) }
    viewModel { SearchViewModel(get(), get()) }
    viewModel { WatchViewModel(get(), get()) }
    viewModel { SubscriptionsViewModel(get()) }
    viewModel { YouViewModel(get()) }
}
