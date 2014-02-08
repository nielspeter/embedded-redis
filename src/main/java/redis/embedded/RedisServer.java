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

    private static enum RedisEnum {
        WINDOWS_32("redis-server.exe"),
        WINDOWS_64("redis-server-64.exe"),
        UNIX("redis-server"),
        MACOSX("redis-server");

        private final String executableName;

        private RedisEnum(String executableName) {
            this.executableName = executableName;
        }

        public static RedisEnum getRedisEnum() {
            String osName = System.getProperty("os.name").toLowerCase();
            String osArch = System.getProperty("os.arch").toLowerCase();

            if (osName.contains("win")) {
                return osArch.contains("64") ? WINDOWS_64 : WINDOWS_32;
            } else if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
                return UNIX;
            } else if ("Mac OS X".equalsIgnoreCase(osName)) {
                return MACOSX;
            } else {
                throw new RuntimeException("Unsupported os/architecture...: " + osName + " on " + osArch);
            }
        }
    }

    private static final String REDIS_READY_PATTERN = ".*The server is now ready to accept connections on port.*";
    private final String LATEST_REDIS_VERSION = "2.8.5";

    private final File command;
    private final int port;
    private final String version;

    private volatile boolean active = false;
    private Process redisProcess;

    public RedisServer() throws IOException {
        this(findFreePort());
    }

    public RedisServer(String version) throws IOException {
        this(version, findFreePort());
    }

    public RedisServer(int port) throws IOException {
        this(null, port);
    }

    public RedisServer(String version, int port) throws IOException {
        this.version = (version != null) ? version : LATEST_REDIS_VERSION;
        this.port = port;
        this.command = extractExecutableFromJar(RedisEnum.getRedisEnum());
    }

    private File extractExecutableFromJar(RedisEnum redisEnum) throws IOException {
        File tmpDir = new File(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString());
        tmpDir.deleteOnExit();

        String redisExecutablePath = "redis" + File.separator + version + File.separator + redisEnum.name().toLowerCase() + File.separator + redisEnum.executableName;
        URL redisExecutableUrl = RedisServer.class.getClassLoader().getResource(redisExecutablePath);
        File redisExecutableFile = new File(tmpDir, redisEnum.executableName);

        FileUtils.copyURLToFile(redisExecutableUrl, redisExecutableFile);
        redisExecutableFile.deleteOnExit();
        redisExecutableFile.setExecutable(true);

        return redisExecutableFile;
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
