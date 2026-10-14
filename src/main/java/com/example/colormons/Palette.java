package com.example.colormons;

/** Group names and the fallback swatch colours the UI draws before you pick anything. */
public class Palette {
    public static final String[] GROUP_NAMES = {"Primary", "Secondary", "Tertiary", "Eyes", "Outline", "Custom"};
    public static final int[] FALLBACK = {0xFFCC33, 0xCC3333, 0x2A2A2A, 0x33BBFF, 0x111111, 0xBB66FF};
    public static final String[] FINISH_NAMES = {"Default", "Glossy", "Matte", "Shiny"};

    public static int shown(MonColors mc, int group) {
        return mc.colors[group] >= 0 ? mc.colors[group] : FALLBACK[group];
    }
}
