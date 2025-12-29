package me.prskid1000.craftagent.event

interface EventHandler {

    fun onEvent(prompt: String)
    fun stopService()
    fun queueIsEmpty(): Boolean
}