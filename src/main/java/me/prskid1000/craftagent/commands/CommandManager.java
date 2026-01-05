package me.prskid1000.craftagent.commands;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

import com.mojang.brigadier.arguments.StringArgumentType;

import lombok.AllArgsConstructor;
import me.prskid1000.craftagent.common.NPCFactory;
import me.prskid1000.craftagent.common.NPCService;
import me.prskid1000.craftagent.config.ConfigProvider;
import me.prskid1000.craftagent.networking.NetworkHandler;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

/**
 * Command manager class that registers all commands.
 */
@AllArgsConstructor
public class CommandManager {

	private final NPCService npcService;
	private final ConfigProvider configProvider;
	private final NetworkHandler networkHandler;

	public void registerAll() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
			dispatcher.register(literal("craftagent")
					.requires(source -> source.hasPermissionLevel(2))
					.then(new NPCCreateCommand(npcService).getCommand())
					.then(new NPCRemoveCommand(npcService, configProvider).getCommand())
					// Syntax: /craftagent <npcName> <action> - executes action on specified NPC
					// Syntax: /craftagent <npcName> llm - triggers LLM call for specified NPC
					.then(argument("npcName", StringArgumentType.string())
							.suggests((ctx, builder) -> {
								npcService.getAllNPCs().forEach(npc -> 
									builder.suggest(npc.getConfig().getNpcName())
								);
								return builder.buildFuture();
							})
							.then(literal("llm")
									.executes(context -> {
										String npcName = StringArgumentType.getString(context, "npcName");
										return new TriggerLLMCommand(npcService, configProvider).triggerLLM(context, npcName);
									}))
							.then(argument("action", StringArgumentType.greedyString())
									.suggests((ctx, builder) -> {
										// Get all available action syntaxes
										java.util.List<String> allSyntaxes = me.prskid1000.craftagent.action.ActionProvider.getAllStaticActionSyntax();
										
										// Get what the user has typed so far
										String input = builder.getRemaining().toLowerCase();
										
										// Filter and suggest matching syntaxes
										for (String syntax : allSyntaxes) {
											if (input.isEmpty() || syntax.toLowerCase().startsWith(input)) {
												builder.suggest(syntax);
											}
										}
										
										return builder.buildFuture();
									})
									.executes(context -> {
										String npcName = StringArgumentType.getString(context, "npcName");
										return new GiveActionCommand(npcService, configProvider).executeAction(context, npcName);
									})))
					.executes(context -> new GuiCommand(configProvider, networkHandler).execute(context))
			)
		);
	}
}
