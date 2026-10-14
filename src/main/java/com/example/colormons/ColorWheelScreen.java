package com.example.colormons;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ColorWheelScreen extends Screen {
    private static final int WHEEL = 140;
    private static ResourceLocation wheelTex;

    private final UUID monId;
    private final String species;
    private final MonColors mc;
    private final List<int[]> history = new ArrayList<>();

    private int selected = 0;
    private float brightness = 1f;
    private int wx, wy, slotX, slotY;

    public ColorWheelScreen(PokemonEntity mon) {
        super(Component.literal(mon.getPokemon().getSpecies().getName() + " Colours"));
        this.monId = mon.getPokemon().getUuid();
        this.species = mon.getPokemon().getSpecies().getResourceIdentifier().getPath();
        this.mc = ColorStore.of(monId);
        for (int i = 0; i < MonColors.GROUPS; i++) {
            if (mc.colors[i] >= 0) { selected = i; break; }
        }
    }

    private void snapshot() {
        history.add(mc.colors.clone());
        if (history.size() > 20) history.remove(0);
    }

    @Override
    protected void init() {
        if (wheelTex == null) buildWheel();
        int left = width / 2 - 300;
        wx = width / 2 - 30;
        wy = height / 2 - 70;
        slotX = width / 2 + 130;
        slotY = height / 2 - 90;

        for (int i = 0; i < MonColors.GROUPS; i++) {
            final int g = i;
            addRenderableWidget(Button.builder(Component.literal(Palette.GROUP_NAMES[i]), b -> selected = g)
                    .bounds(slotX + 26, slotY + i * 26, 100, 20).build());
        }

        for (int i = 0; i < 4; i++) {
            final int f = i;
            addRenderableWidget(Button.builder(Component.literal(Palette.FINISH_NAMES[i]), b -> {
                snapshot();
                mc.finish = f;
                ColorStore.touched(monId);
            }).bounds(left + i * 74, height - 64, 70, 20).build());
        }

        addRenderableWidget(Button.builder(Component.literal("Apply"), b -> ColorStore.touched(monId))
                .bounds(left, height - 40, 70, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Undo"), b -> {
            if (!history.isEmpty()) {
                System.arraycopy(history.remove(history.size() - 1), 0, mc.colors, 0, MonColors.GROUPS);
                ColorStore.touched(monId);
            }
        }).bounds(left + 74, height - 40, 70, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Reset"), b -> {
            snapshot();
            mc.reset();
            ColorStore.clear(monId);
        }).bounds(left + 148, height - 40, 70, 20).build());
        addRenderableWidget(Button.builder(Component.literal(mc.keepDark ? "Dark pixels: keep" : "Dark pixels: normal"), b -> {
            mc.keepDark = !mc.keepDark;
            b.setMessage(Component.literal(mc.keepDark ? "Dark pixels: keep" : "Dark pixels: normal"));
            ColorStore.touched(monId);
        }).bounds(left + 222, height - 40, 100, 20).build());

        for (int i = 0; i < 4; i++) {
            final int s = i;
            addRenderableWidget(Button.builder(Component.literal("S" + (i + 1)), b -> {
                if (ColorStore.SAVED[s][0] >= 0 || ColorStore.SAVED[s][1] >= 0) {
                    snapshot();
                    System.arraycopy(ColorStore.SAVED[s], 0, mc.colors, 0, MonColors.GROUPS);
                    ColorStore.touched(monId);
                }
            }).bounds(width - 100, 20 + i * 22, 30, 18).build());
        }
        addRenderableWidget(Button.builder(Component.literal("Save"), b -> {
            for (int i = 0; i < 4; i++) {
                boolean empty = true;
                for (int v : ColorStore.SAVED[i]) if (v >= 0) empty = false;
                if (empty) {
                    System.arraycopy(mc.colors, 0, ColorStore.SAVED[i], 0, MonColors.GROUPS);
                    ColorStore.save();
                    break;
                }
            }
        }).bounds(width - 66, 20, 46, 18).build());
    }

    private void buildWheel() {
        NativeImage img = new NativeImage(WHEEL, WHEEL, true);
        float r = WHEEL / 2f;
        for (int y = 0; y < WHEEL; y++) for (int x = 0; x < WHEEL; x++) {
            float dx = x - r, dy = y - r, d = (float) Math.sqrt(dx * dx + dy * dy) / r;
            if (d > 1f) { img.setPixelRGBA(x, y, 0); continue; }
            float hue = (float) ((Math.atan2(dy, dx) / (2 * Math.PI) + 1) % 1);
            int rgb = Color.HSBtoRGB(hue, d, 1f);
            int R = (rgb >> 16) & 0xFF, G = (rgb >> 8) & 0xFF, B = rgb & 0xFF;
            img.setPixelRGBA(x, y, 0xFF000000 | (B << 16) | (G << 8) | R);
        }
        wheelTex = minecraft.getTextureManager().register("colormons/wheel", new DynamicTexture(img));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);

        int px = width / 2 - 300, py = height / 2 - 110;
        g.fill(px - 2, py - 2, px + 174, py + 216, 0xAA000000);
        g.drawString(font, species, px + 6, py + 4, 0xAAAAAA);
        g.drawString(font, "preview goes here", px + 6, py + 190, 0x555555);

        g.blit(wheelTex, wx, wy, 0, 0, WHEEL, WHEEL, WHEEL, WHEEL);

        int sx = wx + WHEEL + 6;
        for (int i = 0; i < WHEEL; i++) {
            int v = 255 - i * 255 / WHEEL;
            g.fill(sx, wy + i, sx + 10, wy + i + 1, 0xFF000000 | (v << 16) | (v << 8) | v);
        }
        int knob = wy + (int) ((1 - brightness) * WHEEL);
        g.fill(sx - 2, knob - 1, sx + 12, knob + 1, 0xFFFF0000);

        for (int i = 0; i < MonColors.GROUPS; i++) {
            int y = slotY + i * 26;
            if (i == selected) g.fill(slotX - 2, y - 2, slotX + 24, y + 22, 0xFFFFFF00);
            g.fill(slotX, y, slotX + 20, y + 20, 0xFF000000 | Palette.shown(mc, i));
            if (mc.colors[i] < 0) g.drawString(font, "-", slotX + 8, y + 6, 0xFFFFFF);
        }

        g.drawCenteredString(font, title, width / 2, 12, 0xFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int b) {
        return pick(mx, my) || super.mouseClicked(mx, my, b);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int b, double dx, double dy) {
        return pick(mx, my) || super.mouseDragged(mx, my, b, dx, dy);
    }

    private boolean pick(double mx, double my) {
        float r = WHEEL / 2f;
        int sx = wx + WHEEL + 6;
        if (mx >= sx && mx <= sx + 10 && my >= wy && my <= wy + WHEEL) {
            brightness = 1f - (float) ((my - wy) / WHEEL);
            int c = Palette.shown(mc, selected);
            float[] hsb = Color.RGBtoHSB((c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF, null);
            mc.colors[selected] = Color.HSBtoRGB(hsb[0], hsb[1], brightness) & 0xFFFFFF;
            return true;
        }
        double dx = mx - (wx + r), dy = my - (wy + r), d = Math.sqrt(dx * dx + dy * dy) / r;
        if (d <= 1) {
            float hue = (float) ((Math.atan2(dy, dx) / (2 * Math.PI) + 1) % 1);
            mc.colors[selected] = Color.HSBtoRGB(hue, (float) d, brightness) & 0xFFFFFF;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int b) {
        ColorStore.touched(monId);
        return super.mouseReleased(mx, my, b);
    }

    @Override
    public void onClose() {
        ColorStore.save();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
