package com.unison.binku.Adaptadores // Or your correct package name

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.unison.binku.Constantes
import com.unison.binku.Models.ModeloPost // Ensure correct import
import com.unison.binku.R
import com.unison.binku.databinding.ItemPostCardBinding

class AdaptadorPost(
    private val context: Context,
    private var postList: ArrayList<ModeloPost>,
    private val onDeleteClick: (String) -> Unit,
    private val onLikeClick: (String) -> Unit
) : RecyclerView.Adapter<AdaptadorPost.PostViewHolder>() {

    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    // Cache for avatars is removed as the URL comes with the post now

    inner class PostViewHolder(val binding: ItemPostCardBinding) : RecyclerView.ViewHolder(binding.root) {
        val btnDelete: ImageButton = binding.btnDeletePost
        val tvLocation: TextView = binding.tvPostLocation
        val tvLikeCount: TextView = binding.tvLikeCount
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemPostCardBinding.inflate(LayoutInflater.from(context), parent, false)
        return PostViewHolder(binding)
    }

    override fun getItemCount(): Int = postList.size

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = postList[position]

        // --- >>> CARGAR AVATAR DEL POST <<< ---
        try {
            if (post.urlAvatarAutor.isNotEmpty()) {
                Glide.with(context)
                    .load(post.urlAvatarAutor)
                    .placeholder(R.drawable.ic_perfil) // Placeholder while loading
                    .error(R.drawable.ic_perfil) // Placeholder if error
                    .circleCrop() // Make it circular
                    .into(holder.binding.ivUserAvatar)
            } else {
                // If no avatar URL is stored with the post, show placeholder
                holder.binding.ivUserAvatar.setImageResource(R.drawable.ic_perfil)
            }
        } catch (e: Exception) {
            Log.e("AdaptadorPost", "Error loading avatar: ${e.message}")
            holder.binding.ivUserAvatar.setImageResource(R.drawable.ic_perfil) // Fallback placeholder
        }
        // --- >>> FIN <<< ---


        holder.binding.tvUserName.text = post.nombreAutor
        holder.binding.tvPostContent.text = post.textoPost
        holder.binding.tvPostTime.text = Constantes.obtenerFecha(post.timestamp)

        if (post.ubicacion.isNotEmpty()) {
            val locationText = "en ${post.ubicacion}"
            holder.tvLocation.text = locationText
            holder.tvLocation.visibility = View.VISIBLE
            holder.tvLocation.setOnClickListener { abrirMapa(post.ubicacion) }
        } else {
            holder.tvLocation.visibility = View.GONE
            holder.tvLocation.setOnClickListener(null)
        }

        if (post.imagenUrlPost.isNotEmpty()) {
            holder.binding.ivPostImage.visibility = View.VISIBLE
            Glide.with(context)
                .load(post.imagenUrlPost)
                .placeholder(R.color.guinda_ripple)
                .into(holder.binding.ivPostImage)
        } else {
            holder.binding.ivPostImage.visibility = View.GONE
        }

        // Like logic
        holder.tvLikeCount.text = post.contadorLikes.toString()
        val likeIconColor = if (post.isLikedPorUsuarioActual) R.color.guinda else R.color.gris_oscuro
        holder.binding.btnLike.setIconTintResource(likeIconColor)
        holder.binding.btnLike.setOnClickListener {
            onLikeClick(post.postId)
        }

        // Comment button listener (simple Toast for now)
        holder.binding.btnComment.setOnClickListener {
            Toast.makeText(context, "Comentar post ${post.postId}", Toast.LENGTH_SHORT).show()
        }

        // Delete button logic
        if (post.uidAutor == currentUserId) {
            holder.btnDelete.visibility = View.VISIBLE
            holder.btnDelete.setOnClickListener { onDeleteClick(post.postId) }
        } else {
            holder.btnDelete.visibility = View.GONE
        }
    }

    private fun abrirMapa(locationName: String) {
        val encodedLocation = Uri.encode(locationName)
        val mapUri = Uri.parse("geo:0,0?q=$encodedLocation")
        val mapIntent = Intent(Intent.ACTION_VIEW, mapUri)
        try {
            context.startActivity(mapIntent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "No se encontró una app de mapas instalada", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "No se pudo abrir la app de mapas", Toast.LENGTH_SHORT).show()
        }
    }

    fun updatePosts(newPosts: ArrayList<ModeloPost>) {
        postList = newPosts
        notifyDataSetChanged() // Consider DiffUtil for performance
    }
}