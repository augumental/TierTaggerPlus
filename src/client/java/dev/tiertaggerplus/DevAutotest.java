package dev.tiertaggerplus;

import dev.tiertaggerplus.gui.ConfigScreen;
import dev.tiertaggerplus.gui.TierScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.util.ScreenshotRecorder;

/**
 * Development-only screenshot run, enabled with -Dtiertaggerplus.autotest=name1,name2.
 * Does nothing in normal use.
 */
final class DevAutotest {
    private static int tick = -1;
    private static int step;

    static void register() {
        String prop = System.getProperty("tiertaggerplus.autotest");
        if (prop == null || prop.isBlank()) return;
        String[] names = prop.split(",");
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (tick < 0) {
                if (mc.currentScreen instanceof TitleScreen) tick = 0;
                return;
            }
            tick++;
            int phase = tick % 260;
            if (phase == 1) {
                if (step < names.length) mc.setScreen(new TierScreen(null, names[step]));
                else if (step == names.length) mc.setScreen(new ConfigScreen(null));
                else mc.scheduleStop();
            }
            if (phase == 100 || phase == 250) shot(mc, (step < names.length ? "tier_" + names[step] : "config") + "_" + phase + ".png");
            if (phase == 259) step++;
        });
    }

    private static void shot(MinecraftClient mc, String file) {
        ScreenshotRecorder.saveScreenshot(mc.runDirectory, file, mc.getFramebuffer(), 1, t -> TierTaggerPlus.LOGGER.info("autotest: {}", t.getString()));
    }

    private DevAutotest() {
    }
}
