package com.unison.binku.Fragmentos

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.unison.binku.Adaptadores.AdaptadorPost // Asegúrate que el paquete sea correcto
import com.unison.binku.CrearPostActivity
import com.unison.binku.ViewModels.FeedViewModel
import com.unison.binku.databinding.FragmentFeedBinding
import java.util.UUID

class FragmentFeed : Fragment() {

    private lateinit var binding: FragmentFeedBinding
    private lateinit var mContext: Context
    private lateinit var adaptadorPost: AdaptadorPost
    private lateinit var firebaseAuth: FirebaseAuth // Añadido para obtener datos del usuario

    private val feedViewModel: FeedViewModel by viewModels()

    private val crearPostLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Log.d("FragmentFeed", "Resultado OK recibido de CrearPostActivity")
            val data = result.data
            val postText = data?.getStringExtra("POST_TEXT") ?: ""
            val imageUriString = data?.getStringExtra("POST_IMAGE_URI")

            if (postText.isNotEmpty()) {
                // TODO: Handle image upload before saving if imageUriString is not null
                feedViewModel.agregarPostFirebase(postText, imageUriString)
                Toast.makeText(mContext, "Publicando...", Toast.LENGTH_SHORT).show()
            }
        } else {
            Log.d("FragmentFeed", "Resultado NO OK recibido de CrearPostActivity (Código: ${result.resultCode})")
        }
    }

    override fun onAttach(context: Context) {
        mContext = context
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFeedBinding.inflate(inflater, container, false)
        firebaseAuth = FirebaseAuth.getInstance() // Inicializar FirebaseAuth
        adaptadorPost = AdaptadorPost(mContext, ArrayList())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvPosts.layoutManager = LinearLayoutManager(mContext)
        binding.rvPosts.adapter = adaptadorPost

        // --- EL LISTENER DEL FAB HA SIDO REMOVIDO CORRECTAMENTE ---

        feedViewModel.posts.observe(viewLifecycleOwner, Observer { posts ->
            adaptadorPost.updatePosts(posts)
            actualizarVistaFeed(posts.isEmpty())
        })
    }

    // --- >>> FUNCIÓN AÑADIDA <<< ---
    fun lanzarCrearPost() {
        Log.d("FragmentFeed", "lanzarCrearPost llamado desde MainActivity!") // Log para confirmar
        val intent = Intent(mContext, CrearPostActivity::class.java)
        // Usamos el launcher que ya tienes definido para esperar el resultado
        crearPostLauncher.launch(intent)
    }
    // --- >>> FIN DE LA FUNCIÓN AÑADIDA <<< ---


    private fun actualizarVistaFeed(isEmpty: Boolean) {
        if (isEmpty) {
            binding.placeholderContainer.visibility = View.VISIBLE
            binding.rvPosts.visibility = View.GONE
        } else {
            binding.placeholderContainer.visibility = View.GONE
            binding.rvPosts.visibility = View.VISIBLE
        }
    }
}

