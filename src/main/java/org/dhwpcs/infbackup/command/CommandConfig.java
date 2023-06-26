package org.dhwpcs.infbackup.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.dhwpcs.infbackup.FabricEntrypoint;
import org.dhwpcs.infbackup.config.InfBackupConfig;

import java.util.concurrent.CompletableFuture;

public class CommandConfig {
    private final FabricEntrypoint entrypoint;

    public CommandConfig(FabricEntrypoint entrypoint) {
        this.entrypoint = entrypoint;
    }

    public void register(LiteralArgumentBuilder<ServerCommandSource> source) {
        source.then(LiteralArgumentBuilder.<ServerCommandSource>literal("config")
                .then(RequiredArgumentBuilder.<ServerCommandSource, String>argument("key", StringArgumentType.word())
                        .suggests(this::suggestKey)
                        .executes(this::printConfigInfo)
                        .then(RequiredArgumentBuilder.<ServerCommandSource, String>argument("value", StringArgumentType.greedyString())
                                .suggests(this::suggestValue)
                                .executes(this::setConfig))));
    }

    private int setConfig(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource source = ctx.getSource();
        String key = StringArgumentType.getString(ctx, "key");
        String value = StringArgumentType.getString(ctx, "value");
        if(!InfBackupConfig.has(key)) {
            source.sendFeedback(new LiteralText("No such config:"+key).formatted(Formatting.RED), false);
            return 1;
        }
        if(!entrypoint.config.set(key, value)) {
            source.sendFeedback(new LiteralText("Invalid value:"+ value).formatted(Formatting.RED), false);
            return 1;
        }
        source.sendFeedback(new LiteralText("Successfully set config ").formatted(Formatting.GREEN)
                .append(new LiteralText(key).formatted(Formatting.AQUA))
                .append(new LiteralText(" to ").formatted(Formatting.GREEN))
                .append(new LiteralText(value).formatted(Formatting.AQUA)),
                false
        );
        return 0;
    }

    private CompletableFuture<Suggestions> suggestValue(CommandContext<ServerCommandSource> ctx, SuggestionsBuilder builder) {
        String key = StringArgumentType.getString(ctx, "key");
        return CommandSource.suggestMatching(InfBackupConfig.getSuggestions(key, entrypoint), builder);
    }

    private int printConfigInfo(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource source = ctx.getSource();
        String key = StringArgumentType.getString(ctx, "key");
        if(!InfBackupConfig.has(key)) {
            source.sendFeedback(new LiteralText("No such config:"+key).formatted(Formatting.RED), false);
            return 1;
        }
        Iterable<String> descriptions = InfBackupConfig.getDescription(key);
        source.sendFeedback(Text.of("Description for config "+key+":"), false);
        for (String each : descriptions) {
            source.sendFeedback(Text.of(" "+each), false);
        }
        source.sendFeedback(Text.of("Current value:"+entrypoint.config.get(key).serialize()), false);
        return 0;
    }

    private CompletableFuture<Suggestions> suggestKey(CommandContext<ServerCommandSource> ctx, SuggestionsBuilder builder) {
        return CommandSource.suggestMatching(InfBackupConfig.ENTRIES, builder);
    }
}
