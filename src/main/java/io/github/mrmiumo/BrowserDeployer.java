package io.github.mrmiumo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Manager to deploy/undeploy the browser in order to use it correctly.
 */
class BrowserDeployer {

    private final BasicLogger logger;

    /** Directory in which the expected browser will be deployed */
    private final Path workingDirectory;

    /** Path of the expected deployed browser */
    private Path deployed;

    /** Browser adapted to the current platform */
    private final Browser browser = Browser.getBrowser();

    /**
     * Creates a new Deployer that uses the given directory to deploy
     * the app.
     * @param logger the logger (non null) to print debug stuff
     * @param workingDirectory path of the app storage to deploy the
     *     embedded browser in
     */
    public BrowserDeployer(Path workingDirectory, BasicLogger logger) {
        this.logger = logger;
        this.workingDirectory = workingDirectory;
    }

    /**
     * Put the embedded browser in {@link #workingDirectory} to be able to
     * launch it later. If the browser was already deployed, it well
     * be overwritten.
     */
    public void deploy() {
        if (deployed != null) return;
        try {
            Files.deleteIfExists(workingDirectory.resolve(browser.fileName()));
            deployed = browser.copy(workingDirectory);
            logger.info("EmbeddedBrowser deployed to " + deployed);
        } catch (IOException e) {
            logger.warn("Failed to extract EmbeddedBrowser: " + e);
        }
    }

    /**
     * Removes the browser from {@link #workingDirectory} to prevent
     * starting it from a shortcut.
     */
    public void clean() {
        if (deployed == null) return;
        try {
            Files.delete(deployed);
            deployed = null;
        } catch (IOException e) {
            logger.warn("Failed to clean EmbeddedBrowser: " + e);
        }
    }

    /**
     * Executes the given code if a Browser is currently deployed.
     * @param action the code to run if a browser is deployed
     */
    public void runIfDeployed(Runnable action) {
        if (deployed != null) action.run();
    }

    /**
     * Starts a new instance of the deployed browser
     * @param args options to pass to the browser
     * @return the process associated with the new instance
     * @throws IOException if the executable cannot be started
     */
    public Process newInstance(String[] args) throws IOException {
        if (deployed == null) deploy();
        return Platform.execute(deployed.toAbsolutePath().toString(), args);
    }
}
