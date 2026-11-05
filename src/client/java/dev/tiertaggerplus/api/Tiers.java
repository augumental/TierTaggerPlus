package dev.tiertaggerplus.api;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Tier parsing, scoring and colouring shared by every tierlist. */
public final class Tiers {
    private static final Pattern HML = Pattern.compile("^([HML])T([1-5])$");
    private static final Pattern WORDS = Pattern.compile("^(High|Mid|Middle|Low)\\s+Tier\\s+([1-5])$", Pattern.CASE_INSENSITIVE);

    /** Colours for HT1..LT5 (index = (tier-1)*2 + (low ? 1 : 0)), matching the classic tier tagger palette. */
    private static final int[] HL_COLORS = {
            0xE8BA3A, 0xD5B355, // 1
            0xC4D3E7, 0xA0A7B2, // 2
            0xF89F5A, 0xC67B42, // 3
            0x81749A, 0x655B79, // 4
            0x8F82A8, 0x655B79, // 5
    };

    public static final int RETIRED_COLOR = 0x6E6E6E;
    public static final int UNKNOWN_COLOR = 0xD0D0D0;

    private Tiers() {
    }

    /** "HT", tier number and pos (0 = high) from the uku3lig API format. */
    public static String fromTierPos(int tier, int pos) {
        return (pos == 0 ? "HT" : "LT") + tier;
    }

    /** Normalises any of "HT3", "ht3", "High Tier 3", "Middle Tier 2" ... to "HT3"/"MT2". Letter tiers pass through. */
    public static String normalise(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        Matcher w = WORDS.matcher(s);
        if (w.matches()) {
            char c = Character.toUpperCase(w.group(1).charAt(0));
            return c + "T" + w.group(2);
        }
        String up = s.toUpperCase(Locale.ROOT);
        if (HML.matcher(up).matches()) return up;
        return s;
    }

    public static boolean isRanked(String tier) {
        if (tier == null || tier.isBlank()) return false;
        String t = tier.trim();
        return !(t.equalsIgnoreCase("unranked") || t.contains("?") || t.equals("-") || t.equalsIgnoreCase("none"));
    }

    /** Higher is better; 0 for unknown. */
    public static int score(String tier) {
        if (tier == null) return 0;
        Matcher m = HML.matcher(tier.toUpperCase(Locale.ROOT));
        if (m.matches()) {
            int n = Integer.parseInt(m.group(2));
            int base = (6 - n) * 20; // T1=100 ... T5=20
            return switch (m.group(1)) {
                case "H" -> base;
                case "M" -> base - 5;
                default -> base - 10;
            };
        }
        return switch (tier.trim().toUpperCase(Locale.ROOT)) {
            case "S+" -> 105;
            case "S" -> 100;
            case "S-" -> 95;
            case "A+" -> 90;
            case "A" -> 85;
            case "A-" -> 80;
            case "B+" -> 70;
            case "B" -> 65;
            case "B-" -> 60;
            case "C+" -> 50;
            case "C" -> 45;
            case "C-" -> 40;
            case "D+" -> 30;
            case "D" -> 25;
            case "D-" -> 20;
            default -> 0;
        };
    }

    public static int color(String tier) {
        if (tier == null) return UNKNOWN_COLOR;
        Matcher m = HML.matcher(tier.toUpperCase(Locale.ROOT));
        if (m.matches()) {
            int idx = (Integer.parseInt(m.group(2)) - 1) * 2;
            return switch (m.group(1)) {
                case "H" -> HL_COLORS[idx];
                case "L" -> HL_COLORS[idx + 1];
                default -> mix(HL_COLORS[idx], HL_COLORS[idx + 1]);
            };
        }
        String t = tier.trim().toUpperCase(Locale.ROOT);
        if (t.startsWith("S")) return 0xFF5B5B;
        if (t.startsWith("A")) return 0xFFA53A;
        if (t.startsWith("B")) return 0xF5D547;
        if (t.startsWith("C")) return 0x7EDC6A;
        if (t.startsWith("D")) return 0x6AB4F0;
        return UNKNOWN_COLOR;
    }

    public static int parseHex(String hex, int fallback) {
        if (hex == null) return fallback;
        try {
            return Integer.parseInt(hex.replace("#", ""), 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static int mix(int a, int b) {
        int r = (((a >> 16) & 0xFF) + ((b >> 16) & 0xFF)) / 2;
        int g = (((a >> 8) & 0xFF) + ((b >> 8) & 0xFF)) / 2;
        int bl = ((a & 0xFF) + (b & 0xFF)) / 2;
        return (r << 16) | (g << 8) | bl;
    }

    /** "dia_smp" / "spear-mace" / "DiaSMP" -> "Dia Smp" etc. Used only when the API gives no name. */
    public static String prettify(String id) {
        String s = id.replace('_', ' ').replace('-', ' ');
        StringBuilder out = new StringBuilder();
        for (String part : s.split(" ")) {
            if (part.isEmpty()) continue;
            if (!out.isEmpty()) out.append(' ');
            out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return out.toString();
    }
}
