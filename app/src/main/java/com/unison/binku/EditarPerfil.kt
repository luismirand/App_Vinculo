package com.unison.binku

import android.app.DatePickerDialog
import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.unison.binku.databinding.ActivityEditarPerfilBinding
import java.util.Calendar
import java.util.HashMap

class EditarPerfil : AppCompatActivity() {

    private lateinit var binding: ActivityEditarPerfilBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog
    private lateinit var storage: FirebaseStorage // <-- MODIFICADO
    private var imageUri: Uri? = null
    private var miUrlImagenPerfil = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditarPerfilBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        storage = FirebaseStorage.getInstance() // <-- MODIFICADO
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Por favor espere")
        progressDialog.setCanceledOnTouchOutside(false)

        cargarInfo()

        binding.FABCambiarImg.setOnClickListener {
            selec_imagen_de()
        }

        binding.BtnActualizar.setOnClickListener {
            validarDatos()
        }

        binding.EtFNac.setOnClickListener {
            mostrarSelectorFecha()
        }
    }

    private fun mostrarSelectorFecha() {
        val c = Calendar.getInstance()

        val fechaActual = binding.EtFNac.text.toString().trim()
        if (fechaActual.isNotEmpty()) {
            try {
                val parts = fechaActual.split("/")
                val dia = parts[0].toInt()
                val mes = parts[1].toInt() - 1
                val anio = parts[2].toInt()
                c.set(anio, mes, dia)
            } catch (e: Exception) {
            }
        }

        val anio = c.get(Calendar.YEAR)
        val mes = c.get(Calendar.MONTH)
        val dia = c.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this, { _, year, month, dayOfMonth ->
            val fechaSeleccionada = String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year)
            binding.EtFNac.setText(fechaSeleccionada)
        }, anio, mes, dia)

        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()

        datePickerDialog.show()
    }

    private fun cargarInfo() {

        val ref = FirebaseDatabase.getInstance().getReference("Usuarios")
        ref.child("${firebaseAuth.uid}")
            .addValueEventListener( object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    val nombres = "${snapshot.child("nombres").value}"
                    val imagen = "${snapshot.child("urlImagenPerfil").value}"
                    val f_nac = "${snapshot.child("fecha_nac").value}"
                    val telefono = "${snapshot.child("telefono").value}"
                    val codTelefono = "${snapshot.child("codigoTelefono").value}"

                    miUrlImagenPerfil = imagen

                    binding.EtNombres.setText(nombres)
                    binding.EtFNac.setText(f_nac) // <-- Esto carga la fecha existente
                    binding.EtTelefono.setText(telefono)

                    try{
                        if (imagen.isNotEmpty() && imagen != "null") {
                            Glide.with(applicationContext)
                                .load(imagen)
                                .placeholder(R.drawable.img_perfil)
                                .into(binding.imgPerfil)
                        } else {
                            binding.imgPerfil.setImageResource(R.drawable.img_perfil)
                        }
                    }catch(e: Exception){
                        Toast.makeText(this@EditarPerfil,
                            "${e.message}",
                            Toast.LENGTH_SHORT).show()
                    }

                    try{
                        val codigo = codTelefono.replace("+", "").toInt()
                        binding.selectorCod.setCountryForPhoneCode(codigo)
                    }catch (e: Exception){
                        // No mostrar Toast aquí, es normal si no hay código guardado
                    }

                }
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@EditarPerfil, "Error al cargar datos", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun validarDatos() {
        val nombres = binding.EtNombres.text.toString().trim()
        val fNac = binding.EtFNac.text.toString().trim() // <-- Esto ahora obtiene la fecha del selector
        val telefono = binding.EtTelefono.text.toString().trim()
        val codTelefono = binding.selectorCod.selectedCountryCodeWithPlus

        if (nombres.isEmpty()) {
            Toast.makeText(this, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show()
            return
        }

        progressDialog.setMessage("Actualizando perfil...")
        progressDialog.show()

        if (imageUri == null) {
            // --- MODIFICADO ---
            // Caso 1: El usuario no cambió la foto.
            // Simplemente actualizamos la info con la URL que ya teníamos.
            actualizarInfoEnDB(nombres, fNac, telefono, codTelefono, miUrlImagenPerfil)
        } else {
            // Caso 2: El usuario seleccionó una NUEVA foto.
            // Tenemos que subirla primero.
            subirImagenDePerfil(imageUri!!, nombres, fNac, telefono, codTelefono)
            // --- FIN MODIFICADO ---
        }
    }


    private fun subirImagenDePerfil(
        uri: Uri,
        nombres: String,
        fNac: String,
        telefono: String,
        codTelefono: String
    ) {
        val uid = firebaseAuth.uid ?: return
        Log.d("EditarPerfil", "Iniciando subida de imagen de perfil...")

        val imagenComprimida = ImageCompressor.compressImage(this, uri, quality = 80, maxSizeKb = 500)

        if (imagenComprimida == null) {
            progressDialog.dismiss()
            Toast.makeText(this, "Error al procesar la imagen", Toast.LENGTH_SHORT).show()
            return
        }

        val storageRef = storage.getReference("ProfileImages/$uid/profile.jpg")

        storageRef.putBytes(imagenComprimida)
            .addOnSuccessListener {
                Log.d("EditarPerfil", "Imagen subida a Storage. Obteniendo URL...")

                storageRef.downloadUrl
                    .addOnSuccessListener { downloadUrl ->

                        Log.d("EditarPerfil", "URL obtenida: $downloadUrl")
                        actualizarInfoEnDB(nombres, fNac, telefono, codTelefono, downloadUrl.toString())
                    }
                    .addOnFailureListener { e ->
                        progressDialog.dismiss()
                        Log.e("EditarPerfil", "Error al obtener URL", e)
                        Toast.makeText(this, "Error al obtener URL: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Log.e("EditarPerfil", "Error al subir imagen", e)
                Toast.makeText(this, "Error al subir imagen: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


    private fun actualizarInfoEnDB(nombres: String, fNac: String, telefono: String, codTelefono: String, imagenUrl: String) {
        val uid = firebaseAuth.uid!!
        val ref = FirebaseDatabase.getInstance().getReference("Usuarios")

        val hashMap = HashMap<String, Any>()
        hashMap["nombres"] = nombres
        hashMap["fecha_nac"] = fNac // <-- Se guarda la nueva fecha
        hashMap["telefono"] = telefono
        hashMap["codigoTelefono"] = codTelefono
        hashMap["urlImagenPerfil"] = imagenUrl // <-- USA LA URL (local o de Storage)

        ref.child(uid).updateChildren(hashMap)
            .addOnSuccessListener {
                progressDialog.dismiss()
                Toast.makeText(this, "Perfil actualizado con éxito", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Toast.makeText(this, "Error al actualizar el perfil: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


    private fun selec_imagen_de(){
        val popupMenu = PopupMenu(this, binding.FABCambiarImg)

        popupMenu.menu.add(Menu.NONE,1,1, "Camara")
        popupMenu.menu.add(Menu.NONE,2,2, "Galeria")
        popupMenu.show()

        popupMenu.setOnMenuItemClickListener { item ->
            val itemId = item.itemId
            if(itemId == 1 ){
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                    concederPermisosCamara.launch(arrayOf(android.Manifest.permission.CAMERA))
                }else{
                    concederPermisosCamara.launch(arrayOf(
                        android.Manifest.permission.CAMERA,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ))
                }
            }else if(itemId == 2){
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                    imagenGaleria()
                } else{
                    concederPermisosAlmacenamiento.launch(
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                }

            }
            return@setOnMenuItemClickListener true
        }
    }

    private val concederPermisosCamara =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()) {
                resultado -> var concedidoTodos = true
            for(seConcede in resultado.values) {
                concedidoTodos = concedidoTodos && seConcede
            }
            if (concedidoTodos){
                imagenCamara()
            }else {
                Toast.makeText(
                    this,
                    "El permiso de la camara o almacenamiento se denegaron",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    private val concederPermisosAlmacenamiento=
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()) {
                esConcedido ->
            if (esConcedido){
                imagenGaleria()
            }else {
                Toast.makeText(
                    this,
                    "El permiso de almacenamiento se denego",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    private val resultadoCamara_ARL =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()){ resultado ->
            if(resultado.resultCode == RESULT_OK){
                try{
                    Glide.with(this)
                        .load(imageUri)
                        .placeholder(R.drawable.img_perfil)
                        .into(binding.imgPerfil)
                }catch(e: Exception){
                }
            }else{
                Toast.makeText(
                    this,
                    "La captura de imagen se canceló",
                    Toast.LENGTH_SHORT
                ).show(
                )
            }
        }

    private val resultadoGaleria_ARL =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()){ resultado ->
            if(resultado.resultCode == RESULT_OK){
                val data = resultado.data
                imageUri = data!!.data

                try{
                    Glide.with(this)
                        .load(imageUri)
                        .placeholder(R.drawable.img_perfil)
                        .into(binding.imgPerfil)
                }catch(e: Exception){
                }
            }else{
                Toast.makeText(
                    this,
                    "La selección de imagen se canceló",
                    Toast.LENGTH_SHORT
                ).show(
                )
            }
        }

    private fun imagenCamara() {
        val contentValues = ContentValues()
        contentValues.put(MediaStore.Images.Media.TITLE, "Titulo_imagen")
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, "Descripcion_imagen")
        imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        resultadoCamara_ARL.launch(intent)
    }

    private fun imagenGaleria() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        resultadoGaleria_ARL.launch(intent)
    }
}