package com.unison.binku

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.unison.binku.Fragmentos.FragmentFeed
import com.unison.binku.Fragmentos.FragmentNotificaciones
import com.unison.binku.Fragmentos.FragmentPerfil
import com.unison.binku.Fragmentos.FragmentVideos
import com.unison.binku.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializamos y comprobamos la sesión de Firebase
        firebaseAuth = FirebaseAuth.getInstance()
        comprobarSesion()

        // El fragmento inicial sigue siendo el Feed
        verFragmentoFeed()

        // Listener para la barra de navegación inferior
        binding.BottomNV.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.Item_Feed -> {
                    verFragmentoFeed()
                    true
                }
                R.id.Item_Videos -> {
                    verFragmentoVideos()
                    true
                }
                R.id.Item_Publicar -> {
                    // Acción para el ítem de menú "Publicar"
                    val fragmentFeed = supportFragmentManager.findFragmentByTag("FragmentFeed") as? FragmentFeed
                    fragmentFeed?.lanzarCrearPost()
                    false // No seleccionar este ítem
                }
                R.id.Item_Notificaciones -> {
                    verFragmentoNotificaciones()
                    true
                }
                R.id.Item_Perfil -> {
                    verFragmentoPerfil()
                    true
                }
                else -> {
                    false
                }
            }
        }

        // Listener para el Botón Flotante (FAB)
        binding.FAB.setOnClickListener {
            // Hacemos exactamente lo mismo que con el ítem de menú "Publicar"
            val fragmentFeed = supportFragmentManager.findFragmentByTag("FragmentFeed") as? FragmentFeed
            fragmentFeed?.lanzarCrearPost()
            // Aquí no necesitamos devolver true/false
        }
    } // Fin de onCreate

    // Nueva función para verificar si el usuario ha iniciado sesión
    private fun comprobarSesion() {
        if (firebaseAuth.currentUser == null){
            startActivity(Intent(this, OpcionesLogin::class.java))
            finishAffinity()
        }
    }

    private fun verFragmentoFeed(){
        binding.TituloRL.text="Feed"
        val fragment = FragmentFeed()
        val fragmentTransition = supportFragmentManager.beginTransaction()
        // Asegúrate de que el tag sea consistente
        fragmentTransition.replace(binding.FragmentL1.id, fragment, "FragmentFeed")
        fragmentTransition.commit()
    }

    private fun verFragmentoVideos(){
        binding.TituloRL.text="Videos"
        val fragment = FragmentVideos()
        val fragmentTransition = supportFragmentManager.beginTransaction()
        fragmentTransition.replace(binding.FragmentL1.id, fragment, "FragmentVideos")
        fragmentTransition.commit()
    }

    private fun verFragmentoNotificaciones(){
        binding.TituloRL.text="Notificaciones"
        val fragment = FragmentNotificaciones()
        val fragmentTransition = supportFragmentManager.beginTransaction()
        fragmentTransition.replace(binding.FragmentL1.id, fragment, "FragmentNotificaciones")
        fragmentTransition.commit()
    }

    private fun verFragmentoPerfil(){
        binding.TituloRL.text="Mi Perfil"
        val fragment = FragmentPerfil()
        val fragmentTransition = supportFragmentManager.beginTransaction()
        fragmentTransition.replace(binding.FragmentL1.id, fragment, "FragmentPerfil")
        fragmentTransition.commit()
    }
}

