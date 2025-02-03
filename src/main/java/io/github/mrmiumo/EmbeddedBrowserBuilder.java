package io.github.mrmiumo;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Objects;

import io.github.mrmiumo.EmbeddedBrowser.Size;

/**
 * Builder used to configure EmbeddedBrowser window.
 */
public class EmbeddedBrowserBuilder {

    /** Parent manager to get process from */
    private final EmbeddedBrowserManager manager;

    /** The name of the browser window */
    private final String title;

    /** The url to open in the browser */
    private final String url;

    /** Process ID of the current Java App */
    private final long pid;

    /** Whether or not to display the window always on top */
    private Boolean pin = null;

    /** Path of the icon of the browser */
    private Path icon;

    /** Size in pixels of the window */
    private Size size;
    
    /** Maximum allowed size of the window */
    private Size maxSize;

    /** Minimal allowed size of the window */
    private Size minSize;

    EmbeddedBrowserBuilder(EmbeddedBrowserManager manager, String title, String url) {
        this.manager = Objects.requireNonNull(manager);
        this.title = Objects.requireNonNull(title);
        this.url = Objects.requireNonNull(url);
        this.pid = ProcessHandle.current().pid();
    }

    /**
     * Display the window alway above other window or not.
     * Default value: false
     * @param pin true to pin the window above the other
     * @return this builder
     */
    public EmbeddedBrowserBuilder setAlwaysOnTop(boolean pin) {
        this.pin = pin;
        return this;
    }

    /**
     * Sets the icon of the application with a PNG file readable at
     * the given location.
     * @param icon the path to the desired icon
     * @throws IllegalArgumentException if the icon is not a png file
     * @return this builder
     */
    public EmbeddedBrowserBuilder setIcon(Path icon) {
        if (!icon.getFileName().toString().toLowerCase().endsWith(".png")) {
            throw new IllegalArgumentException("Icon must be a PNG file");
        }
        this.icon = icon;
        return this;
    }

    /**
     * Sets the size of the window when it shows up. This size can
     * then be changed by the user if min-size and max-size allows it.
     * @param width the width of the window in pixels (positive)
     * @param height the height of the window in pixels (positive)
     * @return this builder
     */
    public EmbeddedBrowserBuilder setSize(int width, int height) {
        size = new Size(width, height);
        return this;
    }

    /**
     * Sets the maximal size that the window cannot overpass. If these
     * values are lower than the one defined with {@link #setSize},
     * the result will be unknown.
     * By default, no maximum limit is set.
     * @param width the maximal width of the window in pixels
     * @param height the maximal height of the window in pixels
     * @return this builder
     */
    public EmbeddedBrowserBuilder setMaxSize(int width, int height) {
        maxSize = new Size(width, height);
        return this;
    }

    /**
     * Sets the minimal size that the window cannot underpass. If these
     * values are lower than the one defined with {@link #setSize},
     * the result will be unknown.
     * By default, no minimum limit is set.
     * @param width the minimum width of the window in pixels
     * @param height the minimum height of the window in pixels
     * @return this builder
     */
    public EmbeddedBrowserBuilder setMinSize(int width, int height) {
        minSize = new Size(width, height);
        return this;
    }

    /**
     * Opens a new window configured with the previously given values.
     * @return the browser object linked with the window
     */
    public EmbeddedBrowser display() {
        var args = new ArrayList<String>();
        args.add("-t");
        args.add(title);

        args.add("-u");
        args.add(url);

        args.add("-m");
        args.add(pid + "");

        if (pin == Boolean.TRUE) args.add("-p");

        if (icon != null) {
            args.add("-i");
            args.add(icon.toAbsolutePath().toString());
        }

        if (size != null) {
            args.add("-s");
            args.add(size.width() + "");
            args.add(size.height() + "");
        }

        if (maxSize != null) {
            args.add("-max");
            args.add(maxSize.width() + "");
            args.add(maxSize.height() + "");
        }

        if (minSize != null) {
            args.add("-min");
            args.add(minSize.width() + "");
            args.add(minSize.height() + "");
        }

        return manager.newWindow(args);
    }
}
