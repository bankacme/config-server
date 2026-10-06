package com.bank.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

/**
 * Revisa el contenido del repositorio de configuración ({@code ../bank-config}) antes de que un
 * error llegue a los servicios: archivos que faltan, puertos repetidos o un secreto subido por error.
 */
class BankConfigRepositoryTest {

    private static final Path CONFIG_REPO = Path.of("..", "bank-config");

    private static final Map<String, Integer> EXPECTED_PORTS = Map.of(
            "eureka-server", 8761,
            "api-gateway", 8080,
            "customer-service", 8081,
            "account-service", 8082,
            "credit-service", 8083,
            "transaction-service", 8084,
            "report-service", 8085);

    private static final List<String> FORBIDDEN_WORDS = List.of("private-key", "password", "token");

    @Test
    void everyServiceHasItsFileWithItsPort() throws IOException {
        for (Map.Entry<String, Integer> expected : EXPECTED_PORTS.entrySet()) {
            Path file = CONFIG_REPO.resolve(expected.getKey() + ".yml");
            assertThat(file).exists();
            assertThat(port(load(file))).as(expected.getKey()).isEqualTo(expected.getValue());
        }
    }

    @Test
    void noTwoServicesShareAPort() throws IOException {
        Map<Integer, String> owners = new HashMap<>();
        for (Path file : ymlFiles()) {
            Integer port = port(load(file));
            if (port != null) {
                String previous = owners.put(port, file.getFileName().toString());
                assertThat(previous).as("puerto %d repetido en %s", port, file.getFileName()).isNull();
            }
        }
    }

    @Test
    void mongoUrisUseTheHostOfTheProfile() throws IOException {
        for (Path file : ymlFiles()) {
            String content = Files.readString(file);
            if (content.contains("mongodb://")) {
                assertThat(content).as(file.getFileName().toString())
                        .contains("mongodb://${bank.mongo.host:localhost}:27017/")
                        .doesNotContain("mongodb://localhost");
            }
        }
    }

    @Test
    void noSecretsInTheRepository() throws IOException {
        for (Path file : ymlFiles()) {
            String content = Files.readString(file).toLowerCase(Locale.ROOT);
            for (String word : FORBIDDEN_WORDS) {
                assertThat(content).as("%s contiene '%s'", file.getFileName(), word).doesNotContain(word);
            }
        }
    }

    private static List<Path> ymlFiles() throws IOException {
        try (Stream<Path> files = Files.list(CONFIG_REPO)) {
            return files.filter(file -> file.getFileName().toString().endsWith(".yml")).sorted().toList();
        }
    }

    private static Map<String, Object> load(Path file) throws IOException {
        try (InputStream input = Files.newInputStream(file)) {
            Map<String, Object> values = new Yaml().load(input);
            return values == null ? Map.of() : values;
        }
    }

    @SuppressWarnings("unchecked")
    private static Integer port(Map<String, Object> values) {
        Object server = values.get("server");
        return server instanceof Map<?, ?> map ? (Integer) ((Map<String, Object>) map).get("port") : null;
    }
}
