package io.github.mrmiumo;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Manager that handle embedded browser deploying, window creation and
 * environment cleaning.
 * The embedded browser is base on the Tauri.app project and adapt to
 * the current platform.
 * Before closing the app, the manager object MUST be closed in order
 * to clean properly all resources and prevent any issue with the next
 * usage of the browser.
 */
public class EmbeddedBrowserManager implements AutoCloseable {

    private final BasicLogger logger;

    private static EmbeddedBrowserManager instance;

    /** Deployer that manage deployment of the executable browser */
    private final BrowserDeployer deployer;

    /** Directory in which browser operates */
    private final Path workingDirectory;

    /** Stores launched browser ordered by their launch time */
    private ArrayDeque<EmbeddedBrowser> instances = new ArrayDeque<>();

    /**
     * Creates a new Browser that uses the given directory to deploy
     * the app while it's working.
     * @param workingDirectory path of the app storage to deploy the
     *     embedded browser in
     * @param logger the logger to use in order to output debug info
     *     in the desired location
     */
    private EmbeddedBrowserManager(Path workingDirectory, BasicLogger logger) {
        this.logger = Objects.requireNonNull(logger);
        this.workingDirectory = workingDirectory;
        deployer = new BrowserDeployer(workingDirectory, this.logger);
    }

    /**
     * Gets the existing EmbeddedBrowserManager or creates a new one.
     * @param workingDirectory directory in which the browser can be
     *     deployed (only if no manager has been created before)
     * @param logger (nullable) the logger to use in order to output
     *     debug info in the desired location
     * @return the manager
     */
    public static EmbeddedBrowserManager getManager(Path workingDirectory, BasicLogger logger) {
        if (instance == null) {
            if (logger == null) logger = new SysOutLogger();
            instance = new EmbeddedBrowserManager(workingDirectory, logger);
        }
        return instance;
    }

    /**
     * Configures and open a new window using the EmbeddedBrowser.
     * @param title the title of the window
     * @param url the url to display with the browser
     * @return a builder to fully configure the window
     */
    public EmbeddedBrowserBuilder newWindow(String title, String url) {
        return new EmbeddedBrowserBuilder(this, title, url);
    }

    /**
     * Starts the embedded browser.
     * @return the window, or null in case of failure
     */
    EmbeddedBrowser newWindow(List<String> args) {
        deployer.deploy();
        try {
            var ipc = InterProcessCommunication.from(workingDirectory);
            var arguments = new ArrayList<String>();
            arguments.add(ipc.getPath().toAbsolutePath().toString());
            arguments.addAll(args);
            var window = new EmbeddedBrowser(deployer.newInstance(arguments.toArray(String[]::new)), ipc);
            logger.info("Embedded Browser started");
            instances.offer(window);
            window.onExit(p -> {
                logger.info("Embedded Browser closed");
                instances.remove(window);
            });
            return window;
        } catch (IOException e) {
            logger.error("Failed to start Embedded Browser: " + e);
            return null;
        }
    }
    
    /**
     * Stops all the active Embedded Browser instances and cleans the
     * deployed executable file.
     */
    public void stopAll() {
        deployer.runIfDeployed(() -> {
            instances.forEach(EmbeddedBrowser::stop);
            deployer.clean();
        });
    }

    /**
     * Kills all the processes having the same name as the
     * EmbeddedBrowser. This function enable to stop browsers that are
     * no longer tracked by this class.
     * If possible, the used of {@link #stopAll()} is preferable
     */
    public void killAll() {
        try {
            for (var process : Platform.getProcesses()) {
                if (process.name().contains("EmbeddedBrowser")) Platform.kill(process.pid() + "");
            }
        } catch (IOException e) {
            logger.error("Failed to kill Embedded Browser: " + e);
        }
    }
    
    @Override
    public void close() throws Exception {
        deployer.clean();
    }
}
