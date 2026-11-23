
package ua.edu.nupp.kpp.server.servlet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import ua.edu.nupp.kpp.server.AppLogger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class ApiServlet extends HttpServlet {

    protected final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @FunctionalInterface
    protected interface ApiAction {
        void run() throws IOException, SQLException;
    }

    protected void execute(
            HttpServletRequest request,
            HttpServletResponse response,
            ApiAction action) throws IOException {

        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");

        try {
            action.run();

        } catch (JsonProcessingException e) {
            sendError(response, 400,
                    "Некоректний формат JSON");

        } catch (IllegalArgumentException e) {
            sendError(response, 400, e.getMessage());

        } catch (IllegalStateException e) {
            sendError(response, 409, e.getMessage());

        } catch (SQLException e) {
            AppLogger.error(
                    "Помилка бази даних: " + request.getRequestURI(), e);

            sendError(response, 500,
                    "Помилка роботи з базою даних");
        }
    }

    protected <T> T readBody(
            HttpServletRequest request,
            Class<T> type) throws IOException {

        T object = mapper.readValue(
                request.getInputStream(), type);

        if (object == null) {
            throw new IllegalArgumentException(
                    "Тіло запиту не може бути порожнім");
        }

        return object;
    }

    protected long requiredId(String value) {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Необхідно вказати параметр id");
        }

        try {
            long id = Long.parseLong(value);

            if (id <= 0) {
                throw new IllegalArgumentException(
                        "ID повинен бути додатним");
            }

            return id;

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Некоректний формат ID");
        }
    }

    protected String requiredAction(String action) {

        if (action == null || action.isBlank()) {
            throw new IllegalArgumentException(
                    "Необхідно вказати параметр action");
        }

        return action;
    }

    protected void sendJson(
            HttpServletResponse response,
            int status,
            Object data) throws IOException {

        response.setStatus(status);
        mapper.writeValue(response.getWriter(), data);
    }

    protected void sendError(
            HttpServletResponse response,
            int status,
            String message) throws IOException {

        if (response.isCommitted()) {
            return;
        }

        response.resetBuffer();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", false);
        result.put("error", message);

        sendJson(response, status, result);
    }

    protected Map<String, Object> success(
            String message,
            String field,
            Object value) {

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("message", message);
        result.put(field, value);

        return result;
    }
}
