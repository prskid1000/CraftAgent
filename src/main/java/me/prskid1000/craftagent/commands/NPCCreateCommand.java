package me.prskid1000.craftagent.commands;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import lombok.AllArgsConstructor;
import me.prskid1000.craftagent.auth.UsernameValidator;
import me.prskid1000.craftagent.common.NPCService;
import me.prskid1000.craftagent.config.NPCConfig;
import me.prskid1000.craftagent.llm.LLMType;
import me.prskid1000.craftagent.util.LogUtil;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

@AllArgsConstructor
public class NPCCreateCommand {

	private static final String LLM_TYPE = "llm-type";

	private final NPCService npcService;

	public LiteralArgumentBuilder<ServerCommandSource> getCommand() {
		return literal("add")
				.requires(source -> source.hasPermissionLevel(2))
				.then(argument("name", StringArgumentType.string())
						.then(argument(LLM_TYPE, StringArgumentType.string())
								.suggests((context, builder) -> {
									for (LLMType llmType : LLMType.getEntries()) {
										builder.suggest(llmType.toString());
									}
									return builder.buildFuture();
								}).executes(this::createNpcWithLLM)));
	}

	private int createNpcWithLLM(CommandContext<ServerCommandSource> context) {
		try {
			if (context == null || context.getSource() == null) {
				LogUtil.error("Invalid command context");
				return 0;
			}
			
			ServerPlayerEntity source = context.getSource().getPlayer();
			if (source == null) {
				context.getSource().sendFeedback(() -> LogUtil.formatError("Command must be executed as a Player!"), false);
				return 0;
			}

			String name = StringArgumentType.getString(context, "name");
			
			// Validate input before proceeding
			if (!UsernameValidator.isValid(name)) {
				LogUtil.error("Invalid NPC name: " + name);
				context.getSource().sendFeedback(() -> LogUtil.formatError("Invalid NPC name! Use 3–16 characters: letters, numbers, or underscores only."), false);
				return 0;
			}
			
			String llmTypeStr = StringArgumentType.getString(context, LLM_TYPE);
			LLMType llmType;
			
			try {
				llmType = LLMType.valueOf(llmTypeStr);
			} catch (IllegalArgumentException e) {
				LogUtil.error("Invalid LLM type: $llmTypeStr");
				context.getSource().sendFeedback(() -> LogUtil.formatError("Invalid LLM type!"), false);
				return 0;
			}

			NPCConfig config = NPCConfig.builder(name).llmType(llmType).build();
			npcService.createNpc(config, source.getWorld().getServer(), source.getBlockPos(), source);
			LogUtil.info("Created new NPC: $name");
			return 1;
		} catch (Exception e) {
			LogUtil.error("Error creating NPC", e);
			context.getSource().sendFeedback(() -> LogUtil.formatError("Failed to create NPC"), false);
			return 0;
		}
	}

}
