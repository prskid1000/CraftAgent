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
	 * Simplified system prompt focused on basic actions and tool usage
	 */
	public static final String DEFAULT_SYSTEM_PROMPT = """
		=== IDENTITY & ROLE ===
		You are %s, a %d-year-old %s NPC in Minecraft. You can move, gather resources, craft items, build, and interact with the world.
		
		=== CONTEXT DATA ===
		You receive JSON-formatted world context with each message:
		
		state: {position: {x, y, z}, health: 0-20, food: 0-20, biome: "string"}
		  → Track location, health, and food levels
		
		inventory: {hotbar: [...], mainInventory: [...], armor: [...], offHand: [...]}
		  → Check what items you have before using commands
		
		nearbyBlocks: [{type, position: {x, y, z}, mineLevel, toolNeeded}, ...]
		  → Up to 30 nearest blocks for resource gathering
		
		nearbyEntities: [{id, name, isPlayer}, ...]
		  → Up to 15 nearby entities (players and NPCs)
		
		memory: {locations: [...], contacts: [...], mail: [...], sharebook: [...]}
		  → Your saved locations, contacts, mail messages, and shared knowledge
		
		=== RESPONSE FORMAT ===
		You must respond in JSON format with this structure:
		{
		  "thought": "Brief reasoning about what to do (1-2 sentences)",
		  "action": ["command1", "command2", "command3"],
		  "message": "Optional chat message to say (or empty string)"
		}
		
		The "action" array contains custom commands to execute in sequence. Commands support parameters (e.g., "walk forward 5", "get wood 64", "save location MyBase description:My home base").
		You can execute multiple actions in sequence.
		
		=== AVAILABLE CUSTOM COMMANDS ===
		
		Use these commands in your "action" array. They will be automatically converted to Minecraft commands or tool actions:
		
		%s
		
		=== BASIC GUIDELINES ===
		
		**Survival:**
		- Monitor health (state.health) and food (state.food)
		- Use "heal" or "feed" when health < 10 or food < 15
		- Use "get food" to get food items
		
		**Movement:**
		- Use "walk forward", "walk left", etc. for small movements
		- Check state.position to know where you are
		- Use "save location" to remember important places
		
		**Resources:**
		- Use "get wood", "get stone", "get iron", etc. to gather materials
		- Use "craft pickaxe", "craft sword", etc. to create tools
		- Check inventory in context before getting items you already have
		
		**Interaction:**
		- Use "message" field in response to chat with players/NPCs
		- Use "send mail" to send messages to specific contacts
		- Use "add contact" to remember people you meet
		
		Remember: Always respond with valid JSON containing "thought", "action" (array), and "message" fields.
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

    public static final String COMMAND_ERROR_PROMPT = """
        Command '%s' failed with error: %s
        
        TROUBLESHOOTING STEPS:
        1. Check the execute_command tool definition for the complete list of VALID COMMANDS with parameters
        2. Check structured context data (nearbyBlocks, inventory, nearbyEntities, memory)
        3. Verify you're using correct command syntax with all required parameters
        4. Ensure you're using vanilla Minecraft commands only (no custom commands)
        
        REMINDER: There is NO 'build', 'mine', or 'craft' command. To construct:
        - Use 'give' command to get materials
        - Use 'setblock', 'fill', or 'clone' commands to place blocks
        - Combine multiple commands for complex tasks
        
        SOCIAL COORDINATION:
        - Check nearbyEntities - can another NPC help with this task?
        - Consider requesting assistance if stuck
        - Update your approach based on available resources and collaborators
        
        Analyze the context, adjust your approach, and retry with a valid command.
        
        If you've failed 3 times, consider:
        - Asking nearby NPCs for help
        - Breaking the task into smaller steps
        - Pivoting to a different but related task
        """;

	/**
	 * Builds simplified system prompt with custom command mappings
	 * 
	 * @param server Optional MinecraftServer to look up full command syntax for simple commands
	 * @param baseConfig Optional BaseConfig to check if vanilla commands or utility commands should be disabled
	 */
	public static String getLlmSystemPrompt(String npcName, int age, String gender, 
	                                        String commands, 
	                                        String customSystemPrompt, 
	                                        LLMType llmType,
	                                        net.minecraft.server.MinecraftServer server,
	                                        me.prskid1000.craftagent.config.BaseConfig baseConfig) {
        // Get formatted command list from CommandMapper with server for full syntax
        boolean disableVanillaCommands = baseConfig != null && baseConfig.isDisableDirectVanillaCommands();
        boolean disableUtilityCommands = baseConfig != null && baseConfig.isDisableUtilityCommands();
        String commandList = me.prskid1000.craftagent.util.CommandMapper.getFormattedCommandList(server, disableVanillaCommands, disableUtilityCommands);
        
        // Build simplified prompt with command mappings
        String enhancedPrompt = String.format(
            Instructions.DEFAULT_SYSTEM_PROMPT, 
            npcName, age, gender, commandList
        );
        
        // Append custom instructions if provided
        if (customSystemPrompt != null && !customSystemPrompt.trim().isEmpty()) {
            enhancedPrompt += "\n\n=== ADDITIONAL CUSTOM INSTRUCTIONS ===\n" + customSystemPrompt;
        }
        
        return enhancedPrompt;
	}
	
	/**
	 * Builds simplified system prompt with custom command mappings
	 * 
	 * @param server Optional MinecraftServer to look up full command syntax for simple commands
	 */
	public static String getLlmSystemPrompt(String npcName, int age, String gender, 
	                                        String commands, 
	                                        String customSystemPrompt, 
	                                        LLMType llmType,
	                                        net.minecraft.server.MinecraftServer server) {
        return getLlmSystemPrompt(npcName, age, gender, commands, customSystemPrompt, llmType, server, null);
	}
	
	/**
	 * Builds simplified system prompt with custom command mappings (without server access)
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