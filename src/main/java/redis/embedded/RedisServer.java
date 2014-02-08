package redis.embedded;

import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.URL;
import java.util.UUID;

public class RedisServer {

    private static enum RedisRunScriptEnum {
        WINDOWS_32("redis-server.exe"),
        WINDOWS_64("redis-server-64.exe"),
        UNIX("redis-server"),
        MACOSX("redis-server.app");

        private final String runScript;

        private RedisRunScriptEnum(String runScript) {
            this.runScript = runScript;
        }

        public static String getRedisRunScript() {
            String osName = System.getProperty("os.name").toLowerCase();
            String osArch = System.getProperty("os.arch").toLowerCase();

            if (osName.contains("win")) {
                if (osArch.contains("64")) {
                    return WINDOWS_64.runScript;
                } else {
                    return WINDOWS_32.runScript;
                }
            } else if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
                return UNIX.runScript;
            } else if ("Mac OS X".equalsIgnoreCase(osName)) {
                return MACOSX.runScript;
            } else {
                throw new RuntimeException("Unsupported os/architecture...: " + osName + " on " + osArch);
            }
        }
    }

    private static final String REDIS_READY_PATTERN = ".*The server is now ready to accept connections on port.*";

    private final File command;
    private final int port;

    private volatile boolean active = false;
    private Process redisProcess;

    public RedisServer() throws IOException {
        this(findFreePort());
    }

    public RedisServer(File command, int port) {
        this.command = command;
        this.port = port;
    }

    public RedisServer(int port) throws IOException {
        this.port = port;
        this.command = extractExecutableFromJar(RedisRunScriptEnum.getRedisRunScript());
    }

    private File extractExecutableFromJar(String scriptName) throws IOException {
        File tmpDir = new File(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString());
        tmpDir.deleteOnExit();

        File command = new File(tmpDir, scriptName);
        URL url = RedisServer.class.getClassLoader().getResource(scriptName);
        FileUtils.copyURLToFile(url, command);
        command.deleteOnExit();
        command.setExecutable(true);

        return command;
    }

    public int getPort() {
        return port;
    }

    public boolean isActive() {
        return active;
    }

    public synchronized void start() throws IOException {
        if (active) {
            throw new RuntimeException("This redis server instance is already running...");
        }

        redisProcess = createRedisProcessBuilder().start();
        awaitRedisServerReady();
        active = true;
    }

    private void awaitRedisServerReady() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(redisProcess.getInputStream()));
        try {
            String outputLine;
            do {
                outputLine = reader.readLine();
            } while (outputLine != null && !outputLine.matches(REDIS_READY_PATTERN));
        } finally {
            reader.close();
        }
    }

    private ProcessBuilder createRedisProcessBuilder() {
        ProcessBuilder pb = new ProcessBuilder(command.getAbsolutePath(), "--port", Integer.toString(port));
        pb.directory(command.getParentFile());

        return pb;
    }

    public synchronized void stop() {
        if (active) {
            redisProcess.destroy();
            active = false;
        }
    }

    private static int findFreePort() throws IOException {
        ServerSocket server = new ServerSocket(0);
        int port = server.getLocalPort();
        server.close();
        return port;
    }
}
