package io.github.mrmiumo;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Inter Process Communication Master client.
 * This is a file-based method to exchange information between two
 * processes. One process is the Master that send queries to the other
 * process (Slave) how responds. At the beginning, the two processes
 * agrees on an exchange file that is created for each query and
 * deleted once the response is read by the Master. All responses
 * starts with "> " in files but this prefix is removed when reading
 * query return value.
 * @author Miumo
 */
class InterProcessCommunication implements AutoCloseable {

    /** Service that enable to register watcher over files */
    private final static WatchService WATCH_SERVICE;
    static {
        try {
            WATCH_SERVICE = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            throw new RuntimeException("Unable to start WatchService", e);
        }
    }

    /** Lock used to get ready-state */
    private Object lock = new Object();

    /** Path of the file that will serves to exchange information */
    private final Path file;

    /** Name of the file to avoid getting it over and over again */
    private final String fileName;

    /** Ready state (whether or not the Slave is ready) */
    private boolean ready = false;

    /** Whether the communication is closed or not */
    private AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * Creates a new IPC on the given file. Note that this file must
     * be unique and DO NOT be used by any process other than the Slave.
     * @param file the file to write/read in/from. An example of file
     *     name can be "pid123.ipc"
     * @return the new InterProcessCommunication
     * @throws IOException if the file cannot be read
     */
    public static InterProcessCommunication from(Path folder) throws IOException {
        try {
            var name = "eb" + System.nanoTime() + ".ipc";
            var instance = new InterProcessCommunication(folder.resolve(name));
            instance.waitForSetup();
            return instance;
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    /**
     * Creates a new IPC on the given file. Note that this file must
     * be unique and DO NOT be used by any process other than the Slave.
     * @param file the file to write/read in/from. An example of file
     *     name can be "pid123.ipc"
     */
    private InterProcessCommunication(Path file) {
        this.file = Objects.requireNonNull(file);
        fileName = file.getFileName().toString();
    }

    /**
     * Waits for the Slave to start. The Slave is started when it
     * write its first message in the exchange file.
     * @throws IOException if an error occurs while reading ipc file
     * @throws UncheckedIOException if an error occurs while reading ipc file
     */
    private void waitForSetup() throws IOException {
        if (!Files.exists(file)) Files.createFile(file);
        var watcher = file.getParent().register(WATCH_SERVICE, StandardWatchEventKinds.ENTRY_MODIFY);
        var isPresent = Files.lines(file).findFirst().map(l -> l.startsWith("> ")).orElse(false);
        if (isPresent) {
            watcher.cancel();
            ready = true;
        } else {
            new Thread(() -> {
                try {
                    wait(watcher);
                    synchronized (lock) {
                        ready = true;
                        lock.notifyAll();
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }).start();
        }
    }

    /**
     * Path of the file used to discuss with the other process.
     * @return the path of the file
     */
    public Path getPath() {
        return file;
    }

    /**
     * Tests whether or not the slave is ready to communicate. Note
     * that sending any query before the slave is ready can ends up
     * in long blocking time.
     * @return true if the slave is ready to receive queries
     */
    public boolean isReady() {
        synchronized (lock) {
            return ready;
        }
    }

    /**
     * Blocking method that wait for the slave to be ready.
     * @param timeoutMillis the number of milliseconds to wait before
     *     canceling the wait.
     * @return true if the slave is ready (useful to see if the timeout
     *     was used or not)
     */
    public boolean waitForReady(long timeoutMillis) {
        synchronized (lock) {
            if (ready) return true;
            try {
                lock.wait(timeoutMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); 
                System.err.println("Thread Interrupted");
            }
            return ready;
        }
    }

    /**
     * Sends a new query to the Slave and waits for its response
     * before returning it.
     * Warning: if the slave is not active or malfunctioning, this
     * method may be stuck waiting forever.
     * @param query the string to send to the slave
     * @return the response from the Slave or null if the slave is
     *     not ready yet.
     * @throws IOException in case of error while reading the exchange
     *     file.
     */
    public String query(String query) throws IOException {
        if (!isReady() || closed.get()) return null;
        Files.deleteIfExists(file);
        var watcher = file.getParent().register(WATCH_SERVICE, StandardWatchEventKinds.ENTRY_MODIFY);

        Files.writeString(file, query, StandardOpenOption.CREATE);
        var response = wait(watcher);
        try {
            Files.delete(file);
        } catch (IOException e) {
            // Catch errors if the browser just got closed
            return null;
        }

        return response;
    }
    
    /**
     * Blocking method that waits for the ipc file to be updated with
     * a value starting by "> " and returns the value after that prefix
     * @param watcher the watcher initialized before the creation of
     *     the ipc file to make sure modification event are watched.
     * @return the read response or null
     * @throws IOException if reading the file failed
     */
    private String wait(WatchKey watcher) throws IOException {
        WatchKey key;
        try {
            while (!closed.get() && (key = WATCH_SERVICE.take()) != null) {
                for (var event : key.pollEvents()) {
                    var path = (Path)event.context();
                    if (!fileName.equals(path.getFileName().toString())) continue;
                    var body = Files.readString(file);
                    if (body.startsWith("> ")) {
                        watcher.cancel();
                        return body.substring(2);
                    }
                }
                key.reset();
            }
            watcher.cancel();
            return null;
        } catch (InterruptedException e) {
            throw new IOException("Interrupted while waiting for file update", e);
        } catch (ClosedWatchServiceException e) {
            return null;
        }
    }
    
    @Override
    public void close() throws Exception {
        closed.set(true);
        WATCH_SERVICE.close();
        Files.deleteIfExists(file);
    }
    
    public static void main(String[] args) throws Exception {

        var workingDir = Path.of("C:/Users/aurel/Documents/MineDisney/Code/EmbeddedBrowser");
        var manager = EmbeddedBrowserManager.getManager(workingDir, new SysOutLogger());

        var browser = manager.newWindow("Test IPC", "http://localhost:45000").display();
        System.out.println("IPC ready: " + browser.waitForReady(10000));
        
        var last = "";
        var count = 0;
        while (browser.isAlive()) {
            var response = browser.getSize() + "";
            if (!last.equals(response)) {
                System.out.println(count + " - " + response);
                last = response;
            }
            count++;
            // Thread.sleep(4000);
        }
        manager.close();
    }
}
