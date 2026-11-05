package dev.tiertaggerplus.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Every supported tierlist and how to fetch a player from it. */
public enum TierList {
    MCTIERS("mctiers", "MCTiers", "mctiers.net", 0xF5C542) {
        @Override
        public TierProfile fetch(String name) throws IOException {
            return ukuStyle(this, "https://mctiers.net/api/v2", name, true);
        }
    },
    SUBTIERS("subtiers", "SubTiers", "subtiers.net", 0x4FB3FF) {
        @Override
        public TierProfile fetch(String name) throws IOException {
            return ukuStyle(this, "https://subtiers.net/api/v2", name, true);
        }
    },
    PVPTIERS("pvptiers", "PvPTiers", "pvptiers.com", 0xFF8A3D) {
        @Override
        public TierProfile fetch(String name) throws IOException {
            return pvpTiers(this, name);
        }
    },
    STATTIERS("stattiers", "StatTiers", "stattiers.com", 0x38D6FF) {
        @Override
        public TierProfile fetch(String name) throws IOException {
            return statTiers(this, name);
        }
    },
    TIERTESTS("tiertests", "TierTests", "tiertests.com", 0x3EE07A) {
        @Override
        public TierProfile fetch(String name) throws IOException {
            return tierTests(this, name);
        }
    },
    MSM("msm", "Mace SubModes", "msm.macepvp.site", 0xB07CFF) {
        @Override
        public TierProfile fetch(String name) throws IOException {
            return msm(this, name);
        }
    },
    PVPHQ("pvphq", "PvPHQ", "pvphq.com", 0xFF4D5E) {
        @Override
        public TierProfile fetch(String name) throws IOException {
            return pvpHq(this, name);
        }
    },
    MCPVP("mcpvp", "MCPVP", "mcpvp.com", 0xFFB43B) {
        @Override
        public TierProfile fetch(String name) throws IOException {
            return mcpvp(this, name);
        }
    };

    public final String id;
    public final String displayName;
    public final String website;
    public final int color;

    TierList(String id, String displayName, String website, int color) {
        this.id = id;
        this.displayName = displayName;
        this.website = website;
        this.color = color;
    }

    /** Blocking fetch by Minecraft username. Returns UNRANKED when the player is not on the list. */
    public abstract TierProfile fetch(String name) throws IOException;

    public static TierList byId(String id) {
        for (TierList l : values()) if (l.id.equalsIgnoreCase(id)) return l;
        return null;
    }

    /** Known gamemode names per list (filled from APIs as they're seen, plus defaults). */
    public final Map<String, String> modeNames = new ConcurrentHashMap<>();

    public String modeName(String modeId) {
        return modeNames.getOrDefault(modeId.toLowerCase(Locale.ROOT), Tiers.prettify(modeId));
    }

    void learnMode(String modeId, String name) {
        if (modeId != null && name != null && !name.isBlank()) modeNames.put(modeId.toLowerCase(Locale.ROOT), name);
    }

    static {
        String[][] pvp = {{"crystal", "Crystal"}, {"sword", "Sword"}, {"uhc", "UHC"}, {"pot", "Pot"}, {"neth_pot", "NethPot"},
                {"smp", "SMP"}, {"axe", "Axe"}, {"mace", "Mace"}};
        for (String[] m : pvp) PVPTIERS.learnMode(m[0], m[1]);
        String[][] mc = {{"vanilla", "Vanilla"}, {"uhc", "UHC"}, {"pot", "Pot"}, {"nethop", "NethOP"}, {"smp", "SMP"},
                {"sword", "Sword"}, {"axe", "Axe"}, {"mace", "Mace"}};
        for (String[] m : mc) MCTIERS.learnMode(m[0], m[1]);
        String[][] sub = {{"minecart", "Minecart"}, {"dia_crystal", "Diamond Vanilla"}, {"debuff", "Debuff"}, {"elytra", "Elytra"},
                {"speed", "Speed"}, {"creeper", "Creeper"}, {"manhunt", "Manhunt"}, {"dia_smp", "Diamond SMP"}, {"bow", "Bow"},
                {"bed", "Bed"}, {"og_vanilla", "OG Vanilla"}, {"trident", "Trident"}};
        for (String[] m : sub) SUBTIERS.learnMode(m[0], m[1]);
        String[][] st = {{"diacrystal", "Dia Crystal"}, {"diasmp", "Dia SMP"}, {"diapot", "Dia Pot"}, {"goldogv", "Gold OGV"},
                {"ironcrystal", "Iron Crystal"}, {"nethsword", "Neth Sword"}, {"nethpot", "Neth Pot"}, {"ogv", "OGV"},
                {"smp", "SMP"}, {"spearmace", "Spear Mace"}, {"speedop", "Speed OP"}, {"uhc", "UHC"}};
        for (String[] m : st) STATTIERS.learnMode(m[0], m[1]);
        String[][] hq = {{"spear_mace", "Spear"}, {"netherite_pot", "NethPot"}, {"diamond_smp", "DiaSMP"}, {"uhc", "UHC"}, {"smp", "SMP"}};
        for (String[] m : hq) PVPHQ.learnMode(m[0], m[1]);
        String[][] tt = {{"fireballfight", "Fireball Fight"}, {"nodebuff", "NoDebuff"}, {"bedfight", "Bed Fight"}, {"bedwars", "Bedwars"},
                {"netherite_pot", "NethPot"}, {"smp", "SMP"}, {"uhc", "UHC"}};
        for (String[] m : tt) TIERTESTS.learnMode(m[0], m[1]);
        String[][] cp = {{"early_game", "Early Game"}, {"late_game", "Late Game"}, {"end_game", "End Game"},
                {"diamond_smp", "Diamond SMP"}, {"netherite_pot", "Netherite Pot"}, {"smp", "SMP"}};
        for (String[] m : cp) MCPVP.learnMode(m[0], m[1]);
        String[][] ms = {{"rod-mace", "Rod Mace"}, {"og-mace", "OG Mace"}, {"lt-mace", "LT Mace"}, {"rocket-mace", "Rocket Mace"},
                {"ht-mace", "HT Mace"}, {"spear-mace", "Spear Mace"}};
        for (String[] m : ms) MSM.learnMode(m[0], m[1]);
    }

    // ------------------------------------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------------------------------------

    private static String str(JsonObject o, String k) {
        JsonElement e = o.get(k);
        return e == null || e.isJsonNull() ? null : e.getAsString();
    }

    private static Integer intOrNull(JsonObject o, String k) {
        JsonElement e = o.get(k);
        if (e == null || e.isJsonNull()) return null;
        try {
            return e.getAsInt();
        } catch (Exception ex) {
            return null;
        }
    }

    private static boolean bool(JsonObject o, String k) {
        JsonElement e = o.get(k);
        return e != null && !e.isJsonNull() && e.getAsBoolean();
    }

    private static Ranking ranking(TierList list, String modeId, String modeName, String tier, String peak, boolean retired, Integer color) {
        String t = Tiers.normalise(tier);
        String p = Tiers.isRanked(peak) ? Tiers.normalise(peak) : null;
        if (p != null && (p.equalsIgnoreCase(t) || Tiers.score(p) <= Tiers.score(t))) p = null;
        String id = modeId.toLowerCase(Locale.ROOT);
        if (modeName != null) list.learnMode(id, modeName);
        int c = retired ? Tiers.RETIRED_COLOR : color != null ? color : Tiers.color(t);
        return new Ranking(id, list.modeName(id), t, p, retired, c);
    }

    // ------------------------------------------------------------------------------------------------
    // MCTiers / SubTiers (uku3lig API v2)
    // ------------------------------------------------------------------------------------------------

    private static final Map<TierList, Boolean> MODES_LOADED = new ConcurrentHashMap<>();

    static TierProfile ukuStyle(TierList list, String base, String name, boolean loadModes) throws IOException {
        if (loadModes && MODES_LOADED.putIfAbsent(list, true) == null) {
            try {
                Http.Response m = Http.get(base + "/mode/list");
                if (m.ok()) for (Map.Entry<String, JsonElement> e : m.json().getAsJsonObject().entrySet())
                    list.learnMode(e.getKey(), str(e.getValue().getAsJsonObject(), "title"));
            } catch (Exception ignored) {
                MODES_LOADED.remove(list);
            }
        }
        Http.Response r = Http.get(base + "/profile/by-name/" + Http.enc(name));
        if (r.status() == 404 || r.body().contains("error.not_found")) return TierProfile.unranked(list);
        if (!r.ok()) throw new IOException("HTTP " + r.status());
        return parseUku(list, r.json().getAsJsonObject());
    }

    static TierProfile parseUku(TierList list, JsonObject o) {
        List<Ranking> out = new ArrayList<>();
        JsonObject rankings = o.has("rankings") && o.get("rankings").isJsonObject() ? o.getAsJsonObject("rankings") : new JsonObject();
        for (Map.Entry<String, JsonElement> e : rankings.entrySet()) {
            JsonObject r = e.getValue().getAsJsonObject();
            Integer tier = intOrNull(r, "tier"), pos = intOrNull(r, "pos");
            if (tier == null || pos == null) continue;
            Integer pt = intOrNull(r, "peak_tier"), pp = intOrNull(r, "peak_pos");
            String peak = pt != null && pp != null ? Tiers.fromTierPos(pt, pp) : null;
            out.add(ranking(list, e.getKey(), null, Tiers.fromTierPos(tier, pos), peak, bool(r, "retired"), null));
        }
        return TierProfile.of(list, out, str(o, "region"), intOrNull(o, "points"), intOrNull(o, "overall"));
    }

    // ------------------------------------------------------------------------------------------------
    // PvPTiers
    // ------------------------------------------------------------------------------------------------

    static TierProfile pvpTiers(TierList list, String name) throws IOException {
        Http.Response r = Http.get("https://pvptiers.com/api/search_profile/" + Http.enc(name));
        if (r.status() == 404 || r.status() == 422) return TierProfile.unranked(list);
        if (!r.ok()) throw new IOException("HTTP " + r.status());
        JsonElement j = r.json();
        if (!j.isJsonObject() || !j.getAsJsonObject().has("rankings")) return TierProfile.unranked(list);
        return parseUku(list, j.getAsJsonObject());
    }

    // ------------------------------------------------------------------------------------------------
    // StatTiers: no per-player endpoint, so the two leaderboards are downloaded and indexed (cached 10 min)
    // ------------------------------------------------------------------------------------------------

    private static final String STAT_API = "https://shrill-mode-ce7d.urbruh122.workers.dev";
    private static volatile Map<String, JsonObject> statIndex;
    private static volatile long statFetched;

    static synchronized Map<String, JsonObject> statIndex() throws IOException {
        if (statIndex != null && System.currentTimeMillis() - statFetched < 10 * 60_000) return statIndex;
        Map<String, JsonObject> merged = new HashMap<>();
        IOException last = null;
        int ok = 0;
        for (String path : new String[]{"/leaderboard", "/leaderboard/serverb"}) {
            try {
                Http.Response r = Http.get(STAT_API + path);
                if (!r.ok()) throw new IOException("HTTP " + r.status());
                ok++;
                for (JsonElement e : r.json().getAsJsonObject().getAsJsonArray("players")) {
                    JsonObject p = e.getAsJsonObject();
                    String u = str(p, "username");
                    if (u == null) continue;
                    String key = u.toLowerCase(Locale.ROOT);
                    JsonObject prev = merged.get(key);
                    if (prev == null) {
                        merged.put(key, p.deepCopy());
                    } else {
                        for (String k : new String[]{"tiers", "retired_tiers", "peak_tiers"}) {
                            if (!p.has(k) || !p.get(k).isJsonObject()) continue;
                            if (!prev.has(k) || !prev.get(k).isJsonObject()) prev.add(k, new JsonObject());
                            for (Map.Entry<String, JsonElement> t : p.getAsJsonObject(k).entrySet())
                                if (!prev.getAsJsonObject(k).has(t.getKey())) prev.getAsJsonObject(k).add(t.getKey(), t.getValue());
                        }
                    }
                }
            } catch (IOException ex) {
                last = ex;
            } catch (RuntimeException ex) {
                last = new IOException(ex);
            }
        }
        if (ok == 0) throw last != null ? last : new IOException("no data");
        statIndex = merged;
        statFetched = System.currentTimeMillis();
        return merged;
    }

    static TierProfile statTiers(TierList list, String name) throws IOException {
        JsonObject p = statIndex().get(name.toLowerCase(Locale.ROOT));
        if (p == null) return TierProfile.unranked(list);
        JsonObject tiers = p.has("tiers") && p.get("tiers").isJsonObject() ? p.getAsJsonObject("tiers") : new JsonObject();
        JsonObject retired = p.has("retired_tiers") && p.get("retired_tiers").isJsonObject() ? p.getAsJsonObject("retired_tiers") : new JsonObject();
        JsonObject peaks = p.has("peak_tiers") && p.get("peak_tiers").isJsonObject() ? p.getAsJsonObject("peak_tiers") : new JsonObject();
        List<Ranking> out = new ArrayList<>();
        for (Map.Entry<String, JsonElement> e : tiers.entrySet()) {
            String tier = e.getValue().getAsString();
            if (!Tiers.isRanked(tier)) continue;
            String peak = peaks.has(e.getKey()) ? peaks.get(e.getKey()).getAsString() : null;
            boolean ret = retired.has(e.getKey()) && retired.get(e.getKey()).getAsBoolean();
            out.add(ranking(list, e.getKey(), list.modeNames.containsKey(e.getKey().toLowerCase(Locale.ROOT)) ? null : e.getKey(), tier, peak, ret, null));
        }
        return TierProfile.of(list, out, str(p, "region"), intOrNull(p, "points"), intOrNull(p, "overall_rank"));
    }

    // ------------------------------------------------------------------------------------------------
    // TierTests: no public profile API; the player page embeds its rankings as JSON in the RSC payload
    // ------------------------------------------------------------------------------------------------

    private static final Pattern TT_RANK = Pattern.compile(
            "\"data\":\\{\"mode\":\"([^\"]+)\",\"modeName\":\"([^\"]+)\",\"tier\":\"([^\"]+)\",\"retired\":(true|false)");
    private static final Pattern TT_PEAK = Pattern.compile("\"history\":\\{\"mode\":\"([^\"]+)\",\"peak\":\"([^\"]+)\"");
    private static final Pattern TT_REGION = Pattern.compile("\"region\":\"([A-Z]{2,8})\"");

    static TierProfile tierTests(TierList list, String name) throws IOException {
        Http.Response r = Http.get("https://tiertests.com/players/" + Http.enc(name));
        if (r.status() == 404) return TierProfile.unranked(list);
        if (!r.ok()) throw new IOException("HTTP " + r.status());
        String body = r.body().replace("\\\"", "\"");
        Map<String, String> peaks = new HashMap<>();
        Matcher pm = TT_PEAK.matcher(body);
        while (pm.find()) peaks.put(pm.group(1), pm.group(2));
        List<Ranking> out = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        Matcher m = TT_RANK.matcher(body);
        while (m.find()) {
            if (!seen.add(m.group(1)) || !Tiers.isRanked(m.group(3))) continue;
            out.add(ranking(list, m.group(1), m.group(2), m.group(3), peaks.get(m.group(1)), Boolean.parseBoolean(m.group(4)), null));
        }
        Matcher rm = TT_REGION.matcher(body);
        return TierProfile.of(list, out, rm.find() ? shortRegion(rm.group(1)) : null, null, null);
    }

    // ------------------------------------------------------------------------------------------------
    // MSM (Mace SubModes) public API v1
    // ------------------------------------------------------------------------------------------------

    static TierProfile msm(TierList list, String name) throws IOException {
        Http.Response r = Http.get("https://msm.macepvp.site/api/v1/players/" + Http.enc(name));
        if (r.status() == 404) return TierProfile.unranked(list);
        if (!r.ok()) throw new IOException("HTTP " + r.status());
        JsonObject root = r.json().getAsJsonObject();
        if (!root.has("data") || !root.get("data").isJsonObject()) return TierProfile.unranked(list);
        JsonObject d = root.getAsJsonObject("data");
        List<Ranking> out = new ArrayList<>();
        if (d.has("rankings")) for (JsonElement e : d.getAsJsonArray("rankings")) {
            JsonObject k = e.getAsJsonObject();
            String tier = str(k, "tierName") != null ? str(k, "tierName") : str(k, "tierId");
            if (!Tiers.isRanked(tier) || str(k, "kitId") == null) continue;
            out.add(ranking(list, str(k, "kitId"), str(k, "kitName"), tier, str(k, "peakTierId"), bool(k, "retired"), null));
        }
        return TierProfile.of(list, out, str(d, "region"), intOrNull(d, "points"), intOrNull(d, "position"));
    }

    // ------------------------------------------------------------------------------------------------
    // PvPHQ
    // ------------------------------------------------------------------------------------------------

    static TierProfile pvpHq(TierList list, String name) throws IOException {
        Http.Response r = Http.get("https://pvphq.com/api/v1/players/by-name/" + Http.enc(name));
        if (r.status() == 404) return TierProfile.unranked(list);
        if (!r.ok()) throw new IOException("HTTP " + r.status());
        JsonObject o = r.json().getAsJsonObject();
        List<Ranking> out = new ArrayList<>();
        if (o.has("ranked")) for (JsonElement e : o.getAsJsonArray("ranked")) {
            JsonObject k = e.getAsJsonObject();
            if (bool(k, "unranked")) continue;
            String tier = str(k, "tier");
            if (!Tiers.isRanked(tier) || str(k, "gametype") == null) continue;
            Integer color = k.has("tierColor") ? Tiers.parseHex(str(k, "tierColor"), Tiers.color(tier)) : null;
            out.add(ranking(list, str(k, "gametype"), str(k, "gametypeName"), tier, str(k, "peakTier"), bool(k, "inactive"), color));
        }
        String region = str(o, "closestRegion");
        return TierProfile.of(list, out, region == null ? null : Tiers.prettify(region.toLowerCase(Locale.ROOT)), null, null);
    }

    // ------------------------------------------------------------------------------------------------
    // MCPVP: server-rendered profile page; ranked kits are <button class="ranking-kit" data-kit=.. data-tier-code=..>
    // ------------------------------------------------------------------------------------------------

    private static final Pattern MCPVP_KIT = Pattern.compile(
            "<button class=\"ranking-kit\"[^>]*?data-kit=\"([^\"]+)\"[^>]*?data-label=\"([^\"]+)\"[^>]*>.*?data-tier-code=\"([^\"]*)\"",
            Pattern.DOTALL);

    static TierProfile mcpvp(TierList list, String name) throws IOException {
        Http.Response r = Http.get("https://www.mcpvp.com/@" + Http.enc(name));
        if (r.status() == 404) return TierProfile.unranked(list);
        if (!r.ok()) throw new IOException("HTTP " + r.status());
        // unknown players redirect to the home page
        if (!r.finalUri().getPath().startsWith("/@")) return TierProfile.unranked(list);
        List<Ranking> out = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        Matcher m = MCPVP_KIT.matcher(r.body());
        while (m.find()) {
            String kit = m.group(1);
            if (kit.equals("overall") || !seen.add(kit) || !Tiers.isRanked(m.group(3))) continue;
            out.add(ranking(list, kit, unescapeHtml(m.group(2)), m.group(3), null, false, null));
        }
        return TierProfile.of(list, out, null, null, null);
    }

    private static String unescapeHtml(String s) {
        return s.replace("&amp;", "&").replace("&#039;", "'").replace("&quot;", "\"");
    }

    private static String shortRegion(String r) {
        return switch (r) {
            case "ASIA" -> "AS";
            case "EUROPE" -> "EU";
            case "OCEANIA" -> "OC";
            case "AFRICA" -> "AF";
            default -> r;
        };
    }
}
