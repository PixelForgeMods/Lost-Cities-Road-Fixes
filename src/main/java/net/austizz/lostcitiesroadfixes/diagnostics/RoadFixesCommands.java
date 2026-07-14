package net.austizz.lostcitiesroadfixes.diagnostics;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.austizz.lostcitiesroadfixes.LostCitiesRoadFixes;
import net.austizz.lostcitiesroadfixes.integration.RoadGenerationRuntime;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class RoadFixesCommands {
    private static final int OPERATOR_PERMISSION_LEVEL = 2;

    private RoadFixesCommands() {
    }

    public static void registerEvent(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralCommandNode<CommandSourceStack> root = dispatcher.register(
                Commands.literal(LostCitiesRoadFixes.MOD_ID)
                        .requires(source -> source.hasPermission(OPERATOR_PERMISSION_LEVEL))
                        .then(Commands.literal("status")
                                .executes(context -> status(context.getSource())))
                        .then(Commands.literal("clear_caches")
                                .executes(context -> clearCaches(context.getSource()))));
        dispatcher.register(Commands.literal("lcroadfixes")
                .requires(source -> source.hasPermission(OPERATOR_PERMISSION_LEVEL))
                .redirect(root));
    }

    private static int status(CommandSourceStack source) {
        RoadDiagnosticsSnapshot snapshot = RoadGenerationRuntime.diagnostics();
        for (String line : snapshot.lines()) {
            source.sendSuccess(() -> Component.literal(line), false);
        }
        return snapshot.lines().size();
    }

    private static int clearCaches(CommandSourceStack source) {
        int previous = RoadGenerationRuntime.roadPlanCacheSize()
                + RoadGenerationRuntime.interchangePlanCacheSize();
        RoadGenerationRuntime.invalidatePlans();
        source.sendSuccess(
                () -> Component.literal(
                        "Cleared road and interchange plan caches ("
                                + previous + " entries)"),
                true);
        return previous;
    }
}
