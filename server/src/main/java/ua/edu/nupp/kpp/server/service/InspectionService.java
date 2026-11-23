
package ua.edu.nupp.kpp.server.service;

import ua.edu.nupp.kpp.server.AppLogger;
import ua.edu.nupp.kpp.server.dao.InspectionDAO;
import ua.edu.nupp.kpp.server.dao.TreeDAO;
import ua.edu.nupp.kpp.server.model.Inspection;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class InspectionService {

    private final InspectionDAO inspectionDAO =
            new InspectionDAO();

    private final TreeDAO treeDAO =
            new TreeDAO();

    // CREATE
    public long create(Inspection inspection)
            throws SQLException {

        validate(inspection);
        checkTreeExists(inspection.getTreeId());

        try {
            long id = inspectionDAO.create(inspection);

            AppLogger.info(
                    "Створено огляд ID: " + id
                            + ", дерево ID: "
                            + inspection.getTreeId()
            );

            return id;
        } catch (SQLException e) {
            AppLogger.error(
                    "Помилка створення огляду", e);
            throw e;
        }
    }

    // READ BY ID
    public Inspection findById(long id)
            throws SQLException {

        validateId(id);

        try {
            Inspection inspection =
                    inspectionDAO.findById(id);

            if (inspection == null) {
                AppLogger.warning(
                        "Огляд ID: " + id + " не знайдено");

                throw new IllegalArgumentException(
                        "Огляд з ID " + id + " не знайдено");
            }

            return inspection;
        } catch (SQLException e) {
            AppLogger.error(
                    "Помилка пошуку огляду ID: " + id, e);
            throw e;
        }
    }

    // READ ALL
    public List<Inspection> findAll()
            throws SQLException {

        try {
            return inspectionDAO.findAll();
        } catch (SQLException e) {
            AppLogger.error(
                    "Помилка отримання списку оглядів", e);
            throw e;
        }
    }

    // UPDATE
    public boolean update(Inspection inspection)
            throws SQLException {

        validate(inspection);

        if (inspection.getId() == null) {
            throw new IllegalArgumentException(
                    "ID огляду не може бути порожнім");
        }

        validateId(inspection.getId());

        findById(inspection.getId());
        checkTreeExists(inspection.getTreeId());

        try {
            boolean updated =
                    inspectionDAO.update(inspection);

            if (updated) {
                AppLogger.info(
                        "Оновлено огляд ID: "
                                + inspection.getId());
            }

            return updated;
        } catch (SQLException e) {
            AppLogger.error(
                    "Помилка оновлення огляду ID: "
                            + inspection.getId(), e);
            throw e;
        }
    }

    // DELETE
    public boolean delete(long id)
            throws SQLException {

        validateId(id);
        findById(id);

        try {
            boolean deleted = inspectionDAO.delete(id);

            if (deleted) {
                AppLogger.info(
                        "Видалено огляд ID: " + id);
            }

            return deleted;
        } catch (SQLException e) {
            AppLogger.error(
                    "Помилка видалення огляду ID: " + id, e);
            throw e;
        }
    }

    // VALIDATION
    private void validate(Inspection inspection) {

        if (inspection == null) {
            throw new IllegalArgumentException(
                    "Об'єкт огляду не може бути null");
        }

        if (inspection.getTreeId() == null ||
                inspection.getTreeId() <= 0) {
            throw new IllegalArgumentException(
                    "Необхідно вказати коректний ID дерева");
        }

        if (inspection.getInspectionDate() == null) {
            throw new IllegalArgumentException(
                    "Дата огляду є обов'язковою");
        }

        if (inspection.getInspectionDate()
                .isAfter(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "Дата огляду не може бути в майбутньому");
        }

        if (inspection.getInspector() == null ||
                inspection.getInspector().isBlank() ||
                inspection.getInspector().length() > 120) {
            throw new IllegalArgumentException(
                    "Ім'я інспектора повинно містити "
                            + "від 1 до 120 символів");
        }

        if (inspection.getTechnicalState() == null ||
                inspection.getTechnicalState()
                        .compareTo(BigDecimal.ZERO) < 0 ||
                inspection.getTechnicalState()
                        .compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException(
                    "Технічний стан повинен бути від 0 до 100");
        }

        if (inspection.getNotes() != null &&
                inspection.getNotes().length() > 5000) {
            throw new IllegalArgumentException(
                    "Примітки не можуть перевищувати "
                            + "5000 символів");
        }
    }

    // CHECK TREE
    private void checkTreeExists(long treeId)
            throws SQLException {

        if (treeDAO.findById(treeId) == null) {
            AppLogger.warning(
                    "Дерево ID: " + treeId
                            + " не знайдено для огляду");

            throw new IllegalArgumentException(
                    "Дерево з ID " + treeId
                            + " не існує");
        }
    }

    private void validateId(long id) {
        if (id <= 0) {
            throw new IllegalArgumentException(
                    "ID повинен бути додатним");
        }
    }
}
