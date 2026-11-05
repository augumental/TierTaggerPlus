package dev.tiertaggerplus.render;

import dev.tiertaggerplus.api.Ranking;
import dev.tiertaggerplus.api.TierCache;
import dev.tiertaggerplus.api.TierList;
import dev.tiertaggerplus.api.TierProfile;
import dev.tiertaggerplus.config.TTPConfig;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Builds the "[icon] LT1 | Name | HT2 [icon]" decoration around a player name. */
public final class Tags {
    private record Built(int version, int configHash, Text left, Text right) {
    }

    private static final Map<String, Built> CACHE = new ConcurrentHashMap<>();
    private static final int SEPARATOR_GREY = 0xAAAAAA;
    private static int configVersion;

    private Tags() {
    }

    /** Call after any config change. */
    public static void invalidate() {
        configVersion++;
        CACHE.clear();
    }

    public static Text decorate(String playerName, Text original) {
        TTPConfig cfg = TTPConfig.get();
        if (!cfg.enabled || !TierCache.isValidName(playerName)) return original;
        Built b = build(playerName);
        if (b == null) return original;
        if (b.left.getString().isEmpty() && b.right.getString().isEmpty()) return original;
        return Text.empty().append(b.left).append(original).append(b.right);
    }

    private static @Nullable Built build(String name) {
        int version = TierCache.VERSION.get();
        Built cached = CACHE.get(name);
        if (cached != null && cached.version == version && cached.configHash == configVersion) return cached;
        TierCache.PlayerTiers player = TierCache.get(name);
        if (player == null) return null;
        TTPConfig cfg = TTPConfig.get();
        Text left = side(player, cfg.left, true);
        Text right = side(player, cfg.right, false);
        Built b = new Built(version, configVersion, left, right);
        CACHE.put(name, b);
        return b;
    }

    private static Text side(TierCache.PlayerTiers player, TTPConfig.Slot slot, boolean isLeft) {
        TierList list = slot.tierList();
        if (list == null) return Text.empty();
        TierProfile p = TierCache.request(player, list);
        Optional<Ranking> r = pick(p, slot.mode);
        if (r.isEmpty()) return Text.empty();
        return tagFor(list, r.get(), isLeft);
    }

    /** The ranking a slot should display: a fixed gamemode, falling back to the highest one. */
    public static Optional<Ranking> pick(TierProfile p, String mode) {
        if (p == null || p.status != TierProfile.Status.READY) return Optional.empty();
        boolean retired = TTPConfig.get().showRetired;
        if (mode != null && !mode.equalsIgnoreCase("highest")) {
            Optional<Ranking> m = p.mode(mode);
            if (m.isPresent() && (retired || !m.get().retired())) return m;
        }
        return p.highest(retired);
    }

    public static MutableText tierText(Ranking r) {
        String t = TTPConfig.get().retiredPrefix ? r.displayTier() : r.tier();
        return Text.literal(t).setStyle(Style.EMPTY.withColor(r.color()));
    }

    public static Text tagFor(TierList list, Ranking r, boolean isLeft) {
        TTPConfig cfg = TTPConfig.get();
        MutableText tier = tierText(r);
        String sepStr = cfg.separator == null ? "" : cfg.separator.trim();
        MutableText sep = Text.literal(sepStr.isEmpty() ? " " : " " + sepStr + " ")
                .setStyle(Style.EMPTY.withColor(cfg.coloredSeparator ? r.color() : SEPARATOR_GREY));
        MutableText icons = Text.empty();
        if (cfg.showListLogo) icons.append(Icons.logoGlyph(list));
        if (cfg.showModeIcon) icons.append(Icons.modeGlyph(list, r.modeId()));
        boolean hasIcons = cfg.showListLogo || cfg.showModeIcon;

        MutableText out = Text.empty();
        if (isLeft) {
            if (hasIcons) out.append(icons).append(" ");
            out.append(tier).append(sep);
        } else {
            out.append(sep).append(tier);
            if (hasIcons) out.append(" ").append(icons);
        }
        return out;
    }
}
