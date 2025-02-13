package com.example.magicpfjitc

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.magicpfjitc.databinding.ActivityCrearCartaBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import io.appwrite.Client
import io.appwrite.ID
import io.appwrite.models.InputFile
import io.appwrite.services.Storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.checkerframework.checker.units.qual.C


private lateinit var refBD: DatabaseReference//referencia a la base de datos Firebase
private lateinit var identificador: String//identificador único para el objeto carta
private lateinit var carta_nueva: Carta//objeto carta para crear

//appwrite
private lateinit var appWriteClient: Client//cliente de appwrite
private lateinit var storage: Storage//servicio de almacenamiento de appwrite
private lateinit var miBucketId: String//id del bucket de appwrite
private lateinit var miProyectoId: String//id del proyecto de appwrite
private lateinit var identificadorAppWrite: String//identificador único para el objeto carta en appwrite



class CrearCarta : AppCompatActivity() {

    private var contexto = this
    private lateinit var binding: ActivityCrearCartaBinding
    private lateinit var url: Uri // URL de la imagen seleccionada


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCrearCartaBinding.inflate(layoutInflater)
        setContentView(binding.root)


        miProyectoId = "67a65b4b001c887a9b81" // ID del proyecto de Appwrite
        miBucketId = "67a783770038c6aa0389" // ID del bucket de Appwrite
        identificadorAppWrite = ""

        refBD = FirebaseDatabase.getInstance().reference

        //se inicializan las cosas de appwrite
        appWriteClient = Client(this)
            .setEndpoint("https://cloud.appwrite.io/v1")//necesario para conectarse a la api de appwrite
            .setProject(miProyectoId)
        storage = Storage(appWriteClient)


        // Listener para volver a la pantalla anterior
        binding.volver.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }


        // Listener para seleccionar una imagen para el avatar
        binding.addImagenCarta.setOnClickListener {
            urlGaleria.launch("image/*") // Abre la galería para seleccionar una imagen
        }

        //Iniciamos el spinner
        val spinner = binding.tipoCarta
        val items = arrayOf("Blanco","Rojo","Azul","Negro","Verde")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item,items)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            @SuppressLint("UseCompatLoadingForDrawables")
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedItem = parent?.getItemAtPosition(position).toString()
                //Hacemos un switch para cambiar el color del fondo de la carta segun lo seleccionado en el Spinner
                when (selectedItem) {
                    "Blanco" -> binding.addImagenCarta.background = getDrawable(R.drawable.fondo_transparente_bordes_blancos)
                    "Rojo" -> binding.addImagenCarta.background = getDrawable(R.drawable.fondo_transparente_bordes_rojos)
                    "Azul" -> binding.addImagenCarta.background = getDrawable(R.drawable.fondo_transparente_bordes_azul)
                    "Negro" -> binding.addImagenCarta.background = getDrawable(R.drawable.fondo_transparente_bordes_negro)
                    "Verde" -> binding.addImagenCarta.background = getDrawable(R.drawable.fondo_transparente_bordes_verdes)
                }
                carta_nueva.tipo = selectedItem
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                TODO("Not yet implemented")
            }


        }

        carta_nueva = Carta()

        var campos = false
        var imagen = false

        binding.addCarta.setOnClickListener {

            var lista_cartas = obtenerListaCartas(refBD, this)

            //comprobamos que el nombre no este ya en la base de datos
            if (!existeCarta(lista_cartas, binding.nombreCarta.text.toString())
            ) {

                if (binding.addImagenCarta.drawable != null) {
                    imagen = true
                }
                if (binding.nombreCarta.text.toString() != "" && binding.descripcionCarta.text.toString() != "" && binding.precioCarta.text.toString().toDouble() != 0.0
                ) {
                    campos = true
                }
                if (campos && imagen) {

                    //key única para el
                    identificador = refBD.child("cartas").push().key!!

                    Log.d("ID", identificador)
                    Log.d("Date", carta_nueva.fecha_carta)

                    //subimos la imagen a appwrite storage y los datos a firebase
                    identificadorAppWrite =
                        ID.unique() // coge el identificador y lo adapta a appwrite

                    //necesario para crear un archivo temporal con la imagen
                    val inputStream = this.contentResolver.openInputStream(url)

                    GlobalScope.launch(Dispatchers.IO) {//scope para las funciones de appwrite, pero ya aprovechamos y metemos el código de firebase
                        try {

                            val file = inputStream.use { input ->
                                val tempFile =
                                    kotlin.io.path.createTempFile(identificadorAppWrite).toFile()
                                if (input != null) {
                                    tempFile.outputStream().use { output ->
                                        input.copyTo(output)
                                    }
                                }
                                InputFile.fromFile(tempFile) // tenemos un archivo temporal con la imagen
                            }


                            //se sube la imagen a appwrite
                            storage.createFile(
                                bucketId = miBucketId,//id del bucket, este es el mío
                                fileId = identificadorAppWrite,
                                file = file
                            )

                            val url_avatar =
                                "https://cloud.appwrite.io/v1/storage/buckets/$miBucketId/files/$identificadorAppWrite/preview?project=$miProyectoId"

                            carta_nueva = Carta(
                                identificador,
                                binding.nombreCarta.text.toString(),
                                url_avatar,
                                binding.precioCarta.text.toString().toDouble(),
                                binding.descripcionCarta.text.toString(),
                                carta_nueva.tipo
                            )

                            Log.d("carta", carta_nueva.toString())

                            //subimos los datos a firebase
                            refBD.child("cartas").child(identificador).setValue(carta_nueva).addOnSuccessListener {
                                //volvemos a la pantalla de inicio
                                val intent = Intent(contexto, MainActivity::class.java)
                                startActivity(intent)
                            }


                        } catch (e: Exception) {
                            Log.e("UploadError", "Error al subir la imagen: ${e.message}")
                        }
                    }


                    Toast.makeText(
                        this,
                        "Carta ${carta_nueva.nombre} creada con éxito",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this,
                        "Rellena todos los campos y elige una imagen",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                campos = false
                imagen = false
                //Ponemos el focus en el campo de nombre
                binding.nombreCarta.requestFocus()

            } else {
                Toast.makeText(this, "El nombre del carta ya existe", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }


        }
    }

    private val urlGaleria =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            when (uri) {
                null -> Toast.makeText(
                    this,
                    "No has seleccionado ninguna imagen",
                    Toast.LENGTH_SHORT
                ).show()

                else -> {
                    Toast.makeText(this, "Imagen seleccionada", Toast.LENGTH_SHORT).show()
                    url = uri
                    binding.addImagenCarta.setImageURI(url) // Mostrar la imagen seleccionada en el ImageView
                }
            }
        }

    fun obtenerListaCartas(db_ref: DatabaseReference, contexto: Context): MutableList<Carta> {
        val lista_cartas = mutableListOf<Carta>()

        db_ref.child("cartas")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.children.forEach { carta ->
                        val carta_nueva = carta.getValue(Carta::class.java)
                        lista_cartas.add(carta_nueva!!)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(contexto, "Error al obtener las cartas", Toast.LENGTH_SHORT)
                        .show()
                }
            })
        return lista_cartas
    }

    fun existeCarta(cartas: List<Carta>, nombre: String): Boolean {
        return cartas.any { it.nombre.lowercase() == nombre.lowercase() }
    }
}