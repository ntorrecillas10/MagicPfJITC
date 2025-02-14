package com.example.magicpfjitc

import java.util.Date

class Evento(
    var id: String = "",
    var nombre: String = "",
    var descripcion: String = "",
    var fecha: String = "",
    var aforo: Int = 0,
    var precio: Double = 0.0,
    var imagen: String = "",
    var participantes: MutableList<Usuario> = mutableListOf(),
    var fecha_creacion: String = dateFormat.format(Date())
)