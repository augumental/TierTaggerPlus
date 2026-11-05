package dev.tiertaggerplus;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.tiertaggerplus.api.Http;
import dev.tiertaggerplus.api.TierCache;
import dev.tiertaggerplus.config.TTPConfig;
import dev.tiertaggerplus.gui.ConfigScreen;
import dev.tiertaggerplus.gui.TierScreen;
import dev.tiertaggerplus.render.Tags;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class TierTaggerPlus implements ClientModInitializer {
    public static final String MOD_ID = "tiertaggerplus";
    public static final Logger LOGGER = LoggerFactory.getLogger("TierTagger+");

    public static KeyBinding openKey;
    /** Screens opened from chat commands must wait a tick, or closing the chat screen replaces them. */
    private static Screen pendingScreen;

    @Override
    public void onInitializeClient() {
        TTPConfig.load();
        FabricLoader.getInstance().getModContainer(MOD_ID).ifPresent(m ->
                Http.userAgent = "Mozilla/5.0 (compatible; TierTaggerPlus/" + m.getMetadata().getVersion().getFriendlyString() + ")");
        TierCache.errorLogger = t -> LOGGER.warn(t.getMessage());

        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of(MOD_ID, "main"));
        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.tiertaggerplus.open", GLFW.GLFW_KEY_K, category));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (pendingScreen != null) {
                client.setScreen(pendingScreen);
                pendingScreen = null;
            }
            while (openKey.wasPressed()) {
                if (client.currentScreen == null) client.setScreen(new TierScreen(null, null));
            }
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) -> dispatcher.register(literal("tiers")
                .executes(ctx -> open(new TierScreen(null, null)))
                .then(literal("enable").executes(ctx -> setEnabled(ctx, true)))
                .then(literal("disable").executes(ctx -> setEnabled(ctx, false)))
                .then(literal("toggle").executes(ctx -> setEnabled(ctx, !TTPConfig.get().enabled)))
                .then(literal("config").executes(ctx -> open(new ConfigScreen(null))))
                .then(literal("clear").executes(ctx -> {
                    TierCache.clear();
                    Tags.invalidate();
                    ctx.getSource().sendFeedback(prefix().append(Text.literal("Cache cleared.").formatted(Formatting.GREEN)));
                    return 1;
                }))
                .then(literal("lookup")
                        .then(argument("player", StringArgumentType.word())
                                .suggests((ctx, b) -> {
                                    MinecraftClient mc = MinecraftClient.getInstance();
                                    if (mc.getNetworkHandler() != null)
                                        for (PlayerListEntry e : mc.getNetworkHandler().getPlayerList())
                                            if (e.getProfile().name().toLowerCase().startsWith(b.getRemaining().toLowerCase()))
                                                b.suggest(e.getProfile().name());
                                    return b.buildFuture();
                                })
                                .executes(ctx -> lookup(ctx, StringArgumentType.getString(ctx, "player")))))));

        DevAutotest.register();
        LOGGER.info("TierTagger+ ready");
    }

    private static int open(Screen s) {
        pendingScreen = s;
        return 1;
    }

    private static int lookup(CommandContext<FabricClientCommandSource> ctx, String name) {
        if (!TierCache.isValidName(name)) {
            ctx.getSource().sendError(prefix().append(Text.literal("\"" + name + "\" is not a valid Minecraft username.")));
            return 0;
        }
        return open(new TierScreen(null, name));
    }

    private static int setEnabled(CommandContext<FabricClientCommandSource> ctx, boolean on) {
        TTPConfig.get().enabled = on;
        TTPConfig.save();
        Tags.invalidate();
        ctx.getSource().sendFeedback(prefix().append(Text.literal("Nametag tiers " + (on ? "enabled" : "disabled") + "."))
                .formatted(on ? Formatting.GREEN : Formatting.RED));
        return 1;
    }

    public static net.minecraft.text.MutableText prefix() {
        return Text.literal("[TierTagger+] ").formatted(Formatting.GOLD);
    }
}
