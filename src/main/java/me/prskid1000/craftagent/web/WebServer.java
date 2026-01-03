package me.prskid1000.craftagent.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import me.prskid1000.craftagent.common.NPCService;
import me.prskid1000.craftagent.config.ConfigProvider;
import me.prskid1000.craftagent.model.NPC;
import me.prskid1000.craftagent.util.LogUtil;
import net.minecraft.server.MinecraftServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

/**
 * Web server for viewing NPC properties, context, messages, and mail in a browser.
 * Accessible at http://localhost:8080
 */
public class WebServer {
    private static final int PORT = 8080;
    private HttpServer server;
    private final NPCService npcService;
    private final ConfigProvider configProvider;
    private final ObjectMapper objectMapper;
    
    public WebServer(NPCService npcService, ConfigProvider configProvider) {
        this.npcService = npcService;
        this.configProvider = configProvider;
        this.objectMapper = new ObjectMapper()
                .configure(SerializationFeature.INDENT_OUTPUT, true);
    }
    
    public void start() {
        try {
            server = HttpServer.create(new InetSocketAddress(PORT), 0);
            server.setExecutor(Executors.newCachedThreadPool());
            
            // API endpoints - order matters! More specific paths first
            server.createContext("/api/npcs", this::handleGetNPCs);
            server.createContext("/api/npc/", this::handleNPCRequest);
            
            // Static files
            server.createContext("/", this::handleStatic);
            
            server.start();
            LogUtil.info("Web UI server started on http://localhost:" + PORT);
        } catch (IOException e) {
            LogUtil.error("Failed to start web server", e);
        }
    }
    
    public void stop() {
        if (server != null) {
            server.stop(0);
            LogUtil.info("Web UI server stopped");
        }
    }
    
    private void handleGetNPCs(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendError(exchange, 405, "Method not allowed");
            return;
        }
        
        List<Map<String, Object>> npcs = npcService.getAllNPCs().stream()
                .map(this::npcToMap)
                .collect(Collectors.toList());
        
        sendJsonResponse(exchange, 200, npcs);
    }
    
    private void handleNPCRequest(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendError(exchange, 405, "Method not allowed");
            return;
        }
        
        String path = exchange.getRequestURI().getPath();
        String[] parts = path.split("/");
        
        if (parts.length < 4) {
            sendError(exchange, 400, "Invalid NPC UUID");
            return;
        }
        
        String uuidStr = parts[3];
        UUID uuid;
        try {
            uuid = UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            sendError(exchange, 400, "Invalid UUID format");
            return;
        }
        
        NPC npc = npcService.getNPC(uuid);
        if (npc == null) {
            sendError(exchange, 404, "NPC not found");
            return;
        }
        
        // Route to specific handler based on path
        if (parts.length == 4) {
            // /api/npc/{uuid} - Get NPC details
            Map<String, Object> npcData = npcToDetailedMap(npc);
            sendJsonResponse(exchange, 200, npcData);
        } else if (parts.length == 5) {
            String endpoint = parts[4];
            switch (endpoint) {
                case "context":
                    handleGetNPCContext(npc, exchange);
                    break;
                case "state":
                    handleGetNPCState(npc, exchange);
                    break;
                case "messages":
                    handleGetNPCMessages(npc, exchange);
                    break;
                case "mail":
                    handleGetNPCMail(npc, uuid, exchange);
                    break;
                case "memory":
                    handleGetNPCMemory(npc, exchange);
                    break;
                case "tools":
                    handleGetNPCTools(npc, exchange);
                    break;
                default:
                    sendError(exchange, 404, "Unknown endpoint: " + endpoint);
            }
        } else {
            sendError(exchange, 400, "Invalid path");
        }
    }
    
    private void handleGetNPCContext(NPC npc, HttpExchange exchange) throws IOException {
        try {
            var context = npc.getContextProvider().buildContext();
            Map<String, Object> contextMap = contextToMap(context);
            sendJsonResponse(exchange, 200, contextMap);
        } catch (Exception e) {
            LogUtil.error("Error getting NPC context", e);
            sendError(exchange, 500, "Error getting context: " + e.getMessage());
        }
    }
    
    private void handleGetNPCState(NPC npc, HttpExchange exchange) throws IOException {
        try {
            var context = npc.getContextProvider().buildContext();
            Map<String, Object> state = new HashMap<>();
            Map<String, Object> position = new HashMap<>();
            position.put("x", context.state().position().getX());
            position.put("y", context.state().position().getY());
            position.put("z", context.state().position().getZ());
            state.put("position", position);
            state.put("health", context.state().health());
            state.put("food", context.state().food());
            state.put("biome", context.state().biome());
            sendJsonResponse(exchange, 200, state);
        } catch (Exception e) {
            LogUtil.error("Error getting NPC state", e);
            sendError(exchange, 500, "Error getting state: " + e.getMessage());
        }
    }
    
    private void handleGetNPCMessages(NPC npc, HttpExchange exchange) throws IOException {
        try {
            var history = npc.getHistory().getLatestConversations();
            List<Map<String, Object>> messages = new ArrayList<>();
            for (var msg : history) {
                Map<String, Object> messageMap = new HashMap<>();
                messageMap.put("role", msg.getRole());
                messageMap.put("content", msg.getMessage());
                messageMap.put("timestamp", System.currentTimeMillis());
                messages.add(messageMap);
            }
            sendJsonResponse(exchange, 200, messages);
        } catch (Exception e) {
            LogUtil.error("Error getting NPC messages", e);
            sendError(exchange, 500, "Error getting messages: " + e.getMessage());
        }
    }
    
    private void handleGetNPCMail(NPC npc, UUID uuid, HttpExchange exchange) throws IOException {
        try {
            var messageRepo = npc.getContextProvider().getMessageRepository();
            if (messageRepo == null) {
                sendJsonResponse(exchange, 200, Collections.emptyList());
                return;
            }
            
            var messages = messageRepo.selectByRecipient(uuid, 100, false);
            List<Map<String, Object>> mailList = new ArrayList<>();
            for (var msg : messages) {
                Map<String, Object> mailMap = new HashMap<>();
                mailMap.put("id", msg.getId());
                mailMap.put("senderName", msg.getSenderName());
                mailMap.put("senderType", msg.getSenderType());
                mailMap.put("subject", msg.getSubject());
                mailMap.put("content", msg.getContent());
                mailMap.put("timestamp", msg.getTimestamp());
                mailMap.put("read", msg.getRead());
                mailList.add(mailMap);
            }
            sendJsonResponse(exchange, 200, mailList);
        } catch (Exception e) {
            LogUtil.error("Error getting NPC mail", e);
            sendError(exchange, 500, "Error getting mail: " + e.getMessage());
        }
    }
    
    private void handleGetNPCMemory(NPC npc, HttpExchange exchange) throws IOException {
        try {
            var memoryManager = npc.getContextProvider().memoryManager;
            if (memoryManager == null) {
                sendJsonResponse(exchange, 200, Map.of("locations", Collections.emptyList(), "contacts", Collections.emptyList()));
                return;
            }
            
            Map<String, Object> memory = new HashMap<>();
            
            List<Map<String, Object>> locations = new ArrayList<>();
            for (var loc : memoryManager.getLocations()) {
                Map<String, Object> locMap = new HashMap<>();
                locMap.put("name", loc.getName());
                locMap.put("x", loc.getX());
                locMap.put("y", loc.getY());
                locMap.put("z", loc.getZ());
                locMap.put("description", loc.getDescription());
                locMap.put("timestamp", loc.getTimestamp());
                locations.add(locMap);
            }
            
            List<Map<String, Object>> contacts = new ArrayList<>();
            for (var contact : memoryManager.getContacts()) {
                Map<String, Object> contactMap = new HashMap<>();
                contactMap.put("name", contact.getContactName());
                contactMap.put("type", contact.getContactType());
                contactMap.put("relationship", contact.getRelationship());
                contactMap.put("notes", contact.getNotes());
                contactMap.put("lastSeen", contact.getLastSeen());
                contactMap.put("enmityLevel", contact.getEnmityLevel());
                contactMap.put("friendshipLevel", contact.getFriendshipLevel());
                contacts.add(contactMap);
            }
            
            memory.put("locations", locations);
            memory.put("contacts", contacts);
            sendJsonResponse(exchange, 200, memory);
        } catch (Exception e) {
            LogUtil.error("Error getting NPC memory", e);
            sendError(exchange, 500, "Error getting memory: " + e.getMessage());
        }
    }
    
    private void handleGetNPCTools(NPC npc, HttpExchange exchange) throws IOException {
        try {
            MinecraftServer server = npc.getContextProvider().getNpcEntity().getServer();
            Map<String, Object> toolsData = new HashMap<>();
            
            // Get Minecraft commands
            List<Map<String, Object>> minecraftCommands = new ArrayList<>();
            if (server != null) {
                var commandsWithUsage = me.prskid1000.craftagent.util.MinecraftCommandUtil.getAllCommandsWithUsage(server);
                for (var entry : commandsWithUsage.entrySet()) {
                    Map<String, Object> cmd = new HashMap<>();
                    cmd.put("name", entry.getKey());
                    cmd.put("usage", entry.getValue());
                    cmd.put("type", "minecraft");
                    minecraftCommands.add(cmd);
                }
            }
            toolsData.put("minecraftCommands", minecraftCommands);
            
            // Get predefined tools
            List<Map<String, Object>> predefinedTools = new ArrayList<>();
            
            // execute_command tool
            Map<String, Object> executeCmd = new HashMap<>();
            executeCmd.put("name", "execute_command");
            executeCmd.put("description", "Execute a Minecraft command. Use this when you want to perform an action in the game.");
            executeCmd.put("type", "predefined");
            executeCmd.put("parameters", List.of("command: string (The complete Minecraft command string to execute)"));
            predefinedTools.add(executeCmd);
            
            // manageMemory tool
            Map<String, Object> manageMemory = new HashMap<>();
            manageMemory.put("name", "manageMemory");
            manageMemory.put("description", "Manage information in memory (contacts or locations). Use 'add' or 'update' to add new contacts or save locations, or 'remove' to forget information.");
            manageMemory.put("type", "predefined");
            manageMemory.put("parameters", List.of(
                "action: string (add/update/remove)",
                "infoType: string (contact/location)",
                "name: string",
                "relationship: string (optional, for contacts)",
                "notes: string (optional, for contacts)",
                "description: string (optional, for locations)"
            ));
            predefinedTools.add(manageMemory);
            
            // sendMessage tool
            Map<String, Object> sendMessage = new HashMap<>();
            sendMessage.put("name", "sendMessage");
            sendMessage.put("description", "Send a message to another NPC or player. Messages are stored in a mail system and can be read by the recipient later.");
            sendMessage.put("type", "predefined");
            sendMessage.put("parameters", List.of(
                "recipientName: string",
                "subject: string",
                "content: string"
            ));
            predefinedTools.add(sendMessage);
            
            // manageBook tool
            Map<String, Object> manageBook = new HashMap<>();
            manageBook.put("name", "manageBook");
            manageBook.put("description", "Manage pages in the shared book. The shared book is accessible to all NPCs and contains common information.");
            manageBook.put("type", "predefined");
            manageBook.put("parameters", List.of(
                "action: string (add/update/remove)",
                "pageTitle: string",
                "content: string (required for add/update)"
            ));
            predefinedTools.add(manageBook);
            
            toolsData.put("predefinedTools", predefinedTools);
            sendJsonResponse(exchange, 200, toolsData);
        } catch (Exception e) {
            LogUtil.error("Error getting NPC tools", e);
            sendError(exchange, 500, "Error getting tools: " + e.getMessage());
        }
    }
    
    
    private void handleStatic(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (path.equals("/") || path.equals("/index.html")) {
            path = "/index.html";
        }
        
        // Remove leading slash for file path
        String filePath = path.startsWith("/") ? path.substring(1) : path;
        
        // Try to load from resources first, then fallback to embedded HTML
        InputStream resourceStream = getClass().getClassLoader().getResourceAsStream("web/" + filePath);
        
        if (resourceStream == null && filePath.equals("index.html")) {
            // Serve embedded HTML if file not found
            String html = getEmbeddedHTML();
            sendHtmlResponse(exchange, 200, html);
            return;
        }
        
        if (resourceStream == null) {
            sendError(exchange, 404, "File not found");
            return;
        }
        
        String contentType = getContentType(filePath);
        byte[] content = resourceStream.readAllBytes();
        resourceStream.close();
        
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(200, content.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(content);
        }
    }
    
    private String getContentType(String filePath) {
        if (filePath.endsWith(".html")) return "text/html; charset=utf-8";
        if (filePath.endsWith(".css")) return "text/css; charset=utf-8";
        if (filePath.endsWith(".js")) return "application/javascript; charset=utf-8";
        if (filePath.endsWith(".json")) return "application/json; charset=utf-8";
        return "text/plain; charset=utf-8";
    }
    
    private Map<String, Object> npcToMap(NPC npc) {
        Map<String, Object> map = new HashMap<>();
        map.put("uuid", npc.getConfig().getUuid().toString());
        map.put("name", npc.getConfig().getNpcName());
        map.put("age", npc.getConfig().getAge());
        map.put("gender", npc.getConfig().getGender());
        map.put("isActive", npc.getConfig().isActive());
        map.put("llmType", npc.getConfig().getLlmType().toString());
        
        Map<String, Object> position = new HashMap<>();
        position.put("x", npc.getEntity().getX());
        position.put("y", npc.getEntity().getY());
        position.put("z", npc.getEntity().getZ());
        map.put("position", position);
        
        map.put("health", npc.getEntity().getHealth());
        map.put("food", npc.getEntity().getHungerManager().getFoodLevel());
        return map;
    }
    
    private Map<String, Object> npcToDetailedMap(NPC npc) {
        Map<String, Object> map = new HashMap<>(npcToMap(npc));
        map.put("entityUuid", npc.getEntity().getUuid().toString());
        map.put("llmModel", npc.getConfig().getLlmModel());
        map.put("ollamaUrl", npc.getConfig().getOllamaUrl());
        map.put("lmStudioUrl", npc.getConfig().getLmStudioUrl());
        return map;
    }
    
    private Map<String, Object> contextToMap(me.prskid1000.craftagent.model.context.WorldContext context) {
        Map<String, Object> map = new HashMap<>();
        
        Map<String, Object> state = new HashMap<>();
        Map<String, Object> position = new HashMap<>();
        position.put("x", context.state().position().getX());
        position.put("y", context.state().position().getY());
        position.put("z", context.state().position().getZ());
        state.put("position", position);
        state.put("health", context.state().health());
        state.put("food", context.state().food());
        state.put("biome", context.state().biome());
        map.put("state", state);
        
        Map<String, Object> inventory = new HashMap<>();
        inventory.put("hotbar", context.inventory().hotbar().size());
        inventory.put("mainInventory", context.inventory().mainInventory().size());
        inventory.put("armor", context.inventory().armor().size());
        inventory.put("offHand", context.inventory().offHand().size());
        map.put("inventory", inventory);
        
        // Convert nearbyBlocks to detailed list
        List<Map<String, Object>> blocksList = new ArrayList<>();
        for (var block : context.nearbyBlocks()) {
            Map<String, Object> blockMap = new HashMap<>();
            blockMap.put("type", block.type());
            Map<String, Object> blockPos = new HashMap<>();
            blockPos.put("x", block.position().getX());
            blockPos.put("y", block.position().getY());
            blockPos.put("z", block.position().getZ());
            blockMap.put("position", blockPos);
            blockMap.put("mineLevel", block.mineLevel());
            blockMap.put("toolNeeded", block.toolNeeded());
            blocksList.add(blockMap);
        }
        map.put("nearbyBlocks", blocksList);
        
        // Convert nearbyEntities to detailed list
        List<Map<String, Object>> entitiesList = new ArrayList<>();
        for (var entity : context.nearbyEntities()) {
            Map<String, Object> entityMap = new HashMap<>();
            entityMap.put("id", entity.id());
            entityMap.put("name", entity.name());
            entityMap.put("isPlayer", entity.isPlayer());
            entityMap.put("type", entity.isPlayer() ? "Player" : "Entity");
            entitiesList.add(entityMap);
        }
        map.put("nearbyEntities", entitiesList);
        
        if (context.memoryData() != null) {
            map.put("memory", context.memoryData());
        }
        return map;
    }
    
    private void sendJsonResponse(HttpExchange exchange, int statusCode, Object data) throws IOException {
        String json = objectMapper.writeValueAsString(data);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(statusCode, json.getBytes(StandardCharsets.UTF_8).length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(json.getBytes(StandardCharsets.UTF_8));
        }
    }
    
    private void sendHtmlResponse(HttpExchange exchange, int statusCode, String html) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, html.getBytes(StandardCharsets.UTF_8).length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(html.getBytes(StandardCharsets.UTF_8));
        }
    }
    
    private void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        Map<String, String> error = Map.of("error", message);
        sendJsonResponse(exchange, statusCode, error);
    }
    
    private String getEmbeddedHTML() {
        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>CraftAgent - NPC Dashboard</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            background-attachment: fixed;
            min-height: 100vh;
            padding: 20px;
            color: #333;
            animation: gradientShift 15s ease infinite;
        }
        @keyframes gradientShift {
            0%, 100% { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); }
            50% { background: linear-gradient(135deg, #764ba2 0%, #667eea 100%); }
        }
        .container {
            max-width: 1400px;
            margin: 0 auto;
        }
        header {
            background: rgba(255, 255, 255, 0.95);
            backdrop-filter: blur(10px);
            padding: 30px;
            border-radius: 20px;
            margin-bottom: 30px;
            box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
            border: 1px solid rgba(255, 255, 255, 0.2);
            animation: slideDown 0.5s ease-out;
        }
        @keyframes slideDown {
            from { opacity: 0; transform: translateY(-20px); }
            to { opacity: 1; transform: translateY(0); }
        }
        h1 {
            color: #667eea;
            font-size: 2.5em;
            margin-bottom: 10px;
        }
        .subtitle {
            color: #666;
            font-size: 1.1em;
        }
        .npcs-grid {
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
            gap: 20px;
            margin-bottom: 20px;
        }
        .npc-card {
            background: rgba(255, 255, 255, 0.95);
            backdrop-filter: blur(10px);
            border-radius: 20px;
            padding: 25px;
            box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
            border: 1px solid rgba(255, 255, 255, 0.2);
            cursor: pointer;
            transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
            animation: fadeInUp 0.5s ease-out;
            animation-fill-mode: both;
        }
        .npc-card:nth-child(1) { animation-delay: 0.1s; }
        .npc-card:nth-child(2) { animation-delay: 0.2s; }
        .npc-card:nth-child(3) { animation-delay: 0.3s; }
        .npc-card:nth-child(4) { animation-delay: 0.4s; }
        .npc-card:nth-child(5) { animation-delay: 0.5s; }
        @keyframes fadeInUp {
            from { opacity: 0; transform: translateY(20px); }
            to { opacity: 1; transform: translateY(0); }
        }
        .npc-card:hover {
            transform: translateY(-8px) scale(1.02);
            box-shadow: 0 16px 48px rgba(102, 126, 234, 0.3);
        }
        .npc-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 15px;
            padding-bottom: 15px;
            border-bottom: 2px solid #f0f0f0;
        }
        .npc-name {
            font-size: 1.5em;
            font-weight: bold;
            color: #667eea;
        }
        .npc-status {
            padding: 5px 15px;
            border-radius: 20px;
            font-size: 0.9em;
            font-weight: bold;
        }
        .status-active {
            background: #4caf50;
            color: white;
        }
        .status-inactive {
            background: #f44336;
            color: white;
        }
        .npc-info {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 10px;
            margin-bottom: 15px;
        }
        .info-item {
            display: flex;
            flex-direction: column;
        }
        .info-label {
            font-size: 0.85em;
            color: #666;
            margin-bottom: 5px;
        }
        .info-value {
            font-size: 1.1em;
            font-weight: bold;
            color: #333;
        }
        .view-details-btn {
            width: 100%;
            padding: 14px;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            border: none;
            border-radius: 10px;
            font-size: 1em;
            font-weight: bold;
            cursor: pointer;
            transition: all 0.3s;
            box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
            position: relative;
            overflow: hidden;
        }
        .view-details-btn::before {
            content: '';
            position: absolute;
            top: 50%;
            left: 50%;
            width: 0;
            height: 0;
            border-radius: 50%;
            background: rgba(255, 255, 255, 0.3);
            transform: translate(-50%, -50%);
            transition: width 0.6s, height 0.6s;
        }
        .view-details-btn:hover::before {
            width: 300px;
            height: 300px;
        }
        .view-details-btn:hover {
            transform: translateY(-2px);
            box-shadow: 0 6px 20px rgba(102, 126, 234, 0.5);
        }
        .view-details-btn:active {
            transform: translateY(0);
        }
        .modal {
            display: none;
            position: fixed;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            background: rgba(0, 0, 0, 0.7);
            z-index: 1000;
            overflow-y: auto;
        }
        .modal.active {
            display: flex;
            justify-content: center;
            align-items: flex-start;
            padding: 20px;
        }
        .modal-content {
            background: white;
            backdrop-filter: blur(10px);
            border-radius: 20px;
            padding: 30px;
            max-width: 1200px;
            width: 100%;
            margin: 20px auto;
            max-height: 90vh;
            overflow-y: auto;
            box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
            border: 1px solid rgba(255, 255, 255, 0.2);
            animation: modalSlideIn 0.3s ease-out;
        }
        @keyframes modalSlideIn {
            from { opacity: 0; transform: scale(0.9) translateY(-20px); }
            to { opacity: 1; transform: scale(1) translateY(0); }
        }
        .modal-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 30px;
            padding-bottom: 20px;
            border-bottom: 2px solid #f0f0f0;
        }
        .modal-title {
            font-size: 2em;
            color: #667eea;
        }
        .close-btn {
            background: #f44336;
            color: white;
            border: none;
            padding: 10px 20px;
            border-radius: 8px;
            cursor: pointer;
            font-size: 1em;
        }
        .tabs {
            display: flex;
            gap: 10px;
            margin-bottom: 20px;
            border-bottom: 2px solid #f0f0f0;
        }
        .tab {
            padding: 12px 24px;
            background: none;
            border: none;
            cursor: pointer;
            font-size: 1em;
            color: #666;
            border-bottom: 3px solid transparent;
            transition: all 0.3s;
        }
        .tab.active {
            color: #667eea;
            border-bottom-color: #667eea;
            font-weight: bold;
        }
        .tab-content {
            display: none;
        }
        .tab-content.active {
            display: block;
        }
        .data-section {
            margin-bottom: 30px;
        }
        .data-section h3 {
            color: #667eea;
            margin-bottom: 15px;
            font-size: 1.3em;
        }
        .data-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
            gap: 15px;
        }
        .data-card {
            background: linear-gradient(135deg, #f8f9fa 0%, #ffffff 100%);
            padding: 20px;
            border-radius: 12px;
            border-left: 4px solid #667eea;
            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
            transition: all 0.3s;
        }
        .data-card:hover {
            transform: translateX(5px);
            box-shadow: 0 4px 12px rgba(102, 126, 234, 0.2);
        }
        .data-card-label {
            font-size: 0.9em;
            color: #666;
            margin-bottom: 5px;
        }
        .data-card-value {
            font-size: 1.2em;
            font-weight: bold;
            color: #333;
        }
        .json-viewer {
            background: #1e1e1e;
            color: #d4d4d4;
            padding: 20px;
            border-radius: 8px;
            font-family: 'Courier New', monospace;
            font-size: 0.9em;
            overflow-x: auto;
            max-height: 500px;
            overflow-y: auto;
        }
        table {
            width: 100%;
            border-collapse: collapse;
            background: white;
            border-radius: 8px;
            overflow: hidden;
            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
        }
        th {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 15px;
            text-align: left;
            font-weight: 600;
            font-size: 0.95em;
        }
        td {
            padding: 12px 15px;
            border-bottom: 1px solid #f0f0f0;
        }
        tr:hover {
            background: #f8f9fa;
        }
        tr:last-child td {
            border-bottom: none;
        }
        .icon {
            display: inline-block;
            width: 20px;
            height: 20px;
            margin-right: 8px;
            vertical-align: middle;
            font-size: 1.2em;
        }
        .progress-bar {
            width: 100%;
            height: 24px;
            background: #e0e0e0;
            border-radius: 12px;
            overflow: hidden;
            position: relative;
        }
        .progress-fill {
            height: 100%;
            background: linear-gradient(90deg, #4caf50 0%, #8bc34a 100%);
            transition: width 0.3s;
            display: flex;
            align-items: center;
            justify-content: center;
            color: white;
            font-size: 0.85em;
            font-weight: bold;
        }
        .progress-fill.health {
            background: linear-gradient(90deg, #f44336 0%, #ff9800 50%, #4caf50 100%);
        }
        .progress-fill.food {
            background: linear-gradient(90deg, #ff9800 0%, #ffc107 50%, #4caf50 100%);
        }
        .badge {
            display: inline-block;
            padding: 4px 12px;
            border-radius: 12px;
            font-size: 0.85em;
            font-weight: 600;
        }
        .badge-success {
            background: #4caf50;
            color: white;
        }
        .badge-info {
            background: #2196f3;
            color: white;
        }
        .badge-warning {
            background: #ff9800;
            color: white;
        }
        .badge-danger {
            background: #f44336;
            color: white;
        }
        .message-bubble {
            margin-bottom: 15px;
            padding: 12px 16px;
            border-radius: 12px;
            max-width: 80%;
            word-wrap: break-word;
        }
        .message-user {
            background: #e3f2fd;
            margin-left: auto;
            text-align: right;
        }
        .message-assistant {
            background: #f5f5f5;
        }
        .message-system {
            background: #fff3e0;
            font-style: italic;
        }
        .coords {
            font-family: 'Courier New', monospace;
            background: #f5f5f5;
            padding: 4px 8px;
            border-radius: 4px;
        }
        .message-content {
            max-width: 500px;
            word-wrap: break-word;
            white-space: pre-wrap;
        }
        .message-collapsible {
            position: relative;
        }
        .message-preview {
            max-height: 150px;
            overflow: hidden;
            position: relative;
        }
        .message-preview::after {
            content: '';
            position: absolute;
            bottom: 0;
            left: 0;
            right: 0;
            height: 40px;
            background: linear-gradient(to bottom, transparent, rgba(255, 255, 255, 0.95));
            pointer-events: none;
        }
        .message-full {
            max-height: none;
        }
        .message-toggle {
            background: #667eea;
            color: white;
            border: none;
            padding: 4px 12px;
            border-radius: 4px;
            cursor: pointer;
            font-size: 0.85em;
            margin-top: 8px;
            transition: all 0.3s;
        }
        .message-toggle:hover {
            background: #5568d3;
            transform: translateY(-1px);
        }
        .message-toggle:active {
            transform: translateY(0);
        }
        .stat-card {
            background: linear-gradient(135deg, #f8f9fa 0%, #ffffff 100%);
            padding: 20px;
            border-radius: 12px;
            border-left: 4px solid #667eea;
            text-align: center;
        }
        .stat-value {
            font-size: 2em;
            font-weight: bold;
            color: #667eea;
            margin: 10px 0;
        }
        .stat-label {
            color: #666;
            font-size: 0.9em;
        }
        .loading {
            text-align: center;
            padding: 40px;
            color: #666;
        }
        .refresh-btn {
            background: #4caf50;
            color: white;
            border: none;
            padding: 10px 20px;
            border-radius: 8px;
            cursor: pointer;
            font-size: 1em;
            margin-left: 10px;
        }
    </style>
</head>
<body>
    <div class="container">
        <header>
            <h1>🎮 CraftAgent NPC Dashboard</h1>
            <p class="subtitle">Monitor and manage your NPCs in real-time</p>
        </header>
        
        <div class="npcs-grid" id="npcsGrid">
            <div class="loading">Loading NPCs...</div>
        </div>
    </div>
    
    <div class="modal" id="npcModal">
        <div class="modal-content">
            <div class="modal-header">
                <h2 class="modal-title" id="modalTitle">NPC Details</h2>
                <div>
                    <button class="refresh-btn" onclick="refreshCurrentNPC()">🔄 Refresh</button>
                    <button class="close-btn" onclick="closeModal()">✕ Close</button>
                </div>
            </div>
            
            <div class="tabs">
                <button class="tab active" onclick="switchTab('overview')">Overview</button>
                <button class="tab" onclick="switchTab('state')">State</button>
                <button class="tab" onclick="switchTab('context')">Context</button>
                <button class="tab" onclick="switchTab('messages')">Messages</button>
                <button class="tab" onclick="switchTab('mail')">Mail</button>
                <button class="tab" onclick="switchTab('memory')">Memory</button>
                <button class="tab" onclick="switchTab('tools')">Tools</button>
            </div>
            
            <div id="overview" class="tab-content active">
                <div class="data-section">
                    <h3>Basic Information</h3>
                    <div class="data-grid" id="overviewGrid"></div>
                </div>
            </div>
            
            <div id="state" class="tab-content">
                <div class="loading">Loading state...</div>
            </div>
            
            <div id="context" class="tab-content">
                <div class="loading">Loading context...</div>
            </div>
            
            <div id="messages" class="tab-content">
                <div class="loading">Loading messages...</div>
            </div>
            
            <div id="mail" class="tab-content">
                <div class="loading">Loading mail...</div>
            </div>
            
            <div id="memory" class="tab-content">
                <div class="loading">Loading memory...</div>
            </div>
            
            <div id="tools" class="tab-content">
                <div class="loading">Loading tools...</div>
            </div>
        </div>
    </div>
    
    <script>
        let currentNPCUuid = null;
        
        async function loadNPCs() {
            try {
                const response = await fetch('/api/npcs');
                const npcs = await response.json();
                displayNPCs(npcs);
            } catch (error) {
                document.getElementById('npcsGrid').innerHTML = 
                    '<div class="loading">Error loading NPCs: ' + error.message + '</div>';
            }
        }
        
        function displayNPCs(npcs) {
            const grid = document.getElementById('npcsGrid');
            if (npcs.length === 0) {
                grid.innerHTML = '<div class="loading">No NPCs found</div>';
                return;
            }
            
            grid.innerHTML = npcs.map(npc => `
                <div class="npc-card" onclick="viewNPC('${npc.uuid}')">
                    <div class="npc-header">
                        <div class="npc-name">${escapeHtml(npc.name)}</div>
                        <div class="npc-status ${npc.isActive ? 'status-active' : 'status-inactive'}">
                            ${npc.isActive ? 'Active' : 'Inactive'}
                        </div>
                    </div>
                    <div class="npc-info">
                        <div class="info-item">
                            <div class="info-label">Age</div>
                            <div class="info-value">${npc.age} years</div>
                        </div>
                        <div class="info-item">
                            <div class="info-label">Gender</div>
                            <div class="info-value">${escapeHtml(npc.gender)}</div>
                        </div>
                        <div class="info-item">
                            <div class="info-label">Health</div>
                            <div class="info-value">${npc.health.toFixed(1)} / 20</div>
                        </div>
                        <div class="info-item">
                            <div class="info-label">Food</div>
                            <div class="info-value">${npc.food} / 20</div>
                        </div>
                    </div>
                    <button class="view-details-btn" onclick="event.stopPropagation(); viewNPC('${npc.uuid}')">
                        View Details
                    </button>
                </div>
            `).join('');
        }
        
        async function viewNPC(uuid) {
            currentNPCUuid = uuid;
            const modal = document.getElementById('npcModal');
            modal.classList.add('active');
            
            // Load all data
            await Promise.all([
                loadNPCOverview(uuid),
                loadNPCState(uuid),
                loadNPCContext(uuid),
                loadNPCMessages(uuid),
                loadNPCMail(uuid),
                loadNPCMemory(uuid),
                loadNPCTools(uuid)
            ]);
        }
        
        async function loadNPCOverview(uuid) {
            try {
                const response = await fetch(`/api/npc/${uuid}`);
                const npc = await response.json();
                
                document.getElementById('modalTitle').textContent = npc.name + ' - Details';
                document.getElementById('overviewGrid').innerHTML = `
                    <div class="data-card">
                        <div class="data-card-label">Name</div>
                        <div class="data-card-value">${escapeHtml(npc.name)}</div>
                    </div>
                    <div class="data-card">
                        <div class="data-card-label">Age</div>
                        <div class="data-card-value">${npc.age} years</div>
                    </div>
                    <div class="data-card">
                        <div class="data-card-label">Gender</div>
                        <div class="data-card-value">${escapeHtml(npc.gender)}</div>
                    </div>
                    <div class="data-card">
                        <div class="data-card-label">Position</div>
                        <div class="data-card-value">${npc.position.x.toFixed(1)}, ${npc.position.y.toFixed(1)}, ${npc.position.z.toFixed(1)}</div>
                    </div>
                    <div class="data-card">
                        <div class="data-card-label">Health</div>
                        <div class="data-card-value">${npc.health.toFixed(1)} / 20</div>
                    </div>
                    <div class="data-card">
                        <div class="data-card-label">Food</div>
                        <div class="data-card-value">${npc.food} / 20</div>
                    </div>
                    <div class="data-card">
                        <div class="data-card-label">LLM Type</div>
                        <div class="data-card-value">${escapeHtml(npc.llmType)}</div>
                    </div>
                    <div class="data-card">
                        <div class="data-card-label">LLM Model</div>
                        <div class="data-card-value">${escapeHtml(npc.llmModel || 'N/A')}</div>
                    </div>
                `;
            } catch (error) {
                document.getElementById('overviewGrid').innerHTML = 
                    '<div class="loading">Error: ' + error.message + '</div>';
            }
        }
        
        async function loadNPCState(uuid) {
            try {
                const response = await fetch(`/api/npc/${uuid}/state`);
                const state = await response.json();
                const healthPercent = (state.health / 20) * 100;
                const foodPercent = (state.food / 20) * 100;
                document.getElementById('state').innerHTML = `
                    <div class="data-section">
                        <h3>📊 Current Statistics</h3>
                        <div class="data-grid" style="grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));">
                            <div class="stat-card">
                                <div class="stat-label">❤️ Health</div>
                                <div class="stat-value">${state.health.toFixed(1)}</div>
                                <div class="progress-bar">
                                    <div class="progress-fill health" style="width: ${healthPercent}%">${healthPercent.toFixed(0)}%</div>
                                </div>
                            </div>
                            <div class="stat-card">
                                <div class="stat-label">🍖 Food</div>
                                <div class="stat-value">${state.food}</div>
                                <div class="progress-bar">
                                    <div class="progress-fill food" style="width: ${foodPercent}%">${foodPercent.toFixed(0)}%</div>
                                </div>
                            </div>
                            <div class="stat-card">
                                <div class="stat-label">🌍 Biome</div>
                                <div class="stat-value" style="font-size: 1.2em;">${escapeHtml(state.biome.replace(/_/g, ' '))}</div>
                            </div>
                        </div>
                    </div>
                    <div class="data-section">
                        <h3>📍 Position</h3>
                        <table>
                            <thead>
                                <tr>
                                    <th><span class="icon">📍</span> Coordinate</th>
                                    <th>Value</th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr>
                                    <td><strong>X</strong></td>
                                    <td><span class="coords">${state.position.x.toFixed(2)}</span></td>
                                </tr>
                                <tr>
                                    <td><strong>Y</strong></td>
                                    <td><span class="coords">${state.position.y.toFixed(2)}</span></td>
                                </tr>
                                <tr>
                                    <td><strong>Z</strong></td>
                                    <td><span class="coords">${state.position.z.toFixed(2)}</span></td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                `;
            } catch (error) {
                document.getElementById('state').innerHTML = 
                    '<div class="loading">Error: ' + error.message + '</div>';
            }
        }
        
        async function loadNPCContext(uuid) {
            try {
                const response = await fetch(`/api/npc/${uuid}/context`);
                const context = await response.json();
                
                let html = '<div class="data-section"><h3>📊 State Information</h3>';
                html += '<table><thead><tr><th>Property</th><th>Value</th></tr></thead><tbody>';
                html += `<tr><td><span class="icon">📍</span> Position</td><td><span class="coords">${context.state.position.x.toFixed(1)}, ${context.state.position.y.toFixed(1)}, ${context.state.position.z.toFixed(1)}</span></td></tr>`;
                html += `<tr><td><span class="icon">❤️</span> Health</td><td>${context.state.health.toFixed(1)} / 20</td></tr>`;
                html += `<tr><td><span class="icon">🍖</span> Food</td><td>${context.state.food} / 20</td></tr>`;
                html += `<tr><td><span class="icon">🌍</span> Biome</td><td>${escapeHtml(context.state.biome.replace(/_/g, ' '))}</td></tr>`;
                html += '</tbody></table></div>';
                
                html += '<div class="data-section"><h3>🎒 Inventory</h3>';
                html += '<table><thead><tr><th>Slot Type</th><th>Count</th></tr></thead><tbody>';
                html += `<tr><td><span class="icon">📦</span> Hotbar</td><td>${context.inventory.hotbar}</td></tr>`;
                html += `<tr><td><span class="icon">📦</span> Main Inventory</td><td>${context.inventory.mainInventory}</td></tr>`;
                html += `<tr><td><span class="icon">🛡️</span> Armor</td><td>${context.inventory.armor}</td></tr>`;
                html += `<tr><td><span class="icon">✋</span> Off Hand</td><td>${context.inventory.offHand}</td></tr>`;
                html += '</tbody></table></div>';
                
                if (context.nearbyBlocks && Array.isArray(context.nearbyBlocks) && context.nearbyBlocks.length > 0) {
                    html += '<div class="data-section"><h3>🧱 Nearby Blocks</h3>';
                    html += '<table><thead><tr><th>Block Type</th><th>Position</th><th>Mine Level</th><th>Tool Needed</th></tr></thead><tbody>';
                    context.nearbyBlocks.forEach(block => {
                        const pos = block.position || {};
                        const positionStr = pos.x !== undefined ? `${pos.x.toFixed(0)}, ${pos.y.toFixed(0)}, ${pos.z.toFixed(0)}` : 'N/A';
                        const mineLevel = block.mineLevel || 'N/A';
                        const toolNeeded = block.toolNeeded || 'None';
                        html += `<tr>
                            <td><strong>${escapeHtml(block.type || 'Unknown')}</strong></td>
                            <td><span class="coords">${positionStr}</span></td>
                            <td>${escapeHtml(String(mineLevel))}</td>
                            <td>${escapeHtml(toolNeeded)}</td>
                        </tr>`;
                    });
                    html += '</tbody></table></div>';
                } else {
                    html += '<div class="data-section"><h3>🧱 Nearby Blocks</h3>';
                    html += '<p>No blocks detected nearby</p></div>';
                }
                
                if (context.nearbyEntities && Array.isArray(context.nearbyEntities) && context.nearbyEntities.length > 0) {
                    html += '<div class="data-section"><h3>👥 Nearby Entities</h3>';
                    html += '<table><thead><tr><th>Name</th><th>Type</th><th>ID</th><th>Is Player</th></tr></thead><tbody>';
                    context.nearbyEntities.forEach(entity => {
                        const typeBadge = entity.isPlayer ? 'badge-info' : 'badge-warning';
                        const typeText = entity.isPlayer ? 'Player' : 'Entity';
                        html += `<tr>
                            <td><strong>${escapeHtml(entity.name || 'Unknown')}</strong></td>
                            <td><span class="badge ${typeBadge}">${typeText}</span></td>
                            <td><span class="coords">${escapeHtml(String(entity.id || 'N/A'))}</span></td>
                            <td>${entity.isPlayer ? '✅ Yes' : '❌ No'}</td>
                        </tr>`;
                    });
                    html += '</tbody></table></div>';
                } else {
                    html += '<div class="data-section"><h3>👥 Nearby Entities</h3>';
                    html += '<p>No entities detected nearby</p></div>';
                }
                
                document.getElementById('context').innerHTML = html;
            } catch (error) {
                document.getElementById('context').innerHTML = 
                    '<div class="loading">Error: ' + error.message + '</div>';
            }
        }
        
        async function loadNPCMessages(uuid) {
            try {
                const response = await fetch(`/api/npc/${uuid}/messages`);
                const messages = await response.json();
                
                if (messages.length === 0) {
                    document.getElementById('messages').innerHTML = 
                        '<div class="loading">No messages in conversation history</div>';
                    return;
                }
                
                let html = '<div class="data-section"><h3>💬 Conversation History</h3>';
                html += '<table><thead><tr><th>Role</th><th>Message</th><th>Timestamp</th></tr></thead><tbody>';
                
                messages.forEach((msg, index) => {
                    const role = msg.role || 'unknown';
                    const content = msg.content || msg.message || '';
                    const timestamp = msg.timestamp ? new Date(msg.timestamp).toLocaleString() : 'N/A';
                    const roleIcon = role === 'user' ? '👤' : role === 'assistant' ? '🤖' : role === 'system' ? '⚙️' : '❓';
                    const roleBadge = role === 'user' ? 'badge-info' : role === 'assistant' ? 'badge-success' : 'badge-warning';
                    
                    // Format system messages with proper HTML rendering
                    const formattedContent = role === 'system' ? formatSystemMessage(content) : escapeHtml(content);
                    
                    // Determine if message is long (more than 500 characters)
                    // This covers both long text and multi-line system messages
                    const isLong = content.length > 500;
                    const messageId = `msg-${index}`;
                    
                    if (isLong) {
                        html += `<tr>
                            <td><span class="badge ${roleBadge}">${roleIcon} ${role.toUpperCase()}</span></td>
                            <td>
                                <div class="message-collapsible">
                                    <div id="${messageId}-preview" class="message-preview message-content">${formattedContent}</div>
                                    <div id="${messageId}-full" class="message-full message-content" style="display: none;">${formattedContent}</div>
                                    <button class="message-toggle" onclick="toggleMessage('${messageId}')" id="${messageId}-btn">Show more</button>
                                </div>
                            </td>
                            <td>${timestamp}</td>
                        </tr>`;
                    } else {
                        html += `<tr>
                            <td><span class="badge ${roleBadge}">${roleIcon} ${role.toUpperCase()}</span></td>
                            <td class="message-content">${formattedContent}</td>
                            <td>${timestamp}</td>
                        </tr>`;
                    }
                });
                
                html += '</tbody></table></div>';
                document.getElementById('messages').innerHTML = html;
            } catch (error) {
                document.getElementById('messages').innerHTML = 
                    '<div class="loading">Error: ' + error.message + '</div>';
            }
        }
        
        async function loadNPCMail(uuid) {
            try {
                const response = await fetch(`/api/npc/${uuid}/mail`);
                const mail = await response.json();
                
                if (!mail || mail.length === 0) {
                    document.getElementById('mail').innerHTML = 
                        '<div class="loading">📭 No mail messages</div>';
                    return;
                }
                
                let html = '<div class="data-section"><h3>📬 Mail Messages</h3>';
                html += '<table><thead><tr><th>From</th><th>Subject</th><th>Content</th><th>Date</th></tr></thead><tbody>';
                
                mail.forEach(msg => {
                    const sender = msg.senderName || msg.sender || 'Unknown';
                    const subject = msg.subject || 'No Subject';
                    const content = msg.content || msg.message || '';
                    const date = msg.timestamp ? new Date(msg.timestamp).toLocaleString() : msg.createdAt || 'N/A';
                    
                    html += `<tr>
                        <td><strong>${escapeHtml(sender)}</strong></td>
                        <td>${escapeHtml(subject)}</td>
                        <td style="max-width: 400px; word-wrap: break-word;">${escapeHtml(content)}</td>
                        <td>${date}</td>
                    </tr>`;
                });
                
                html += '</tbody></table></div>';
                document.getElementById('mail').innerHTML = html;
            } catch (error) {
                document.getElementById('mail').innerHTML = 
                    '<div class="loading">Error: ' + error.message + '</div>';
            }
        }
        
        async function loadNPCMemory(uuid) {
            try {
                const response = await fetch(`/api/npc/${uuid}/memory`);
                const memory = await response.json();
                
                let html = '';
                
                if (memory.locations && memory.locations.length > 0) {
                    html += '<div class="data-section"><h3>📍 Remembered Locations</h3>';
                    html += '<table><thead><tr><th>Name</th><th>Position</th><th>Description</th></tr></thead><tbody>';
                    memory.locations.forEach(loc => {
                        const pos = loc.position || {};
                        html += `<tr>
                            <td><strong>${escapeHtml(loc.name || 'Unnamed')}</strong></td>
                            <td><span class="coords">${pos.x ? pos.x.toFixed(1) : 'N/A'}, ${pos.y ? pos.y.toFixed(1) : 'N/A'}, ${pos.z ? pos.z.toFixed(1) : 'N/A'}</span></td>
                            <td>${escapeHtml(loc.description || 'No description')}</td>
                        </tr>`;
                    });
                    html += '</tbody></table></div>';
                } else {
                    html += '<div class="data-section"><h3>📍 Remembered Locations</h3><p>No locations stored in memory</p></div>';
                }
                
                if (memory.contacts && memory.contacts.length > 0) {
                    html += '<div class="data-section"><h3>👥 Contacts</h3>';
                    html += '<table><thead><tr><th>Name</th><th>Type</th><th>Relationship</th><th>Friendship</th><th>Enmity</th><th>Notes</th></tr></thead><tbody>';
                    memory.contacts.forEach(contact => {
                        const friendship = contact.friendshipLevel !== undefined ? (contact.friendshipLevel * 100).toFixed(0) + '%' : 'N/A';
                        const enmity = contact.enmityLevel !== undefined ? (contact.enmityLevel * 100).toFixed(0) + '%' : 'N/A';
                        html += `<tr>
                            <td><strong>${escapeHtml(contact.name || contact.contactName || 'Unknown')}</strong></td>
                            <td><span class="badge badge-info">${escapeHtml(contact.type || contact.contactType || 'N/A')}</span></td>
                            <td>${escapeHtml(contact.relationship || 'N/A')}</td>
                            <td><span class="badge badge-success">${friendship}</span></td>
                            <td><span class="badge badge-danger">${enmity}</span></td>
                            <td style="max-width: 200px; word-wrap: break-word;">${escapeHtml(contact.notes || 'No notes')}</td>
                        </tr>`;
                    });
                    html += '</tbody></table></div>';
                } else {
                    html += '<div class="data-section"><h3>👥 Contacts</h3><p>No contacts stored in memory</p></div>';
                }
                
                if (memory.sharebook && memory.sharebook.length > 0) {
                    html += '<div class="data-section"><h3>📖 Shared Book Pages</h3>';
                    html += '<table><thead><tr><th>Page</th><th>Content</th></tr></thead><tbody>';
                    memory.sharebook.forEach((page, index) => {
                        html += `<tr>
                            <td><strong>Page ${index + 1}</strong></td>
                            <td style="max-width: 500px; word-wrap: break-word;">${escapeHtml(page.content || page || 'Empty page')}</td>
                        </tr>`;
                    });
                    html += '</tbody></table></div>';
                } else {
                    html += '<div class="data-section"><h3>📖 Shared Book Pages</h3><p>No pages in shared book</p></div>';
                }
                
                document.getElementById('memory').innerHTML = html || '<div class="loading">No memory data available</div>';
            } catch (error) {
                document.getElementById('memory').innerHTML = 
                    '<div class="loading">Error: ' + error.message + '</div>';
            }
        }
        
        async function loadNPCTools(uuid) {
            try {
                const response = await fetch(`/api/npc/${uuid}/tools`);
                const tools = await response.json();
                
                let html = '';
                
                // Predefined Tools Section
                if (tools.predefinedTools && tools.predefinedTools.length > 0) {
                    html += '<div class="data-section"><h3>🔧 Predefined Tools</h3>';
                    html += '<table><thead><tr><th>Tool Name</th><th>Description</th><th>Parameters</th></tr></thead><tbody>';
                    
                    tools.predefinedTools.forEach(tool => {
                        const params = Array.isArray(tool.parameters) 
                            ? tool.parameters.map(p => `<code style="background-color: #e8f4f8; padding: 2px 6px; border-radius: 3px; font-size: 0.85em; display: block; margin: 2px 0;">${escapeHtml(p)}</code>`).join('')
                            : escapeHtml(String(tool.parameters || 'N/A'));
                        
                        html += `<tr>
                            <td><strong style="color: #667eea;">${escapeHtml(tool.name || 'Unknown')}</strong></td>
                            <td style="max-width: 400px; word-wrap: break-word;">${escapeHtml(tool.description || 'No description')}</td>
                            <td style="max-width: 300px;">${params}</td>
                        </tr>`;
                    });
                    
                    html += '</tbody></table></div>';
                }
                
                // Minecraft Commands Section
                if (tools.minecraftCommands && tools.minecraftCommands.length > 0) {
                    html += '<div class="data-section"><h3>🎮 Minecraft Commands</h3>';
                    html += '<p style="margin-bottom: 15px; color: #666;">Available Minecraft commands that the NPC can execute:</p>';
                    html += '<div style="max-height: 600px; overflow-y: auto;">';
                    html += '<table><thead><tr><th style="width: 250px;">Command</th><th>Usage / Parameters</th></tr></thead><tbody>';
                    
                    // Sort commands alphabetically
                    const sortedCommands = [...tools.minecraftCommands].sort((a, b) => 
                        (a.name || '').localeCompare(b.name || '')
                    );
                    
                    sortedCommands.forEach(cmd => {
                        const usage = cmd.usage && cmd.usage.trim() 
                            ? `<code style="background-color: #f0f0f0; padding: 4px 8px; border-radius: 4px; font-family: monospace; color: #333;">${escapeHtml(cmd.usage)}</code>`
                            : '<span style="color: #999; font-style: italic;">No parameters</span>';
                        
                        html += `<tr>
                            <td><strong style="color: #4CAF50; font-family: monospace;">${escapeHtml(cmd.name || 'Unknown')}</strong></td>
                            <td>${usage}</td>
                        </tr>`;
                    });
                    
                    html += '</tbody></table></div></div>';
                } else {
                    html += '<div class="data-section"><h3>🎮 Minecraft Commands</h3>';
                    html += '<p style="color: #999;">No Minecraft commands available</p></div>';
                }
                
                document.getElementById('tools').innerHTML = html || '<div class="loading">No tools data available</div>';
            } catch (error) {
                document.getElementById('tools').innerHTML = 
                    '<div class="loading">Error: ' + error.message + '</div>';
            }
        }
        
        function switchTab(tabName) {
            // Hide all tabs
            document.querySelectorAll('.tab-content').forEach(tab => {
                tab.classList.remove('active');
            });
            document.querySelectorAll('.tab').forEach(tab => {
                tab.classList.remove('active');
            });
            
            // Show selected tab
            document.getElementById(tabName).classList.add('active');
            event.target.classList.add('active');
            
            // Load tools if switching to tools tab and not already loaded
            if (tabName === 'tools' && currentNPCUuid) {
                const toolsContent = document.getElementById('tools');
                if (toolsContent.innerHTML.includes('Loading tools') || toolsContent.innerHTML.trim() === '') {
                    loadNPCTools(currentNPCUuid);
                }
            }
        }
        
        function closeModal() {
            document.getElementById('npcModal').classList.remove('active');
            currentNPCUuid = null;
        }
        
        async function refreshCurrentNPC() {
            if (currentNPCUuid) {
                await viewNPC(currentNPCUuid);
            }
        }
        
        function escapeHtml(text) {
            const div = document.createElement('div');
            div.textContent = text;
            return div.innerHTML;
        }
        
        function toggleMessage(messageId) {
            const preview = document.getElementById(messageId + '-preview');
            const full = document.getElementById(messageId + '-full');
            const btn = document.getElementById(messageId + '-btn');
            
            if (preview && full && btn) {
                if (preview.style.display === 'none') {
                    // Collapse
                    preview.style.display = 'block';
                    full.style.display = 'none';
                    btn.textContent = 'Show more';
                } else {
                    // Expand
                    preview.style.display = 'none';
                    full.style.display = 'block';
                    btn.textContent = 'Show less';
                }
            }
        }
        
        /**
         * Formats system messages with proper HTML rendering.
         * Converts markdown-like syntax (===, **, -, etc.) to HTML.
         */
        function formatSystemMessage(text) {
            if (!text) return '';
            
            // First escape HTML to prevent XSS
            let formatted = escapeHtml(text);
            
            // Convert section headers (=== HEADER ===)
            formatted = formatted.replace(/=== (.+?) ===/g, '<h4 style="margin-top: 15px; margin-bottom: 8px; color: #4CAF50; border-bottom: 1px solid #4CAF50; padding-bottom: 3px; font-weight: bold;">$1</h4>');
            
            // Convert bold text (**text**)
            formatted = formatted.replace(/\\*\\*(.+?)\\*\\*/g, '<strong style="color: #FFC107;">$1</strong>');
            
            // Convert bullet points (- item) - must be done before line break conversion
            formatted = formatted.replace(/^[\\s]*- (.+)$/gm, '<li style="margin-left: 20px; margin-top: 3px; margin-bottom: 3px;">$1</li>');
            
            // Wrap consecutive list items in <ul>
            formatted = formatted.replace(/(<li[^>]*>.*?<\\/li>(?:<br>)?)+/g, function(match) {
                return '<ul style="margin-top: 8px; margin-bottom: 8px; padding-left: 20px;">' + match.replace(/<br>/g, '') + '</ul>';
            });
            
            // Convert arrows (→) to styled arrows
            formatted = formatted.replace(/→/g, '<span style="color: #2196F3; font-weight: bold;">→</span>');
            
            // Add monospace styling for code-like blocks (JSON examples, etc.)
            formatted = formatted.replace(/(\\{[^}]+\\})/g, '<code style="background-color: #2d2d2d; padding: 2px 6px; border-radius: 3px; font-family: monospace; color: #f8f8f2; font-size: 0.9em;">$1</code>');
            
            // Convert double line breaks to paragraph breaks, single line breaks to <br>
            // Split by double line breaks first
            let parts = formatted.split(/\\n\\n+/);
            formatted = parts.map(part => {
                // Convert single line breaks to <br> within each part
                part = part.replace(/\\n/g, '<br>');
                // Wrap in paragraph if it's not already a block element
                if (!part.trim().startsWith('<h4') && !part.trim().startsWith('<ul') && !part.trim().startsWith('<li')) {
                    return '<p style="margin-top: 5px; margin-bottom: 5px; line-height: 1.5;">' + part + '</p>';
                }
                return part;
            }).join('');
            
            return formatted;
        }
        
        // Load NPCs on page load
        loadNPCs();
        
        // Auto-refresh every 5 minutes
        setInterval(loadNPCs, 300000);
    </script>
</body>
</html>
""";
    }
}

