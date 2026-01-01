package me.prskid1000.craftagent.listener

import me.prskid1000.craftagent.common.NPCService
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.server.MinecraftServer

/**
 * Listener that updates NPC age based on server ticks.
 * Age increases: 1 year = 24000 ticks (20 minutes real time)
 */
class AgeUpdateListener(
    private val npcService: NPCService
) : AEventListener() {

    companion object {
        private const val TICKS_PER_YEAR = 24000L // 20 minutes = 1 year
    }

    override fun register() {
        ServerTickEvents.END_SERVER_TICK.register { server ->
            updateAges(server)
        }
    }

    private fun updateAges(server: MinecraftServer) {
        val currentTick = server.overworld.time
        npcService.uuidToNpc.forEach { (uuid, npc) ->
            val config = npc.config
            val lastUpdateTick = config.lastAgeUpdateTick
            
            // Initialize last update tick if not set
            if (lastUpdateTick == 0L) {
                config.setLastAgeUpdateTick(currentTick)
                return@forEach
            }
            
            // Calculate ticks passed
            val ticksPassed = currentTick - lastUpdateTick
            
            // Update age if enough ticks have passed
            if (ticksPassed >= TICKS_PER_YEAR) {
                val yearsToAdd = (ticksPassed / TICKS_PER_YEAR).toInt()
                val newAge = config.age + yearsToAdd
                config.setAge(newAge)
                config.setLastAgeUpdateTick(currentTick - (ticksPassed % TICKS_PER_YEAR))
            }
        }
    }
}

