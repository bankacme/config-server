package com.bank.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;

/**
 * Levanta el Config Server en modo {@code native} sobre la carpeta hermana {@code ../bank-config}
 * (sin clonar desde GitHub) y comprueba lo que reciben los servicios.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.profiles.active=native",
    "spring.cloud.config.server.native.search-locations=file:../bank-config/"
})
class ConfigServerApplicationTests {

    @Autowired
    private TestRestTemplate rest;

    @Test
    void servesServiceFileMergedWithCommonOne() {
        assertThat(resolve("customer-service", "default", "server.port")).isEqualTo(8081);
        assertThat(resolve("customer-service", "default", "bank.zone")).isEqualTo("America/Lima");
        assertThat(resolve("customer-service", "default", "eureka.client.service-url.defaultZone"))
                .asString().contains("localhost:8761");
    }

    @Test
    void dockerProfileSwitchesHostsToNetworkNames() {
        assertThat(resolve("account-service", "docker", "bank.mongo.host")).isEqualTo("mongo");
        assertThat(resolve("account-service", "docker", "eureka.client.service-url.defaultZone"))
                .isEqualTo("http://eureka-server:8761/eureka");
        assertThat(resolve("account-service", "docker", "server.port")).isEqualTo(8082);
    }

    @Test
    void withoutProfileMongoHostFallsBackToLocalhost() {
        assertThat(resolve("account-service", "default", "bank.mongo.host")).isNull();
        assertThat((String) resolve("account-service", "default", "spring.data.mongodb.uri"))
                .contains(":27017/");
    }

    /** Primer valor en orden de precedencia, como lo ve el cliente. */
    @SuppressWarnings("unchecked")
    private Object resolve(String application, String profile, String key) {
        Map<String, Object> environment = rest.getForObject("/" + application + "/" + profile, Map.class);
        List<Map<String, Object>> sources = (List<Map<String, Object>>) environment.get("propertySources");
        return sources.stream()
                .map(source -> (Map<String, Object>) source.get("source"))
                .filter(values -> values.containsKey(key))
                .map(values -> values.get(key))
                .findFirst()
                .orElse(null);
    }
}
