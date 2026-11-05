package dev.tiertaggerplus.api;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** A player's result on one tierlist. Immutable once built. */
public final class TierProfile {
    public enum Status { LOADING, READY, UNRANKED, ERROR }

    public final TierList list;
    public final Status status;
    public final List<Ranking> rankings;
    public final @Nullable String region;
    public final @Nullable Integer points;
    public final @Nullable Integer position;
    public final @Nullable String error;
    public final long fetchedAt = System.currentTimeMillis();

    private TierProfile(TierList list, Status status, List<Ranking> rankings, @Nullable String region,
                        @Nullable Integer points, @Nullable Integer position, @Nullable String error) {
        this.list = list;
        this.status = status;
        List<Ranking> sorted = new ArrayList<>(rankings);
        sorted.sort(Comparator.comparing(Ranking::retired).thenComparing(Comparator.comparingInt(Ranking::score).reversed()));
        this.rankings = List.copyOf(sorted);
        this.region = region;
        this.points = points;
        this.position = position;
        this.error = error;
    }

    public static TierProfile loading(TierList list) {
        return new TierProfile(list, Status.LOADING, List.of(), null, null, null, null);
    }

    public static TierProfile unranked(TierList list) {
        return new TierProfile(list, Status.UNRANKED, List.of(), null, null, null, null);
    }

    public static TierProfile error(TierList list, String error) {
        return new TierProfile(list, Status.ERROR, List.of(), null, null, null, error);
    }

    public static TierProfile of(TierList list, List<Ranking> rankings, @Nullable String region, @Nullable Integer points, @Nullable Integer position) {
        if (rankings.isEmpty()) return new TierProfile(list, Status.UNRANKED, List.of(), region, points, position, null);
        return new TierProfile(list, Status.READY, rankings, region, points, position, null);
    }

    public boolean isDone() {
        return status != Status.LOADING;
    }

    /** Best non-retired ranking (falls back to best retired one if requested). */
    public Optional<Ranking> highest(boolean allowRetired) {
        return rankings.stream()
                .filter(r -> allowRetired || !r.retired())
                .max(Comparator.comparing((Ranking r) -> !r.retired()).thenComparingInt(Ranking::score));
    }

    public Optional<Ranking> mode(String modeId) {
        return rankings.stream().filter(r -> r.modeId().equalsIgnoreCase(modeId)).findFirst();
    }
}
