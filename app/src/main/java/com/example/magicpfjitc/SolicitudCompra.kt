package com.example.magicpfjitc

import java.util.Date

data class SolicitudCompra(
    var id: String = "",
    var carta_id: String = "",
    var comprador_id: String = "",
    var precio: Double = 0.0,
    var estado: String = "",
    var fecha: String = dateFormat.format(Date())
)