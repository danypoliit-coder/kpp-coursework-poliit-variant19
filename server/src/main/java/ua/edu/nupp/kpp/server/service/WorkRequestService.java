
package ua.edu.nupp.kpp.server.service;

import ua.edu.nupp.kpp.server.AppLogger;
import ua.edu.nupp.kpp.server.dao.TreeDAO;
import ua.edu.nupp.kpp.server.dao.WorkRequestDAO;
import ua.edu.nupp.kpp.server.model.WorkRequest;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class WorkRequestService {

    public static final String CREATED = "СТВОРЕНО";
    public static final String VERIFIED = "ПЕРЕВІРЕНО";
    public static final String PLANNED = "ЗАПЛАНОВАНО";
    public static final String COMPLETED = "ВИКОНАНО";

    private final WorkRequestDAO requestDAO =
            new WorkRequestDAO();

    private final TreeDAO treeDAO = new TreeDAO();

    // CREATE
    public long create(WorkRequest request)
            throws SQLException {

        validate(request);

        if (!CREATED.equals(request.getStatus())) {
            throw new IllegalArgumentException(
                    "Нова заявка повинна мати статус СТВОРЕНО");
        }

        checkTreeExists(request.getTreeId());

        try {
            long id = requestDAO.create(request);

            AppLogger.info(
                    "Створено заявку ID: " + id
                            + ", дерево ID: "
                            + request.getTreeId());

            return id;
        } catch (SQLException e) {
            AppLogger.error(
                    "Помилка створення заявки", e);
            throw e;
        }
    }

    // READ BY ID
    public WorkRequest findById(long id)
            throws SQLException {

        validateId(id);

        try {
            WorkRequest request = requestDAO.findById(id);

            if (request == null) {
                AppLogger.warning(
                        "Заявка ID: " + id + " не знайдена");

                throw new IllegalArgumentException(
                        "Заявку з ID " + id + " не знайдено");
            }

            return request;
        } catch (SQLException e) {
            AppLogger.error(
                    "Помилка пошуку заявки ID: " + id, e);
            throw e;
        }
    }

    // READ ALL
    public List<WorkRequest> findAll()
            throws SQLException {

        try {
            return requestDAO.findAll();
        } catch (SQLException e) {
            AppLogger.error(
                    "Помилка отримання списку заявок", e);
            throw e;
        }
    }

    // UPDATE
    public boolean update(WorkRequest request)
            throws SQLException {

        validate(request);

        if (request.getId() == null) {
            throw new IllegalArgumentException(
                    "ID заявки не може бути порожнім");
        }

        WorkRequest existing = findById(request.getId());

        if (!existing.getStatus().equals(request.getStatus())) {
            throw new IllegalArgumentException(
                    "Для зміни статусу використовуйте changeStatus()");
        }

        if (!existing.getTreeId().equals(request.getTreeId())) {
            throw new IllegalArgumentException(
                    "Не можна змінювати дерево існуючої заявки");
        }

        if (COMPLETED.equals(existing.getStatus())) {
            AppLogger.warning(
                    "Спроба редагувати виконану заявку ID: "
                            + request.getId());

            throw new IllegalStateException(
                    "Виконану заявку не можна редагувати");
        }

        checkTreeExists(request.getTreeId());

        try {
            boolean updated = requestDAO.update(request);

            if (updated) {
                AppLogger.info(
                        "Оновлено заявку ID: "
                                + request.getId());
            }

            return updated;
        } catch (SQLException e) {
            AppLogger.error(
                    "Помилка оновлення заявки ID: "
                            + request.getId(), e);
            throw e;
        }
    }

    // CHANGE STATUS
    public boolean changeStatus(long id, String newStatus)
            throws SQLException {

        WorkRequest request = findById(id);

        if (!isValidStatus(newStatus)) {
            AppLogger.warning(
                    "Невідомий статус заявки ID: "
                            + id + ": " + newStatus);

            throw new IllegalArgumentException(
                    "Невідомий статус: " + newStatus);
        }

        String currentStatus = request.getStatus();

        if (!isAllowedTransition(currentStatus, newStatus)) {
            AppLogger.warning(
                    "Недопустимий перехід заявки ID: "
                            + id + ": "
                            + currentStatus + " -> " + newStatus);

            throw new IllegalStateException(
                    "Недопустимий перехід: "
                            + currentStatus + " -> " + newStatus);
        }

        if (PLANNED.equals(newStatus)) {
            if (request.getPlannedDate() == null) {
                throw new IllegalStateException(
                        "Для планування необхідно вказати дату");
            }

            if (request.getPlannedDate()
                    .isBefore(LocalDate.now())) {
                throw new IllegalStateException(
                        "Дата планування не може бути в минулому");
            }
        }

        request.setStatus(newStatus);

        try {
            boolean updated = requestDAO.update(request);

            if (updated) {
                AppLogger.info(
                        "Змінено статус заявки ID: " + id
                                + ": " + currentStatus
                                + " -> " + newStatus);
            }

            return updated;
        } catch (SQLException e) {
            AppLogger.error(
                    "Помилка зміни статусу заявки ID: "
                            + id, e);
            throw e;
        }
    }

    // DELETE
    public boolean delete(long id)
            throws SQLException {

        WorkRequest request = findById(id);

        if (PLANNED.equals(request.getStatus()) ||
                COMPLETED.equals(request.getStatus())) {

            AppLogger.warning(
                    "Спроба видалити заявку ID: " + id
                            + " зі статусом "
                            + request.getStatus());

            throw new IllegalStateException(
                    "Не можна видалити заплановану "
                            + "або виконану заявку");
        }

        try {
            boolean deleted = requestDAO.delete(id);

            if (deleted) {
                AppLogger.info(
                        "Видалено заявку ID: " + id);
            }

            return deleted;
        } catch (SQLException e) {
            AppLogger.error(
                    "Помилка видалення заявки ID: " + id, e);
            throw e;
        }
    }

    // VALIDATION
    private void validate(WorkRequest request) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Об'єкт заявки не може бути null");
        }

        if (request.getTreeId() == null ||
                request.getTreeId() <= 0) {
            throw new IllegalArgumentException(
                    "Необхідно вказати коректний ID дерева");
        }

        if (request.getDescription() == null ||
                request.getDescription().isBlank() ||
                request.getDescription().length() > 500) {
            throw new IllegalArgumentException(
                    "Опис повинен містити від 1 до 500 символів");
        }

        if (!isValidStatus(request.getStatus())) {
            throw new IllegalArgumentException(
                    "Недопустимий статус заявки");
        }
    }

    private boolean isValidStatus(String status) {
        return CREATED.equals(status) ||
                VERIFIED.equals(status) ||
                PLANNED.equals(status) ||
                COMPLETED.equals(status);
    }

    private boolean isAllowedTransition(
            String current, String next) {

        return switch (current) {
            case CREATED -> VERIFIED.equals(next);
            case VERIFIED -> PLANNED.equals(next);
            case PLANNED -> COMPLETED.equals(next);
            default -> false;
        };
    }

    private void checkTreeExists(long treeId)
            throws SQLException {

        if (treeDAO.findById(treeId) == null) {
            AppLogger.warning(
                    "Дерево ID: " + treeId
                            + " не знайдено для заявки");

            throw new IllegalArgumentException(
                    "Дерево з ID " + treeId + " не існує");
        }
    }

    private void validateId(long id) {
        if (id <= 0) {
            throw new IllegalArgumentException(
                    "ID повинен бути додатним");
        }
    }
}
