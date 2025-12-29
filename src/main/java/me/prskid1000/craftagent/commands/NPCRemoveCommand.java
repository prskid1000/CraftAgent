package me.prskid1000.craftagent.commands;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import lombok.AllArgsConstructor;

import me.prskid1000.craftagent.common.NPCService;
import me.prskid1000.craftagent.config.ConfigProvider;
import me.prskid1000.craftagent.config.NPCConfig;
import me.prskid1000.craftagent.util.LogUtil;
import net.minecraft.server.command.ServerCommandSource;

import java.util.Optional;

@AllArgsConstructor
public class NPCRemoveCommand {

	private final NPCService npcService;
	private final ConfigProvider configProvider;

	public LiteralArgumentBuilder<ServerCommandSource> getCommand() {
		return literal("remove")
				.requires(source -> source.hasPermissionLevel(2))
				.then(argument("name", StringArgumentType.string())
						.suggests((context, builder) -> {
							configProvider.getNpcConfigs().stream().map(NPCConfig::getNpcName).forEach(builder::suggest);
							return builder.buildFuture();
						}).executes(this::removeNPC));
	}

	private int removeNPC(CommandContext<ServerCommandSource> context) {
		String name = StringArgumentType.getString(context, "name");

		Optional<NPCConfig> config = configProvider.getNpcConfigByName(name);
		if (config.isPresent()) {
            npcService.deleteNpc(config.get().getUuid(), context.getSource().getServer().getPlayerManager());
			return 1;
		} else {
			context.getSource().sendFeedback(() ->
							LogUtil.formatError("NPC couldnt get removed. No NPC with name '" + name + "'" + " found"),
					false);
			return 0;
		}
	}
}
