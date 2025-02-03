package io.github.mrmiumo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Methods related to the embedded browser than adapts to the different
 * platforms and Operating Systems.
 */
enum Browser {
    WINDOWS("EmbeddedBrowser.exe"),
    MAC_OS(null),
    LINUX(null);

    private final String fileName;

    Browser(String fileName) {
        this.fileName = fileName;
    }

    /**
     * Copies the executable file of this browser to the given
     * destination directory.
     * @param folder the existing directory to copy the file in
     * @return the path at which the file got copied
     * @throws IOException if an error occurs while copying the file
     */
    public Path copy(Path folder) throws IOException {
        if (fileName == null) throw new UnsupportedOperationException("This operating system is not supported yet");
        folder = folder.resolve(fileName);
        try (
            var dst = Files.newOutputStream(folder);
            var src = BrowserDeployer.class.getResourceAsStream("/embeddedBrowsers/" + fileName);
        ) {
            src.transferTo(dst);
        }
        return folder;
    }

    /**
     * Gets the file name of the browser regarding the current platform
     * @return the filename of the browser
     */
    public String fileName() {
        return fileName;
    }
    /**
     * Acquire the browser adapted to the current platform.
     * @return the browser
     */
    public static Browser getBrowser() {
        return switch (Platform.OS) {
            case WINDOWS -> WINDOWS;
            case MAC_OS -> MAC_OS;
            case LINUX -> LINUX;
        };
    }
}
