package com.unison.binku

import android.Manifest // Importar Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues // Importar ContentValues
import android.content.Intent
import android.content.pm.PackageManager // Importar PackageManager
import android.location.Geocoder // Importar Geocoder
import android.location.Location // Importar Location
import android.net.Uri
import android.os.Build // Importar Build
import android.os.Bundle
import android.provider.MediaStore // Importar MediaStore
import android.util.Log
import android.view.Menu // Importar Menu
import android.view.View
import android.widget.PopupMenu // Importar PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat // Importar ActivityCompat
import androidx.core.content.ContextCompat // Importar ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient // Importar FusedLocationProviderClient
import com.google.android.gms.location.LocationServices // Importar LocationServices
import com.unison.binku.databinding.ActivityCrearPostBinding // Asegúrate que el binding se genere correctamente
import java.util.Locale // Importar Locale

class CrearPostActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCrearPostBinding
    private var imagenUri: Uri? = null // Guarda la URI de la imagen seleccionada (Cámara o Galería)

    // Cliente para servicios de ubicación
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLatitude: Double = 0.0
    private var currentLongitude: Double = 0.0

    // --- LAUNCHERS ---

    // Launcher para el resultado de la Galería
    private val galeriaLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            imagenUri = uri
            mostrarVistaPreviaImagen()
        } else {
            // Opcional: Toast.makeText(this, "Selección cancelada", Toast.LENGTH_SHORT).show()
        }
    }

    // Launcher para permisos de Cámara
    private val requestCameraPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        val storageGranted = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE] ?: false
        } else { true }

        if (cameraGranted && storageGranted) {
            abrirCamara()
        } else {
            Toast.makeText(this, "Permiso de cámara/almacenamiento denegado", Toast.LENGTH_SHORT).show()
        }
    }

    // Launcher para el resultado de la Cámara
    private val cameraActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // La foto se guardó en la 'imagenUri' que creamos
            mostrarVistaPreviaImagen()
        } else {
            Toast.makeText(this, "Captura cancelada", Toast.LENGTH_SHORT).show()
            // Si canceló, la URI temporal podría quedar vacía, la limpiamos por si acaso
            if (imagenUri != null) {
                // contentResolver.delete(imagenUri!!, null, null) // Opcional: borrar archivo temporal si cancela
                imagenUri = null
            }
        }
    }

    // Launcher para permisos de Ubicación
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

    // --- LIFECYCLE ---

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCrearPostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializar servicios de ubicación
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // --- LISTENERS ---
        binding.btnSelectImage.setOnClickListener {
            mostrarOpcionesSeleccionImagen()
        }

        binding.btnRemoveImage.setOnClickListener {
            quitarImagenSeleccionada()
        }

        binding.btnGetLocation.setOnClickListener {
            solicitarPermisoUbicacion()
        }

        binding.btnPublishPost.setOnClickListener {
            publicarPost()
        }
    }

    // --- FUNCIONES PARA MANEJAR IMAGEN ---

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
        val cameraPermission = Manifest.permission.CAMERA
        val storagePermission = Manifest.permission.WRITE_EXTERNAL_STORAGE
        val cameraGranted = ContextCompat.checkSelfPermission(this, cameraPermission) == PackageManager.PERMISSION_GRANTED
        val storageNeededAndGranted = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            ContextCompat.checkSelfPermission(this, storagePermission) == PackageManager.PERMISSION_GRANTED
        } else { true }

        if (cameraGranted && storageNeededAndGranted) {
            abrirCamara()
        } else {
            val permissionsToRequest = mutableListOf(cameraPermission)
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P && !storageNeededAndGranted) {
                permissionsToRequest.add(storagePermission)
            }
            requestCameraPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    private fun abrirCamara() {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.TITLE, "Binku_Post_${System.currentTimeMillis()}")
            put(MediaStore.Images.Media.DESCRIPTION, "Foto tomada para Binku")
        }
        // Crear URI usando MediaStore (funciona en todas las versiones de Android)
        imagenUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        if (imagenUri == null) {
            Toast.makeText(this, "No se pudo crear archivo para la foto", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, imagenUri) // Indicar dónde guardar
        }
        cameraActivityResultLauncher.launch(intent)
    }

    private fun abrirGaleria() {
        galeriaLauncher.launch("image/*") // Lanza el launcher de galería
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


    // --- FUNCIONES PARA MANEJAR UBICACIÓN ---

    private fun solicitarPermisoUbicacion() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED -> {
                obtenerUbicacionActual()
            }
            // Opcional: Mostrar explicación si ya se denegó antes
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                Toast.makeText(this, "Se necesita permiso de ubicación para etiquetar tu post", Toast.LENGTH_LONG).show()
                requestLocationPermissionLauncher.launch(arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION
                ))
            }
            else -> { // Pedir permiso
                requestLocationPermissionLauncher.launch(arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION
                ))
            }
        }
    }

    @SuppressLint("MissingPermission") // La verificación se hace en solicitarPermisoUbicacion
    private fun obtenerUbicacionActual() {
        // Doble verificación por seguridad
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
            binding.etPostLocation.setText("$latitude, $longitude") // Mostrar coordenadas como fallback
            return
        }
        val geocoder = Geocoder(this, Locale.getDefault())
        try {
            // Usar getFromLocation (puede necesitar API 33+ para la versión asíncrona)
            val addresses = geocoder.getFromLocation(latitude, longitude, 1) // Obtener 1 resultado
            if (addresses != null && addresses.isNotEmpty()) {
                val address = addresses[0]
                // Construir nombre: Ciudad, Estado (o solo Ciudad si no hay Estado)
                val locationName = listOfNotNull(address.locality, address.adminArea).joinToString(", ")
                if (locationName.isNotBlank()) {
                    binding.etPostLocation.setText(locationName)
                    Log.d("CrearPostActivity", "Nombre de ubicación encontrado: $locationName")
                } else {
                    binding.etPostLocation.setText("Ubicación cercana")
                    Log.d("CrearPostActivity", "Geocoder devolvió dirección sin ciudad/estado.")
                }
            } else {
                binding.etPostLocation.setText("Ubicación desconocida")
                Log.d("CrearPostActivity", "Geocoder no encontró dirección para coords.")
            }
        } catch (e: Exception) { // IOException, IllegalArgumentException
            Log.e("CrearPostActivity", "Error de Geocoding: ${e.message}")
            binding.etPostLocation.setText("$latitude, $longitude") // Mostrar coords si falla geocoding
        }
    }

    // --- FUNCIÓN FINAL PARA PUBLICAR ---

    private fun publicarPost() {
        val postText = binding.etPostText.text.toString().trim()
        val postLocation = binding.etPostLocation.text.toString().trim()

        if (postText.isEmpty()) {
            Toast.makeText(this, "Escribe algo para publicar...", Toast.LENGTH_SHORT).show()
            return // No publicar si no hay texto
        }

        // Crear Intent para devolver los datos
        val resultIntent = Intent()
        resultIntent.putExtra("POST_TEXT", postText)
        resultIntent.putExtra("POST_IMAGE_URI", imagenUri?.toString()) // Convertir Uri a String (puede ser null)
        resultIntent.putExtra("POST_LOCATION", postLocation)

        // Establecer resultado y cerrar
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }
}

