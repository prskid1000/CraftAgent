package me.prskid1000.craftagent.mixin;

import net.minecraft.entity.data.TrackedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.entity.player.PlayerEntity;

@Mixin(PlayerEntity.class)
public interface PlayerEntityAccessor {

    @Accessor("PLAYER_MODEL_PARTS")
    static TrackedData<Byte> getPlayerModelParts() {
        throw new AssertionError();
    }

}