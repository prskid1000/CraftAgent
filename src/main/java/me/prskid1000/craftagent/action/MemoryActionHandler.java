package me.prskid1000.craftagent.action;

import me.prskid1000.craftagent.config.BaseConfig;
import me.prskid1000.craftagent.database.repositories.SharebookRepository;
import me.prskid1000.craftagent.memory.MemoryManager;
import me.prskid1000.craftagent.model.database.SharebookPage;
import me.prskid1000.craftagent.util.LogUtil;

import java.util.UUID;

/**
 * Handles memory actions: SharedBook and PrivateBook.
 * Format: "sharedbook add <title> <content>" or "sharedbook remove <title>"
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
        if (action == null || action.trim().isEmpty()) return false;
        
        String[] parts = action.trim().split("\\s+", 4);
        if (parts.length < 3) return false;
        
        String bookType = parts[0].toLowerCase();
        String operation = parts[1].toLowerCase();
        String pageTitle = parts[2];
        String content = parts.length > 3 ? parts[3] : "";
        
        return switch (bookType) {
            case "sharedbook" -> handleSharedBook(operation, pageTitle, content);
            case "privatebook" -> handlePrivateBook(operation, pageTitle, content);
            default -> false;
        };
    }
    
    private boolean handleSharedBook(String op, String title, String content) {
        if (sharebookRepository == null) return false;
        
        return switch (op) {
            case "add" -> {
                if (content.isEmpty()) yield false;
                SharebookPage page = new SharebookPage(title, content.trim(), 
                    npcUuid.toString(), System.currentTimeMillis());
                sharebookRepository.insertOrUpdate(page, baseConfig.getMaxSharebookPages());
                yield true;
            }
            case "remove" -> {
                sharebookRepository.delete(title, npcUuid.toString());
                yield true;
            }
            default -> false;
        };
    }
    
    private boolean handlePrivateBook(String op, String title, String content) {
        if (memoryManager == null) return false;
        
        return switch (op) {
            case "add" -> {
                if (content.isEmpty()) yield false;
                memoryManager.savePage(title, content.trim());
                yield true;
            }
            case "remove" -> {
                memoryManager.deletePage(title);
                yield true;
            }
            default -> false;
        };
    }
    
    public boolean isValidAction(String action) {
        if (action == null || action.trim().isEmpty()) return false;
        String[] parts = action.trim().split("\\s+", 4);
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
        
        return !"add".equals(op) || parts.length >= 4;
    }
}

