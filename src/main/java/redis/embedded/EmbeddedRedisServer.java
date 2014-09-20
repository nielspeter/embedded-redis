package redis.embedded;

import java.io.*;
import java.net.ServerSocket;
import java.net.URISyntaxException;
import java.util.UUID;

public class EmbeddedRedisServer {

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
                throw new RuntimeException("Unsupported OS: " + osName);
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

    public EmbeddedRedisServer() throws IOException, URISyntaxException {
        this(findFreePort());
    }

    public EmbeddedRedisServer(String version) throws IOException, URISyntaxException {
        this(version, findFreePort());
    }

    public EmbeddedRedisServer(int port) throws IOException, URISyntaxException {
        this(null, port);
    }

    public EmbeddedRedisServer(String version, int port) throws IOException, URISyntaxException {
        this.version = (version != null) ? version : LATEST_REDIS_VERSION;
        this.port = port;
        this.command = extractExecutableFromJar(RedisServerEnum.getOsDependentRedisServerEnum());
    }

    private File extractExecutableFromJar(RedisServerEnum redisServerEnum) throws IOException, URISyntaxException {
        String redisExecutablePath = "redis" + File.separator + version + File.separator + redisServerEnum.name().toLowerCase() + File.separator + redisServerEnum.executableName;
        InputStream redisExecutableInputStream = EmbeddedRedisServer.class.getClassLoader().getResourceAsStream(redisExecutablePath);
        if (redisExecutableInputStream == null) throw new IllegalStateException("Redis executable not found in the JAR at location: " + redisExecutablePath);

        File tmpDir = new File(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString());
        tmpDir.deleteOnExit();
        tmpDir.mkdirs();

        File redisExecutableFile = new File(tmpDir, redisServerEnum.executableName);
        redisExecutableFile.createNewFile();

        copyFile(redisExecutableInputStream, redisExecutableFile);

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
        pb.redirectErrorStream();

        return pb;
    }

    public synchronized void stop() throws InterruptedException {
        if (active) {
            redisProcess.destroy();
            redisProcess.waitFor();
            active = false;
        }
    }

    private static int findFreePort() throws IOException {
        ServerSocket server = new ServerSocket(0);
        int port = server.getLocalPort();
        server.close();
        return port;
    }

    private static void copyFile(InputStream is, File destFile) throws IOException {
        OutputStream fos = null;
        int readBytes;
        byte[] buffer = new byte[4096];
        try {
            fos = new FileOutputStream(destFile);
            while ((readBytes = is.read(buffer)) > 0) {
                fos.write(buffer, 0, readBytes);
            }
        } finally {
            if (is != null) is.close();
            if (fos != null) fos.close();
        }
    }
}
