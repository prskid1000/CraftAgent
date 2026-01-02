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
                case "messages":
                    handleGetNPCMessages(npc, exchange);
                    break;
                case "mail":
                    handleGetNPCMail(npc, uuid, exchange);
                    break;
                case "memory":
                    handleGetNPCMemory(npc, exchange);
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
        
        map.put("nearbyBlocks", context.nearbyBlocks().size());
        map.put("nearbyEntities", context.nearbyEntities().size());
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
            min-height: 100vh;
            padding: 20px;
            color: #333;
        }
        .container {
            max-width: 1400px;
            margin: 0 auto;
        }
        header {
            background: rgba(255, 255, 255, 0.95);
            padding: 20px;
            border-radius: 15px;
            margin-bottom: 20px;
            box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
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
            border-radius: 15px;
            padding: 20px;
            box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
            cursor: pointer;
            transition: transform 0.3s, box-shadow 0.3s;
        }
        .npc-card:hover {
            transform: translateY(-5px);
            box-shadow: 0 12px 40px rgba(0, 0, 0, 0.15);
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
            padding: 12px;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            border: none;
            border-radius: 8px;
            font-size: 1em;
            font-weight: bold;
            cursor: pointer;
            transition: opacity 0.3s;
        }
        .view-details-btn:hover {
            opacity: 0.9;
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
            border-radius: 15px;
            padding: 30px;
            max-width: 1200px;
            width: 100%;
            margin: 20px auto;
            max-height: 90vh;
            overflow-y: auto;
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
            background: #f8f9fa;
            padding: 15px;
            border-radius: 8px;
            border-left: 4px solid #667eea;
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
                <button class="tab" onclick="switchTab('context')">Context</button>
                <button class="tab" onclick="switchTab('messages')">Messages</button>
                <button class="tab" onclick="switchTab('mail')">Mail</button>
                <button class="tab" onclick="switchTab('memory')">Memory</button>
            </div>
            
            <div id="overview" class="tab-content active">
                <div class="data-section">
                    <h3>Basic Information</h3>
                    <div class="data-grid" id="overviewGrid"></div>
                </div>
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
                loadNPCContext(uuid),
                loadNPCMessages(uuid),
                loadNPCMail(uuid),
                loadNPCMemory(uuid)
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
        
        async function loadNPCContext(uuid) {
            try {
                const response = await fetch(`/api/npc/${uuid}/context`);
                const context = await response.json();
                document.getElementById('context').innerHTML = 
                    '<div class="json-viewer">' + JSON.stringify(context, null, 2) + '</div>';
            } catch (error) {
                document.getElementById('context').innerHTML = 
                    '<div class="loading">Error: ' + error.message + '</div>';
            }
        }
        
        async function loadNPCMessages(uuid) {
            try {
                const response = await fetch(`/api/npc/${uuid}/messages`);
                const messages = await response.json();
                document.getElementById('messages').innerHTML = 
                    '<div class="json-viewer">' + JSON.stringify(messages, null, 2) + '</div>';
            } catch (error) {
                document.getElementById('messages').innerHTML = 
                    '<div class="loading">Error: ' + error.message + '</div>';
            }
        }
        
        async function loadNPCMail(uuid) {
            try {
                const response = await fetch(`/api/npc/${uuid}/mail`);
                const mail = await response.json();
                document.getElementById('mail').innerHTML = 
                    '<div class="json-viewer">' + JSON.stringify(mail, null, 2) + '</div>';
            } catch (error) {
                document.getElementById('mail').innerHTML = 
                    '<div class="loading">Error: ' + error.message + '</div>';
            }
        }
        
        async function loadNPCMemory(uuid) {
            try {
                const response = await fetch(`/api/npc/${uuid}/memory`);
                const memory = await response.json();
                document.getElementById('memory').innerHTML = 
                    '<div class="json-viewer">' + JSON.stringify(memory, null, 2) + '</div>';
            } catch (error) {
                document.getElementById('memory').innerHTML = 
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
        
        // Load NPCs on page load
        loadNPCs();
        
        // Auto-refresh every 5 seconds
        setInterval(loadNPCs, 5000);
    </script>
</body>
</html>
""";
    }
}

