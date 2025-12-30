package com.dayscounter

import android.app.Application

/**
 * Класс Application для приложения Days Counter.
 *
 * Проект использует ручной подход к внедрению зависимостей через factory методы.
 */
class DaysCounterApplication : Application() {
    companion object {
        private const val TAG = "DaysCounterApp"

        /**
         * Экземпляр приложения для доступа из не-Android компонентов.
         */
        lateinit var instance: DaysCounterApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        android.util.Log.d(TAG, "Приложение инициализировано")
    }
}
