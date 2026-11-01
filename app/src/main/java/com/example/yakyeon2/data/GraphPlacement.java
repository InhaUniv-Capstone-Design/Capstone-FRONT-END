package com.example.yakyeon2.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Even circle when the boxes fit. Otherwise rows with the same gap.
 * Coordinates are pixels inside the given canvas.
 */
public final class GraphPlacement {

    public static final class Spot {
        private final int left;
        private final int top;

        public Spot(int left, int top) {
            this.left = left;
            this.top = top;
        }

        public int getLeft() {
            return left;
        }

        public int getTop() {
            return top;
        }
    }

    public static final class Result {
        private final List<Spot> spots;
        private final int contentHeight;
        private final boolean circled;

        public Result(List<Spot> spots, int contentHeight, boolean circled) {
            this.spots = Collections.unmodifiableList(new ArrayList<>(spots));
            this.contentHeight = contentHeight;
            this.circled = circled;
        }

        public List<Spot> spots() {
            return spots;
        }

        public int getContentHeight() {
            return contentHeight;
        }

        public boolean isCircled() {
            return circled;
        }
    }

    private GraphPlacement() {
    }

    public static Result place(int width, int height, int[] widths, int[] heights, int gap) {
        if (widths == null || heights == null || widths.length != heights.length) {
            throw new IllegalArgumentException("widths and heights must match");
        }
        int space = Math.max(0, gap);
        int count = widths.length;
        if (count == 0) {
            return new Result(new ArrayList<Spot>(), Math.max(0, height), false);
        }
        if (width < 1 || height < 1) {
            List<Spot> origin = new ArrayList<>();
            for (int index = 0; index < count; index++) {
                origin.add(new Spot(0, 0));
            }
            return new Result(origin, Math.max(0, height), false);
        }
        Result circle = circle(width, height, widths, heights, space);
        if (circle != null && separated(circle.spots(), widths, heights, space)) {
            return circle;
        }
        return rows(width, height, widths, heights, space);
    }

    private static Result circle(int width, int height, int[] widths, int[] heights, int gap) {
        int count = widths.length;
        List<Spot> spots = new ArrayList<>();
        if (count == 1) {
            spots.add(new Spot(centered(width, widths[0]), centered(height, heights[0])));
            return new Result(spots, height, true);
        }
        int maxWidth = 0;
        int maxHeight = 0;
        for (int index = 0; index < count; index++) {
            maxWidth = Math.max(maxWidth, widths[index]);
            maxHeight = Math.max(maxHeight, heights[index]);
        }
        int radiusX = (width - maxWidth) / 2 - gap;
        int radiusY = (height - maxHeight) / 2 - gap;
        if (radiusX < 1 || radiusY < 1) {
            return null;
        }
        for (int index = 0; index < count; index++) {
            double angle = -Math.PI / 2d + (2d * Math.PI * index) / count;
            int centerX = width / 2 + (int) Math.round(radiusX * Math.cos(angle));
            int centerY = height / 2 + (int) Math.round(radiusY * Math.sin(angle));
            int left = clamp(centerX - widths[index] / 2, 0, Math.max(0, width - widths[index]));
            int top = clamp(centerY - heights[index] / 2, 0, Math.max(0, height - heights[index]));
            spots.add(new Spot(left, top));
        }
        return new Result(spots, height, true);
    }

    private static Result rows(int width, int height, int[] widths, int[] heights, int gap) {
        List<Spot> spots = new ArrayList<>();
        int x = 0;
        int y = 0;
        int rowHeight = 0;
        int bottom = 0;
        for (int index = 0; index < widths.length; index++) {
            int boxWidth = Math.max(0, widths[index]);
            int boxHeight = Math.max(0, heights[index]);
            if (x > 0 && x + boxWidth > width) {
                x = 0;
                y += rowHeight + gap;
                rowHeight = 0;
            }
            spots.add(new Spot(x, y));
            x += boxWidth + gap;
            rowHeight = Math.max(rowHeight, boxHeight);
            bottom = y + rowHeight;
        }
        return new Result(spots, Math.max(height, bottom), false);
    }

    private static boolean separated(List<Spot> spots, int[] widths, int[] heights, int gap) {
        for (int left = 0; left < spots.size(); left++) {
            for (int right = left + 1; right < spots.size(); right++) {
                Spot a = spots.get(left);
                Spot b = spots.get(right);
                boolean closeX = a.getLeft() < b.getLeft() + widths[right] + gap
                        && b.getLeft() < a.getLeft() + widths[left] + gap;
                boolean closeY = a.getTop() < b.getTop() + heights[right] + gap
                        && b.getTop() < a.getTop() + heights[left] + gap;
                if (closeX && closeY) {
                    return false;
                }
            }
        }
        return true;
    }

    private static int centered(int canvas, int box) {
        return clamp((canvas - box) / 2, 0, Math.max(0, canvas - box));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
