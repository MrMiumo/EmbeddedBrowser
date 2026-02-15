package io.github.mrmiumo;

import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.sun.jna.Library;
import com.sun.jna.Native;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.internal.win32.OS;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * Enable to create new windows, customize and manipulate them.
 * @author MrMiumo
 */
public class EmbeddedBrowser {

    /** Handle to the window */
    private final Display display;

    /** Handle to the window */
    private final Shell shell;

    /** Handle to browser inside the window */
    private final Browser browser;

    /**
     * 
     * Creates a new EmbeddedBrowser that only contains a web browser using the
     * Windows WebView (Edge).
     * @param name the name of the window to create<br>
     *     /!\ Only used internally, see {@link #setTitle(String)} for the window title)
     * @param decorated true for a classic window, false to hide title
     *     bar and borders
     * @return the new EmbeddedBrowser (hidden)
     * @throws SWTError if a handle could not be obtained for browser
     *     creation
     */
    public static EmbeddedBrowser create(String name, boolean decorated) throws SWTError {
        Kernel32.INSTANCE.SetEnvironmentVariableA("WEBVIEW2_DEFAULT_BACKGROUND_COLOR", "0");
        var future = new CompletableFuture<EmbeddedBrowser>();
        var thread = new Thread(() -> {
            var window = new EmbeddedBrowser(decorated);
            future.complete(window);
            while (!window.shell.isDisposed()) {
                if (!window.display.readAndDispatch()) window.display.sleep();
            }
        });
        thread.setDaemon(true);
        thread.setName("UIthread - " + name);
        thread.start();
        try {
            return future.get();
        } catch (InterruptedException|ExecutionException e) {
            throw new IllegalStateException("Failed to start window", e);
        }
    }

    /**
     * Creates a new EmbeddedBrowser that uses the given directory to deploy
     * the app while it's working.
     * @param decorated true for a classic window, false to hide title
     *     bar and borders
     * @throws SWTError if a handle could not be obtained for browser
     *     creation
     */
    private EmbeddedBrowser(boolean decorated) throws SWTError {
        display = new Display();
        OS.setTheme(true);
        shell = new Shell(display, decorated ? SWT.SHELL_TRIM : SWT.TOOL | SWT.NO_TRIM | SWT.NO_BACKGROUND);
        shell.setLayout(new FillLayout());
        browser = new Browser(shell, SWT.EDGE | SWT.TRANSPARENT);
        shell.setVisible(false);
    }

    /**
     * Run an action when the process exit (expected stop or not)
     * @param action the action to run
     * @return this browser
     */
    public EmbeddedBrowser onExit(Consumer<Point> action) {
        return exec(() -> {
            shell.addListener(SWT.Dispose, e -> action.accept(shell.getSize()));
        });
    }

    /**
     * Force this browser to terminate now.
     */
    public void stop() {
        exec(() -> {
            if (!shell.isDisposed()) shell.dispose();
        });
    }

    /**
     * Checks if this embedded browser is still alive or have been closed
     * @return true if alive, false if terminated
     */
    public boolean isAlive() {
        return exec(() -> !shell.isDisposed());
    }

    /**
     * Pin the window always on top of other windows.
     * @return this browser
     */
    public EmbeddedBrowser pin() {
        return exec(() -> {
            OS.SetWindowPos(
                shell.handle,
                OS.HWND_TOPMOST,
                0, 0, 0, 0,
                OS.SWP_NOMOVE | OS.SWP_NOSIZE
            );
        });
    }

    /**
     * Unpin the window so that is no longer is always on top of other
     * windows. This will have no effect if the window was not pinned.
     * @return this browser
     */
    public EmbeddedBrowser unpin() {
        return exec(() -> {
            OS.SetWindowPos(
                shell.handle,
                OS.HWND_NOTOPMOST,
                0, 0, 0, 0,
                OS.SWP_NOMOVE | OS.SWP_NOSIZE
            );
        });
    }

    /**
     * Make the window visible.
     * This will have no effect if the window was already visible.
     * @return this browser
     */
    public EmbeddedBrowser show() {
        return exec(() -> shell.setVisible(true));
    }

    /**
     * Make the window invisible (even in the task bar).
     * This will have no effect if the window was already hidden.
     * @return this browser
     */
    public EmbeddedBrowser hide() {
        return exec(() -> shell.setVisible(false));
    }

    /**
     * Obtains the currently set title of the window.
     * @return the title of the window
     */
    public String getTitle() {
        return exec(() -> shell.getText());
    }

    /**
     * Changes the title of the window
     * @param title the new name of the window
     * @return this browser
     */
    public EmbeddedBrowser setTitle(String title) {
        return exec(() -> shell.setText(title));
    }

    /**
     * Changes the URL displayed by the browser
     * @param url the new url starting with "http://" or "https://"
     * @return this browser
     */
    public EmbeddedBrowser setUrl(String url) {
        return exec(() -> {
            browser.setUrl(url);
        });
    }

    /**
     * Changes the icon of the browser window
     * @param icon the PNG file to set as icon
     * @return this browser
     */
    public EmbeddedBrowser setIcon(InputStream icon) {
        return exec(() -> shell.setImage(new Image(display, icon)));
    }

    /**
     * Obtains the dimensions of the current monitor
     * @return the size of the screen
     */
    public Point getScreenSize() {
        var screen = exec(() -> display.getPrimaryMonitor().getBounds());
        return new Point(screen.width, screen.height);
    }

    /**
     * Obtains the dimensions of the current window
     * @return the size of the window
     */
    public Point getSize() {
        return exec(() -> shell.getSize());
    }

    /**
     * Resizes the browser window to the given size
     * @param size the width and height of the window to set
     * @return this browser
     */
    public EmbeddedBrowser setSize(Point size) {
        return exec(() -> shell.setSize(size));
    }

    /**
     * Sets the maximal size that the window cannot overpass. If these
     * values are lower than the one defined with {@link #setSize},
     * the result will be unknown.
     * By default, no maximum limit is set.
     * @param max the maximal width and height of the window in pixels
     * @return this browser
     */
    public EmbeddedBrowser setMaxSize(Point max) {
        return exec(() -> shell.setMaximumSize(max));
    }

    /**
     * Sets the minimal size that the window cannot underpass. If these
     * values are lower than the one defined with {@link #setSize},
     * the result will be unknown.
     * By default, no minimum limit is set.
     * @param min the minimum width and height of the window in pixels
     * @return this browser
     */
    public EmbeddedBrowser setMinSize(Point min) {
        return exec(() -> shell.setMinimumSize(min));
    }
    
    /**
     * Move the browser window to the given position.
     * Be careful: the position may leads the window to go outside the
     * screen! 
     * @param position the new coordinates of the window
     * @return this browser
     */
    public EmbeddedBrowser setPosition(Point position) {
        return exec(() -> shell.setLocation(position));
    }

    /**
     * Run some code that modifies the window on the UI thread.
     * @param action the code to run
     * @return this browser
     */
    private EmbeddedBrowser exec(Runnable action) {
        display.syncExec(action);
        return this;
    }

    /**
     * Run some code that modifies the window on the UI thread and gets
     * the return value
     * @param action the code to run
     * @return the return value of the given action
     */
    private <T> T exec(Supplier<T> action) {
        var response =  new Object(){
            T value = null;
        };
        display.syncExec(() -> response.value = action.get());
        return response.value;
    }

    /**
     * Allows to have transparent browser
     */
    interface Kernel32 extends Library {
        Kernel32 INSTANCE = Native.load("kernel32", Kernel32.class);
        boolean SetEnvironmentVariableA(String lpName, String lpValue);
    }
}

