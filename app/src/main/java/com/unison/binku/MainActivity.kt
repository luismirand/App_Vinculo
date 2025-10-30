package com.unison.binku

// --- >>> AÑADIR ESTOS IMPORTS <<< ---
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels // <-- Importante, de 'activity'
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.unison.binku.Fragmentos.FragmentFeed
import com.unison.binku.Fragmentos.FragmentNotificaciones
import com.unison.binku.Fragmentos.FragmentPerfil
import com.unison.binku.Fragmentos.FragmentVideos
import com.unison.binku.ViewModels.FeedViewModel // <-- Importar el ViewModel
import com.unison.binku.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var firebaseAuth: FirebaseAuth

    // --- >>> CÓDIGO NUEVO AÑADIDO <<< ---

    // 1. ViewModel "Scoped" (ligado) a esta Activity
    private val feedViewModel: FeedViewModel by viewModels()

    // 2. Activity Result Launcher (movido desde FragmentFeed)
    private val crearPostLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Log.d("MainActivity", "Result OK recibido de CrearPostActivity")
            val data = result.data
            val postText = data?.getStringExtra("POST_TEXT") ?: ""
            val imageUriString = data?.getStringExtra("POST_IMAGE_URI")
            val postLocation = data?.getStringExtra("POST_LOCATION") ?: ""

            if (postText.isNotEmpty()) {
                // Llamar al ViewModel de la Activity
                feedViewModel.agregarPostFirebase(postText, imageUriString, postLocation)
                Toast.makeText(this, "Publicando...", Toast.LENGTH_SHORT).show()
            }
        } else {
            Log.d("MainActivity", "Result NOT OK recibido (Code: ${result.resultCode})")
        }
    }

    // 3. Función de lanzamiento (ahora pertenece a la Activity)
    private fun lanzarCrearPost() {
        Log.d("MainActivity", "lanzarCrearPost called!")
        val intent = Intent(this, CrearPostActivity::class.java) // 'this' es el Context
        crearPostLauncher.launch(intent)
    }

    // --- >>> FIN DEL CÓDIGO NUEVO <<< ---


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
                // --- >>> CAMBIO DE LÓGICA <<< ---
                R.id.Item_Publicar -> {
                    // Ahora llama a la función de esta Activity
                    lanzarCrearPost()
                    false // No seleccionar este ítem
                }
                // --- >>> FIN DEL CAMBIO <<< ---
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
        // --- >>> CAMBIO DE LÓGICA <<< ---
        binding.FAB.setOnClickListener {
            // Ahora llama a la función de esta Activity
            lanzarCrearPost()
        }
        // --- >>> FIN DEL CAMBIO <<< ---

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