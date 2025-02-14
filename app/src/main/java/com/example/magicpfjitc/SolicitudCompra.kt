package com.example.magicpfjitc

data class SolicitudCompra(
    val carta_id: String,
    val comprador_id: String,
    val precio: Double,
    val estado: String,
    val fecha: String
)