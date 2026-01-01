package me.prskid1000.craftagent.event

interface EventHandler {

    /**
     * @deprecated Use updateState() instead. This method now just calls updateState() for backward compatibility.
     */
    @Deprecated("Use updateState() instead", ReplaceWith("updateState(prompt)"))
    fun onEvent(prompt: String)

    /**
     * Updates state only (store messages, display chat, etc.) without triggering LLM.
     * This is called when events/messages occur.
     */
    fun updateState(prompt: String)

    /**
     * Processes LLM call. Called by scheduler periodically.
     * @return true if LLM call succeeded, false otherwise
     */
    fun processLLM(): Boolean

    fun stopService()
    fun queueIsEmpty(): Boolean
}