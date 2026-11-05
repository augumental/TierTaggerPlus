package dev.tiertaggerplus.render;

import dev.tiertaggerplus.api.TierList;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Resolves tierlist/gamemode icons, as inline font glyphs (for nametags) or textures (for the GUI). */
public final class Icons {
    public static final String MOD_ID = "tiertaggerplus";
    private static final StyleSpriteSource FONT = new StyleSpriteSource.Font(Identifier.of(MOD_ID, "icons"));

    private Icons() {
    }

    private static String textureName(TierList list, String modeId) {
        String m = modeId.toLowerCase(Locale.ROOT);
        String t = IconMap.TEXTURE.get(list.id + "/" + m);
        if (t != null) return t;
        // tolerate small id variations an API might introduce (e.g. "ht_cart", "neth-pot")
        String stripped = m.replaceFirst("^(ht|lt|mt)[_-]", "");
        for (String cand : new String[]{stripped, m.replace('-', '_'), m.replace('_', '-'), m.replace("_", "").replace("-", "")}) {
            t = IconMap.TEXTURE.get(list.id + "/" + cand);
            if (t != null) return t;
        }
        return IconMap.TEXTURE.get(list.id + "/*");
    }

    /** 32x32 texture for a gamemode on a tierlist. */
    public static Identifier modeTexture(TierList list, String modeId) {
        return Identifier.of(MOD_ID, "textures/icon/" + textureName(list, modeId) + ".png");
    }

    /** 64x64 logo texture for a tierlist. */
    public static Identifier logoTexture(TierList list) {
        return Identifier.of(MOD_ID, "textures/icon/" + IconMap.TEXTURE.get(list.id + "/@logo") + ".png");
    }

    /** Inline 8px glyph for a gamemode (white so the texture keeps its own colours). */
    public static MutableText modeGlyph(TierList list, String modeId) {
        return glyph(textureName(list, modeId));
    }

    public static MutableText logoGlyph(TierList list) {
        return glyph(IconMap.TEXTURE.get(list.id + "/@logo"));
    }

    private static MutableText glyph(String texture) {
        Character c = texture == null ? null : IconMap.CHAR.get(texture);
        if (c == null) return Text.empty();
        return Text.literal(String.valueOf(c)).setStyle(Style.EMPTY.withFont(FONT).withColor(0xFFFFFF).withItalic(false));
    }

    /** Gamemode ids we ship icons for, for the config screen's mode picker. */
    public static List<String> knownModes(TierList list) {
        List<String> out = new ArrayList<>();
        String prefix = list.id + "/";
        for (String k : IconMap.TEXTURE.keySet())
            if (k.startsWith(prefix) && !k.endsWith("/*") && !k.endsWith("/@logo")) out.add(k.substring(prefix.length()));
        for (String k : list.modeNames.keySet()) if (!out.contains(k)) out.add(k);
        out.sort(String::compareTo);
        return out;
    }
}
