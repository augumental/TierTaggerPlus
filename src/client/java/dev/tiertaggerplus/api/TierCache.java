package dev.tiertaggerplus.api;

import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/** Per-player, per-tierlist cache. All network I/O happens on a small worker pool. */
public final class TierCache {
    private static final Pattern VALID_NAME = Pattern.compile("^[A-Za-z0-9_]{2,16}$");
    private static final long TTL_OK = 15 * 60_000L;
    private static final long TTL_ERROR = 60_000L;

    private static final AtomicInteger THREADS = new AtomicInteger();
    private static final ExecutorService POOL = Executors.newFixedThreadPool(6, r -> {
        Thread t = new Thread(r, "TierTaggerPlus-" + THREADS.incrementAndGet());
        t.setDaemon(true);
        return t;
    });

    private static final Map<String, PlayerTiers> PLAYERS = new ConcurrentHashMap<>();

    /** Bumped whenever any result arrives, so renderers can cheaply rebuild cached tags. */
    public static final AtomicInteger VERSION = new AtomicInteger();

    public static Consumer<Throwable> errorLogger = Throwable::printStackTrace;

    private TierCache() {
    }

    public static boolean isValidName(String name) {
        return name != null && VALID_NAME.matcher(name).matches();
    }

    public static @Nullable PlayerTiers get(String name) {
        if (!isValidName(name)) return null;
        return PLAYERS.computeIfAbsent(name.toLowerCase(Locale.ROOT), k -> new PlayerTiers(name));
    }

    /** Returns the cached profile, starting a fetch if missing or stale. Never blocks. */
    public static TierProfile request(PlayerTiers player, TierList list) {
        TierProfile cur = player.profiles.get(list);
        long age = cur == null ? Long.MAX_VALUE : System.currentTimeMillis() - cur.fetchedAt;
        boolean stale = cur == null
                || (cur.status == TierProfile.Status.ERROR && age > TTL_ERROR)
                || (cur.status != TierProfile.Status.LOADING && age > TTL_OK);
        if (stale) {
            TierProfile loading = TierProfile.loading(list);
            if (cur == null ? player.profiles.putIfAbsent(list, loading) == null : player.profiles.replace(list, cur, loading)) {
                CompletableFuture.runAsync(() -> {
                    TierProfile result;
                    try {
                        result = list.fetch(player.name);
                    } catch (Throwable t) {
                        errorLogger.accept(new RuntimeException("[" + list.id + "] " + player.name + ": " + t.getMessage(), t));
                        result = TierProfile.error(list, t.getMessage() == null ? t.getClass().getSimpleName() : t.getMessage());
                    }
                    player.profiles.put(list, result);
                    VERSION.incrementAndGet();
                }, POOL);
                return cur != null && cur.status != TierProfile.Status.ERROR ? cur : loading;
            }
        }
        TierProfile now = player.profiles.get(list);
        return now != null ? now : TierProfile.loading(list);
    }

    /** Forces a re-fetch of every list for the player. */
    public static void refresh(PlayerTiers player) {
        player.profiles.clear();
        VERSION.incrementAndGet();
        for (TierList l : TierList.values()) request(player, l);
    }

    public static void clear() {
        PLAYERS.clear();
        VERSION.incrementAndGet();
    }

    public static final class PlayerTiers {
        public final String name;
        final Map<TierList, TierProfile> profiles = new ConcurrentHashMap<>(new EnumMap<>(TierList.class));

        PlayerTiers(String name) {
            this.name = name;
        }

        public @Nullable TierProfile peek(TierList list) {
            return profiles.get(list);
        }
    }
}
