package me.prskid1000.craftagent.util;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * Utility class for sending chat messages without AltoClef.
 * Uses vanilla Minecraft functionality.
 */
public class ChatUtil {

    private ChatUtil() {
        // Utility class - no instantiation
    }

    /**
     * Sends a chat message from an NPC to the chat.
     * Broadcasts the message to all players in the same world.
     * 
     * @param player The player entity (NPC) sending the message
     * @param message The message to send
     */
    public static void sendChatMessage(ServerPlayerEntity player, String message) {
        if (player == null || message == null || message.trim().isEmpty()) {
            return;
        }
        
        try {
            // Send message to all players in the same world
            Text messageText = Text.literal(message);
            player.getWorld().getPlayers().forEach(p -> {
                p.sendMessage(messageText, false);
            });
        } catch (Exception e) {
            LogUtil.error("Error sending chat message: " + message, e);
        }
    }

    /**
     * Sends a chat message only to nearby players (within 64 blocks).
     * 
     * @param player The player entity (NPC) sending the message
     * @param message The message to send
     * @param maxDistance Maximum distance in blocks (default: 64)
     */
    public static void sendChatMessageToNearby(ServerPlayerEntity player, String message, double maxDistance) {
        if (player == null || message == null || message.trim().isEmpty()) {
            return;
        }
        
        try {
            Text messageText = Text.literal(message);
            double maxDistanceSq = maxDistance * maxDistance;
            
            player.getWorld().getPlayers().forEach(p -> {
                if (p.squaredDistanceTo(player) <= maxDistanceSq) {
                    p.sendMessage(messageText, false);
                }
            });
        } catch (Exception e) {
            LogUtil.error("Error sending chat message to nearby: " + message, e);
        }
    }
}

