package dev.tiertaggerplus.gui;

import dev.tiertaggerplus.api.Ranking;
import dev.tiertaggerplus.api.TierCache;
import dev.tiertaggerplus.api.TierList;
import dev.tiertaggerplus.api.TierProfile;
import dev.tiertaggerplus.api.Tiers;
import dev.tiertaggerplus.config.TTPConfig;
import dev.tiertaggerplus.render.Icons;
import dev.tiertaggerplus.render.Tags;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.entity.player.PlayerSkinType;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The K menu: search bar, the looked-up player's skin waving on the left, and every tierlist's
 * rankings side by side on the right.
 */
public class TierScreen extends Screen {
    private static final int CARD_MIN_W = 150;
    private static final int GAP = 6;
    private static final int ROW_H = 12;
    private static final int HEADER_H = 24;

    private final @Nullable Screen parent;
    private String playerName;
    private TierCache.@Nullable PlayerTiers player;
    private SkinTextures skin = DefaultSkinHelper.getSteve();
    private boolean skinLoading;

    private TextFieldWidget search;
    private PlayerEntityModel wideModel, slimModel;
    private float skinYaw = -25f, skinPitch = -5f;
    private boolean draggingSkin;
    private long waveStart = Util.getMeasuringTimeMs();

    private double scroll;
    private int contentHeight;
    private final List<String> suggestions = new ArrayList<>();

    // layout (computed in init)
    private int panelX, panelY, panelW, panelH, skinX, skinY, skinW, skinH, areaX, areaY, areaW, areaH;

    public TierScreen(@Nullable Screen parent, @Nullable String name) {
        super(Text.translatable("tiertaggerplus.gui.title"));
        this.parent = parent;
        this.playerName = name != null ? name : MinecraftClient.getInstance().getSession().getUsername();
    }

    @Override
    protected void init() {
        var models = client.getLoadedEntityModels();
        wideModel = new PlayerEntityModel(models.getModelPart(EntityModelLayers.PLAYER), false);
        slimModel = new PlayerEntityModel(models.getModelPart(new net.minecraft.client.render.entity.model.EntityModelLayer(net.minecraft.util.Identifier.ofVanilla("player_slim"), "main")), true);

        int searchW = Math.min(200, width - 200);
        String prev = search != null ? search.getText() : "";
        search = new TextFieldWidget(textRenderer, width / 2 - searchW / 2 - 25, 8, searchW, 18, Text.translatable("tiertaggerplus.gui.search"));
        search.setMaxLength(16);
        search.setTextPredicate(s -> s.matches("[A-Za-z0-9_]*"));
        search.setPlaceholder(Text.translatable("tiertaggerplus.gui.search").formatted(Formatting.DARK_GRAY));
        search.setText(prev);
        search.setChangedListener(this::updateSuggestions);
        addDrawableChild(search);
        addDrawableChild(ButtonWidget.builder(Text.translatable("tiertaggerplus.gui.search_button"), b -> doSearch())
                .dimensions(search.getX() + searchW + 4, 7, 50, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.translatable("tiertaggerplus.gui.me"), b -> show(client.getSession().getUsername()))
                .dimensions(6, 7, 40, 20).tooltip(Tooltip.of(Text.translatable("tiertaggerplus.gui.me.tooltip"))).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("tiertaggerplus.gui.settings"), b -> client.setScreen(new ConfigScreen(this)))
                .dimensions(width - 66, 7, 60, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("tiertaggerplus.gui.refresh"), b -> {
                    if (player != null) TierCache.refresh(player);
                }).dimensions(6, height - 26, 60, 20)
                .tooltip(Tooltip.of(Text.translatable("tiertaggerplus.gui.refresh.tooltip"))).build());

        panelX = 6;
        panelY = 34;
        panelW = MathHelper.clamp(width / 4, 100, 170);
        panelH = height - panelY - 32;
        skinW = panelW - 16;
        skinH = Math.max(40, Math.min(panelH - 70, (int) (skinW * 1.7)));
        skinX = panelX + 8;
        skinY = panelY + 20;
        areaX = panelX + panelW + 8;
        areaY = panelY;
        areaW = width - areaX - 6;
        areaH = height - areaY - 32;

        show(playerName);
    }

    private void show(String name) {
        if (!TierCache.isValidName(name)) return;
        boolean changed = player == null || !player.name.equalsIgnoreCase(name);
        playerName = name;
        player = TierCache.get(name);
        if (player != null) for (TierList l : TierList.values()) if (TTPConfig.get().isShownInGui(l)) TierCache.request(player, l);
        if (changed) {
            scroll = 0;
            waveStart = Util.getMeasuringTimeMs();
            skin = DefaultSkinHelper.getSteve();
            skinLoading = true;
            String requested = name;
            SkinLoader.load(name).thenAccept(s -> client.execute(() -> {
                if (requested.equalsIgnoreCase(playerName)) {
                    skin = s;
                    skinLoading = false;
                }
            }));
        }
        suggestions.clear();
    }

    private void doSearch() {
        String q = search.getText().trim();
        if (TierCache.isValidName(q)) {
            show(q);
            search.setText("");
            search.setFocused(false);
            setFocused(null);
        }
    }

    private void updateSuggestions(String text) {
        suggestions.clear();
        if (text.isEmpty() || client.getNetworkHandler() == null) return;
        String low = text.toLowerCase(Locale.ROOT);
        for (PlayerListEntry e : client.getNetworkHandler().getPlayerList()) {
            String n = e.getProfile().name();
            if (n.toLowerCase(Locale.ROOT).startsWith(low) && !n.equalsIgnoreCase(text)) suggestions.add(n);
            if (suggestions.size() >= 6) break;
        }
        suggestions.sort(String.CASE_INSENSITIVE_ORDER);
    }

    // ------------------------------------------------------------------------------------------
    // rendering
    // ------------------------------------------------------------------------------------------

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        if (player != null) for (TierList l : TierList.values()) if (TTPConfig.get().isShownInGui(l)) TierCache.request(player, l);

        renderPlayerPanel(ctx, mouseX, mouseY);
        renderCards(ctx, mouseX, mouseY);
        renderSuggestions(ctx, mouseX, mouseY);

        ctx.drawText(textRenderer, Text.translatable("tiertaggerplus.gui.hint").formatted(Formatting.DARK_GRAY),
                72, height - 20, 0xFFFFFFFF, false);
    }

    private void renderPlayerPanel(DrawContext ctx, int mouseX, int mouseY) {
        ctx.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0x90000000);
        ctx.drawStrokedRectangle(panelX, panelY, panelW, panelH, 0x40FFFFFF);

        String shown = player != null ? player.name : playerName;
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(shown).formatted(Formatting.BOLD), panelX + panelW / 2, panelY + 6, 0xFFFFFFFF);

        // waving skin
        PlayerEntityModel model = skin.model() == PlayerSkinType.SLIM ? slimModel : wideModel;
        animate(model);
        float scale = 0.97f * skinH / 2.125f;
        ctx.addPlayerSkin(model, skin.body().texturePath(), scale, skinPitch, skinYaw, -1.0625f, skinX, skinY, skinX + skinW, skinY + skinH);
        if (skinLoading)
            ctx.drawCenteredTextWithShadow(textRenderer, Text.translatable("tiertaggerplus.gui.loading_skin").formatted(Formatting.GRAY),
                    panelX + panelW / 2, skinY + skinH - 10, 0xFFFFFFFF);

        // summary under the skin
        int y = skinY + skinH + 6;
        int ranked = 0, total = 0, done = 0;
        Ranking best = null;
        TierList bestList = null;
        if (player != null) for (TierList l : TierList.values()) {
            if (!TTPConfig.get().isShownInGui(l)) continue;
            total++;
            TierProfile p = player.peek(l);
            if (p == null || !p.isDone()) continue;
            done++;
            if (p.status == TierProfile.Status.READY) {
                ranked++;
                Ranking h = p.highest(false).orElse(null);
                if (h != null && (best == null || h.score() > best.score())) {
                    best = h;
                    bestList = l;
                }
            }
        }
        int cx = panelX + panelW / 2;
        if (done < total) {
            ctx.drawCenteredTextWithShadow(textRenderer, Text.translatable("tiertaggerplus.gui.searching", done, total).formatted(Formatting.GRAY), cx, y, 0xFFFFFFFF);
        } else {
            ctx.drawCenteredTextWithShadow(textRenderer, Text.translatable("tiertaggerplus.gui.ranked_on", ranked, total).formatted(Formatting.GRAY), cx, y, 0xFFFFFFFF);
        }
        if (best != null) {
            MutableText line = Text.empty().append(Icons.modeGlyph(bestList, best.modeId())).append(" ")
                    .append(Tags.tierText(best)).append(Text.literal(" " + best.modeName()).formatted(Formatting.WHITE));
            ctx.drawCenteredTextWithShadow(textRenderer, Text.translatable("tiertaggerplus.gui.best").formatted(Formatting.GRAY), cx, y + 12, 0xFFFFFFFF);
            ctx.drawCenteredTextWithShadow(textRenderer, line, cx, y + 23, 0xFFFFFFFF);
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(bestList.displayName).setStyle(Style.EMPTY.withColor(bestList.color)), cx, y + 34, 0xFFFFFFFF);
        }
    }

    /** A friendly wave: right arm raised and swinging, head bobbing along. */
    private void animate(PlayerEntityModel m) {
        float t = (Util.getMeasuringTimeMs() - waveStart) / 1000f;
        m.head.resetTransform();
        m.rightArm.resetTransform();
        m.leftArm.resetTransform();
        m.body.resetTransform();
        m.rightLeg.resetTransform();
        m.leftLeg.resetTransform();

        // raise the arm over ~0.4s, then wave side to side
        float raise = MathHelper.clamp(t / 0.4f, 0f, 1f);
        raise = raise * raise * (3 - 2 * raise);
        float wave = MathHelper.sin(t * 9f) * 0.35f * raise;
        m.rightArm.roll = raise * 2.55f + wave;
        m.rightArm.pitch = -0.15f * raise;
        m.head.roll = MathHelper.sin(t * 9f) * 0.06f * raise;
        m.head.yaw = MathHelper.sin(t * 1.3f) * 0.12f;
        m.leftArm.roll = -0.07f - MathHelper.sin(t * 2.2f) * 0.04f;
        m.leftArm.pitch = MathHelper.sin(t * 1.8f) * 0.05f;
    }

    private record Card(TierList list, TierProfile profile, int height) {
    }

    private void renderCards(DrawContext ctx, int mouseX, int mouseY) {
        if (player == null) return;
        List<Card> cards = new ArrayList<>();
        for (TierList l : TierList.values()) {
            if (!TTPConfig.get().isShownInGui(l)) continue;
            TierProfile p = player.peek(l);
            if (p == null) p = TierProfile.loading(l);
            int rows = p.status == TierProfile.Status.READY ? p.rankings.size() : 1;
            cards.add(new Card(l, p, HEADER_H + rows * ROW_H + 6));
        }
        if (cards.isEmpty()) return;

        int cols = Math.max(1, Math.min(cards.size(), (areaW + GAP) / (CARD_MIN_W + GAP)));
        int cardW = (areaW - (cols - 1) * GAP) / cols;
        int[] colY = new int[cols];

        ctx.enableScissor(areaX, areaY, areaX + areaW, areaY + areaH);
        List<Text> tooltip = null;
        boolean mouseInArea = mouseX >= areaX && mouseX < areaX + areaW && mouseY >= areaY && mouseY < areaY + areaH;
        for (Card c : cards) {
            int col = 0;
            for (int i = 1; i < cols; i++) if (colY[i] < colY[col]) col = i;
            int x = areaX + col * (cardW + GAP);
            int y = areaY + colY[col] - (int) scroll;
            colY[col] += c.height + GAP;
            List<Text> tt = drawCard(ctx, c, x, y, cardW, mouseInArea ? mouseX : -1, mouseY);
            if (tt != null) tooltip = tt;
        }
        ctx.disableScissor();

        int max = 0;
        for (int h : colY) max = Math.max(max, h);
        contentHeight = max;
        scroll = MathHelper.clamp(scroll, 0, Math.max(0, contentHeight - areaH));

        if (contentHeight > areaH) {
            int barH = Math.max(16, areaH * areaH / contentHeight);
            int barY = areaY + (int) ((areaH - barH) * (scroll / (contentHeight - areaH)));
            ctx.fill(width - 4, areaY, width - 2, areaY + areaH, 0x30FFFFFF);
            ctx.fill(width - 4, barY, width - 2, barY + barH, 0xA0FFFFFF);
        }
        if (tooltip != null) ctx.drawTooltip(textRenderer, tooltip, mouseX, mouseY);
    }

    private @Nullable List<Text> drawCard(DrawContext ctx, Card c, int x, int y, int w, int mouseX, int mouseY) {
        TierList l = c.list;
        TierProfile p = c.profile;
        int h = c.height;
        if (y > areaY + areaH || y + h < areaY) return null;
        int accent = 0xFF000000 | l.color;
        ctx.fill(x, y, x + w, y + h, 0xB0101014);
        ctx.fill(x, y, x + w, y + HEADER_H, (0x30 << 24) | l.color);
        ctx.fill(x, y, x + w, y + 1, accent);
        ctx.drawStrokedRectangle(x, y, w, h, (0x50 << 24) | l.color);

        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, Icons.logoTexture(l), x + 4, y + 4, 0, 0, 16, 16, 64, 64, 64, 64);
        ctx.drawText(textRenderer, Text.literal(l.displayName).setStyle(Style.EMPTY.withColor(l.color).withBold(true)), x + 24, y + 5, 0xFFFFFFFF, true);
        String info = info(p);
        ctx.drawText(textRenderer, Text.literal(info).formatted(Formatting.GRAY), x + 24, y + 14, 0xFFFFFFFF, false);

        List<Text> tooltip = null;
        if (mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + HEADER_H) {
            tooltip = new ArrayList<>(List.of(Text.literal(l.displayName).setStyle(Style.EMPTY.withColor(l.color)),
                    Text.literal(l.website).formatted(Formatting.DARK_GRAY)));
            if (p.position != null) tooltip.add(Text.translatable("tiertaggerplus.gui.overall", p.position).formatted(Formatting.GRAY));
            if (p.points != null) tooltip.add(Text.translatable("tiertaggerplus.gui.points", p.points).formatted(Formatting.GRAY));
            if (p.region != null) tooltip.add(Text.translatable("tiertaggerplus.gui.region", p.region).formatted(Formatting.GRAY));
        }

        int ry = y + HEADER_H + 3;
        switch (p.status) {
            case LOADING -> {
                String dots = ".".repeat((int) (Util.getMeasuringTimeMs() / 300 % 4));
                ctx.drawText(textRenderer, Text.translatable("tiertaggerplus.gui.loading").append(dots).formatted(Formatting.GRAY), x + 6, ry + 2, 0xFFFFFFFF, false);
            }
            case UNRANKED -> ctx.drawText(textRenderer, Text.translatable("tiertaggerplus.gui.unranked").formatted(Formatting.DARK_GRAY), x + 6, ry + 2, 0xFFFFFFFF, false);
            case ERROR -> {
                ctx.drawText(textRenderer, Text.translatable("tiertaggerplus.gui.error").formatted(Formatting.RED), x + 6, ry + 2, 0xFFFFFFFF, false);
                if (mouseX >= x && mouseX < x + w && mouseY >= ry && mouseY < ry + ROW_H)
                    tooltip = List.of(Text.literal(String.valueOf(p.error)).formatted(Formatting.RED), Text.translatable("tiertaggerplus.gui.error.tooltip").formatted(Formatting.GRAY));
            }
            case READY -> {
                for (Ranking r : p.rankings) {
                    boolean hover = mouseX >= x && mouseX < x + w && mouseY >= ry && mouseY < ry + ROW_H;
                    if (hover) ctx.fill(x + 1, ry, x + w - 1, ry + ROW_H, 0x20FFFFFF);
                    ctx.drawTexture(RenderPipelines.GUI_TEXTURED, Icons.modeTexture(l, r.modeId()), x + 5, ry + 1, 0, 0, 10, 10, 32, 32, 32, 32);
                    MutableText tier = Tags.tierText(r);
                    int tierW = textRenderer.getWidth(tier);
                    int nameMax = w - 22 - tierW - 8;
                    String nm = r.modeName();
                    if (r.peak() != null && TTPConfig.get() != null) {
                        MutableText peak = Text.literal("(" + r.peak() + ")").setStyle(Style.EMPTY.withColor(darken(Tiers.color(r.peak()))));
                        int pw = textRenderer.getWidth(peak);
                        if (nameMax - pw - 4 > 30) {
                            ctx.drawText(textRenderer, peak, x + w - 6 - tierW - 3 - pw, ry + 2, 0xFFFFFFFF, false);
                            nameMax -= pw + 4;
                        }
                    }
                    if (textRenderer.getWidth(nm) > nameMax) nm = textRenderer.trimToWidth(nm, Math.max(0, nameMax - 6)) + "…";
                    ctx.drawText(textRenderer, Text.literal(nm).formatted(r.retired() ? Formatting.DARK_GRAY : Formatting.WHITE), x + 19, ry + 2, 0xFFFFFFFF, false);
                    ctx.drawText(textRenderer, tier, x + w - 6 - tierW, ry + 2, 0xFFFFFFFF, true);
                    if (hover) tooltip = rankingTooltip(l, r);
                    ry += ROW_H;
                }
            }
        }
        return tooltip;
    }

    private static String info(TierProfile p) {
        List<String> parts = new ArrayList<>();
        if (p.region != null) parts.add(p.region);
        if (p.position != null) parts.add("#" + p.position);
        if (p.points != null) parts.add(p.points + " pts");
        if (parts.isEmpty() && p.status == TierProfile.Status.READY) parts.add(p.rankings.size() + (p.rankings.size() == 1 ? " mode" : " modes"));
        return String.join(" · ", parts);
    }

    private static List<Text> rankingTooltip(TierList l, Ranking r) {
        List<Text> out = new ArrayList<>();
        out.add(Text.empty().append(Icons.modeGlyph(l, r.modeId())).append(" ").append(Text.literal(r.modeName()).formatted(Formatting.WHITE)));
        out.add(Text.literal(longTier(r.tier())).setStyle(Style.EMPTY.withColor(r.color())));
        if (r.peak() != null) out.add(Text.translatable("tiertaggerplus.gui.peak", longTier(r.peak())).setStyle(Style.EMPTY.withColor(Tiers.color(r.peak()))));
        if (r.retired()) out.add(Text.translatable("tiertaggerplus.gui.retired").formatted(Formatting.DARK_GRAY));
        out.add(Text.literal(l.displayName).setStyle(Style.EMPTY.withColor(l.color)));
        return out;
    }

    private static String longTier(String t) {
        if (t.length() == 3 && t.charAt(1) == 'T') {
            String level = switch (t.charAt(0)) {
                case 'H' -> "High";
                case 'M' -> "Mid";
                default -> "Low";
            };
            return level + " Tier " + t.charAt(2);
        }
        return "Tier " + t;
    }

    private static int darken(int rgb) {
        int r = ((rgb >> 16) & 0xFF) * 3 / 4, g = ((rgb >> 8) & 0xFF) * 3 / 4, b = (rgb & 0xFF) * 3 / 4;
        return (r << 16) | (g << 8) | b;
    }

    private void renderSuggestions(DrawContext ctx, int mouseX, int mouseY) {
        if (suggestions.isEmpty() || !search.isFocused()) return;
        int x = search.getX(), y = search.getY() + search.getHeight() + 1, w = search.getWidth();
        ctx.fill(x, y, x + w, y + suggestions.size() * 12 + 2, 0xF0101014);
        ctx.drawStrokedRectangle(x, y, w, suggestions.size() * 12 + 2, 0x60FFFFFF);
        for (int i = 0; i < suggestions.size(); i++) {
            int ry = y + 1 + i * 12;
            boolean hover = mouseX >= x && mouseX < x + w && mouseY >= ry && mouseY < ry + 12;
            if (hover) ctx.fill(x + 1, ry, x + w - 1, ry + 12, 0x40FFFFFF);
            ctx.drawText(textRenderer, suggestions.get(i), x + 4, ry + 2, hover ? 0xFFFFFF55 : 0xFFE0E0E0, false);
        }
    }

    // ------------------------------------------------------------------------------------------
    // input
    // ------------------------------------------------------------------------------------------

    @Override
    public boolean keyPressed(KeyInput input) {
        if (search.isFocused() && (input.key() == GLFW.GLFW_KEY_ENTER || input.key() == GLFW.GLFW_KEY_KP_ENTER)) {
            if (!suggestions.isEmpty() && !TierCache.isValidName(search.getText())) search.setText(suggestions.get(0));
            doSearch();
            return true;
        }
        if (search.isFocused() && input.key() == GLFW.GLFW_KEY_TAB && !suggestions.isEmpty()) {
            search.setText(suggestions.get(0));
            return true;
        }
        if (!search.isFocused() && dev.tiertaggerplus.TierTaggerPlus.openKey.matchesKey(input)) {
            close();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (search.isFocused() && !suggestions.isEmpty()) {
            int x = search.getX(), y = search.getY() + search.getHeight() + 1;
            for (int i = 0; i < suggestions.size(); i++) {
                int ry = y + 1 + i * 12;
                if (click.x() >= x && click.x() < x + search.getWidth() && click.y() >= ry && click.y() < ry + 12) {
                    search.setText(suggestions.get(i));
                    doSearch();
                    return true;
                }
            }
        }
        if (click.x() >= skinX && click.x() < skinX + skinW && click.y() >= skinY && click.y() < skinY + skinH) {
            draggingSkin = true;
            waveStart = Util.getMeasuringTimeMs(); // wave again when poked
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double dx, double dy) {
        if (draggingSkin) {
            skinYaw += (float) dx * 2.5f;
            skinPitch = MathHelper.clamp(skinPitch - (float) dy * 2.5f, -50f, 50f);
            return true;
        }
        return super.mouseDragged(click, dx, dy);
    }

    @Override
    public boolean mouseReleased(Click click) {
        draggingSkin = false;
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double h, double v) {
        if (mouseX >= areaX) {
            scroll = MathHelper.clamp(scroll - v * 24, 0, Math.max(0, contentHeight - areaH));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, h, v);
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
