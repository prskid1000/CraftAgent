package me.prskid1000.craftagent.listener

import me.prskid1000.craftagent.model.NPC
import net.minecraft.entity.player.PlayerEntity
import java.util.UUID

abstract class AEventListener(
    protected val npcs: Map<UUID, NPC>
) : IEventListener {

    abstract override fun register()

    protected fun getMatchingNpc(player: PlayerEntity): NPC? {
        return npcs.values.firstOrNull { it.entity.uuid == player.uuid }
    }

}
