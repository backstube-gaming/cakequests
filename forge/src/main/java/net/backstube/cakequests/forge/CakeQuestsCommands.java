package net.backstube.cakequests.forge;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

public final class CakeQuestsCommands {
    private CakeQuestsCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("cakequests")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("reset")
                        .then(Commands.argument("players", EntityArgument.players())
                                .executes(context -> reset(context.getSource(), EntityArgument.getPlayers(context, "players")))))
                .then(Commands.literal("grant")
                        .then(Commands.argument("id", StringArgumentType.string())
                                .then(Commands.argument("players", EntityArgument.players())
                                        .executes(context -> grant(context.getSource(), StringArgumentType.getString(context, "id"), EntityArgument.getPlayers(context, "players"))))))
                .then(Commands.literal("revoke")
                        .then(Commands.argument("id", StringArgumentType.string())
                                .then(Commands.argument("players", EntityArgument.players())
                                        .executes(context -> revoke(context.getSource(), StringArgumentType.getString(context, "id"), EntityArgument.getPlayers(context, "players"))))))
                .then(Commands.literal("grant-to")
                        .then(Commands.argument("id", StringArgumentType.string())
                                .then(Commands.argument("players", EntityArgument.players())
                                        .executes(context -> grantTo(context.getSource(), StringArgumentType.getString(context, "id"), EntityArgument.getPlayers(context, "players")))))));
    }

    private static int reset(CommandSourceStack source, Collection<ServerPlayer> players) {
        int changed = CakeQuestsProgressManager.reset(players);
        source.sendSuccess(new TextComponent("Reset Cake Quests progress for " + changed + " player(s)."), true);
        return changed;
    }

    private static int grant(CommandSourceStack source, String id, Collection<ServerPlayer> players) {
        int changed = CakeQuestsProgressManager.grant(players, id);
        source.sendSuccess(new TextComponent("Granted Cake Quests progress '" + id + "' for " + changed + " player(s)."), true);
        return changed;
    }

    private static int revoke(CommandSourceStack source, String id, Collection<ServerPlayer> players) {
        int changed = CakeQuestsProgressManager.revoke(players, id);
        source.sendSuccess(new TextComponent("Revoked Cake Quests progress '" + id + "' for " + changed + " player(s)."), true);
        return changed;
    }

    private static int grantTo(CommandSourceStack source, String id, Collection<ServerPlayer> players) {
        int changed = CakeQuestsProgressManager.grantTo(players, id);
        source.sendSuccess(new TextComponent("Set Cake Quests progress '" + id + "' to target for " + changed + " player(s)."), true);
        return changed;
    }
}
