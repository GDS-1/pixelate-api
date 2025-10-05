package com.spriteconverter.pixelate_api.service;

import com.spriteconverter.pixelate_api.model.QuantizationStrategy;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ColorQuantizationService {

    public BufferedImage quantize(BufferedImage image, int targetColorCount, QuantizationStrategy strategy) {
        if (strategy == null) {
            strategy = QuantizationStrategy.MEDIAN_CUT;
        }

        List<int[]> palette;

        switch (strategy) {
            case POPULARITY:
                palette = popularityQuantization(image, targetColorCount);
                break;
            case WEIGHTED:
                palette = weightedMedianCut(image, targetColorCount);
                break;
            case K_MEANS:
                palette = kmeansQuantization(image, targetColorCount);
                break;
            case MEDIAN_CUT:
            default:
                palette = medianCutQuantization(image, targetColorCount);
                break;
        }

        return applyPalette(image, palette);
    }

    private List<int[]> popularityQuantization(BufferedImage image, int targetColorCount) {
        // Count color frequencies (skip transparent)
        Map<Integer, Integer> colorFrequency = new HashMap<>();

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                Color color = new Color(image.getRGB(x, y), true);

                // SKIP TRANSPARENT PIXELS
                if (color.getAlpha() < 128) continue;

                // Store only RGB, ignore alpha
                int rgb = new Color(color.getRed(), color.getGreen(), color.getBlue()).getRGB();
                colorFrequency.put(rgb, colorFrequency.getOrDefault(rgb, 0) + 1);
            }
        }

        // Sort by frequency and take top N
        List<Map.Entry<Integer, Integer>> sortedColors = colorFrequency.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(targetColorCount)
                .collect(Collectors.toList());

        // Convert to palette format
        List<int[]> palette = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : sortedColors) {
            Color color = new Color(entry.getKey());
            palette.add(new int[]{color.getRed(), color.getGreen(), color.getBlue(), 255});
        }

        return palette;
    }

    private List<int[]> weightedMedianCut(BufferedImage image, int targetColorCount) {
        // Extract pixels with their frequencies (skip transparent)
        Map<String, WeightedColor> colorMap = new HashMap<>();

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                Color color = new Color(image.getRGB(x, y), true);

                if (color.getAlpha() < 128) continue;

                int[] rgb = new int[]{color.getRed(), color.getGreen(), color.getBlue()};
                String key = Arrays.toString(rgb);

                if (colorMap.containsKey(key)) {
                    colorMap.get(key).count++;
                } else {
                    colorMap.put(key, new WeightedColor(rgb, 1));
                }
            }
        }

        List<WeightedColor> weightedColors = new ArrayList<>(colorMap.values());

        // Start with one box containing all colors
        Queue<WeightedColorBox> boxes = new PriorityQueue<>((a, b) -> Integer.compare(b.getTotalWeight(), a.getTotalWeight()));
        boxes.add(new WeightedColorBox(weightedColors));

        // Keep splitting boxes until we have enough colors
        while (boxes.size() < targetColorCount) {
            if (boxes.isEmpty()) break;

            WeightedColorBox box = boxes.poll();

            if (box.size() <= 1) {
                boxes.add(box);
                break;
            }

            WeightedColorBox[] split = box.split();
            if (split != null) {
                boxes.add(split[0]);
                boxes.add(split[1]);
            } else {
                boxes.add(box);
                break;
            }
        }

        // Get weighted average color from each box
        List<int[]> palette = new ArrayList<>();
        for (WeightedColorBox box : boxes) {
            int[] color = box.getWeightedAverageColor();
            palette.add(new int[]{color[0], color[1], color[2], 255});
        }

        return palette;
    }

    private List<int[]> medianCutQuantization(BufferedImage image, int targetColorCount) {
        List<int[]> pixels = extractPixels(image);

        if (pixels.isEmpty()) {
            return new ArrayList<>();
        }

        return medianCut(pixels, targetColorCount);
    }

    private List<int[]> extractPixels(BufferedImage image) {
        List<int[]> pixels = new ArrayList<>();
        int width = image.getWidth();
        int height = image.getHeight();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = new Color(image.getRGB(x, y), true);

                if (color.getAlpha() < 128) continue;

                pixels.add(new int[]{color.getRed(), color.getGreen(), color.getBlue()});
            }
        }

        return pixels;
    }

    private List<int[]> medianCut(List<int[]> pixels, int targetColorCount) {
        Queue<ColorBox> boxes = new PriorityQueue<>((a, b) -> Integer.compare(b.size(), a.size()));
        boxes.add(new ColorBox(pixels));

        while (boxes.size() < targetColorCount) {
            if (boxes.isEmpty()) break;

            ColorBox box = boxes.poll();

            if (box.size() <= 1) {
                boxes.add(box);
                break;
            }

            ColorBox[] split = box.split();
            if (split != null) {
                boxes.add(split[0]);
                boxes.add(split[1]);
            } else {
                boxes.add(box);
                break;
            }
        }

        List<int[]> palette = new ArrayList<>();
        for (ColorBox box : boxes) {
            int[] color = box.getAverageColor();
            palette.add(new int[]{color[0], color[1], color[2], 255});
        }

        return palette;
    }

    private BufferedImage applyPalette(BufferedImage original, List<int[]> palette) {
        BufferedImage result = new BufferedImage(
                original.getWidth(),
                original.getHeight(),
                BufferedImage.TYPE_INT_ARGB
        );

        for (int y = 0; y < original.getHeight(); y++) {
            for (int x = 0; x < original.getWidth(); x++) {
                Color originalColor = new Color(original.getRGB(x, y), true);

                if (originalColor.getAlpha() < 128) {
                    result.setRGB(x, y, new Color(0, 0, 0, 0).getRGB());
                    continue;
                }

                int[] pixel = new int[]{
                        originalColor.getRed(),
                        originalColor.getGreen(),
                        originalColor.getBlue()
                };

                int[] nearestColor = findNearestColor(pixel, palette);
                Color newColor = new Color(nearestColor[0], nearestColor[1], nearestColor[2], 255);
                result.setRGB(x, y, newColor.getRGB());
            }
        }

        return result;
    }

    private int[] findNearestColor(int[] pixel, List<int[]> palette) {
        int[] nearest = palette.get(0);
        double minDistance = colorDistanceRGB(pixel, nearest);

        for (int[] paletteColor : palette) {
            double distance = colorDistanceRGB(pixel, paletteColor);
            if (distance < minDistance) {
                minDistance = distance;
                nearest = paletteColor;
            }
        }

        return nearest;
    }

    private double colorDistanceRGB(int[] c1, int[] c2) {
        int dr = c1[0] - c2[0];
        int dg = c1[1] - c2[1];
        int db = c1[2] - c2[2];
        return Math.sqrt(dr * dr + dg * dg + db * db);
    }

    private double colorDistance(int[] c1, int[] c2) {
        return colorDistanceRGB(c1, c2);
    }

    private List<int[]> kmeansQuantization(BufferedImage image, int targetColorCount) {
        Map<String, WeightedColor> colorMap = new HashMap<>();

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                Color color = new Color(image.getRGB(x, y), true);

                if (color.getAlpha() < 128) continue;

                int[] rgb = new int[]{color.getRed(), color.getGreen(), color.getBlue()};
                String key = Arrays.toString(rgb);

                if (colorMap.containsKey(key)) {
                    colorMap.get(key).count++;
                } else {
                    colorMap.put(key, new WeightedColor(rgb, 1));
                }
            }
        }

        List<WeightedColor> uniqueColors = new ArrayList<>(colorMap.values());

        if (uniqueColors.size() <= targetColorCount) {
            return uniqueColors.stream()
                    .map(wc -> new int[]{wc.color[0], wc.color[1], wc.color[2], 255})
                    .collect(Collectors.toList());
        }

        List<int[]> centroids = initializeCentroidsKMeansPlusPlus(uniqueColors, targetColorCount);

        int maxIterations = 50;
        boolean changed = true;
        int iteration = 0;

        while (changed && iteration < maxIterations) {
            changed = false;
            iteration++;

            Map<Integer, List<WeightedColor>> clusters = new HashMap<>();
            for (int i = 0; i < centroids.size(); i++) {
                clusters.put(i, new ArrayList<>());
            }

            for (WeightedColor wc : uniqueColors) {
                int nearestCentroid = findNearestCentroidIndex(wc.color, centroids);
                clusters.get(nearestCentroid).add(wc);
            }

            for (int i = 0; i < centroids.size(); i++) {
                List<WeightedColor> cluster = clusters.get(i);

                if (!cluster.isEmpty()) {
                    int[] newCentroid = calculateWeightedCentroid(cluster);

                    if (!Arrays.equals(centroids.get(i), newCentroid)) {
                        centroids.set(i, newCentroid);
                        changed = true;
                    }
                }
            }
        }

        List<int[]> finalPalette = new ArrayList<>();
        for (int[] centroid : centroids) {
            if (centroid != null) {
                finalPalette.add(new int[]{centroid[0], centroid[1], centroid[2], 255});
            }
        }

        return finalPalette;
    }

    private List<int[]> initializeCentroidsKMeansPlusPlus(List<WeightedColor> colors, int k) {
        List<int[]> centroids = new ArrayList<>();
        Random random = new Random(42); // Fixed seed for reproducibility

        // Choose first centroid randomly (weighted by frequency)
        int totalWeight = colors.stream().mapToInt(c -> c.count).sum();
        int randomWeight = random.nextInt(totalWeight);
        int currentWeight = 0;

        for (WeightedColor wc : colors) {
            currentWeight += wc.count;
            if (currentWeight >= randomWeight) {
                centroids.add(Arrays.copyOf(wc.color, wc.color.length));
                break;
            }
        }

        while (centroids.size() < k) {
            double[] distances = new double[colors.size()];
            double totalDistance = 0;

            for (int i = 0; i < colors.size(); i++) {
                double minDist = Double.MAX_VALUE;
                for (int[] centroid : centroids) {
                    double dist = colorDistance(colors.get(i).color, centroid);
                    minDist = Math.min(minDist, dist);
                }
                distances[i] = minDist * minDist;
                totalDistance += distances[i] * colors.get(i).count;
            }

            double randomValue = random.nextDouble() * totalDistance;
            double cumulative = 0;

            for (int i = 0; i < colors.size(); i++) {
                cumulative += distances[i] * colors.get(i).count;
                if (cumulative >= randomValue) {
                    centroids.add(Arrays.copyOf(colors.get(i).color, colors.get(i).color.length));
                    break;
                }
            }
        }

        return centroids;
    }

    private int findNearestCentroidIndex(int[] color, List<int[]> centroids) {
        int nearestIndex = 0;
        double minDistance = colorDistance(color, centroids.get(0));

        for (int i = 1; i < centroids.size(); i++) {
            double distance = colorDistance(color, centroids.get(i));
            if (distance < minDistance) {
                minDistance = distance;
                nearestIndex = i;
            }
        }

        return nearestIndex;
    }

    private int[] calculateWeightedCentroid(List<WeightedColor> cluster) {
        if (cluster.isEmpty()) {
            return new int[]{0, 0, 0};
        }

        long r = 0, g = 0, b = 0;
        int totalWeight = 0;

        for (WeightedColor wc : cluster) {
            int weight = wc.count;
            r += wc.color[0] * weight;
            g += wc.color[1] * weight;
            b += wc.color[2] * weight;
            totalWeight += weight;
        }

        if (totalWeight == 0) {
            return new int[]{0, 0, 0};
        }

        return new int[]{
                (int) (r / totalWeight),
                (int) (g / totalWeight),
                (int) (b / totalWeight)
        };
    }

    private static class WeightedColor {
        int[] color;
        int count;

        WeightedColor(int[] color, int count) {
            this.color = color;
            this.count = count;
        }
    }

    private static class WeightedColorBox {
        private List<WeightedColor> colors;

        public WeightedColorBox(List<WeightedColor> colors) {
            this.colors = new ArrayList<>(colors);
        }

        public int size() {
            return colors.size();
        }

        public int getTotalWeight() {
            return colors.stream().mapToInt(c -> c.count).sum();
        }

        public WeightedColorBox[] split() {
            if (colors.size() <= 1) return null;

            int[] mins = {255, 255, 255};
            int[] maxs = {0, 0, 0};

            for (WeightedColor wc : colors) {
                for (int i = 0; i < 3; i++) {
                    mins[i] = Math.min(mins[i], wc.color[i]);
                    maxs[i] = Math.max(maxs[i], wc.color[i]);
                }
            }

            int[] ranges = {maxs[0] - mins[0], maxs[1] - mins[1], maxs[2] - mins[2]};
            int splitChannel = 0;
            int maxRange = ranges[0];

            for (int i = 1; i < 3; i++) {
                if (ranges[i] > maxRange) {
                    maxRange = ranges[i];
                    splitChannel = i;
                }
            }

            final int channel = splitChannel;
            colors.sort(Comparator.comparingInt(c -> c.color[channel]));

            int totalWeight = getTotalWeight();
            int targetWeight = totalWeight / 2;
            int currentWeight = 0;
            int splitIndex = 0;

            for (int i = 0; i < colors.size(); i++) {
                currentWeight += colors.get(i).count;
                if (currentWeight >= targetWeight) {
                    splitIndex = i + 1;
                    break;
                }
            }

            if (splitIndex == 0) splitIndex = 1;
            if (splitIndex >= colors.size()) splitIndex = colors.size() - 1;

            List<WeightedColor> left = new ArrayList<>(colors.subList(0, splitIndex));
            List<WeightedColor> right = new ArrayList<>(colors.subList(splitIndex, colors.size()));

            return new WeightedColorBox[]{new WeightedColorBox(left), new WeightedColorBox(right)};
        }

        public int[] getWeightedAverageColor() {
            if (colors.isEmpty()) return new int[]{0, 0, 0};

            long r = 0, g = 0, b = 0;
            int totalWeight = 0;

            for (WeightedColor wc : colors) {
                int weight = wc.count;
                r += wc.color[0] * weight;
                g += wc.color[1] * weight;
                b += wc.color[2] * weight;
                totalWeight += weight;
            }

            if (totalWeight == 0) return new int[]{0, 0, 0};

            return new int[]{
                    (int) (r / totalWeight),
                    (int) (g / totalWeight),
                    (int) (b / totalWeight)
            };
        }
    }

    private static class ColorBox {
        private List<int[]> colors;

        public ColorBox(List<int[]> colors) {
            this.colors = new ArrayList<>(colors);
        }

        public int size() {
            return colors.size();
        }

        public ColorBox[] split() {
            if (colors.size() <= 1) return null;

            int[] mins = {255, 255, 255};
            int[] maxs = {0, 0, 0};

            for (int[] color : colors) {
                for (int i = 0; i < 3; i++) {
                    mins[i] = Math.min(mins[i], color[i]);
                    maxs[i] = Math.max(maxs[i], color[i]);
                }
            }

            int[] ranges = {maxs[0] - mins[0], maxs[1] - mins[1], maxs[2] - mins[2]};
            int splitChannel = 0;
            int maxRange = ranges[0];

            for (int i = 1; i < 3; i++) {
                if (ranges[i] > maxRange) {
                    maxRange = ranges[i];
                    splitChannel = i;
                }
            }

            final int channel = splitChannel;
            colors.sort(Comparator.comparingInt(c -> c[channel]));

            int medianIndex = colors.size() / 2;

            List<int[]> left = new ArrayList<>(colors.subList(0, medianIndex));
            List<int[]> right = new ArrayList<>(colors.subList(medianIndex, colors.size()));

            return new ColorBox[]{new ColorBox(left), new ColorBox(right)};
        }

        public int[] getAverageColor() {
            if (colors.isEmpty()) return new int[]{0, 0, 0};

            long r = 0, g = 0, b = 0;

            for (int[] color : colors) {
                r += color[0];
                g += color[1];
                b += color[2];
            }

            int count = colors.size();
            return new int[]{
                    (int) (r / count),
                    (int) (g / count),
                    (int) (b / count)
            };
        }
    }
}