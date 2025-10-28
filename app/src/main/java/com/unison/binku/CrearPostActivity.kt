package com.unison.binku

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.unison.binku.databinding.ActivityCrearPostBinding

class CrearPostActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCrearPostBinding
    private var imagenUri: Uri? = null // Para guardar la URI de la imagen seleccionada

    // Launcher para abrir la galería
    private val galeriaLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            imagenUri = uri
            // Mostrar la imagen seleccionada en el ImageView
            binding.ivPostImagePreview.setImageURI(uri)
            binding.ivPostImagePreview.visibility = View.VISIBLE
        } else {
            Toast.makeText(this, "Selección cancelada", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCrearPostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Listener para el botón de seleccionar imagen
        binding.btnSelectImage.setOnClickListener {
            galeriaLauncher.launch("image/*")
        }

        // Listener para el botón de publicar
        binding.btnPublishPost.setOnClickListener {
            publicarPost()
        }
    }

    private fun publicarPost() {
        val postText = binding.etPostText.text.toString().trim()

        // Validar que al menos haya texto
        if (postText.isEmpty()) {
            Toast.makeText(this, "Escribe algo para publicar...", Toast.LENGTH_SHORT).show()
            return
        }

        // Crear un Intent para devolver los datos al FragmentFeed
        val resultIntent = Intent()
        resultIntent.putExtra("POST_TEXT", postText)
        // Convertimos la URI a String para pasarla fácilmente
        resultIntent.putExtra("POST_IMAGE_URI", imagenUri?.toString())

        // Establecer el resultado como OK y adjuntar los datos
        setResult(Activity.RESULT_OK, resultIntent)

        // Cerrar esta actividad
        finish()
    }
}
