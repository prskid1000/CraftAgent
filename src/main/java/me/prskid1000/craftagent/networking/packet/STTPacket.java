package me.prskid1000.craftagent.networking.packet;

import io.wispforest.endec.Endec;
import io.wispforest.endec.StructEndec;
import io.wispforest.endec.impl.StructEndecBuilder;
import me.prskid1000.craftagent.model.stt.STTType;

public record STTPacket(STTType type) {

    public static final StructEndec<STTPacket> ENDEC = StructEndecBuilder.of(
            Endec.forEnum(STTType.class).fieldOf("type", STTPacket::type),
            STTPacket::new
    );

    @Override
    public String toString() {
        return "STTPacket={type=" + type + "}";
    }
}
