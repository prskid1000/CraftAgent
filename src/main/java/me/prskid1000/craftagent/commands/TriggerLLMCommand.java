package me.prskid1000.craftagent.commands;

import com.mojang.brigadier.context.CommandContext;
import lombok.AllArgsConstructor;
import me.prskid1000.craftagent.common.NPCService;
import me.prskid1000.craftagent.config.ConfigProvider;
import me.prskid1000.craftagent.model.NPC;
import me.prskid1000.craftagent.util.LogUtil;
import net.minecraft.server.command.ServerCommandSource;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

@AllArgsConstructor
public class TriggerLLMCommand {

    private final NPCService npcService;
    private final ConfigProvider configProvider;

    public int triggerLLM(CommandContext<ServerCommandSource> context, String npcName) {
        try {
            if (npcName == null || npcName.trim().isEmpty()) {
                context.getSource().sendFeedback(() ->
                        LogUtil.formatError("NPC name is required!"), false);
                return 0;
            }

            // Find the target NPC by name (case-insensitive)
            NPC targetNpc = null;
            Collection<NPC> npcs = npcService.getAllNPCs();
            for (NPC npc : npcs) {
                if (npc.getConfig().getNpcName().equalsIgnoreCase(npcName.trim())) {
                    targetNpc = npc;
                    break;
                }
            }
            
            if (targetNpc == null) {
                context.getSource().sendFeedback(() ->
                        LogUtil.formatError("NPC with name '" + npcName + "' not found!"), false);
                return 0;
            }

            String npcDisplayName = targetNpc.getConfig().getNpcName();
            
            // Check if LLM requests are skipped for this NPC
            if (targetNpc.getConfig().isSkipLLMRequests()) {
                context.getSource().sendFeedback(() ->
                        LogUtil.formatError("LLM requests are disabled for NPC '" + npcDisplayName + "'!"), false);
                return 0;
            }

            // Get server for executing feedback on server thread
            var server = context.getSource().getServer();
            if (server == null) {
                context.getSource().sendFeedback(() ->
                        LogUtil.formatError("Server is not available!"), false);
                return 0;
            }

            // Trigger LLM call asynchronously to avoid blocking the server thread
            CompletableFuture.runAsync(() -> {
                try {
                    boolean success = targetNpc.getEventHandler().processLLM();
                    // Send feedback on server thread
                    server.execute(() -> {
                        if (success) {
                            context.getSource().sendFeedback(() ->
                                    LogUtil.formatInfo("Successfully triggered LLM call for NPC '" + npcDisplayName + "'"), false);
                        } else {
                            context.getSource().sendFeedback(() ->
                                    LogUtil.formatError("LLM call failed for NPC '" + npcDisplayName + "'"), false);
                        }
                    });
                } catch (Exception e) {
                    LogUtil.error("Error triggering LLM call for NPC: " + npcDisplayName, e);
                    // Send feedback on server thread
                    server.execute(() -> {
                        context.getSource().sendFeedback(() ->
                                LogUtil.formatError("Error triggering LLM call: " + e.getMessage()), false);
                    });
                }
            });

            // Return immediately with a message that the LLM call is being processed
            context.getSource().sendFeedback(() ->
                    LogUtil.formatInfo("Triggering LLM call for NPC '" + npcDisplayName + "'..."), false);
            
            return 1;
        } catch (Exception e) {
            LogUtil.error("Error triggering LLM call", e);
            context.getSource().sendFeedback(() ->
                    LogUtil.formatError("Failed to trigger LLM call: " + e.getMessage()), false);
            return 0;
        }
    }
}

