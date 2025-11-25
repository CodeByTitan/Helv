package ca.unb.mobiledev.handyhub.di

import ca.unb.mobiledev.handyhub.home.data.repository.ServiceRepositoryImpl
import ca.unb.mobiledev.handyhub.home.domain.repository.ServiceRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        val firestore = FirebaseFirestore.getInstance()
        // Enable offline persistence for better performance
        firestore.firestoreSettings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .setCacheSizeBytes(com.google.firebase.firestore.FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
            .build()
        return firestore
    }

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage {
        return FirebaseStorage.getInstance()
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindServiceRepository(
        serviceRepositoryImpl: ServiceRepositoryImpl
    ): ServiceRepository

    @Binds
    abstract fun bindAuthRepository(
        authRepositoryImpl: ca.unb.mobiledev.handyhub.auth.data.repository.AuthRepositoryImpl
    ): ca.unb.mobiledev.handyhub.auth.domain.repository.AuthRepository

    @Binds
    abstract fun bindMessagesRepository(
        messagesRepositoryImpl: ca.unb.mobiledev.handyhub.messages.data.repository.MessagesRepositoryImpl
    ): ca.unb.mobiledev.handyhub.messages.domain.repository.MessagesRepository

    @Binds
    abstract fun bindWorkersRepository(
        workersRepositoryImpl: ca.unb.mobiledev.handyhub.home.data.repository.WorkersRepositoryImpl
    ): ca.unb.mobiledev.handyhub.home.domain.repository.WorkersRepository
}
