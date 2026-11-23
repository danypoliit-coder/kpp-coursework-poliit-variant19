
package ua.edu.nupp.kpp.server.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import ua.edu.nupp.kpp.server.AppLogger;
import ua.edu.nupp.kpp.server.model.Tree;
import ua.edu.nupp.kpp.server.service.TreeService;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

public class TreeServlet extends HttpServlet {

    private final TreeService treeService = new TreeService();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        prepareResponse(response);

        try {
            String action = request.getParameter("action");
            String idParameter = request.getParameter("id");

            // GET /api/trees
            if (action == null && idParameter == null) {
                writeJson(response, HttpServletResponse.SC_OK,
                        treeService.findAll());
                return;
            }

            // GET /api/trees?id=1
            if (action == null) {
                long id = parseId(idParameter);

                writeJson(response, HttpServletResponse.SC_OK,
                        treeService.findById(id));
                return;
            }

            // GET /api/trees?action=risk&id=1&f3=90
            if ("risk".equals(action)) {
                long id = parseId(idParameter);

                String f3Parameter = request.getParameter("f3");

                if (f3Parameter == null || f3Parameter.isBlank()) {
                    throw new IllegalArgumentException(
                            "Необхідно вказати параметр f3");
                }

                BigDecimal f3 = new BigDecimal(f3Parameter);

                BigDecimal risk =
                        treeService.calculateRisk(id, f3);

                Map<String, Object> result = new LinkedHashMap<>();
                result.put("treeId", id);
                result.put("interventionGeometry", f3);
                result.put("risk", risk);

                writeJson(response, HttpServletResponse.SC_OK, result);
                return;
            }

            throw new IllegalArgumentException(
                    "Невідома дія: " + action);

        } catch (IllegalArgumentException e) {
            writeError(response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    e.getMessage());

        } catch (SQLException e) {
            AppLogger.error("Помилка GET /api/trees", e);

            writeError(response,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Помилка роботи з базою даних");
        }
    }

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        prepareResponse(response);

        try {
            String action = request.getParameter("action");

            if (action == null || action.isBlank()) {
                throw new IllegalArgumentException(
                        "Необхідно вказати параметр action");
            }

            switch (action) {

                // POST /api/trees?action=create
                case "create" -> {
                    Tree tree = readTree(request);

                    long id = treeService.create(tree);

                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("message", "Дерево успішно створено");
                    result.put("id", id);

                    writeJson(response,
                            HttpServletResponse.SC_CREATED, result);
                }

                // POST /api/trees?action=update
                case "update" -> {
                    Tree tree = readTree(request);

                    if (tree.getId() == null) {
                        throw new IllegalArgumentException(
                                "Для оновлення необхідно вказати id");
                    }

                    boolean updated = treeService.update(tree);

                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("message", "Дерево оновлено");
                    result.put("updated", updated);

                    writeJson(response,
                            HttpServletResponse.SC_OK, result);
                }

                // POST /api/trees?action=delete&id=1
                case "delete" -> {
                    long id = parseId(request.getParameter("id"));

                    boolean deleted = treeService.delete(id);

                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("message", "Дерево видалено");
                    result.put("deleted", deleted);

                    writeJson(response,
                            HttpServletResponse.SC_OK, result);
                }

                default -> throw new IllegalArgumentException(
                        "Невідома дія: " + action);
            }

        } catch (IllegalArgumentException e) {
            writeError(response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    e.getMessage());

        } catch (SQLException e) {
            AppLogger.error("Помилка POST /api/trees", e);

            writeError(response,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Помилка роботи з базою даних");

        } catch (IOException e) {
            writeError(response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Некоректний формат JSON");
        }
    }

    private Tree readTree(HttpServletRequest request)
            throws IOException {

        Tree tree = mapper.readValue(
                request.getInputStream(), Tree.class);

        if (tree == null) {
            throw new IllegalArgumentException(
                    "JSON не може бути порожнім");
        }

        return tree;
    }

    private long parseId(String value) {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Необхідно вказати ID дерева");
        }

        try {
            long id = Long.parseLong(value);

            if (id <= 0) {
                throw new IllegalArgumentException(
                        "ID дерева повинен бути додатним");
            }

            return id;

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Некоректний ID дерева");
        }
    }

    private void prepareResponse(HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
    }

    private void writeJson(
            HttpServletResponse response,
            int status,
            Object data) throws IOException {

        response.setStatus(status);
        mapper.writeValue(response.getOutputStream(), data);
    }

    private void writeError(
            HttpServletResponse response,
            int status,
            String message) throws IOException {

        Map<String, Object> error = new LinkedHashMap<>();
        error.put("success", false);
        error.put("error", message);

        writeJson(response, status, error);
    }
}
