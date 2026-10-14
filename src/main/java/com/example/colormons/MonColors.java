package com.example.colormons;

import java.util.ArrayList;
import java.util.List;

/** Saved palette for one Pokemon. */
public class MonColors {
    public static final int GROUPS = 6;
    /** -1 means leave this group alone. Index: 0 Primary, 1 Secondary, 2 Tertiary, 3 Eyes, 4 Outline, 5 Custom. */
    public int[] colors = {-1, -1, -1, -1, -1, -1};
    /** 0 Default, 1 Glossy, 2 Matte, 3 Shiny. */
    public int finish = 0;
    public boolean keepDark = false;

    public boolean isEmpty() {
        if (finish == 1 || finish == 2) return false;
        for (int c : colors) if (c >= 0) return false;
        return true;
    }

    public void reset() {
        colors = new int[]{-1, -1, -1, -1, -1, -1};
        finish = 0;
        keepDark = false;
    }
}
