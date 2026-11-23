
package ua.edu.nupp.kpp.server.service;

import ua.edu.nupp.kpp.server.AppLogger;
import ua.edu.nupp.kpp.server.dao.WorkOrderDAO;
import ua.edu.nupp.kpp.server.dao.WorkRequestDAO;
import ua.edu.nupp.kpp.server.model.WorkOrder;
import ua.edu.nupp.kpp.server.model.WorkRequest;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class WorkOrderService {

    private final WorkOrderDAO orderDAO =
            new WorkOrderDAO();

    private final WorkRequestDAO requestDAO =
            new WorkRequestDAO();

    // CREATE
    public long create(WorkOrder order)
            throws SQLException {

        validate(order);

        WorkRequest request =
                checkRequestExists(order.getRequestId());

        if (!WorkRequestService.PLANNED.equals(
                request.getStatus())) {

            AppLogger.warning(
                    "Спроба створити наряд для заявки ID: "
                            + order.getRequestId()
                            + " зі статусом "
                            + request.getStatus());

            throw new IllegalStateException(
                    "Наряд можна створити лише для "
                            + "запланованої заявки");
        }

        try {
            long id = orderDAO.create(order);

            AppLogger.info(
                    "Створено наряд ID: " + id
                            + ", заявка ID: "
                            + order.getRequestId());

            return id;
        } catch (SQLException e) {
            AppLogger.error(
                    "Помилка створення наряду", e);
            throw e;
        }
    }

    // READ BY ID
    public WorkOrder findById(long id)
            throws SQLException {

        validateId(id);

        try {
            WorkOrder order = orderDAO.findById(id);

            if (order == null) {
                AppLogger.warning(
                        "Наряд ID: " + id + " не знайдено");

                throw new IllegalArgumentException(
                        "Наряд з ID " + id + " не знайдено");
            }

            return order;
        } catch (SQLException e) {
            AppLogger.error(
                    "Помилка пошуку наряду ID: " + id, e);
            throw e;
        }
    }

    // READ ALL
    public List<WorkOrder> findAll()
            throws SQLException {

        try {
            return orderDAO.findAll();
        } catch (SQLException e) {
            AppLogger.error(
                    "Помилка отримання списку нарядів", e);
            throw e;
        }
    }

    // UPDATE
    public boolean update(WorkOrder order)
            throws SQLException {

        validate(order);

        if (order.getId() == null) {
            throw new IllegalArgumentException(
                    "ID наряду не може бути порожнім");
        }

        WorkOrder existing = findById(order.getId());

        if (!existing.getRequestId()
                .equals(order.getRequestId())) {
            throw new IllegalArgumentException(
                    "Не можна змінювати заявку існуючого наряду");
        }

        WorkRequest request =
                checkRequestExists(order.getRequestId());

        if (WorkRequestService.COMPLETED.equals(
                request.getStatus())) {

            AppLogger.warning(
                    "Спроба змінити наряд ID: "
                            + order.getId()
                            + " виконаної заявки");

            throw new IllegalStateException(
                    "Не можна редагувати наряд "
                            + "виконаної заявки");
        }

        try {
            boolean updated = orderDAO.update(order);

            if (updated) {
                AppLogger.info(
                        "Оновлено наряд ID: "
                                + order.getId());
            }

            return updated;
        } catch (SQLException e) {
            AppLogger.error(
                    "Помилка оновлення наряду ID: "
                            + order.getId(), e);
            throw e;
        }
    }

    // DELETE
    public boolean delete(long id)
            throws SQLException {

        WorkOrder order = findById(id);

        WorkRequest request =
                checkRequestExists(order.getRequestId());

        if (WorkRequestService.COMPLETED.equals(
                request.getStatus())) {

            AppLogger.warning(
                    "Спроба видалити наряд ID: "
                            + id + " виконаної заявки");

            throw new IllegalStateException(
                    "Не можна видалити наряд "
                            + "виконаної заявки");
        }

        try {
            boolean deleted = orderDAO.delete(id);

            if (deleted) {
                AppLogger.info(
                        "Видалено наряд ID: " + id);
            }

            return deleted;
        } catch (SQLException e) {
            AppLogger.error(
                    "Помилка видалення наряду ID: " + id, e);
            throw e;
        }
    }

    // VALIDATION
    private void validate(WorkOrder order) {

        if (order == null) {
            throw new IllegalArgumentException(
                    "Об'єкт наряду не може бути null");
        }

        if (order.getRequestId() == null ||
                order.getRequestId() <= 0) {
            throw new IllegalArgumentException(
                    "Необхідно вказати коректний ID заявки");
        }

        if (order.getAssignedTo() == null ||
                order.getAssignedTo().isBlank() ||
                order.getAssignedTo().length() > 120) {
            throw new IllegalArgumentException(
                    "Виконавець повинен містити "
                            + "від 1 до 120 символів");
        }

        if (order.getOrderDate() == null) {
            throw new IllegalArgumentException(
                    "Дата наряду є обов'язковковою");
        }

        if (order.getOrderDate()
                .isBefore(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "Дата наряду не може бути в минулому");
        }

        if (order.getCompletionNotes() != null &&
                order.getCompletionNotes().length() > 5000) {
            throw new IllegalArgumentException(
                    "Примітки не можуть перевищувати "
                            + "5000 символів");
        }
    }

    // CHECK REQUEST
    private WorkRequest checkRequestExists(long requestId)
            throws SQLException {

        WorkRequest request =
                requestDAO.findById(requestId);

        if (request == null) {
            AppLogger.warning(
                    "Заявка ID: " + requestId
                            + " не знайдена для наряду");

            throw new IllegalArgumentException(
                    "Заявка з ID " + requestId
                            + " не існує");
        }

        return request;
    }

    private void validateId(long id) {
        if (id <= 0) {
            throw new IllegalArgumentException(
                    "ID повинен бути додатним");
        }
    }
}
