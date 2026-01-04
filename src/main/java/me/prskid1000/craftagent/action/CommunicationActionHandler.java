package me.prskid1000.craftagent.action;

import me.prskid1000.craftagent.common.NPCService;
import me.prskid1000.craftagent.config.BaseConfig;
import me.prskid1000.craftagent.database.repositories.MessageRepository;
import me.prskid1000.craftagent.model.NPC;
import me.prskid1000.craftagent.model.database.Message;
import me.prskid1000.craftagent.util.LogUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles communication actions: sending mail.
 * Format: "mail send <recipient_name> '<message>'"
 * 
 * IMPORTANT: Message MUST be wrapped in single quotes (') or double quotes (") to handle multi-word messages and special characters.
 * Examples: 
 *   "mail send Alice 'Found iron mine, want to mine together?'"
 *   "mail send Alice \"Found iron mine, want to mine together?\""
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
        if (action == null || action.trim().isEmpty()) {
            LogUtil.error("CommunicationActionHandler: Action is null or empty");
            return false;
        }
        
        String trimmed = action.trim();
        String[] parts = trimmed.split("\\s+", 4);
        if (parts.length < 4) {
            LogUtil.error("CommunicationActionHandler: Invalid action format (need 4 parts): " + action);
            return false;
        }
        
        String actionType = parts[0].toLowerCase();
        String operation = parts[1].toLowerCase();
        String recipientName = parts[2];
        String messageContent = parts[3];
        
        // Message content MUST be in single or double quotes
        if (!messageContent.isEmpty()) {
            boolean singleQuoted = messageContent.startsWith("'") && messageContent.endsWith("'");
            boolean doubleQuoted = messageContent.startsWith("\"") && messageContent.endsWith("\"");
            
            if (!singleQuoted && !doubleQuoted) {
                LogUtil.error("CommunicationActionHandler: Message must be wrapped in single quotes (') or double quotes (\"). Action: " + action);
                return false;
            }
            
            // Strip surrounding quotes (single or double)
            messageContent = messageContent.substring(1, messageContent.length() - 1);
        }
        
        // Replace newlines and normalize whitespace
        messageContent = messageContent.replaceAll("\\r\\n|\\r|\\n", " ").replaceAll("\\s+", " ").trim();
        
        LogUtil.info("CommunicationActionHandler: Processing action - type: " + actionType + ", op: " + operation + ", recipient: " + recipientName + ", message length: " + messageContent.length());
        
        return switch (actionType) {
            case "mail" -> switch (operation) {
                case "send" -> sendMessage(recipientName, messageContent);
                default -> {
                    LogUtil.error("CommunicationActionHandler: Unknown mail operation: " + operation);
                    yield false;
                }
            };
            default -> {
                LogUtil.error("CommunicationActionHandler: Unknown action type: " + actionType);
                yield false;
            }
        };
    }
    
    private boolean sendMessage(String recipientName, String content) {
        if (messageRepository == null) {
            LogUtil.error("CommunicationActionHandler: MessageRepository is null");
            return false;
        }
        
        if (content.isEmpty()) {
            LogUtil.error("CommunicationActionHandler: Cannot send mail with empty message. Recipient: " + recipientName);
            return false;
        }
        
        // Find recipient NPC by name
        NPC recipientNpc = null;
        for (NPC npc : npcService.getAllNPCs()) {
            if (npc.getConfig().getNpcName().equalsIgnoreCase(recipientName)) {
                recipientNpc = npc;
                break;
            }
        }
        
        if (recipientNpc == null) {
            LogUtil.info("CommunicationActionHandler: Recipient NPC not found: " + recipientName);
            return false;
        }
        
        try {
            // Create and send message
            Message message = new Message(
                0,
                recipientNpc.getConfig().getUuid(),
                npcUuid,
                npcName,
                "NPC",
                content.trim(),
                System.currentTimeMillis(),
                false
            );
            
            messageRepository.insert(message, baseConfig.getMaxMessages());
            LogUtil.info("CommunicationActionHandler: Successfully sent mail from " + npcName + " to " + recipientName + ": " + content);
            
            return true;
        } catch (Exception e) {
            LogUtil.error("CommunicationActionHandler: Error sending mail to " + recipientName, e);
            return false;
        }
    }
    
    public boolean isValidAction(String action) {
        if (action == null || action.trim().isEmpty()) return false;
        
        // Normalize newlines and extra whitespace
        String normalized = action.replaceAll("\\r\\n|\\r|\\n", " ").replaceAll("\\s+", " ").trim();
        String[] parts = normalized.split("\\s+", 4);
        if (parts.length < 4) return false;
        
        String actionType = parts[0].toLowerCase();
        String op = parts[1].toLowerCase();
        
        if (!"mail".equals(actionType) || !"send".equals(op)) {
            return false;
        }
        
        // Message content MUST be wrapped in single or double quotes
        String messageContent = parts[3];
        boolean singleQuoted = messageContent.startsWith("'") && messageContent.endsWith("'");
        boolean doubleQuoted = messageContent.startsWith("\"") && messageContent.endsWith("\"");
        if (!singleQuoted && !doubleQuoted) return false;
        
        // Strip quotes to check if message is not empty
        messageContent = messageContent.substring(1, messageContent.length() - 1);
        
        // Message should not be empty after stripping quotes
        return !messageContent.trim().isEmpty();
    }
}

