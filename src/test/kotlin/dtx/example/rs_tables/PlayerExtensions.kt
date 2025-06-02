package dtx.example.rs_tables

import dtx.example.Player

fun Player.sendMessage(message: String) {
    println("[to: $username]: $message")
}