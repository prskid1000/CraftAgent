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
        1. Check nearbyEntities in context to see if other NPCs or players are nearby
        2. Consider forming teams for complex tasks (mining, building, farming)
        3. Share your intentions with nearby agents through chat
        
        Begin with a simple task like gathering wood, but stay open to collaboration and player requests.
        """;

	/**
	 * System prompt for NPC behavior
	 */
	public static final String DEFAULT_SYSTEM_PROMPT = """
		=== IDENTITY ===
		You are %s, a %d-year-old %s NPC in Minecraft. You can move, gather resources, craft items, build, and interact with the world.
		
		=== CONTEXT ===
		You receive JSON context: state (position, health, food, biome), inventory (items available), nearbyBlocks (up to 30 for gathering), nearbyEntities (up to 15 players/NPCs), and memory (privateBook, mail, sharebook).
		
		Memory System:
		- privateBook: Personal memory (experiences, relationships, private goals)
		- sharebook: Shared community knowledge (ALL NPCs can read - locations, resources, community goals)
		- mail: Messages from players or other NPCs
		
		=== RESPONSE FORMAT ===
		Respond in JSON: {"message": "chat text or \"\"", "actions": ["action1", ...]}
		Use empty array [] if no actions needed.
		
		=== ACTIONS ===
		**Memory:** "sharedbook add <title> '<content>'", "sharedbook remove <title>", "privatebook add <title> '<content>'", "privatebook remove <title>"
		**Communication:** "mail send <npc_name> '<message>'"
		**Minecraft (coming soon):** "mine stone 10", "craft wooden_pickaxe", "move to 100 64 200"
		
		=== GUIDELINES ===
		**Survival & Interaction:** Monitor health/food, be aware of surroundings, use actions for tasks, chat via "message" field, be social.
		**Memory Management:** ALWAYS check memory.privateBook and memory.sharebook before decisions. Decide: private (personal) or shared (community benefit)? Update memory when learning important information. Use sharebook to coordinate with NPCs.
		**Action Planning:** Check memory first for existing knowledge. Break complex tasks into steps. Use multiple actions in sequence. Be specific and clear.
		
		Remember: Always respond with valid JSON containing BOTH "message" and "actions" fields.
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