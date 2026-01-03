package me.prskid1000.craftagent.listener

import me.prskid1000.craftagent.model.NPC
import net.minecraft.entity.player.PlayerEntity
import java.util.UUID

/**
 * Base class for event listeners with common utility methods
 */
abstract class BaseEventListener : EventListener {

    abstract override fun register()

    protected fun getMatchingNpc(npcs: Map<UUID, NPC>, player: PlayerEntity): NPC? {
        return npcs.values.firstOrNull { it.entity.uuid == player.uuid }
    }

}
