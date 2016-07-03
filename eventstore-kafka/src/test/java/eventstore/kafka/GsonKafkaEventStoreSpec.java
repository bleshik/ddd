package eventstore.kafka;

import eventstore.impl.AbstractEventStoreSpec;
import eventstore.kafka.gson.GsonKafkaEventStore;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import kafka.server.KafkaConfig;
import kafka.server.KafkaServerStartable;
import org.apache.curator.test.TestingServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import static org.apache.kafka.streams.StreamsConfig.*;

public class GsonKafkaEventStoreSpec extends AbstractEventStoreSpec {
    private static final UUID uid = UUID.randomUUID();

    private static TestingServer zkTestServer;
    private static KafkaServerStartable kafkaServer;
    private static int kafkaPort;
    private static Path kafkaLogDirs;

    public GsonKafkaEventStoreSpec() {
        super(Arrays.asList(KafkaEventStoreOptimisticLockingStrategy.values()).stream().map((s) -> {
            return (Supplier<GsonKafkaEventStore>)(() -> new GsonKafkaEventStore(
                    "GsonKafkaEventStoreSpec" + s + uid + currentTest.get(),
                    "localhost:" + kafkaPort,
                    "localhost:" + zkTestServer.getPort(),
                    s
                    ));
        }).collect(Collectors.toList()));
    }

    @BeforeClass
    public static void before() throws Exception {
        zkTestServer = new TestingServer();
        kafkaServer  = KafkaServerStartable.fromProps(new Properties() {{
            kafkaPort = getRandomFreePort();
            put("broker.id", 1);
            put("advertised.listeners", "plaintext://localhost:" + kafkaPort);
            put("listeners", "plaintext://localhost:" + kafkaPort);
            put("zookeeper.connect", "localhost:" + zkTestServer.getPort());
            put("host.name", "localhost");
            put("log.dirs", (kafkaLogDirs =
                    Files.createTempDirectory(Paths.get(System.getProperty("java.io.tmpdir")), "kafka")).toString());
        }});
        kafkaServer.startup();
    }

    @AfterClass
    public static void after() throws IOException {
        try {
            kafkaServer.shutdown();
        } finally {
            try {
                deleteDirectory(kafkaLogDirs);
            } finally {
                zkTestServer.stop();
            }
        }
    }

    private static void deleteDirectory(Path directory) throws IOException {
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static int getRandomFreePort() throws IOException {
        try(ServerSocket socket = new ServerSocket(0)) { return socket.getLocalPort(); }
    }
}
