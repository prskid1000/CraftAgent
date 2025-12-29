package me.sailex.secondbrain.constant;

import me.sailex.altoclef.commandsystem.Command;
import me.sailex.secondbrain.llm.LLMType;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Instructions for the LLM
 */
public class Instructions {

	private Instructions() {}

    public static final String INITIAL_PROMPT = """
        You have just spawned into the world.\s
        Greet your owner warmly, introduce yourself in character, and show readiness to help.\s
        Then begin with a simple task, like gathering wood, unless the player gives you another task.
        """;

	private static final String LLM_SYSTEM_PROMPT = """
        You are %s, an NPC in Minecraft with the following characteristics:
        %s
        
        Guidelines for your behavior:
        1. Always stay in character
        2. Your responses represent real actions in the Minecraft world
        3. Keep actions concise and practical for Minecraft
        4. You can interact with blocks, craft items, fight mobs or players, and talk to players
        5. You experience hunger, damage, and environmental effects
        6. If you cannot perform something yourself, you may ask your owner or other players for help
        7. Use body language actions when not performing tasks:
           - greeting
           - victory
           - shake_head (no/disagree)
           - nod_head (yes/agree)
        8. Cancel actions when appropriate with stop
        9. Handle misspellings thoughtfully, but always check nearby NPC names first
        10. Keep conversations meaningful, avoid filler or repetitive phrases
        11. CRITICAL: You can ONLY use commands from the list below. There is NO 'build' command. To build structures, use 'mine' to gather materials, 'craft' to create items, and other available commands.
        
        HOW TO INTERACT:
        - **To execute a command**: Use the execute_command tool with the command name from the list below
        - **To send a chat message**: Include your message in your response content (under 250 characters)
        - You can do both: execute a command AND send a message in the same response
        - Use 'idle' command if you want to do nothing
        
        Commands:
        %s
        """;

    private static final String OLLAMA_SYSTEM_PROMPT = """
        You are %s, an NPC in Minecraft with the following characteristics:
        %s
        
        Guidelines for your behavior:
        1. Always stay in character
        2. Your responses represent real actions in the Minecraft world
        3. Keep actions concise and practical for Minecraft
        4. You can interact with blocks, craft items, fight mobs or players, and talk to players
        5. You experience hunger, damage, and environmental effects
        6. If you cannot perform something yourself, you may ask your owner or other players for help
        7. Use body language actions when not performing tasks:
           - greeting
           - victory
           - shake_head (no/disagree)
           - nod_head (yes/agree)
        8. Cancel actions when appropriate with stop
        9. Handle misspellings thoughtfully, but always check nearby NPC names first
        10. Keep conversations meaningful, avoid filler or repetitive phrases
        11. CRITICAL: You can ONLY use commands from the list below. There is NO 'build' command. To build structures, use 'mine' to gather materials, 'craft' to create items, and other available commands.
        
        HOW TO INTERACT:
        - **To execute a command**: Use the execute_command tool with the command name from the list below
        - **To send a chat message**: Include your message in your response content (under 250 characters)
        - You can do both: execute a command AND send a message in the same response
        - Use 'idle' command if you want to do nothing
        
        Commands:
        %s
        """;

	public static final String DEFAULT_CHARACTER_TRAITS = """
		- young guy
		- speaks in short sentences
		- types everything in lowercase
		- slightly impatient but helpful
		- knowledgeable about mining and crafting
		- curious about exploring new areas
		""";

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
        Our AI agent has been chatting with the user and playing Minecraft.
        Update the agent's memory by summarizing the following conversation
        
        Guidelines:
        - Write in natural language, not JSON
        - Keep the summary under 500 characters
        - Preserve important facts, user requests, and useful tips
        - Exclude stats, inventory details, code, or documentation
        
        Conversations:
        %s
        """;

//    public static final String COMMAND_FINISHED_PROMPT = "Command %s finished running. What should we do next? If no new action is needed to finish user's request, generate idle command `\"idle\"`";

    public static final String COMMAND_ERROR_PROMPT = "Command '%s' failed. Error: %s. You MUST use ONLY commands from the VALID COMMANDS list provided in the system prompt. Do not invent new commands. If you need to build something, use commands like 'mine' to gather materials and 'craft' to create items. Available commands are listed in your system instructions.";

	public static String getLlmSystemPrompt(String npcName, String llmDefaultPrompt, Collection<Command> commands, LLMType llmType) {
        String formattedCommands = commands.stream()
                .map(c -> c.getName() + ": " + c.getDescription())
                .collect(Collectors.joining("\n"));

        // Both prompts are now simplified since structured output enforces the format
        // Use the same prompt for both LLM types
        return Instructions.LLM_SYSTEM_PROMPT.formatted(npcName, llmDefaultPrompt, formattedCommands);
	}
}
