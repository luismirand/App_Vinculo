package com.unison.binku.Adaptadores

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
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.unison.binku.Constantes
import com.unison.binku.Models.ModeloPost
import com.unison.binku.R
import com.unison.binku.databinding.ItemPostCardBinding

class AdaptadorPost(
    private val context: Context,
    private var postList: ArrayList<ModeloPost>,
    private val onDeleteClick: (String) -> Unit,
    private val onLikeClick: (String) -> Unit
) : RecyclerView.Adapter<AdaptadorPost.PostViewHolder>() {

    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

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

        // Avatar
        try {
            if (post.urlAvatarAutor.isNotEmpty()) {
                Glide.with(context)
                    .load(post.urlAvatarAutor)
                    .placeholder(R.drawable.ic_perfil)
                    .error(R.drawable.ic_perfil)
                    .circleCrop()
                    .into(holder.binding.ivUserAvatar)
            } else {
                holder.binding.ivUserAvatar.setImageResource(R.drawable.ic_perfil)
            }
        } catch (e: Exception) {
            Log.e("AdaptadorPost", "Error loading avatar: ${e.message}")
            holder.binding.ivUserAvatar.setImageResource(R.drawable.ic_perfil)
        }

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

        // Imagen del post: soporta URL remota y content:// local
        if (post.imagenUrlPost.isNotEmpty()) {
            holder.binding.ivPostImage.visibility = View.VISIBLE
            try {
                Glide.with(context)
                    .load(post.imagenUrlPost) // puede ser https:// o content://
                    .placeholder(R.color.guinda_ripple)
                    .into(holder.binding.ivPostImage)
            } catch (e: Exception) {
                // Fallback para content:// si Glide fallara por permisos
                try {
                    holder.binding.ivPostImage.setImageURI(Uri.parse(post.imagenUrlPost))
                } catch (ie: Exception) {
                    Log.e("AdaptadorPost", "Error mostrando imagen local: ${ie.message}")
                }
            }
        } else {
            holder.binding.ivPostImage.visibility = View.GONE
        }

        // Likes
        holder.tvLikeCount.text = post.contadorLikes.toString()
        val likeIconColor = if (post.isLikedPorUsuarioActual) R.color.guinda else R.color.gris_oscuro
        holder.binding.btnLike.setIconTintResource(likeIconColor)
        holder.binding.btnLike.setOnClickListener { onLikeClick(post.postId) }

        // Comentarios (placeholder)
        holder.binding.btnComment.setOnClickListener {
            Toast.makeText(context, "Comentar post ${post.postId}", Toast.LENGTH_SHORT).show()
        }

        // Borrar (solo autor)
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
        notifyDataSetChanged()
    }
}
