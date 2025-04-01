package io.github.mrmiumo;

import java.util.Optional;

/**
 * Dimensions of a window defined by a width and a height in px.
 * @param width the number of pixels horizontally
 * @param height the number of pixels vertically
 */
public record Size(int width, int height) {
    /**
     * Creates a new Size object containing a width and a height
     * in pixels
     * @param width the number of pixel horizontally
     * @param height the number of pixel vertically
     */
    public Size {
        if (width < 0 || height < 0) throw new IllegalArgumentException("Size must be positive");
    }

    /**
     * Parses the given string to create a Size object from it.
     * A valid string to parse with the " " separator is like "123 456"
     * @param s the string to parse without spaces
     * @return the size, or empty if invalid
     */
    public static Optional<Size> from(String s) {
        if (s == null) return Optional.empty();
        var split = s.split(" ");
        if (split.length != 2) return Optional.empty();
        try {
            return Optional.of(new Size(Integer.valueOf(split[0]), Integer.valueOf(split[1])));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /**
     * Ensure the height and width of this Size or in bounds for
     * the given min and max size. If not, adjust values to make
     * this size respect the min and max constraints.
     * If min and max are inverted or invalid in any way, 
     * @param min the minimum allowed width and height
     * @param max the maximum allowed width and height
     * @return the coerced size
     */
    public Size coerce(Size min, Size max) {
        max = max == null ? new Size(Integer.MAX_VALUE, Integer.MAX_VALUE) : max; // ensure not null
        min = min == null ? new Size(50, 25) : new Size(Math.max(min.width, 0), Math.max(min.height, 0)); // ensure positive

        int width = this.width;
        if (min.width > max.width && width < 50) width = 50;
        else width = Math.max(Math.min(width, max.width), min.width);

        int height = this.height;
        if (min.height > max.height && height < 25) height = 25;
        else height = Math.max(Math.min(height, max.height), min.height);

        return new Size(width, height);
    }

    /**
     * Parses the given string to create a Size object from it.
     * Acts the same as `from(s).orElse(null)`
     * @param s the string to parse without spaces
     * @return the size, or null if invalid
     */
    public static Size valueOf(String s) {
        return from(s).orElse(null);
    }

    @Override
    public String toString() {
        return width + " " + height;
    }
}
