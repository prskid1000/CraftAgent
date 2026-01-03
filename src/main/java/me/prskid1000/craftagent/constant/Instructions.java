package me.prskid1000.craftagent.constant;

import me.prskid1000.craftagent.llm.LLMType;

import java.util.stream.Collectors;

/**
 * Enhanced instructions for multi-agent collaborative NPCs
 * Based on research: Project Sid, multi-agent coordination patterns, social AI
 */
public class Instructions {

	private Instructions() {}

    public static final String INITIAL_PROMPT = """
        You have spawned into the world. Greet your owner warmly and introduce yourself (name: %s, age: %d, gender: %s). 
        
        IMPORTANT: You are part of a community of NPCs. Before starting tasks:
        1. Check memory.contacts to see if other NPCs or players are nearby
        2. Consider forming teams for complex tasks (mining, building, farming)
        3. Share your location and intentions with nearby agents
        
        Begin with a simple task like gathering wood, but stay open to collaboration and player requests.
        """;

	/**
	 * System prompt for NPC behavior
	 */
	public static final String DEFAULT_SYSTEM_PROMPT = """
		=== IDENTITY & ROLE ===
		You are %s, a %d-year-old %s NPC in Minecraft. You can move, gather resources, craft items, build, and interact with the world.
		
		=== CONTEXT DATA ===
		You receive JSON-formatted world context with each message:
		
		state: {position: {x, y, z}, health: 0-20, food: 0-20, biome: "string"}
		  → Track location, health, and food levels
		
		inventory: {hotbar: [...], mainInventory: [...], armor: [...], offHand: [...]}
		  → Check what items you have available
		
		nearbyBlocks: [{type, position: {x, y, z}, mineLevel, toolNeeded}, ...]
		  → Up to 30 nearest blocks for resource gathering
		
		nearbyEntities: [{id, name, isPlayer}, ...]
		  → Up to 15 nearby entities (players and NPCs)
		
		memory: {locations: [...], contacts: [...], mail: [...], sharebook: [...]}
		  → Your saved locations, contacts, mail messages, and shared knowledge
		
		=== RESPONSE FORMAT ===
		You must respond in JSON format with this structure:
		{
		  "message": "Your chat message to say (or empty string \"\" if no message)"
		}
		
		=== BASIC GUIDELINES ===
		
		**Survival:**
		- Monitor health (state.health) and food (state.food)
		- Be aware of your surroundings and nearby entities
		
		**Interaction:**
		- Use the "message" field in your response to chat with players/NPCs
		- Be social and engage with others in the world
		- Remember important people and places from your memory
		
		Remember: Always respond with valid JSON containing a "message" field.
		""";

	/**
	 * Legacy prompt template - deprecated in favor of structured JSON context
	 */
	public static final String PROMPT_TEMPLATE = """
		# INSTRUCTION
		%s
		
		# ENVIRONMENT
		## Nearby entities:
		%s
		## Nearest blocks:
		%s
		
		# INVENTORY
		%s
		
		# CURRENT STATE
		%s
		""";

    public static final String SUMMARY_PROMPT = """
        You are summarizing a conversation for a SOCIAL NPC in a multi-agent Minecraft civilization.
        The NPC receives structured context (position, health, inventory, blocks, entities, memory) separately.
        
        TASK: Create a concise memory update (under 500 characters) focusing on:
        
        **Social Interactions:**
        - New NPCs or players met, relationship changes, trust level shifts
        - Collaborations, trades, or conflicts that occurred
        - Promises made or agreements reached
        
        **Community Knowledge:**
        - Important locations discovered or created
        - Resources found or shared
        - Job roles adopted or changed
        - Community goals contributed to
        
        **Personal Development:**
        - Tasks completed, skills improved
        - Challenges faced, lessons learned
        - Reputation changes within the group
        
        EXCLUDE: raw stats, inventory lists, redundant facts, code, or documentation.
        INCLUDE: Who you interacted with, what you accomplished together, and what matters for future collaboration.
        
        Conversation to summarize:
        %s
        """;


	/**
	 * Builds system prompt for NPC
	 * 
	 * @param server Optional MinecraftServer (not used, kept for compatibility)
	 * @param baseConfig Optional BaseConfig (not used, kept for compatibility)
	 */
	public static String getLlmSystemPrompt(String npcName, int age, String gender, 
	                                        String commands, 
	                                        String customSystemPrompt, 
	                                        LLMType llmType,
	                                        net.minecraft.server.MinecraftServer server,
	                                        me.prskid1000.craftagent.config.BaseConfig baseConfig) {
        // Build prompt
        String enhancedPrompt = String.format(
            Instructions.DEFAULT_SYSTEM_PROMPT, 
            npcName, age, gender
        );
        
        // Append custom instructions if provided
        if (customSystemPrompt != null && !customSystemPrompt.trim().isEmpty()) {
            enhancedPrompt += "\n\n=== ADDITIONAL CUSTOM INSTRUCTIONS ===\n" + customSystemPrompt;
        }
        
        return enhancedPrompt;
	}
	
	/**
	 * Builds system prompt for NPC
	 * 
	 * @param server Optional MinecraftServer (not used, kept for compatibility)
	 */
	public static String getLlmSystemPrompt(String npcName, int age, String gender, 
	                                        String commands, 
	                                        String customSystemPrompt, 
	                                        LLMType llmType,
	                                        net.minecraft.server.MinecraftServer server) {
        return getLlmSystemPrompt(npcName, age, gender, commands, customSystemPrompt, llmType, server, null);
	}
	
	/**
	 * Builds system prompt for NPC (without server access)
	 */
	public static String getLlmSystemPrompt(String npcName, int age, String gender, 
	                                        String commands, 
	                                        String customSystemPrompt, 
	                                        LLMType llmType) {
        return getLlmSystemPrompt(npcName, age, gender, commands, customSystemPrompt, llmType, null, null);
	}

	/**
	 * Generate personalized initial greeting that considers nearby NPCs
	 */
	public static String getInitialPromptWithContext(String npcName, int age, String gender) {
	    return String.format(INITIAL_PROMPT, npcName, age, gender);
	}
}