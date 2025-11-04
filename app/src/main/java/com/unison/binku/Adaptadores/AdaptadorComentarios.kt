package com.unison.binku.Adaptadores

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.unison.binku.Models.ModeloComentario
import com.unison.binku.R
import com.unison.binku.databinding.ItemComentarioBinding

class AdaptadorComentarios(
    private val context: Context,
    private var comentarios: ArrayList<ModeloComentario>,
    private val onLikeClick: (String) -> Unit
) : RecyclerView.Adapter<AdaptadorComentarios.VH>() {

    inner class VH(val b: ItemComentarioBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemComentarioBinding.inflate(LayoutInflater.from(context), parent, false)
        return VH(binding)
    }

    override fun getItemCount(): Int = comentarios.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val c = comentarios[position]
        val b = holder.b

        b.tvNombre.text = c.nombreAutor
        b.tvTexto.text = c.texto

        if (c.urlAvatarAutor.isNotBlank()) {
            Glide.with(context)
                .load(c.urlAvatarAutor)
                .placeholder(R.drawable.ic_perfil_black)
                .error(R.drawable.ic_perfil_black)
                .circleCrop()
                .into(b.ivAvatar)
        } else {

            b.ivAvatar.setImageResource(R.drawable.ic_perfil_black)
        }


        if (c.imagenUrl.isNotBlank()) {
            b.ivImagen.visibility = View.VISIBLE
            Glide.with(context).load(c.imagenUrl).into(b.ivImagen)
        } else {
            b.ivImagen.visibility = View.GONE
        }

        b.tvLikes.text = c.contadorLikes.toString()
        setLikeIcon(b.btnLike, c.isLikedPorUsuarioActual)

        b.btnLike.setOnClickListener {
            onLikeClick(c.commentId)
        }
    }


    private fun setLikeIcon(btn: ImageButton, liked: Boolean) {
        val colorRes = if (liked) R.color.guinda else R.color.gris_oscuro
        val color = ContextCompat.getColor(context, colorRes)

        btn.setColorFilter(color)
        btn.setImageResource(R.drawable.ic_like)


        fun update(newList: ArrayList<ModeloComentario>) {
            comentarios = newList
            notifyDataSetChanged()
        }
    }
}