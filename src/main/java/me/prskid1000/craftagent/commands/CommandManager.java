package me.prskid1000.craftagent.commands;

import static net.minecraft.server.command.CommandManager.literal;

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
					.executes(context -> new GuiCommand(configProvider, networkHandler).execute(context))
			)
		);
	}
}
