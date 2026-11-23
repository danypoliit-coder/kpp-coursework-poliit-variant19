
package ua.edu.nupp.kpp.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

public class HttpApiTestRunner {

    private static final String BASE_URL =
            "http://localhost:8080/greenery-care/api/trees";

    private static final ObjectMapper mapper =
            new ObjectMapper();

    private static final HttpClient client =
            HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

    public static void main(String[] args) {

        Long createdTreeId = null;

        try {
            System.out.println(
                    "=== ТЕСТУВАННЯ HTTP API ДЕРЕВ ===");

            // 1. CREATE
            Map<String, Object> tree = new LinkedHashMap<>();

            tree.put("species", "Тестовий клен");
            tree.put("address", "Тестова адреса HTTP API");
            tree.put("latitude", new BigDecimal("49.588300"));
            tree.put("longitude", new BigDecimal("34.551400"));
            tree.put("technicalState", new BigDecimal("75.00"));
            tree.put("peopleIntensity", new BigDecimal("60.00"));
            tree.put("greenArea", new BigDecimal("80.00"));

            JsonNode created = post(
                    BASE_URL + "?action=create",
                    mapper.writeValueAsString(tree),
                    201
            );

            createdTreeId = created.path("id").asLong();

            check(createdTreeId > 0,
                    "Не отримано ID створеного дерева");

            System.out.println(
                    "[OK] CREATE: дерево ID " + createdTreeId);

            // 2. READ BY ID
            JsonNode found = get(
                    BASE_URL + "?id=" + createdTreeId,
                    200
            );

            check("Тестовий клен".equals(
                            found.path("species").asText()),
                    "Помилка читання дерева");

            System.out.println("[OK] READ BY ID");

            // 3. UPDATE
            tree.put("id", createdTreeId);
            tree.put("species", "Оновлений тестовий клен");
            tree.put("technicalState", new BigDecimal("85.00"));

            JsonNode updated = post(
                    BASE_URL + "?action=update",
                    mapper.writeValueAsString(tree),
                    200
            );

            check(updated.path("updated").asBoolean(),
                    "Не вдалося оновити дерево");

            JsonNode afterUpdate = get(
                    BASE_URL + "?id=" + createdTreeId,
                    200
            );

            check("Оновлений тестовий клен".equals(
                            afterUpdate.path("species").asText()),
                    "Дані дерева не змінилися");

            System.out.println("[OK] UPDATE");

            // 4. RISK CALCULATION
            JsonNode riskResult = get(
                    BASE_URL + "?action=risk&id="
                            + createdTreeId + "&f3=90",
                    200
            );

            BigDecimal actualRisk =
                    riskResult.path("risk").decimalValue();

            BigDecimal expectedRisk =
                    new BigDecimal("77.50");

            check(actualRisk.compareTo(expectedRisk) == 0,
                    "Неправильний ризик: " + actualRisk);

            System.out.println(
                    "[OK] RISK: " + actualRisk);

            // 5. INVALID REQUEST
            JsonNode invalid = get(
                    BASE_URL + "?id=abc",
                    400
            );

            check(!invalid.path("success").asBoolean(true),
                    "Некоректний запит не був відхилений");

            System.out.println("[OK] INVALID REQUEST");

            // 6. READ ALL
            JsonNode allTrees = get(BASE_URL, 200);

            check(allTrees.isArray(),
                    "Список дерев повинен бути масивом");

            boolean exists = false;

            for (JsonNode item : allTrees) {
                if (item.path("id").asLong() == createdTreeId) {
                    exists = true;
                    break;
                }
            }

            check(exists,
                    "Створене дерево відсутнє у списку");

            System.out.println("[OK] READ ALL");

            // 7. DELETE
            JsonNode deleted = post(
                    BASE_URL + "?action=delete&id="
                            + createdTreeId,
                    null,
                    200
            );

            check(deleted.path("deleted").asBoolean(),
                    "Не вдалося видалити дерево");

            createdTreeId = null;

            System.out.println("[OK] DELETE");

            System.out.println();
            System.out.println(
                    "=== УСІ HTTP API ТЕСТИ ПРОЙДЕНО ===");

        } catch (Exception e) {

            System.err.println(
                    "=== ПОМИЛКА HTTP API ТЕСТУ ===");

            e.printStackTrace();

        } finally {

            if (createdTreeId != null) {

                try {
                    post(
                            BASE_URL + "?action=delete&id="
                                    + createdTreeId,
                            null,
                            200
                    );

                    System.out.println(
                            "Тестове дерево очищено");

                } catch (Exception e) {

                    System.err.println(
                            "Не вдалося очистити тестове дерево ID: "
                                    + createdTreeId);
                }
            }
        }
    }

    private static JsonNode get(
            String url,
            int expectedStatus)
            throws IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        return send(request, expectedStatus);
    }

    private static JsonNode post(
            String url,
            String json,
            int expectedStatus)
            throws IOException, InterruptedException {

        HttpRequest.BodyPublisher body =
                json == null
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(
                        json, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type",
                        "application/json; charset=UTF-8")
                .POST(body)
                .build();

        return send(request, expectedStatus);
    }

    private static JsonNode send(
            HttpRequest request,
            int expectedStatus)
            throws IOException, InterruptedException {

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString(
                        StandardCharsets.UTF_8)
        );

        if (response.statusCode() != expectedStatus) {
            throw new IllegalStateException(
                    "Очікувався HTTP " + expectedStatus
                            + ", отримано HTTP "
                            + response.statusCode()
                            + "\nВідповідь: "
                            + response.body()
            );
        }

        return mapper.readTree(response.body());
    }

    private static void check(
            boolean condition,
            String message) {

        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
