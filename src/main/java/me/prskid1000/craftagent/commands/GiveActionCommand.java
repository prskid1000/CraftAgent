package me.prskid1000.craftagent.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import lombok.AllArgsConstructor;

import me.prskid1000.craftagent.action.ActionExecutor;
import me.prskid1000.craftagent.common.NPCService;
import me.prskid1000.craftagent.config.ConfigProvider;
import me.prskid1000.craftagent.model.NPC;
import me.prskid1000.craftagent.util.LogUtil;
import net.minecraft.server.command.ServerCommandSource;

import java.util.Collection;

@AllArgsConstructor
public class GiveActionCommand {

    private final NPCService npcService;
    private final ConfigProvider configProvider;

    public int executeAction(CommandContext<ServerCommandSource> context, String npcName) {
        try {
            String action = StringArgumentType.getString(context, "action");
            
            if (action == null || action.trim().isEmpty()) {
                context.getSource().sendFeedback(() ->
                        LogUtil.formatError("Action cannot be empty!"), false);
                return 0;
            }

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

            // Get repositories from context provider
            var messageRepository = targetNpc.getContextProvider().getMessageRepository();
            var sharebookRepository = targetNpc.getContextProvider().getSharebookRepository();
            
            if (messageRepository == null || sharebookRepository == null) {
                context.getSource().sendFeedback(() ->
                        LogUtil.formatError("NPC repositories not initialized!"), false);
                return 0;
            }

            // Create action provider with all handlers using factory
            var actionProvider = me.prskid1000.craftagent.action.ActionProviderFactory.create(
                    targetNpc.getEntity(),
                    targetNpc.getContextProvider(),
                    targetNpc.getConfig().getUuid(),
                    targetNpc.getConfig().getNpcName(),
                    targetNpc.getContextProvider().memoryManager,
                    messageRepository,
                    sharebookRepository,
                    npcService,
                    targetNpc.getContextProvider().getBaseConfig()
            );
            var actionExecutor = new ActionExecutor(targetNpc.getEntity(), actionProvider);

            // Execute the action
            actionExecutor.executeAction(action.trim());

            String npcDisplayName = targetNpc.getConfig().getNpcName();
            context.getSource().sendFeedback(() ->
                    LogUtil.formatInfo("Executed action on NPC '" + npcDisplayName + "': " + action), false);
            
            return 1;
        } catch (Exception e) {
            LogUtil.error("Error executing action", e);
            context.getSource().sendFeedback(() ->
                    LogUtil.formatError("Failed to execute action: " + e.getMessage()), false);
            return 0;
        }
    }
}

