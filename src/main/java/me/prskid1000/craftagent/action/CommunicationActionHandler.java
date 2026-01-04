package me.prskid1000.craftagent.action;

import me.prskid1000.craftagent.common.NPCService;
import me.prskid1000.craftagent.config.BaseConfig;
import me.prskid1000.craftagent.database.repositories.MessageRepository;
import me.prskid1000.craftagent.model.NPC;
import me.prskid1000.craftagent.model.database.Message;
import me.prskid1000.craftagent.util.LogUtil;

import java.util.UUID;

/**
 * Handles communication actions: sending mail.
 * Format: "mail send <recipient_name> <message>"
 */
public class CommunicationActionHandler {
    
    private final MessageRepository messageRepository;
    private final NPCService npcService;
    private final UUID npcUuid;
    private final String npcName;
    private final BaseConfig baseConfig;
    
    public CommunicationActionHandler(MessageRepository messageRepository, NPCService npcService,
                                     UUID npcUuid, String npcName, BaseConfig baseConfig) {
        this.messageRepository = messageRepository;
        this.npcService = npcService;
        this.npcUuid = npcUuid;
        this.npcName = npcName;
        this.baseConfig = baseConfig;
    }
    
    public boolean handleAction(String action) {
        if (action == null || action.trim().isEmpty()) return false;
        
        String[] parts = action.trim().split("\\s+", 4);
        if (parts.length < 4) return false;
        
        String actionType = parts[0].toLowerCase();
        String operation = parts[1].toLowerCase();
        String recipientName = parts[2];
        String messageContent = parts[3];
        
        return switch (actionType) {
            case "mail" -> switch (operation) {
                case "send" -> sendMessage(recipientName, messageContent);
                default -> false;
            };
            default -> false;
        };
    }
    
    private boolean sendMessage(String recipientName, String content) {
        if (messageRepository == null || content.isEmpty()) return false;
        
        // Find recipient NPC by name
        NPC recipientNpc = null;
        for (NPC npc : npcService.getAllNPCs()) {
            if (npc.config.npcName.equalsIgnoreCase(recipientName)) {
                recipientNpc = npc;
                break;
            }
        }
        
        if (recipientNpc == null) {
            LogUtil.debug("Recipient NPC not found: " + recipientName);
            return false;
        }
        
        // Create and send message
        Message message = new Message(
            0,
            recipientNpc.config.uuid,
            npcUuid,
            npcName,
            content.trim(),
            System.currentTimeMillis(),
            false
        );
        
        messageRepository.insert(message, baseConfig.maxMessages);
        LogUtil.info("NPC " + npcName + " sent message to " + recipientName + ": " + content);
        return true;
    }
    
    public boolean isValidAction(String action) {
        if (action == null || action.trim().isEmpty()) return false;
        String[] parts = action.trim().split("\\s+", 4);
        if (parts.length < 4) return false;
        
        String actionType = parts[0].toLowerCase();
        String op = parts[1].toLowerCase();
        
        return switch (actionType) {
            case "mail" -> "send".equals(op);
            default -> false;
        };
    }
}

