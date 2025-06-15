package io.github.mrmiumo;

/**
 * Position of a window defined in pixels
 * @param x the x coordinate in pixels (horizontal)
 * @param y the y coordinate in pixels (vertical)
 */
public record Position(int x, int y) {
    @Override
    public String toString() {
        return x + " " + y;
    }
}
