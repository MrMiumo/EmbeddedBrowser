package io.github.mrmiumo;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Java-side representation of the browser's window to manipulate it.
 * @author Miumo
 */
public class EmbeddedBrowser {

    /** Logger of the browser */
    private final BasicLogger logger;

    /** Parent process of the browser */
    private final Process process;

    /** Channel to communicate with the browser */
    private final InterProcessCommunication ipc;

    /** Saved size when the window was closed */
    private Size onExitSize = null;

    /**
     * Creates a new Browser that uses the given directory to deploy
     * the app while it's working.
     * @param process the started process holding the browser
     * @param ipc channel setup correctly to discuss with the window
     * @throws IOException if the IPC failed to be created
     */
    EmbeddedBrowser(Process process, InterProcessCommunication ipc) throws IOException {
        this(process, ipc, new SysOutLogger());
    }

    /**
     * Creates a new Browser that uses the given directory to deploy
     * the app while it's working.
     * @param process the started process holding the browser
     * @param ipc channel setup correctly to discuss with the window
     * @param logger the logger (non null) to print debug stuff
     * @throws IOException if the IPC failed to be created
     */
    EmbeddedBrowser(Process process, InterProcessCommunication ipc, BasicLogger logger) throws IOException {
        this.logger = logger;
        this.process = process;
        this.ipc = ipc;
    }

    /**
     * Run an action when the process exit (expected stop or not)
     * @param action the action to run
     */
    void onExit(Consumer<? super Process> action) {
        process.onExit().thenAccept(p -> {
            try {
                onExitSize = Size.from(ipc.read()).orElse(null);
                ipc.close();
            } catch (Exception e) {
                logger.error("Unexpected error while closing IPC: " + e);
            }
            action.accept(p);
        });
    }

    /**
     * Force this browser to terminate now.
     */
    public void stop() {
        try {
            process.destroyForcibly().waitFor();
            ipc.close();
        } catch (Exception e) {
            logger.error("Unexpected error while closing IPC: " + e);
        }
    }

    /**
     * Blocking method that wait for the window to be ready.
     * @param timeoutMillis the number of milliseconds to wait before
     *     canceling the wait.
     * @return true if the window is ready (useful to see if the timeout
     *     was used or not)
     */
    public boolean waitForReady(long timeoutMillis) {
       return ipc.waitForReady(timeoutMillis);
    }

    /**
     * Checks if this window of the embedded browser is still alive
     * @return true if alive, false if terminated
     */
    public boolean isAlive() {
        return process.isAlive();
    }

    /**
     * Obtains the currently set title of the window.
     * @return the title of the window, empty if an error occurred
     *     (because the browser is not ready yet or if any IOException
     *     is thrown while communicating with the browser's process)
     */
    public Optional<String> getTitle() {
        try {
            return Optional.ofNullable(ipc.query("-gt"));
        } catch (IOException e) {
            logger.error("Failed to get window title: " + e);
            return Optional.empty();
        }
    }

    /**
     * Obtains the dimensions of the current window
     * @return the size of the window
     */
    public Optional<Size> getSize() {
        if (onExitSize != null) return Optional.of(onExitSize);
        try {
            return Size.from(ipc.query("-gs"));
        } catch (IOException e) {
            logger.error("Failed to get window size: " + e);
            return Optional.empty();
        }
    }

    /**
     * Pin the window always on top of other windows.
     * @return false in case of failure, true otherwise
     */
    public boolean pin() {
        try {
            return isOk(ipc.query("-p true"));
        } catch (IOException e) {
            logger.error("Failed to pin window: " + e);
            return false;
        }
    }

    /**
     * Unpin the window so that is no longer is always on top of other
     * windows. This will have no effect if the window was not pinned.
     * @return false in case of failure, true otherwise
     */
    public boolean unpin() {
        try {
            return isOk(ipc.query("-p false"));
        } catch (IOException e) {
            logger.error("Failed to pin window: " + e);
            return false;
        }
    }

    /**
     * Make the window visible.
     * This will have no effect if the window was already visible.
     * @return false in case of failure, true otherwise
     */
    public boolean show() {
        try {
            return isOk(ipc.query("-v true"));
        } catch (IOException e) {
            logger.error("Failed to show window: " + e);
            return false;
        }
    }

    /**
     * Make the window invisible (even in the task bar).
     * This will have no effect if the window was already hidden.
     * @return false in case of failure, true otherwise
     */
    public boolean hide() {
        try {
            return isOk(ipc.query("-v false"));
        } catch (IOException e) {
            logger.error("Failed to hide window: " + e);
            return false;
        }
    }

    /**
     * Changes the title of this browser window
     * @param title the new name of the window
     * @return false in case of failure, true otherwise
     */
    public boolean setTitle(String title) {
        try {
            return isOk(ipc.query("-t \"" + title + "\""));
        } catch (IOException e) {
            logger.error("Failed to pin window: " + e);
            return false;
        }
    }

    /**
     * Changes the URL displayed by the browser
     * @param url the new url starting with "http://" or "https://"
     * @return false in case of failure, true otherwise
     */
    public boolean setUrl(String url) {
        try {
            return isOk(ipc.query("-u \"" + url + "\""));
        } catch (IOException e) {
            logger.error("Failed to change window url: " + e);
            return false;
        }
    }

    /**
     * Changes the icon of the browser window
     * @param icon the path to an existing PNG file
     * @return false in case of failure, true otherwise
     */
    public boolean setIcon(Path icon) {
        try {
            return isOk(ipc.query("-i \"" + icon.toAbsolutePath() + "\""));
        } catch (IOException e) {
            logger.error("Failed to change window icon: " + e);
            return false;
        }
    }

    /**
     * Resizes the browser window to the given size
     * @param size the width and height of the window to set
     * @return false in case of failure, true otherwise
     */
    public boolean setSize(Size size) {
        try {
            return isOk(ipc.query("-s " + size));
        } catch (IOException e) {
            logger.error("Failed to change window size: " + e);
            return false;
        }
    }

    /**
     * Tests if the given string is OK or not.
     * @param s the string to check
     * @return true if OK, false otherwise
     */
    private boolean isOk(String s) {
        return s != null && "OK".equals(s);
    }
}
