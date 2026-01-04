package me.prskid1000.craftagent.action;

import me.prskid1000.craftagent.config.BaseConfig;
import me.prskid1000.craftagent.database.repositories.SharebookRepository;
import me.prskid1000.craftagent.memory.MemoryManager;
import me.prskid1000.craftagent.model.database.SharebookPage;
import me.prskid1000.craftagent.util.LogUtil;

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
public class MemoryActionHandler {
    
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
    
    public boolean handleAction(String action) {
        if (action == null || action.trim().isEmpty()) {
            LogUtil.error("MemoryActionHandler: Action is null or empty");
            return false;
        }
        
        String trimmed = action.trim();
        
        // Parse with quote awareness
        String[] parsed = parseQuotedArguments(trimmed);
        if (parsed.length < 3) {
            LogUtil.error("MemoryActionHandler: Invalid action format (need at least 3 parts): " + action);
            return false;
        }
        
        String bookType = parsed[0].toLowerCase();
        String operation = parsed[1].toLowerCase();
        String pageTitle = parsed[2];
        String content = parsed.length > 3 ? parsed[3] : "";
        
        // For "add" operations, content MUST be in single or double quotes
        if ("add".equals(operation)) {
            if (content.isEmpty()) {
                LogUtil.error("MemoryActionHandler: Content is required for 'add' operation. Action: " + action);
                return false;
            }
            
            boolean singleQuoted = content.startsWith("'") && content.endsWith("'");
            boolean doubleQuoted = content.startsWith("\"") && content.endsWith("\"");
            
            if (!singleQuoted && !doubleQuoted) {
                LogUtil.error("MemoryActionHandler: Content must be wrapped in single quotes (') or double quotes (\") for 'add' operation. Action: " + action);
                return false;
            }
            
            // Strip surrounding quotes (single or double)
            content = content.substring(1, content.length() - 1);
        }
        
        // Strip quotes from title if present
        if ((pageTitle.startsWith("'") && pageTitle.endsWith("'")) || 
            (pageTitle.startsWith("\"") && pageTitle.endsWith("\""))) {
            pageTitle = pageTitle.substring(1, pageTitle.length() - 1);
        }
        
        // Replace newlines and normalize whitespace
        content = content.replaceAll("\\r\\n|\\r|\\n", " ").replaceAll("\\s+", " ").trim();
        pageTitle = pageTitle.trim();
        
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
    
    /**
     * Parses a string into arguments, respecting quoted strings (both single and double quotes).
     * Handles cases like: sharedbook add "Title" "Content with spaces"
     */
    private String[] parseQuotedArguments(String input) {
        java.util.List<String> args = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        char quoteChar = 0;
        boolean inQuotes = false;
        
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            
            if (!inQuotes && (c == '\'' || c == '"')) {
                // Start of quoted string
                inQuotes = true;
                quoteChar = c;
                current.append(c);
            } else if (inQuotes && c == quoteChar) {
                // End of quoted string
                current.append(c);
                inQuotes = false;
                quoteChar = 0;
            } else if (!inQuotes && Character.isWhitespace(c)) {
                // Whitespace outside quotes - end of argument
                if (current.length() > 0) {
                    args.add(current.toString());
                    current.setLength(0);
                }
            } else {
                // Regular character
                current.append(c);
            }
        }
        
        // Add last argument if any
        if (current.length() > 0) {
            args.add(current.toString());
        }
        
        return args.toArray(new String[0]);
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
    
    public boolean isValidAction(String action) {
        if (action == null || action.trim().isEmpty()) return false;
        
        // Normalize newlines and extra whitespace
        String normalized = action.replaceAll("\\r\\n|\\r|\\n", " ").trim();
        String[] parts = parseQuotedArguments(normalized);
        if (parts.length < 3) return false;
        
        String bookType = parts[0].toLowerCase();
        String op = parts[1].toLowerCase();
        
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
            if (parts.length < 4) return false;
            String content = parts[3];
            
            // Content must be wrapped in single or double quotes
            boolean singleQuoted = content.startsWith("'") && content.endsWith("'");
            boolean doubleQuoted = content.startsWith("\"") && content.endsWith("\"");
            if (!singleQuoted && !doubleQuoted) return false;
            
            // Strip quotes to check if content is not empty
            content = content.substring(1, content.length() - 1);
            
            // Content should not be empty after stripping quotes
            return !content.trim().isEmpty();
        }
        
        return true; // "remove" operation only needs 3 parts
    }
}

