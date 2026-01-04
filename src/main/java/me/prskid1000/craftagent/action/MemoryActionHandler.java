package me.prskid1000.craftagent.action;

import me.prskid1000.craftagent.config.BaseConfig;
import me.prskid1000.craftagent.database.repositories.SharebookRepository;
import me.prskid1000.craftagent.memory.MemoryManager;
import me.prskid1000.craftagent.model.database.SharebookPage;
import me.prskid1000.craftagent.util.LogUtil;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Handles memory actions: SharedBook and PrivateBook.
 * Format: "sharedbook add <title> '<content>'" or "sharedbook remove <title>"
 * 
 * IMPORTANT: Content MUST be wrapped in single quotes (') or double quotes (") for "add" operations.
 * This allows multi-word content and special characters to be parsed correctly.
 * Examples: 
 *   "sharedbook add location_oak_forest 'Oak forest at coordinates x=23, y=64, z=4.'"
 *   "sharedbook add location_oak_forest \"Oak forest at coordinates x=23, y=64, z=4.\""
 */
public class MemoryActionHandler implements ActionSyntaxProvider {
    
    private final MemoryManager memoryManager;
    private final SharebookRepository sharebookRepository;
    private final UUID npcUuid;
    private final String npcName;
    private final BaseConfig baseConfig;
    
    public MemoryActionHandler(MemoryManager memoryManager, SharebookRepository sharebookRepository,
                               UUID npcUuid, String npcName, BaseConfig baseConfig) {
        this.memoryManager = memoryManager;
        this.sharebookRepository = sharebookRepository;
        this.npcUuid = npcUuid;
        this.npcName = npcName;
        this.baseConfig = baseConfig;
    }
    
    public boolean handleAction(String originalAction, String[] parsed) {
        if (parsed == null || parsed.length < 3) {
            LogUtil.error("MemoryActionHandler: Invalid action format (need at least 3 parts): " + originalAction);
            return false;
        }
        
        String bookType = parsed[0].toLowerCase();
        String operation = parsed[1].toLowerCase();
        String pageTitle = parsed[2].trim();
        String content = parsed.length > 3 ? parsed[3] : "";
        
        // For "add" operations, content MUST be in single or double quotes
        if ("add".equals(operation)) {
            if (content.isEmpty()) {
                LogUtil.error("MemoryActionHandler: Content is required for 'add' operation. Action: " + originalAction);
                return false;
            }
            
            // Check if content (4th argument, index 3) was quoted in the original string
            String trimmed = originalAction.trim();
            if (!ActionParser.wasArgumentQuoted(trimmed, parsed, 3)) {
                LogUtil.error("MemoryActionHandler: Content must be wrapped in single quotes (') or double quotes (\") for 'add' operation. Action: " + originalAction);
                return false;
            }
        }
        
        // Replace newlines and normalize whitespace
        content = content.replaceAll("\\r\\n|\\r|\\n", " ").replaceAll("\\s+", " ").trim();
        
        LogUtil.info("MemoryActionHandler: Processing action - type: " + bookType + ", op: " + operation + ", title: " + pageTitle + ", content length: " + content.length());
        
        return switch (bookType) {
            case "sharedbook" -> handleSharedBook(operation, pageTitle, content);
            case "privatebook" -> handlePrivateBook(operation, pageTitle, content);
            default -> {
                LogUtil.error("MemoryActionHandler: Unknown book type: " + bookType);
                yield false;
            }
        };
    }
    
    
    
    private boolean handleSharedBook(String op, String title, String content) {
        if (sharebookRepository == null) {
            LogUtil.error("MemoryActionHandler: SharebookRepository is null");
            return false;
        }
        
        return switch (op) {
            case "add" -> {
                if (content.isEmpty()) {
                    LogUtil.error("MemoryActionHandler: Cannot add sharedbook page with empty content. Title: " + title);
                    yield false;
                }
                try {
                    SharebookPage page = new SharebookPage(title, content.trim(), 
                        npcUuid.toString(), System.currentTimeMillis());
                    sharebookRepository.insertOrUpdate(page, baseConfig.getMaxSharebookPages());
                    LogUtil.info("MemoryActionHandler: Successfully added sharedbook page: " + title);
                    yield true;
                } catch (Exception e) {
                    LogUtil.error("MemoryActionHandler: Error adding sharedbook page: " + title, e);
                    yield false;
                }
            }
            case "remove" -> {
                try {
                    sharebookRepository.delete(title, npcUuid.toString());
                    LogUtil.info("MemoryActionHandler: Successfully removed sharedbook page: " + title);
                    yield true;
                } catch (Exception e) {
                    LogUtil.error("MemoryActionHandler: Error removing sharedbook page: " + title, e);
                    yield false;
                }
            }
            default -> {
                LogUtil.error("MemoryActionHandler: Unknown sharedbook operation: " + op);
                yield false;
            }
        };
    }
    
    private boolean handlePrivateBook(String op, String title, String content) {
        if (memoryManager == null) {
            LogUtil.error("MemoryActionHandler: MemoryManager is null");
            return false;
        }
        
        return switch (op) {
            case "add" -> {
                if (content.isEmpty()) {
                    LogUtil.error("MemoryActionHandler: Cannot add privatebook page with empty content. Title: " + title);
                    yield false;
                }
                try {
                    memoryManager.savePage(title, content.trim());
                    LogUtil.info("MemoryActionHandler: Successfully added privatebook page: " + title);
                    yield true;
                } catch (Exception e) {
                    LogUtil.error("MemoryActionHandler: Error adding privatebook page: " + title, e);
                    yield false;
                }
            }
            case "remove" -> {
                try {
                    memoryManager.deletePage(title);
                    LogUtil.info("MemoryActionHandler: Successfully removed privatebook page: " + title);
                    yield true;
                } catch (Exception e) {
                    LogUtil.error("MemoryActionHandler: Error removing privatebook page: " + title, e);
                    yield false;
                }
            }
            default -> {
                LogUtil.error("MemoryActionHandler: Unknown privatebook operation: " + op);
                yield false;
            }
        };
    }
    
    public boolean isValidAction(String action, String[] parsed) {
        if (parsed == null || parsed.length < 3) return false;
        
        String bookType = parsed[0].toLowerCase();
        String op = parsed[1].toLowerCase();
        
        boolean validBookType = switch (bookType) {
            case "sharedbook", "privatebook" -> true;
            default -> false;
        };
        if (!validBookType) return false;
        
        boolean validOp = switch (op) {
            case "add", "remove" -> true;
            default -> false;
        };
        if (!validOp) return false;
        
        // For "add" operation, need at least 4 parts (bookType, op, title, content)
        // Content MUST be wrapped in single or double quotes
        if ("add".equals(op)) {
            if (parsed.length < 4) return false;
            
            // Check if content was quoted in original string
            String trimmed = action.trim();
            if (!ActionParser.wasArgumentQuoted(trimmed, parsed, 3)) return false;
            
            // Content should not be empty
            String content = parsed[3];
            return !content.trim().isEmpty();
        }
        
        return true; // "remove" operation only needs 3 parts
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
            "sharedbook add <title> '<content>'",
            "sharedbook remove <title>",
            "privatebook add <title> '<content>'",
            "privatebook remove <title>"
        );
    }
}

