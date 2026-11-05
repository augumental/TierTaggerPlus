package dev.tiertaggerplus.api;

import org.jetbrains.annotations.Nullable;

/**
 * One gamemode ranking, normalised across every tierlist.
 *
 * @param modeId   the tierlist's own id for the gamemode (lower-cased)
 * @param modeName human readable gamemode name
 * @param tier     displayed tier, e.g. "HT3", "LT1", "MT2", "B+"
 * @param peak     peak tier if different from {@code tier}, else null
 * @param retired  whether the ranking is retired
 * @param color    RGB colour for the tier text
 */
public record Ranking(String modeId, String modeName, String tier, @Nullable String peak, boolean retired, int color) {
    /** Higher is better. Used to pick the "highest" ranking and to sort. */
    public int score() {
        return Tiers.score(tier);
    }

    public int peakScore() {
        return peak == null ? score() : Math.max(score(), Tiers.score(peak));
    }

    public String displayTier() {
        return (retired ? "R" : "") + tier;
    }
}
