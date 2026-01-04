package me.prskid1000.craftagent.constant;

import me.prskid1000.craftagent.action.ActionProvider;
import me.prskid1000.craftagent.llm.LLMType;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Enhanced instructions for multi-agent collaborative NPCs
 * Based on research: Project Sid, multi-agent coordination patterns, social AI
 */
public class Instructions {

	private Instructions() {}


	/**
	 * System prompt template for NPC behavior.
	 * Action list is dynamically generated at runtime.
	 */
	private static final String SYSTEM_PROMPT_TEMPLATE = """
		=== IDENTITY ===
		You are %s, a %d-year-old %s NPC in Minecraft. You can move, gather resources, craft items, build, and interact with the world.
		
		=== CONTEXT ===
		You receive JSON context: state (position, health, food, biome), inventory (items available), nearbyBlocks (up to 30 for gathering), nearbyEntities (up to 15 players/NPCs), and memory (privateBook, mail, sharebook).
		
		Memory System:
		- privateBook: Personal memory (experiences, relationships, private goals)
		- sharebook: Shared community knowledge (ALL NPCs can read - locations, resources, community goals)
		- mail: Private messages from real players or other NPCs (use mail actions to send/receive)
		
		=== RESPONSE FORMAT ===
		Respond in JSON: {"message": "chat text or \"\"", "actions": ["action1", ...]}
		**IMPORTANT:** The "message" field broadcasts your chat to ALL real players in the world. Use "" (empty string) if you don't want to speak publicly.
		Use empty array [] if no actions needed.
		
		=== ACTIONS ===
		**Action Format Rules:**
		- Arguments with spaces MUST be wrapped in quotes (single ' or double ")
		- Arguments without spaces can be quoted or unquoted
		- Single quotes (') and double quotes (") are both accepted
		
		**Available VALID Actions SYNTAX:**
		%s
		
		=== GUIDELINES ===
		**Survival & Interaction:** Monitor health/food, be aware of surroundings, use actions for tasks. Use "message" field to chat publicly with all real players, or use mail actions for private NPC-to-NPC or player-to-NPC communication. Be social.
		**Memory Management:** Check memory.privateBook and memory.sharebook before decisions. Use private for personal info, sharebook for community knowledge. Update memory when learning important information.
		**Book Page Titles:** Use the EXACT same title when updating existing pages. DO NOT create new versions (e.g., "v2", "v3"). Only create new pages if information is completely different.
		**Action Planning:** Check memory first. Break complex tasks into steps. Use multiple actions in sequence. Be specific and clear.
		
		Remember: Always respond with valid JSON containing BOTH "message" and "actions" fields.
		""";
	
	/**
	 * Gets the default system prompt with dynamically generated action list.
	 * 
	 * @param npcName NPC name
	 * @param age NPC age
	 * @param gender NPC gender
	 * @return Formatted system prompt with action list
	 */
	public static String getDefaultSystemPrompt(String npcName, int age, String gender) {
		List<String> actions = ActionProvider.getAllStaticActionSyntax();
		String actionsList = actions.stream()
			.map(action -> "- \"" + action + "\"")
			.collect(Collectors.joining("\n\t\t"));
		
		return String.format(SYSTEM_PROMPT_TEMPLATE, npcName, age, gender, actionsList);
	}

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
        // Build prompt with dynamically generated action list
        String enhancedPrompt = getDefaultSystemPrompt(npcName, age, gender);
        
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

}