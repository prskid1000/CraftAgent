package me.prskid1000.craftagent.action;

import me.prskid1000.craftagent.common.NPCService;
import me.prskid1000.craftagent.config.BaseConfig;
import me.prskid1000.craftagent.database.repositories.MessageRepository;
import me.prskid1000.craftagent.model.NPC;
import me.prskid1000.craftagent.model.database.Message;
import me.prskid1000.craftagent.util.LogUtil;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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
public class CommunicationActionHandler implements ActionSyntaxProvider {
    
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
    
    public boolean handleAction(String originalAction, String[] parsed) {
        if (parsed == null || parsed.length < 4) {
            LogUtil.error("CommunicationActionHandler: Invalid action format (need 4 parts): " + originalAction);
            return false;
        }
        
        String actionType = parsed[0].toLowerCase();
        String operation = parsed[1].toLowerCase();
        String recipientName = parsed[2].trim();
        String messageContent = parsed[3];
        
        // Message content MUST be in single or double quotes
        // Check if message (4th argument, index 3) was quoted in the original string
        String trimmed = originalAction.trim();
        if (!ActionParser.wasArgumentQuoted(trimmed, parsed, 3)) {
            LogUtil.error("CommunicationActionHandler: Message must be wrapped in single quotes (') or double quotes (\"). Action: " + originalAction);
            return false;
        }
        
        if (messageContent.isEmpty()) {
            LogUtil.error("CommunicationActionHandler: Message cannot be empty. Action: " + originalAction);
            return false;
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
            LogUtil.error("CommunicationActionHandler: MessageRepository is null for NPC: " + npcName + " (" + npcUuid + ")");
            return false;
        }
        
        if (npcService == null) {
            LogUtil.error("CommunicationActionHandler: NPCService is null for NPC: " + npcName + " (" + npcUuid + ")");
            return false;
        }
        
        if (baseConfig == null) {
            LogUtil.error("CommunicationActionHandler: BaseConfig is null for NPC: " + npcName + " (" + npcUuid + ")");
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
            long timestamp = System.currentTimeMillis();
            String trimmedContent = content.trim();
            
            LogUtil.info("CommunicationActionHandler: Sending mail - from: " + npcName + " (" + npcUuid + "), to: " + recipientName + " (" + recipientNpc.getConfig().getUuid() + "), content length: " + trimmedContent.length());
            
            // Create and send message
            Message message = new Message(
                0,
                recipientNpc.getConfig().getUuid(),
                npcUuid,
                npcName,
                "NPC",
                trimmedContent,
                timestamp,
                false
            );
            
            messageRepository.insert(message, baseConfig.getMaxMessages());
            
            // Verify the message was actually added by checking recent messages
            var recentMessages = messageRepository.selectByRecipient(recipientNpc.getConfig().getUuid(), 10, false);
            boolean verified = recentMessages.stream()
                .anyMatch(msg -> msg.getSenderUuid().equals(npcUuid) 
                    && msg.getContent().equals(trimmedContent)
                    && Math.abs(msg.getTimestamp() - timestamp) < 1000); // Within 1 second
            
            if (verified) {
                LogUtil.info("CommunicationActionHandler: Successfully sent mail from " + npcName + " to " + recipientName + " (verified in database)");
                return true;
            } else {
                LogUtil.error("CommunicationActionHandler: Failed to verify mail after insert - from: " + npcName + ", to: " + recipientName);
                return false;
            }
        } catch (Exception e) {
            LogUtil.error("CommunicationActionHandler: Error sending mail to " + recipientName + " from " + npcName, e);
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean isValidAction(String action, String[] parsed) {
        if (parsed == null || parsed.length < 4) return false;
        
        String actionType = parsed[0].toLowerCase();
        String op = parsed[1].toLowerCase();
        
        if (!"mail".equals(actionType) || !"send".equals(op)) {
            return false;
        }
        
        // Message content MUST be wrapped in single or double quotes
        String trimmed = action.trim();
        if (!ActionParser.wasArgumentQuoted(trimmed, parsed, 3)) return false;
        
        // Message should not be empty
        String messageContent = parsed[3];
        return !messageContent.trim().isEmpty();
    }
    
    @Override
    public List<String> getActionSyntax() {
        return getStaticActionSyntax();
    }
    
    /**
     * Static method to get action syntax without creating an instance.
     * Used for generating instructions at runtime.
     */
    public static List<String> getStaticActionSyntax() {
        return Arrays.asList(
            "mail send <npc_name> '<message>'"
        );
    }
}

