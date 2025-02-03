package io.github.mrmiumo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;

class Platform {

    private static final Runtime RUNTIME = Runtime.getRuntime();

    /** The currently used operating system */
    public static final Os OS = Os.getCurrent();

    /** List of known browser to try open page with on Linux */
    private static final List<String> BROWSERS = List.of("xdg-open",
        "google-chrome", "firefox", "mozilla", "epiphany", "konqueror",
        "netscape", "opera", "links", "lynx", "chromium", "brave-browser");

    /**
     * Run a system command.
     * @param cmd the command to call
     * @param args arguments to pass to the command
     * @return A new Process object for managing the subprocess
     * @throws IOException If an I/O error occurs
     */
    public static Process execute(String cmd, String... args) throws IOException {
        String[] arr = new String[args.length + 1];
        arr[0] = cmd;
        for (var i = 1 ; i < arr.length ; i++) arr[i] = args[i - 1];
        return RUNTIME.exec(arr);
    }

    /**
     * Run a system command.
     * @param cmd the command to call
     * @param args arguments to pass to the command
     * @return A new Process object for managing the subprocess
     * @throws IOException If an I/O error occurs
     */
    public static Process execute(Path location, String cmd, String... args) throws IOException {
        var baseArg = switch (OS) {
            case MAC_OS -> new String[] {"/bin/sh", "-c", cmd};
            case WINDOWS -> new String[] {"cmd", "/k", "start", cmd};
            case LINUX -> new String[] {"/bin/sh", "-c", cmd};
        };
        String[] arr = new String[args.length + baseArg.length];
        var i = 0;
        for (; i < baseArg.length ; i++) arr[i] = baseArg[i];
        for (; i < arr.length ; i++) arr[i] = args[i - baseArg.length];
        return RUNTIME.exec(arr, null, location.toFile());
    }

    /**
     * Run a system command.
     * @param cmd the command to call
     * @param args arguments to pass to the command
     * @return A new Process object for managing the subprocess
     * @throws UncheckedIOException If an I/O error occurs
     */
    private static Process exec(String cmd, String... args) {
        try {
            return execute(cmd, args);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static int tryExec(String cmd, String... args) {
        try {
            return execute(cmd, args).getInputStream().read();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Opens a web-page or a file with the default application
     * associated with this kind of file.
     * @param url the path of the resource to open
     * @return the process started by the command
     * @throws IOException If an I/O error occurs
     */
    public static Process open(String url) throws IOException {
        try {
            return switch (OS) {
                case MAC_OS -> exec("open", url);
                case WINDOWS -> exec("explorer", url);
                case LINUX -> BROWSERS.stream()
                    .filter(browser -> tryExec("which", browser) != -1)
                    .findAny()
                    .map(browser -> exec(browser, url))
                    .orElseGet(() -> exit(2));
            };
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    /**
     * Kills the process having the given PID.
     * @param pid the Process ID of the task to kill
     * @return the process associated with the kill command
     * @throws IOException If an I/O error occurs
     */
    public static Process kill(String pid) throws IOException {
        return switch (OS) {
            case MAC_OS -> execute("kill", "-9", pid);
            case WINDOWS -> execute("taskkill", "/F", "/T", "/PID", pid);
            case LINUX -> execute("kill", "-9", pid);
        };
    }

    /**
     * Finds all process running on this computer.
     * @return the pid and name of all running process
     * @throws IOException if processes cannot be accessed
     */
    public static List<ProcessDetails> getProcesses() throws IOException {
        try {
            var process = OS == Os.WINDOWS ? exec("tasklist", "/FO", "csv", "/nh") : exec("ps", "-aux", "--no-header");
            Function<String, ProcessDetails> parser = OS == Os.WINDOWS ? Platform::parseTaskWindows : Platform::parseTaskLinux;
            try (var reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                return reader.lines()
                    .map(parser)
                    .toList();
            }
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    /**
     * {@link Platform#exit(int)} function that can be used in a supplier
     * The return value will never be reached since the application is
     * stopped before but this makes the compiler happy!
     * @param <T> the expected return value
     * @param code the exit status code
     * @return always null
     */
    private static <T> T exit(int code) {
        Platform.exit(code);
        return null;
    }

    /**
     * Parses the details of the process represented by a line such as
     * "name","pid","sessionName","sessionId","Memory"
     * 
     * @param line the line to parse
     * @return the process details
     */
    private static ProcessDetails parseTaskWindows(String line) {
        var tokens = line.split(",");
        var name = tokens[0].substring(1, tokens[0].length() - 1);
        var pid = tokens[1].substring(1, tokens[1].length() - 1);
        return new ProcessDetails(name, Integer.valueOf(pid));
    }

    /**
     * Parses the details of the process represented by a line such as
     * USER         PID %CPU %MEM    VSZ   RSS TTY      STAT START   TIME COMMAND
     * 
     * @param line the line to parse
     * @return the process details
     */
    private static ProcessDetails parseTaskLinux(String line) {
        var name = line.substring(line.lastIndexOf(" ") + 1);
        var start = 15;
        while (name.charAt(start) != ' ') start--;
        var pid = line.substring(start + 1, 15);
        return new ProcessDetails(name, Integer.valueOf(pid));
    }

    public enum Os {
        MAC_OS("Mac_OS"),
        WINDOWS("Windows"),
        LINUX("");

        private final String name;

        private Os(String name) {
            this.name = name;
        }
    
        public static Os getCurrent() {
            var operatingSystem = System.getProperty("os.name");
            for (var os : values()) {
                if (operatingSystem.startsWith(os.name)) return os;
            }
            return LINUX;
        }
    }

    public record ProcessDetails(String name, int pid) {}
}
