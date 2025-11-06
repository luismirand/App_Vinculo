package com.unison.binku

import android.app.Application
import com.google.firebase.database.FirebaseDatabase

class MiAppFirebase : Application() {

    override fun onCreate() {
        super.onCreate()

        // Habilita la persistencia de datos sin conexión.
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
    }
}