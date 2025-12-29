package me.prskid1000.craftagent.callback;

import me.prskid1000.craftagent.model.stt.STTType;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public interface STTCallback {

    Event<STTCallback> EVENT = EventFactory.createArrayBacked(
            STTCallback.class, listeners -> (type) -> {
                for (STTCallback listener : listeners) {
                    listener.onSTTAction(type);
                }
            });

    void onSTTAction(STTType type);
}
