package dev.tiertaggerplus.gui;

import com.google.gson.JsonObject;
import com.mojang.authlib.yggdrasil.ProfileResult;
import dev.tiertaggerplus.api.Http;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.entity.player.SkinTextures;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/** Resolves a username to its skin: tab list first, then Mojang's API + the vanilla skin provider. */
final class SkinLoader {
    private static final Map<String, CompletableFuture<SkinTextures>> CACHE = new ConcurrentHashMap<>();

    private SkinLoader() {
    }

    static CompletableFuture<SkinTextures> load(String name) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getNetworkHandler() != null) {
            PlayerListEntry e = mc.getNetworkHandler().getPlayerListEntry(name);
            if (e != null && e.getProfile().id().version() == 4) return CompletableFuture.completedFuture(e.getSkinTextures());
        }
        return CACHE.computeIfAbsent(name.toLowerCase(), k -> CompletableFuture.supplyAsync(() -> {
            try {
                Http.Response r = Http.get("https://api.mojang.com/users/profiles/minecraft/" + Http.enc(name));
                if (!r.ok()) return null;
                JsonObject o = r.json().getAsJsonObject();
                String id = o.get("id").getAsString();
                return UUID.fromString(id.replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
            } catch (Exception ex) {
                return null;
            }
        }).thenCompose(uuid -> {
            if (uuid == null) return CompletableFuture.completedFuture(DefaultSkinHelper.getSteve());
            return CompletableFuture.supplyAsync(() -> {
                        ProfileResult res = mc.getApiServices().sessionService().fetchProfile(uuid, false);
                        return res == null ? null : res.profile();
                    })
                    .thenCompose(p -> p == null ? CompletableFuture.completedFuture(Optional.<SkinTextures>empty()) : mc.getSkinProvider().fetchSkinTextures(p))
                    .thenApply(o -> o.orElseGet(() -> DefaultSkinHelper.getSkinTextures(uuid)))
                    .exceptionally(t -> DefaultSkinHelper.getSkinTextures(uuid));
        }).exceptionally(t -> DefaultSkinHelper.getSteve()));
    }
}
