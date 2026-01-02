package com.example.vhapmod;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TextComponent;
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
    }
    
    private static int connect(CommandContext<CommandSourceStack> ctx, String password) {
        String host = StringArgumentType.getString(ctx, "host");
        int port = IntegerArgumentType.getInteger(ctx, "port");
        String slotName = StringArgumentType.getString(ctx, "slotName");
        
        APWebSocketClient client = VaultHuntersAPMod.getAPClient();
        
        if (client == null) {
            ctx.getSource().sendFailure(new TextComponent("§cAP client not initialized!"));
            return 0;
        }
        
        if (client.isConnected()) {
            ctx.getSource().sendFailure(new TextComponent("§cAlready connected! Use /apdisconnect first."));
            return 0;
        }
        
        ctx.getSource().sendSuccess(new TextComponent(
            String.format("§aConnecting to %s:%d as %s...", host, port, slotName)
        ), true);
        
        client.connect(host, port, slotName, password).thenRun(() -> {
            LOGGER.info("Connection initiated from command");
        });
        
        return 1;
    }
    
    private static int disconnect(CommandContext<CommandSourceStack> ctx) {
        APWebSocketClient client = VaultHuntersAPMod.getAPClient();
        
        if (client == null) {
            ctx.getSource().sendFailure(new TextComponent("§cAP client not initialized!"));
            return 0;
        }
        
        if (!client.isConnected()) {
            ctx.getSource().sendFailure(new TextComponent("§cNot connected!"));
            return 0;
        }
        
        client.disconnect();
        ctx.getSource().sendSuccess(new TextComponent("§aDisconnected from AP server"), true);
        
        return 1;
    }
    
    private static int status(CommandContext<CommandSourceStack> ctx) {
        APWebSocketClient client = VaultHuntersAPMod.getAPClient();
        
        if (client == null) {
            ctx.getSource().sendFailure(new TextComponent("§cAP client not initialized!"));
            return 0;
        }
        
        if (client.isConnected()) {
            ctx.getSource().sendSuccess(new TextComponent("§aConnected to AP server"), false);
        } else {
            ctx.getSource().sendSuccess(new TextComponent("§eNot connected"), false);
        }
        
        return 1;
    }
}
