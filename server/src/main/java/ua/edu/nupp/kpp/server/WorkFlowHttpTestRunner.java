
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
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

public class WorkFlowHttpTestRunner {

    private static final String BASE =
            "http://localhost:8080/greenery-care/api";

    private static final ObjectMapper mapper =
            new ObjectMapper();

    private static final HttpClient client =
            HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

    private static Long treeId;
    private static Long inspectionId;
    private static Long requestId;
    private static Long orderId;

    public static void main(String[] args) {

        try {
            System.out.println(
                    "=== HTTP ТЕСТУВАННЯ БІЗНЕС-ПРОЦЕСІВ ===");

            createTree();
            testInspection();
            testWorkRequest();
            testWorkOrder();
            testCompletedRequest();

            System.out.println();
            System.out.println(
                    "=== УСІ HTTP ТЕСТИ БІЗНЕС-ПРОЦЕСІВ ПРОЙДЕНО ===");

        } catch (Exception e) {

            System.err.println();
            System.err.println(
                    "=== ПОМИЛКА HTTP ТЕСТУВАННЯ ===");

            e.printStackTrace();

        } finally {
            cleanup();
        }
    }

    private static void createTree() throws Exception {

        Map<String, Object> tree = new LinkedHashMap<>();

        tree.put("species", "Тестова липа");
        tree.put("address", "Тестова адреса Workflow API");
        tree.put("latitude", new BigDecimal("49.588300"));
        tree.put("longitude", new BigDecimal("34.551400"));
        tree.put("technicalState", new BigDecimal("80"));
        tree.put("peopleIntensity", new BigDecimal("65"));
        tree.put("greenArea", new BigDecimal("70"));

        JsonNode result = postJson(
                "/trees?action=create", tree, 201);

        treeId = requiredCreatedId(result);

        System.out.println(
                "[OK] Створено тестове дерево ID: " + treeId);
    }

    private static void testInspection() throws Exception {

        Map<String, Object> inspection = new LinkedHashMap<>();

        inspection.put("treeId", treeId);
        inspection.put("inspectionDate",
                LocalDate.now().toString());
        inspection.put("notes", "HTTP тест огляду");
        inspection.put("technicalState", 80);
        inspection.put("inspector", "Тестовий інспектор");

        JsonNode created = postJson(
                "/inspections?action=create",
                inspection, 201);

        inspectionId = requiredCreatedId(created);

        JsonNode found = get(
                "/inspections?id=" + inspectionId, 200);

        check(found.path("treeId").asLong() == treeId,
                "Неправильний ID дерева в огляді");

        inspection.put("id", inspectionId);
        inspection.put("notes", "Оновлений HTTP огляд");

        JsonNode updated = postJson(
                "/inspections?action=update",
                inspection, 200);

        check(updated.path("updated").asBoolean(),
                "Огляд не оновлено");

        JsonNode afterUpdate = get(
                "/inspections?id=" + inspectionId, 200);

        check("Оновлений HTTP огляд".equals(
                        afterUpdate.path("notes").asText()),
                "Зміни огляду не збережено");

        System.out.println(
                "[OK] INSPECTION: CREATE, READ, UPDATE");
    }

    private static void testWorkRequest() throws Exception {

        Map<String, Object> workRequest =
                new LinkedHashMap<>();

        workRequest.put("treeId", treeId);
        workRequest.put("description",
                "Тестове санітарне обрізання");
        workRequest.put("status", "СТВОРЕНО");
        workRequest.put("plannedDate", null);

        JsonNode created = postJson(
                "/requests?action=create",
                workRequest, 201);

        requestId = requiredCreatedId(created);

        JsonNode found = get(
                "/requests?id=" + requestId, 200);

        check("СТВОРЕНО".equals(
                        found.path("status").asText()),
                "Неправильний початковий статус");

        // Заборонений перехід
        JsonNode invalid = postEmpty(
                "/requests?action=changeStatus&id="
                        + requestId
                        + "&status=" + encode("ВИКОНАНО"),
                409);

        check(!invalid.path("success").asBoolean(true),
                "Заборонений перехід не відхилено");

        System.out.println(
                "[OK] Заборонений перехід СТВОРЕНО -> ВИКОНАНО");

        changeStatus("ПЕРЕВІРЕНО");

        // Спочатку встановлюємо дату планування
        workRequest.put("id", requestId);
        workRequest.put("status", "ПЕРЕВІРЕНО");
        workRequest.put("plannedDate",
                LocalDate.now().plusDays(7).toString());

        JsonNode updated = postJson(
                "/requests?action=update",
                workRequest, 200);

        check(updated.path("updated").asBoolean(),
                "Не вдалося встановити дату планування");

        changeStatus("ЗАПЛАНОВАНО");

        System.out.println(
                "[OK] REQUEST: CREATE, READ, UPDATE");
        System.out.println(
                "[OK] СТВОРЕНО -> ПЕРЕВІРЕНО -> ЗАПЛАНОВАНО");
    }

    private static void testWorkOrder() throws Exception {

        Map<String, Object> order = new LinkedHashMap<>();

        order.put("requestId", requestId);
        order.put("assignedTo", "Тестова бригада №1");
        order.put("orderDate",
                LocalDate.now().plusDays(1).toString());
        order.put("completionNotes", null);

        JsonNode created = postJson(
                "/orders?action=create",
                order, 201);

        orderId = requiredCreatedId(created);

        JsonNode found = get(
                "/orders?id=" + orderId, 200);

        check(found.path("requestId").asLong() == requestId,
                "Неправильний ID заявки в наряді");

        order.put("id", orderId);
        order.put("assignedTo", "Тестова бригада №2");

        JsonNode updated = postJson(
                "/orders?action=update",
                order, 200);

        check(updated.path("updated").asBoolean(),
                "Наряд не оновлено");

        JsonNode afterUpdate = get(
                "/orders?id=" + orderId, 200);

        check("Тестова бригада №2".equals(
                        afterUpdate.path("assignedTo").asText()),
                "Зміни наряду не збережено");

        System.out.println(
                "[OK] ORDER: CREATE, READ, UPDATE");
    }

    private static void testCompletedRequest()
            throws Exception {

        changeStatus("ВИКОНАНО");

        JsonNode completed = get(
                "/requests?id=" + requestId, 200);

        check("ВИКОНАНО".equals(
                        completed.path("status").asText()),
                "Заявка не отримала статус ВИКОНАНО");

        // Виконану заявку не можна видаляти
        JsonNode invalidDelete = postEmpty(
                "/requests?action=delete&id=" + requestId,
                409);

        check(!invalidDelete.path("success").asBoolean(true),
                "Видалення виконаної заявки не заблоковано");

        // Наряд виконаної заявки не можна видаляти
        JsonNode invalidOrderDelete = postEmpty(
                "/orders?action=delete&id=" + orderId,
                409);

        check(!invalidOrderDelete.path("success").asBoolean(true),
                "Видалення наряду виконаної заявки не заблоковано");

        System.out.println(
                "[OK] ЗАПЛАНОВАНО -> ВИКОНАНО");
        System.out.println(
                "[OK] Захист виконаної заявки та наряду");
    }

    private static void changeStatus(String status)
            throws Exception {

        JsonNode result = postEmpty(
                "/requests?action=changeStatus&id="
                        + requestId
                        + "&status=" + encode(status),
                200);

        check(result.path("changed").asBoolean(),
                "Не вдалося змінити статус на " + status);
    }

    private static void cleanup() {

        System.out.println();
        System.out.println("=== ОЧИЩЕННЯ ТЕСТОВИХ ДАНИХ ===");

        // Видаляємо огляд окремо, перевіряючи DELETE.
        if (inspectionId != null) {
            try {
                JsonNode result = postEmpty(
                        "/inspections?action=delete&id="
                                + inspectionId,
                        200);

                check(result.path("deleted").asBoolean(),
                        "Огляд не видалено");

                inspectionId = null;

                System.out.println(
                        "[OK] INSPECTION DELETE");

            } catch (Exception e) {
                System.err.println(
                        "[ERROR] Очищення огляду: "
                                + e.getMessage());
            }
        }

        // Видалення дерева запускає ON DELETE CASCADE
        // для тестової заявки та наряду.
        if (treeId != null) {
            try {
                JsonNode result = postEmpty(
                        "/trees?action=delete&id=" + treeId,
                        200);

                check(result.path("deleted").asBoolean(),
                        "Дерево не видалено");

                System.out.println(
                        "[OK] Тестове дерево видалено");
                System.out.println(
                        "[OK] Пов'язані тестові записи очищено");

                treeId = null;
                requestId = null;
                orderId = null;

            } catch (Exception e) {
                System.err.println(
                        "[ERROR] Очищення дерева: "
                                + e.getMessage());
            }
        }
    }

    private static JsonNode get(
            String path,
            int expectedStatus) throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + path))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        return send(request, expectedStatus);
    }

    private static JsonNode postJson(
            String path,
            Object body,
            int expectedStatus) throws Exception {

        String json = mapper.writeValueAsString(body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + path))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type",
                        "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(
                        json, StandardCharsets.UTF_8))
                .build();

        return send(request, expectedStatus);
    }

    private static JsonNode postEmpty(
            String path,
            int expectedStatus) throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + path))
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        return send(request, expectedStatus);
    }

    private static JsonNode send(
            HttpRequest request,
            int expectedStatus) throws Exception {

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString(
                        StandardCharsets.UTF_8));

        if (response.statusCode() != expectedStatus) {
            throw new IllegalStateException(
                    "Очікувався HTTP " + expectedStatus
                            + ", отримано HTTP "
                            + response.statusCode()
                            + "\nВідповідь: "
                            + response.body());
        }

        return mapper.readTree(response.body());
    }

    private static long requiredCreatedId(JsonNode result) {

        long id = result.path("id").asLong();

        check(id > 0, "Сервер не повернув коректний ID");

        return id;
    }

    private static String encode(String value) {
        return URLEncoder.encode(
                value, StandardCharsets.UTF_8);
    }

    private static void check(
            boolean condition,
            String message) {

        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
