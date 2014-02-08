package redis.embedded;

import java.io.*;
import java.net.ServerSocket;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.UUID;

public class RedisServer {

    private static enum RedisServerEnum {
        WINDOWS("redis-server.exe"),
        LINUX("redis-server"),
        MACOSX("redis-server");

        private final String executableName;

        private RedisServerEnum(String executableName) {
            this.executableName = executableName;
        }

        public static RedisServerEnum getOsDependentRedisServerEnum() {
            String osName = System.getProperty("os.name").toLowerCase();

            if (osName.contains("win")) {
                return WINDOWS;
            } else if (osName.equals("linux")) {
                return LINUX;
            } else if ("mac os x".equals(osName)) {
                return MACOSX;
            } else {
                throw new RuntimeException("Unsupported os/architecture...: " + osName);
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

    public RedisServer() throws IOException, URISyntaxException {
        this(findFreePort());
    }

    public RedisServer(String version) throws IOException, URISyntaxException {
        this(version, findFreePort());
    }

    public RedisServer(int port) throws IOException, URISyntaxException {
        this(null, port);
    }

    public RedisServer(String version, int port) throws IOException, URISyntaxException {
        this.version = (version != null) ? version : LATEST_REDIS_VERSION;
        this.port = port;
        this.command = extractExecutableFromJar(RedisServerEnum.getOsDependentRedisServerEnum());
    }

    private File extractExecutableFromJar(RedisServerEnum redisServerEnum) throws IOException, URISyntaxException {
        File tmpDir = new File(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString());
        tmpDir.deleteOnExit();
        tmpDir.mkdirs();

        String redisExecutablePath = "redis" + File.separator + version + File.separator + redisServerEnum.name().toLowerCase() + File.separator + redisServerEnum.executableName;
        URL redisExecutableUrl = RedisServer.class.getClassLoader().getResource(redisExecutablePath);
        File redisExecutableFile = new File(tmpDir, redisServerEnum.executableName);
        redisExecutableFile.createNewFile();

        copyFile(new File(redisExecutableUrl.toURI()), redisExecutableFile);

        redisExecutableFile.setExecutable(true);
        redisExecutableFile.deleteOnExit();

        return redisExecutableFile;
    }

    public int getPort() {
        return port;
    }

    public String getVersion() {
        return version;
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

    private static void copyFile(File sourceFile, File destFile) throws IOException {
        FileChannel source = null;
        FileChannel destination = null;

        try {
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();
            destination.transferFrom(source, 0, source.size());
        } finally {
            if (source != null) {
                source.close();
            }
            if (destination != null) {
                destination.close();
            }
        }
    }
}
