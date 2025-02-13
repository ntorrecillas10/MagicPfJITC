package com.example.magicpfjitc

class Evento(
    var id: String = "",
    var nombre: String = "",
    var descripcion: String = "",
    var fecha: String = "",
    var aforo: Int = 0,
    var imagen: String = "",
    var participantes: MutableList<String> = mutableListOf()
)