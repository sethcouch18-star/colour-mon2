package com.example.colormons;

import com.mojang.blaze3d.platform.NativeImage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Works out which of the six groups each pixel belongs to.
 * Groups: 0 Primary, 1 Secondary, 2 Tertiary, 3 Eyes, 4 Outline, 5 Custom (never assigned here).
 */
public class TextureAnalyzer {
    public static final int GROUP_COUNT = 6;
    public static final int EYES = 3;
    public static final int OUTLINE = 4;

    public static int[] analyse(NativeImage img, NativeImage mask) {
        int w = img.getWidth(), h = img.getHeight();
        int[] group = new int[w * h];
        Arrays.fill(group, -1);

        if (mask != null && mask.getWidth() == w && mask.getHeight() == h) {
            maskGroups(img, mask, group);
            return group;
        }

        kMeansBody(img, group);
        findOutlines(img, group);
        findEyes(img, group);
        return group;
    }

    /** Red = Primary, green = Secondary, blue = Tertiary, yellow = Eyes. */
    private static void maskGroups(NativeImage img, NativeImage mask, int[] group) {
        int w = img.getWidth(), h = img.getHeight();
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            int m = mask.getPixelRGBA(x, y);
            int r = m & 0xFF, g = (m >> 8) & 0xFF, b = (m >> 16) & 0xFF, a = (m >>> 24) & 0xFF;
            if (a <= 0) continue;
            int max = Math.max(r, Math.max(g, b));
            if (max <= 128) continue;
            int idx = y * w + x;
            boolean reddish = r >= g && r >= b;
            boolean yellowish = r > 128 && g > 128 && b < 100;
            if (yellowish) group[idx] = EYES;
            else if (reddish) group[idx] = 0;
            else if (g >= b) group[idx] = 1;
            else group[idx] = 2;
        }
    }

    /** The three biggest colour clusters become Primary, Secondary and Tertiary. */
    private static void kMeansBody(NativeImage img, int[] group) {
        int w = img.getWidth(), h = img.getHeight();
        List<int[]> px = new ArrayList<>();
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            int p = img.getPixelRGBA(x, y);
            if (((p >>> 24) & 0xFF) == 0) continue;
            px.add(new int[]{x, y, p & 0xFF, (p >> 8) & 0xFF, (p >> 16) & 0xFF});
        }
        if (px.size() < 3) return;

        px.sort(Comparator.comparingDouble(q -> q[2] * 0.299 + q[3] * 0.587 + q[4] * 0.114));
        float[][] cen = new float[3][3];
        for (int k = 0; k < 3; k++) {
            int[] s = px.get(Math.min(px.size() - 1, px.size() * (2 * k + 1) / 6));
            cen[k] = new float[]{s[2], s[3], s[4]};
        }

        int[] assign = new int[px.size()];
        for (int it = 0; it < 10; it++) {
            float[][] sum = new float[3][3];
            int[] n = new int[3];
            for (int i = 0; i < px.size(); i++) {
                int[] q = px.get(i);
                int best = 0;
                float bd = Float.MAX_VALUE;
                for (int k = 0; k < 3; k++) {
                    float dr = q[2] - cen[k][0], dg = q[3] - cen[k][1], db = q[4] - cen[k][2];
                    float d = dr * dr + dg * dg + db * db;
                    if (d < bd) { bd = d; best = k; }
                }
                assign[i] = best;
                n[best]++;
                sum[best][0] += q[2]; sum[best][1] += q[3]; sum[best][2] += q[4];
            }
            for (int k = 0; k < 3; k++) {
                if (n[k] > 0) cen[k] = new float[]{sum[k][0] / n[k], sum[k][1] / n[k], sum[k][2] / n[k]};
            }
        }

        int[] count = new int[3];
        for (int a : assign) count[a]++;
        Integer[] order = {0, 1, 2};
        Arrays.sort(order, (a, b) -> count[b] - count[a]);
        for (int i = 0; i < px.size(); i++) {
            int[] q = px.get(i);
            group[q[1] * w + q[0]] = order[assign[i]];
        }
    }

    /** Dark pixels with much brighter neighbours are the border line. */
    private static void findOutlines(NativeImage img, int[] group) {
        int w = img.getWidth(), h = img.getHeight();
        float[] lum = new float[w * h];
        for (int i = 0; i < lum.length; i++) lum[i] = lum(img, i % w, i / w);

        for (int y = 1; y < h - 1; y++) for (int x = 1; x < w - 1; x++) {
            int i = y * w + x;
            if (lum[i] > 0.22f) continue;
            float maxN = 0f;
            for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dy == 0) continue;
                maxN = Math.max(maxN, lum[(y + dy) * w + (x + dx)]);
            }
            if (maxN - lum[i] > 0.30f) group[i] = OUTLINE;
        }
    }

    /** Small dark blobs surrounded by brightness are eyes. */
    private static void findEyes(NativeImage img, int[] group) {
        int w = img.getWidth(), h = img.getHeight();
        boolean[] seen = new boolean[w * h];

        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            int start = y * w + x;
            if (seen[start] || group[start] == OUTLINE) continue;
            if (lum(img, x, y) > 0.30f) continue;

            List<Integer> blob = new ArrayList<>();
            List<Integer> stack = new ArrayList<>();
            stack.add(start);
            seen[start] = true;
            boolean touchesEdge = false;

            while (!stack.isEmpty()) {
                int cur = stack.remove(stack.size() - 1);
                blob.add(cur);
                int cx = cur % w, cy = cur / w;
                if (cx == 0 || cy == 0 || cx == w - 1 || cy == h - 1) touchesEdge = true;
                for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) {
                    int nx = cx + dx, ny = cy + dy;
                    if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
                    int ni = ny * w + nx;
                    if (seen[ni]) continue;
                    if (lum(img, nx, ny) > 0.30f) continue;
                    seen[ni] = true;
                    stack.add(ni);
                }
            }

            if (touchesEdge || blob.size() < 2 || blob.size() > 40) continue;

            float around = 0f;
            int n = 0;
            boolean[] inside = new boolean[w * h];
            for (int b : blob) inside[b] = true;
            for (int b : blob) {
                int bx = b % w, by = b / w;
                for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) {
                    int nx = bx + dx, ny = by + dy;
                    if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
                    int ni = ny * w + nx;
                    if (inside[ni] || group[ni] == OUTLINE) continue;
                    around += lum(img, nx, ny);
                    n++;
                }
            }
            if (n == 0 || around / n < 0.55f) continue;
            for (int b : blob) group[b] = EYES;
        }
    }

    public static float lum(NativeImage img, int x, int y) {
        int p = img.getPixelRGBA(x, y);
        return ((p & 0xFF) * 0.299f + ((p >> 8) & 0xFF) * 0.587f + ((p >> 16) & 0xFF) * 0.114f) / 255f;
    }
}
