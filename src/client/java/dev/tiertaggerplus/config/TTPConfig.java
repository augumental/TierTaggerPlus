package dev.tiertaggerplus.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.tiertaggerplus.api.TierList;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Everything the user can customise. Saved to config/tiertaggerplus.json. */
public class TTPConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("tiertaggerplus.json");
    private static TTPConfig instance;

    /** What one side of the nametag shows. list = tierlist id or "off"; mode = gamemode id or "highest". */
    public static class Slot {
        public String list;
        public String mode;

        public Slot(String list, String mode) {
            this.list = list;
            this.mode = mode;
        }

        public TierList tierList() {
            return TierList.byId(list);
        }
    }

    public boolean enabled = true;
    public Slot left = new Slot("mctiers", "highest");
    public Slot right = new Slot("pvptiers", "highest");

    /** Show the gamemode icon next to the tier. */
    public boolean showModeIcon = true;
    /** Show the tierlist's logo next to the tier (helps when both sides use different lists). */
    public boolean showListLogo = false;
    /** Tag players in the tab list too. */
    public boolean showInTab = true;
    /** Fall back to retired rankings when the player has no active one. */
    public boolean showRetired = true;
    /** Colour the separator like the tier instead of grey. */
    public boolean coloredSeparator = false;
    /** Prefix retired tiers with "R". */
    public boolean retiredPrefix = true;
    /** Separator between tier and name. */
    public String separator = "|";
    /** Which tierlists appear in the K menu, in order. */
    public Map<String, Boolean> guiLists = new LinkedHashMap<>();

    public static TTPConfig get() {
        if (instance == null) load();
        return instance;
    }

    public static void load() {
        TTPConfig c = null;
        try {
            if (Files.exists(FILE)) c = GSON.fromJson(Files.readString(FILE), TTPConfig.class);
        } catch (Exception e) {
            System.err.println("[TierTagger+] Could not read config, using defaults: " + e);
        }
        if (c == null) c = new TTPConfig();
        if (c.left == null) c.left = new Slot("off", "highest");
        if (c.right == null) c.right = new Slot("off", "highest");
        if (c.separator == null) c.separator = "|";
        if (c.guiLists == null) c.guiLists = new LinkedHashMap<>();
        for (TierList l : TierList.values()) c.guiLists.putIfAbsent(l.id, true);
        instance = c;
    }

    public static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(get()));
        } catch (IOException e) {
            System.err.println("[TierTagger+] Could not save config: " + e);
        }
    }

    public boolean isShownInGui(TierList l) {
        return guiLists.getOrDefault(l.id, true);
    }
}
