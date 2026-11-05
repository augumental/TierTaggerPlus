package dev.tiertaggerplus.gui;

import dev.tiertaggerplus.api.Ranking;
import dev.tiertaggerplus.api.TierCache;
import dev.tiertaggerplus.api.TierList;
import dev.tiertaggerplus.config.TTPConfig;
import dev.tiertaggerplus.render.Icons;
import dev.tiertaggerplus.render.Tags;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Customise what the nametag tags show, plus which lists appear in the K menu. */
public class ConfigScreen extends Screen {
    private final @Nullable Screen parent;
    private TextFieldWidget separatorField;
    private int sepLabelX, sepLabelY, listsY;

    public ConfigScreen(@Nullable Screen parent) {
        super(Text.translatable("tiertaggerplus.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        TTPConfig cfg = TTPConfig.get();
        int colW = Math.min(150, (width - 30) / 2);
        int lx = width / 2 - colW - 5, rx = width / 2 + 5;
        int y = 46;

        // --- nametag slots ---
        addDrawableChild(slotListButton(lx, y, colW, "left", cfg.left));
        addDrawableChild(slotListButton(rx, y, colW, "right", cfg.right));
        y += 22;
        addDrawableChild(slotModeButton(lx, y, colW, cfg.left));
        addDrawableChild(slotModeButton(rx, y, colW, cfg.right));
        y += 28;

        // --- toggles ---
        addDrawableChild(toggle(lx, y, colW, "enabled", () -> cfg.enabled, v -> cfg.enabled = v));
        addDrawableChild(toggle(rx, y, colW, "tab", () -> cfg.showInTab, v -> cfg.showInTab = v));
        y += 22;
        addDrawableChild(toggle(lx, y, colW, "mode_icon", () -> cfg.showModeIcon, v -> cfg.showModeIcon = v));
        addDrawableChild(toggle(rx, y, colW, "list_logo", () -> cfg.showListLogo, v -> cfg.showListLogo = v));
        y += 22;
        addDrawableChild(toggle(lx, y, colW, "retired", () -> cfg.showRetired, v -> cfg.showRetired = v));
        addDrawableChild(toggle(rx, y, colW, "retired_prefix", () -> cfg.retiredPrefix, v -> cfg.retiredPrefix = v));
        y += 22;
        addDrawableChild(toggle(lx, y, colW, "colored_separator", () -> cfg.coloredSeparator, v -> cfg.coloredSeparator = v));
        sepLabelX = rx + 4;
        sepLabelY = y + 6;
        separatorField = new TextFieldWidget(textRenderer, rx + colW - 40, y, 40, 20, Text.translatable("tiertaggerplus.config.separator"));
        separatorField.setMaxLength(4);
        separatorField.setText(cfg.separator);
        separatorField.setChangedListener(s -> {
            cfg.separator = s;
            changed();
        });
        addDrawableChild(separatorField);
        y += 30;

        // --- tierlists in the K menu ---
        listsY = y;
        int per = 4;
        int bw = Math.min(110, (width - 20 - (per - 1) * 4) / per);
        int sx = width / 2 - (per * bw + (per - 1) * 4) / 2;
        TierList[] lists = TierList.values();
        for (int i = 0; i < lists.length; i++) {
            TierList l = lists[i];
            int bx = sx + (i % per) * (bw + 4), by = y + 12 + (i / per) * 22;
            addDrawableChild(ButtonWidget.builder(listToggleLabel(l), b -> {
                        cfg.guiLists.put(l.id, !cfg.isShownInGui(l));
                        b.setMessage(listToggleLabel(l));
                        changed();
                    }).dimensions(bx, by, bw, 20)
                    .tooltip(Tooltip.of(Text.translatable("tiertaggerplus.config.gui_list.tooltip", l.displayName, l.website))).build());
        }

        addDrawableChild(ButtonWidget.builder(Text.translatable("tiertaggerplus.config.clear_cache"), b -> {
            TierCache.clear();
            Tags.invalidate();
        }).dimensions(width / 2 - 155, height - 28, 150, 20).build());
        addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, b -> close()).dimensions(width / 2 + 5, height - 28, 150, 20).build());
    }

    private Text listToggleLabel(TierList l) {
        boolean on = TTPConfig.get().isShownInGui(l);
        return Text.empty().append(Icons.logoGlyph(l)).append(" ")
                .append(Text.literal(l.displayName).setStyle(on ? Style.EMPTY.withColor(l.color) : Style.EMPTY.withColor(Formatting.DARK_GRAY).withStrikethrough(true)));
    }

    private ButtonWidget slotListButton(int x, int y, int w, String side, TTPConfig.Slot slot) {
        return ButtonWidget.builder(slotListLabel(side, slot), b -> {
                    List<String> ids = new ArrayList<>();
                    ids.add("off");
                    for (TierList l : TierList.values()) ids.add(l.id);
                    int i = Math.max(0, ids.indexOf(slot.list));
                    boolean back = MinecraftClient.getInstance().isShiftPressed();
                    slot.list = ids.get(Math.floorMod(i + (back ? -1 : 1), ids.size()));
                    slot.mode = "highest";
                    changed();
                    clearAndInit();
                }).dimensions(x, y, w, 20)
                .tooltip(Tooltip.of(Text.translatable("tiertaggerplus.config.slot_list.tooltip"))).build();
    }

    private Text slotListLabel(String side, TTPConfig.Slot slot) {
        TierList l = slot.tierList();
        MutableText v = l == null ? Text.translatable("options.off").formatted(Formatting.RED)
                : Text.empty().append(Icons.logoGlyph(l)).append(" ").append(Text.literal(l.displayName).setStyle(Style.EMPTY.withColor(l.color)));
        return Text.translatable("tiertaggerplus.config.slot." + side).append(": ").append(v);
    }

    private ButtonWidget slotModeButton(int x, int y, int w, TTPConfig.Slot slot) {
        TierList l = slot.tierList();
        ButtonWidget b = ButtonWidget.builder(slotModeLabel(slot), btn -> {
                    if (l == null) return;
                    List<String> modes = new ArrayList<>();
                    modes.add("highest");
                    modes.addAll(Icons.knownModes(l));
                    int i = Math.max(0, modes.indexOf(slot.mode));
                    slot.mode = modes.get(Math.floorMod(i + (MinecraftClient.getInstance().isShiftPressed() ? -1 : 1), modes.size()));
                    btn.setMessage(slotModeLabel(slot));
                    changed();
                }).dimensions(x, y, w, 20)
                .tooltip(Tooltip.of(Text.translatable("tiertaggerplus.config.slot_mode.tooltip"))).build();
        b.active = l != null;
        return b;
    }

    private Text slotModeLabel(TTPConfig.Slot slot) {
        TierList l = slot.tierList();
        MutableText v;
        if (l == null || slot.mode.equalsIgnoreCase("highest")) v = Text.translatable("tiertaggerplus.config.highest").formatted(Formatting.GOLD);
        else v = Text.empty().append(Icons.modeGlyph(l, slot.mode)).append(" " + l.modeName(slot.mode));
        return Text.translatable("tiertaggerplus.config.mode").append(": ").append(v);
    }

    private ButtonWidget toggle(int x, int y, int w, String key, Supplier<Boolean> get, Consumer<Boolean> set) {
        return ButtonWidget.builder(toggleLabel(key, get.get()), b -> {
                    set.accept(!get.get());
                    b.setMessage(toggleLabel(key, get.get()));
                    changed();
                }).dimensions(x, y, w, 20)
                .tooltip(Tooltip.of(Text.translatable("tiertaggerplus.config." + key + ".tooltip"))).build();
    }

    private static Text toggleLabel(String key, boolean on) {
        return Text.translatable("tiertaggerplus.config." + key).append(": ")
                .append(on ? ScreenTexts.ON.copy().formatted(Formatting.GREEN) : ScreenTexts.OFF.copy().formatted(Formatting.RED));
    }

    private void changed() {
        TTPConfig.save();
        Tags.invalidate();
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, 8, 0xFFFFFFFF);
        renderPreview(ctx, width / 2, 24);

        ctx.drawText(textRenderer, Text.translatable("tiertaggerplus.config.separator"), sepLabelX, sepLabelY, 0xFFE0E0E0, true);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.translatable("tiertaggerplus.config.gui_lists").formatted(Formatting.GRAY),
                width / 2, listsY, 0xFFFFFFFF);
    }

    /** Live preview of the nametag with sample tiers. */
    private void renderPreview(DrawContext ctx, int cx, int y) {
        TTPConfig cfg = TTPConfig.get();
        Text name = Text.literal(MinecraftClient.getInstance().getSession().getUsername());
        MutableText full = Text.empty();
        if (cfg.enabled) {
            full.append(sample(cfg.left, true));
            full.append(name);
            full.append(sample(cfg.right, false));
        } else {
            full.append(name);
        }
        int w = textRenderer.getWidth(full);
        ctx.fill(cx - w / 2 - 2, y - 1, cx + w / 2 + 2, y + 9, 0x40000000);
        ctx.drawText(textRenderer, full, cx - w / 2, y, 0xFFFFFFFF, false);
    }

    private Text sample(TTPConfig.Slot slot, boolean left) {
        TierList l = slot.tierList();
        if (l == null) return Text.empty();
        String mode = slot.mode.equalsIgnoreCase("highest") ? firstMode(l) : slot.mode;
        String tier = left ? "LT1" : "HT2";
        Ranking r = new Ranking(mode, l.modeName(mode), tier, null, false, dev.tiertaggerplus.api.Tiers.color(tier));
        return Tags.tagFor(l, r, left);
    }

    private static String firstMode(TierList l) {
        List<String> modes = Icons.knownModes(l);
        for (String pref : new String[]{"sword", "crystal", "vanilla", "smp", "uhc"}) if (modes.contains(pref)) return pref;
        return modes.isEmpty() ? "sword" : modes.get(0);
    }

    @Override
    public void close() {
        TTPConfig.save();
        Tags.invalidate();
        client.setScreen(parent);
    }
}
