package com.example.vhapmod;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Commands for managing AP connection
 */
public class APCommands {
    
    private static final Logger LOGGER = LogManager.getLogger();
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("apconnect")
                .requires(source -> source.hasPermission(0)) // OP level 2
                .then(Commands.argument("host", StringArgumentType.string())
                    .then(Commands.argument("port", IntegerArgumentType.integer(1, 65535))
                        .then(Commands.argument("slotName", StringArgumentType.string())
                            .executes(ctx -> connect(ctx, ""))
                            .then(Commands.argument("password", StringArgumentType.string())
                                .executes(ctx -> connect(ctx, StringArgumentType.getString(ctx, "password")))
                            )
                        )
                    )
                )
        );
        
        dispatcher.register(
            Commands.literal("apdisconnect")
                .requires(source -> source.hasPermission(0))
                .executes(APCommands::disconnect)
        );
        
        dispatcher.register(
            Commands.literal("apstatus")
                .executes(APCommands::status)
        );

        dispatcher.register(
            Commands.literal("apdumpskills")
                .requires(source -> source.hasPermission(2))
                .executes(APCommands::dumpSkills)
        );

        dispatcher.register(
            Commands.literal("apunlock")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("skill")
                    .then(Commands.argument("name", StringArgumentType.string())
                        .executes(ctx -> unlockSkill(ctx))
                    )
                )
                .then(Commands.literal("talent")
                    .then(Commands.argument("name", StringArgumentType.string())
                        .executes(ctx -> unlockTalent(ctx))
                    )
                )
                .then(Commands.literal("expertise")
                    .then(Commands.argument("name", StringArgumentType.string())
                        .executes(ctx -> unlockExpertise(ctx))
                    )
                )
                .then(Commands.literal("mod")
                    .then(Commands.argument("name", StringArgumentType.string())
                        .executes(ctx -> unlockMod(ctx))
                    )
                )
        );
    }
    
    private static int connect(CommandContext<CommandSourceStack> ctx, String password) {
        String host = StringArgumentType.getString(ctx, "host");
        int port = IntegerArgumentType.getInteger(ctx, "port");
        String slotName = StringArgumentType.getString(ctx, "slotName");
        
        APWebSocketClient client = VaultHuntersAPMod.getAPClient();
        
        if (client == null) {
            ctx.getSource().sendFailure(new TextComponent("AP client not initialized!").withStyle(ChatFormatting.RED));
            return 0;
        }
        
        if (client.isConnected()) {
            ctx.getSource().sendFailure(new TextComponent("Already connected! Use /apdisconnect first.").withStyle(ChatFormatting.RED));
            return 0;
        }

        if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
            client.setTargetPlayer(player);
        }
        
        ctx.getSource().sendSuccess(new TextComponent(
            String.format("Connecting to %s:%d as %s...", host, port, slotName)
        ).withStyle(ChatFormatting.GREEN), true);

        APConnectionConfig config = APConnectionConfig.load();
        config.host = host;
        config.port = port;
        config.slotName = slotName;
        config.password = password != null ? password : "";
        config.autoConnect = true;
        config.save();
        
        client.connect(host, port, slotName, password).whenComplete((unused, throwable) -> {
            if (throwable == null) {
                LOGGER.info("Connection initiated from command");
                return;
            }

            String reason = throwable.getMessage() != null ? throwable.getMessage() : throwable.getClass().getSimpleName();
            ctx.getSource().getServer().execute(() ->
                ctx.getSource().sendFailure(new TextComponent("AP connection failed: " + reason).withStyle(ChatFormatting.RED))
            );
        });
        
        return 1;
    }
    
    private static int disconnect(CommandContext<CommandSourceStack> ctx) {
        APWebSocketClient client = VaultHuntersAPMod.getAPClient();
        
        if (client == null) {
            ctx.getSource().sendFailure(new TextComponent("AP client not initialized!").withStyle(ChatFormatting.RED));
            return 0;
        }
        
        if (!client.isConnected()) {
            ctx.getSource().sendFailure(new TextComponent("Not connected!").withStyle(ChatFormatting.RED));
            return 0;
        }
        
        client.disconnect();
        ctx.getSource().sendSuccess(new TextComponent("Disconnected from AP server").withStyle(ChatFormatting.GREEN), true);
        
        return 1;
    }
    
    private static int status(CommandContext<CommandSourceStack> ctx) {
        APWebSocketClient client = VaultHuntersAPMod.getAPClient();

        if (client == null) {
            ctx.getSource().sendFailure(new TextComponent("AP client not initialized!").withStyle(ChatFormatting.RED));
            return 0;
        }

        if (client.isConnected()) {
            ctx.getSource().sendSuccess(new TextComponent("Connected to AP server").withStyle(ChatFormatting.GREEN), false);
        } else {
            ctx.getSource().sendSuccess(new TextComponent("Not connected").withStyle(ChatFormatting.YELLOW), false);
        }

        return 1;
    }

    private static int dumpSkills(CommandContext<CommandSourceStack> ctx) {
        try {
            LOGGER.info("=== DUMPING ALL SKILL/TALENT/EXPERTISE IDs ===");

            net.minecraft.server.level.ServerPlayer player = ctx.getSource().getPlayerOrException();

            // Dump abilities
            Class<?> playerAbilitiesDataClass = Class.forName("iskallia.vault.world.data.PlayerAbilitiesData");
            Object abilityData = VHDataReader.getWorldData(playerAbilitiesDataClass, player.getLevel());
            java.lang.reflect.Method getAbilities = abilityData.getClass().getMethod("getAbilities", player.getUUID().getClass());
            Object abilityTree = getAbilities.invoke(abilityData, player.getUUID());

            if (abilityTree != null) {
                java.lang.reflect.Method iterate = abilityTree.getClass().getMethod("iterate", Class.class, java.util.function.Consumer.class);
                java.util.List<Object> skills = new java.util.ArrayList<>();
                java.util.function.Consumer<Object> collector = skills::add;
                Class<?> skillClass = Class.forName("iskallia.vault.skill.base.Skill");
                iterate.invoke(abilityTree, skillClass, collector);

                LOGGER.info("=== ABILITIES/SKILLS ===");
                for (Object skill : skills) {
                    java.lang.reflect.Method getId = skill.getClass().getMethod("getId");
                    java.lang.reflect.Method getName = skill.getClass().getMethod("getName");
                    String id = (String) getId.invoke(skill);
                    String name = (String) getName.invoke(skill);
                    LOGGER.info("ID: '{}', Name: '{}'", id, name);
                }
            }

            // Dump talents
            Class<?> playerTalentsDataClass = Class.forName("iskallia.vault.world.data.PlayerTalentsData");
            Object talentData = VHDataReader.getWorldData(playerTalentsDataClass, player.getLevel());
            java.lang.reflect.Method getTalents = talentData.getClass().getMethod("getTalents", player.getUUID().getClass());
            Object talentTree = getTalents.invoke(talentData, player.getUUID());

            if (talentTree != null) {
                java.lang.reflect.Method iterate = talentTree.getClass().getMethod("iterate", Class.class, java.util.function.Consumer.class);
                java.util.List<Object> talents = new java.util.ArrayList<>();
                java.util.function.Consumer<Object> collector = talents::add;
                Class<?> skillClass = Class.forName("iskallia.vault.skill.base.Skill");
                iterate.invoke(talentTree, skillClass, collector);

                LOGGER.info("=== TALENTS ===");
                for (Object talent : talents) {
                    java.lang.reflect.Method getId = talent.getClass().getMethod("getId");
                    java.lang.reflect.Method getName = talent.getClass().getMethod("getName");
                    String id = (String) getId.invoke(talent);
                    String name = (String) getName.invoke(talent);
                    LOGGER.info("ID: '{}', Name: '{}'", id, name);
                }
            }

            // Dump expertises
            Class<?> playerExpertisesDataClass = Class.forName("iskallia.vault.world.data.PlayerExpertisesData");
            Object expertiseData = VHDataReader.getWorldData(playerExpertisesDataClass, player.getLevel());
            java.lang.reflect.Method getExpertises = expertiseData.getClass().getMethod("getExpertises", player.getUUID().getClass());
            Object expertiseTree = getExpertises.invoke(expertiseData, player.getUUID());

            if (expertiseTree != null) {
                java.lang.reflect.Method iterate = expertiseTree.getClass().getMethod("iterate", Class.class, java.util.function.Consumer.class);
                java.util.List<Object> expertises = new java.util.ArrayList<>();
                java.util.function.Consumer<Object> collector = expertises::add;
                Class<?> skillClass = Class.forName("iskallia.vault.skill.base.Skill");
                iterate.invoke(expertiseTree, skillClass, collector);

                LOGGER.info("=== EXPERTISES ===");
                for (Object expertise : expertises) {
                    java.lang.reflect.Method getId = expertise.getClass().getMethod("getId");
                    java.lang.reflect.Method getName = expertise.getClass().getMethod("getName");
                    String id = (String) getId.invoke(expertise);
                    String name = (String) getName.invoke(expertise);
                    LOGGER.info("ID: '{}', Name: '{}'", id, name);
                }
            }

            ctx.getSource().sendSuccess(new TextComponent("Skill dump complete! Check logs.").withStyle(ChatFormatting.GREEN), false);

        } catch (Exception e) {
            LOGGER.error("Failed to dump skills", e);
            ctx.getSource().sendFailure(new TextComponent("Error: " + e.getMessage()));
        }

        return 1;
    }

    private static int unlockSkill(CommandContext<CommandSourceStack> ctx) {
        try {
            net.minecraft.server.level.ServerPlayer player = ctx.getSource().getPlayerOrException();
            String skillName = StringArgumentType.getString(ctx, "name");

            APSkillLockManager.unlockSkill(player, skillName);
            ctx.getSource().sendSuccess(
                new TextComponent("Unlocked skill: " + skillName).withStyle(ChatFormatting.GREEN),
                true
            );
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(new TextComponent("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int unlockTalent(CommandContext<CommandSourceStack> ctx) {
        try {
            net.minecraft.server.level.ServerPlayer player = ctx.getSource().getPlayerOrException();
            String talentName = StringArgumentType.getString(ctx, "name");

            APSkillLockManager.unlockTalent(player, talentName);
            ctx.getSource().sendSuccess(
                new TextComponent("Unlocked talent: " + talentName).withStyle(ChatFormatting.GREEN),
                true
            );
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(new TextComponent("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int unlockExpertise(CommandContext<CommandSourceStack> ctx) {
        try {
            net.minecraft.server.level.ServerPlayer player = ctx.getSource().getPlayerOrException();
            String expertiseName = StringArgumentType.getString(ctx, "name");

            APSkillLockManager.unlockExpertise(player, expertiseName);
            ctx.getSource().sendSuccess(
                new TextComponent("Unlocked expertise: " + expertiseName).withStyle(ChatFormatting.GREEN),
                true
            );
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(new TextComponent("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int unlockMod(CommandContext<CommandSourceStack> ctx) {
        try {
            net.minecraft.server.level.ServerPlayer player = ctx.getSource().getPlayerOrException();
            String modName = StringArgumentType.getString(ctx, "name");

            APSkillLockManager.unlockMod(player, modName);
            ctx.getSource().sendSuccess(
                new TextComponent("Unlocked mod: " + modName).withStyle(ChatFormatting.GREEN),
                true
            );
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(new TextComponent("Error: " + e.getMessage()));
            return 0;
        }
    }
}
