package com.unison.binku

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.unison.binku.databinding.ActivityCrearPostBinding
import java.util.Locale
import android.location.Address // --- >>> AÑADIR ESTE IMPORT <<< ---

class CrearPostActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCrearPostBinding
    private var imagenUri: Uri? = null

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLatitude: Double = 0.0
    private var currentLongitude: Double = 0.0

    private val galeriaLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            imagenUri = uri
            mostrarVistaPreviaImagen()
        } else {
            Toast.makeText(this, "Selección cancelada", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestCameraPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false

        // Android 13+ necesita READ_MEDIA_IMAGES para leer la foto del MediaStore
        val readImagesGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.READ_MEDIA_IMAGES] ?: false
        } else {
            true
        }

        // Android 9 o menor puede requerir WRITE_EXTERNAL_STORAGE
        val storageGranted = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE] ?: false
        } else {
            true
        }

        if (cameraGranted && readImagesGranted && storageGranted) {
            abrirCamara()
        } else {
            Toast.makeText(this, "Permisos insuficientes para usar la cámara", Toast.LENGTH_SHORT).show()
        }
    }

    private val cameraActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            mostrarVistaPreviaImagen()
        } else {
            Toast.makeText(this, "Captura cancelada", Toast.LENGTH_SHORT).show()
            if (imagenUri != null) {
                contentResolver.delete(imagenUri!!, null, null)
                imagenUri = null
            }
        }
    }

    private val requestLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            obtenerUbicacionActual()
        } else {
            Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCrearPostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        binding.btnSelectImage.setOnClickListener { mostrarOpcionesSeleccionImagen() }
        binding.btnRemoveImage.setOnClickListener { quitarImagenSeleccionada() }
        binding.btnGetLocation.setOnClickListener { solicitarPermisoUbicacion() }
        binding.btnPublishPost.setOnClickListener { publicarPost() }
    }

    private fun mostrarOpcionesSeleccionImagen() {
        val popupMenu = PopupMenu(this, binding.btnSelectImage)
        popupMenu.menu.add(Menu.NONE, 1, 1, "Cámara")
        popupMenu.menu.add(Menu.NONE, 2, 2, "Galería")
        popupMenu.show()

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> { solicitarPermisoCamara(); true }
                2 -> { abrirGaleria(); true }
                else -> false
            }
        }
    }

    private fun solicitarPermisoCamara() {
        val permissionsToRequest = mutableListOf(Manifest.permission.CAMERA)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        requestCameraPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
    }

    private fun abrirCamara() {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.TITLE, "Binku_Post_${System.currentTimeMillis()}")
            put(MediaStore.Images.Media.DESCRIPTION, "Foto tomada para Binku")
        }
        imagenUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        if (imagenUri == null) {
            Toast.makeText(this, "No se pudo crear archivo para la foto", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, imagenUri)
        }
        cameraActivityResultLauncher.launch(intent)
    }

    private fun abrirGaleria() {
        galeriaLauncher.launch("image/*")
    }

    private fun mostrarVistaPreviaImagen() {
        if (imagenUri != null) {
            binding.ivPostImagePreview.setImageURI(imagenUri)
            binding.ivPostImagePreview.visibility = View.VISIBLE
            binding.btnRemoveImage.visibility = View.VISIBLE
            binding.btnSelectImage.text = "Cambiar foto"
        }
    }

    private fun quitarImagenSeleccionada() {
        imagenUri = null
        binding.ivPostImagePreview.setImageURI(null)
        binding.ivPostImagePreview.visibility = View.GONE
        binding.btnRemoveImage.visibility = View.GONE
        binding.btnSelectImage.text = "Añadir foto"
    }

    private fun solicitarPermisoUbicacion() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED -> {
                obtenerUbicacionActual()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                Toast.makeText(this, "Se necesita permiso de ubicación para etiquetar tu post", Toast.LENGTH_LONG).show()
                requestLocationPermissionLauncher.launch(arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION
                ))
            }
            else -> {
                requestLocationPermissionLauncher.launch(arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION
                ))
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun obtenerUbicacionActual() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this,"Concede permisos de ubicación primero", Toast.LENGTH_SHORT).show()
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    currentLatitude = location.latitude
                    currentLongitude = location.longitude
                    Log.d("CrearPostActivity", "Ubicación GPS obtenida: $currentLatitude, $currentLongitude")
                    convertirCoordenadasAUbicacion(currentLatitude, currentLongitude)
                } else {
                    Log.d("CrearPostActivity", "lastLocation es null. GPS podría estar desactivado o sin señal.")
                    Toast.makeText(this, "No se pudo obtener ubicación. ¿GPS activado?", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("CrearPostActivity", "Error al obtener ubicación GPS: ${e.message}")
                Toast.makeText(this, "Error al obtener ubicación: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun convertirCoordenadasAUbicacion(latitude: Double, longitude: Double) {
        if (!Geocoder.isPresent()) {
            Toast.makeText(this, "Servicio Geocoder no disponible", Toast.LENGTH_SHORT).show()
            binding.etPostLocation.setText("$latitude, $longitude")
            return
        }
        val geocoder = Geocoder(this, Locale.getDefault())
        try {
            val addresses: List<Address>? = geocoder.getFromLocation(latitude, longitude, 1)

            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val locationName = address.getAddressLine(0)

                if (!locationName.isNullOrBlank()) {
                    binding.etPostLocation.setText(locationName)
                    Log.d("CrearPostActivity", "Dirección completa encontrada: $locationName")
                } else {
                    val fallbackName = listOfNotNull(address.locality, address.adminArea).joinToString(", ")
                    if (fallbackName.isNotBlank()) {
                        binding.etPostLocation.setText(fallbackName)
                    } else {
                        binding.etPostLocation.setText("Ubicación cercana")
                    }
                    Log.d("CrearPostActivity", "Geocoder devolvió dirección vacía.")
                }
            } else {
                binding.etPostLocation.setText("Ubicación desconocida")
                Log.d("CrearPostActivity", "Geocoder no encontró dirección para coords.")
            }
        } catch (e: Exception) {
            Log.e("CrearPostActivity", "Error de Geocoding: ${e.message}")
            binding.etPostLocation.setText("$latitude, $longitude")
        }
    }

    private fun publicarPost() {
        val postText = binding.etPostText.text.toString().trim()
        val postLocation = binding.etPostLocation.text.toString().trim()

        if (postText.isEmpty()) {
            Toast.makeText(this, "Escribe algo para publicar...", Toast.LENGTH_SHORT).show()
            return
        }

        val resultIntent = Intent()
        resultIntent.putExtra("POST_TEXT", postText)
        resultIntent.putExtra("POST_IMAGE_URI", imagenUri?.toString())
        resultIntent.putExtra("POST_LOCATION", postLocation)

        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }
}
