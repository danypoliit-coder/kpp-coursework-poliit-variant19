
package ua.edu.nupp.kpp.client.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import ua.edu.nupp.kpp.client.model.Tree;
import ua.edu.nupp.kpp.client.model.Inspection;
import ua.edu.nupp.kpp.client.model.WorkRequest;
import ua.edu.nupp.kpp.client.model.WorkOrder;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

public class ApiClient {

    private static final String BASE_URL =
            "http://localhost:8080/greenery-care/api";

    private final HttpClient client;

    private final ObjectMapper mapper;

    public ApiClient() {

        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
    }

    // ==================== TREES ====================

    public List<Tree> getTrees() throws Exception {

        String json = get("/trees");

        return mapper.readValue(
                json,
                mapper.getTypeFactory()
                        .constructCollectionType(
                                List.class, Tree.class)
        );
    }

    public Tree getTree(long id) throws Exception {

        String json = get("/trees?id=" + id);

        return mapper.readValue(json, Tree.class);
    }

    public long createTree(Tree tree) throws Exception {

        JsonNode result = postJson(
                "/trees?action=create", tree);

        return result.path("id").asLong();
    }

    public boolean updateTree(Tree tree) throws Exception {

        JsonNode result = postJson(
                "/trees?action=update", tree);

        return result.path("updated").asBoolean();
    }

    public boolean deleteTree(long id) throws Exception {

        JsonNode result = postEmpty(
                "/trees?action=delete&id=" + id);

        return result.path("deleted").asBoolean();
    }

    public BigDecimal calculateTreeRisk(
            long treeId,
            BigDecimal f3) throws Exception {

        String json = get(
                "/trees?action=risk&id="
                        + treeId
                        + "&f3=" + encode(f3.toPlainString())
        );

        JsonNode result = mapper.readTree(json);

        return result.path("risk").decimalValue();
    }

    // ================= INSPECTIONS =================

    public List<Inspection> getInspections()
            throws Exception {

        String json = get("/inspections");

        return mapper.readValue(
                json,
                mapper.getTypeFactory()
                        .constructCollectionType(
                                List.class, Inspection.class)
        );
    }

    public Inspection getInspection(long id)
            throws Exception {

        return mapper.readValue(
                get("/inspections?id=" + id),
                Inspection.class
        );
    }

    public long createInspection(Inspection inspection)
            throws Exception {

        JsonNode result = postJson(
                "/inspections?action=create",
                inspection);

        return result.path("id").asLong();
    }

    public boolean updateInspection(Inspection inspection)
            throws Exception {

        JsonNode result = postJson(
                "/inspections?action=update",
                inspection);

        return result.path("updated").asBoolean();
    }

    public boolean deleteInspection(long id)
            throws Exception {

        JsonNode result = postEmpty(
                "/inspections?action=delete&id=" + id);

        return result.path("deleted").asBoolean();
    }

    // ================= WORK REQUESTS =================

    public List<WorkRequest> getWorkRequests()
            throws Exception {

        return mapper.readValue(
                get("/requests"),
                mapper.getTypeFactory()
                        .constructCollectionType(
                                List.class, WorkRequest.class)
        );
    }

    public WorkRequest getWorkRequest(long id)
            throws Exception {

        return mapper.readValue(
                get("/requests?id=" + id),
                WorkRequest.class
        );
    }

    public long createWorkRequest(WorkRequest request)
            throws Exception {

        JsonNode result = postJson(
                "/requests?action=create",
                request);

        return result.path("id").asLong();
    }

    public boolean updateWorkRequest(WorkRequest request)
            throws Exception {

        JsonNode result = postJson(
                "/requests?action=update",
                request);

        return result.path("updated").asBoolean();
    }

    public boolean changeRequestStatus(
            long id,
            String status) throws Exception {

        JsonNode result = postEmpty(
                "/requests?action=changeStatus&id="
                        + id
                        + "&status=" + encode(status));

        return result.path("changed").asBoolean();
    }

    public boolean deleteWorkRequest(long id)
            throws Exception {

        JsonNode result = postEmpty(
                "/requests?action=delete&id=" + id);

        return result.path("deleted").asBoolean();
    }

    // ================= WORK ORDERS =================

    public List<WorkOrder> getWorkOrders()
            throws Exception {

        return mapper.readValue(
                get("/orders"),
                mapper.getTypeFactory()
                        .constructCollectionType(
                                List.class, WorkOrder.class)
        );
    }

    public WorkOrder getWorkOrder(long id)
            throws Exception {

        return mapper.readValue(
                get("/orders?id=" + id),
                WorkOrder.class
        );
    }

    public long createWorkOrder(WorkOrder order)
            throws Exception {

        JsonNode result = postJson(
                "/orders?action=create",
                order);

        return result.path("id").asLong();
    }

    public boolean updateWorkOrder(WorkOrder order)
            throws Exception {

        JsonNode result = postJson(
                "/orders?action=update",
                order);

        return result.path("updated").asBoolean();
    }

    public boolean deleteWorkOrder(long id)
            throws Exception {

        JsonNode result = postEmpty(
                "/orders?action=delete&id=" + id);

        return result.path("deleted").asBoolean();
    }

    // ================= HTTP METHODS =================

    private String get(String path) throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        return send(request);
    }

    private JsonNode postJson(
            String path,
            Object body) throws Exception {

        String json = mapper.writeValueAsString(body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type",
                        "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(
                        json, StandardCharsets.UTF_8))
                .build();

        return mapper.readTree(send(request));
    }

    private JsonNode postEmpty(String path)
            throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        return mapper.readTree(send(request));
    }

    private String send(HttpRequest request)
            throws IOException, InterruptedException {

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString(
                        StandardCharsets.UTF_8)
        );

        int status = response.statusCode();

        if (status < 200 || status >= 300) {

            String message = response.body();

            try {
                JsonNode error = mapper.readTree(message);

                if (error.has("error")) {
                    message = error.get("error").asText();
                }

            } catch (Exception ignored) {
                // Залишаємо початковий текст помилки
            }

            throw new IOException(
                    "HTTP " + status + ": " + message);
        }

        return response.body();
    }

    private String encode(String value) {

        return URLEncoder.encode(
                value, StandardCharsets.UTF_8);
    }
}
