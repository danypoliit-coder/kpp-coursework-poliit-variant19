
package ua.edu.nupp.kpp.server.service;

import ua.edu.nupp.kpp.server.AppLogger;
import ua.edu.nupp.kpp.server.dao.TreeDAO;
import ua.edu.nupp.kpp.server.model.Tree;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

public class TreeService {

    private final TreeDAO treeDAO = new TreeDAO();

    private final RiskCalculator riskCalculator =
            new RiskCalculator();

    // CREATE
    public long create(Tree tree) throws SQLException {
        validate(tree);

        try {
            long id = treeDAO.create(tree);

            AppLogger.info(
                    "Створено дерево з ID: " + id
            );

            return id;

        } catch (SQLException e) {
            AppLogger.error(
                    "Помилка створення дерева", e
            );
            throw e;
        }
    }

    // READ BY ID
    public Tree findById(long id) throws SQLException {
        validateId(id);

        try {
            Tree tree = treeDAO.findById(id);

            if (tree == null) {
                AppLogger.warning(
                        "Дерево з ID " + id + " не знайдено"
                );

                throw new IllegalArgumentException(
                        "Дерево з ID " + id + " не знайдено"
                );
            }

            return tree;

        } catch (SQLException e) {
            AppLogger.error(
                    "Помилка пошуку дерева ID: " + id, e
            );
            throw e;
        }
    }

    // READ ALL
    public List<Tree> findAll() throws SQLException {
        try {
            return treeDAO.findAll();

        } catch (SQLException e) {
            AppLogger.error(
                    "Помилка отримання списку дерев", e
            );
            throw e;
        }
    }

    // UPDATE
    public boolean update(Tree tree) throws SQLException {
        validate(tree);

        if (tree.getId() == null) {
            throw new IllegalArgumentException(
                    "ID дерева не може бути порожнім"
            );
        }

        validateId(tree.getId());
        findById(tree.getId());

        try {
            boolean updated = treeDAO.update(tree);

            if (updated) {
                AppLogger.info(
                        "Оновлено дерево з ID: "
                                + tree.getId()
                );
            }

            return updated;

        } catch (SQLException e) {
            AppLogger.error(
                    "Помилка оновлення дерева ID: "
                            + tree.getId(), e
            );
            throw e;
        }
    }

    // DELETE
    public boolean delete(long id) throws SQLException {
        validateId(id);
        findById(id);

        try {
            boolean deleted = treeDAO.delete(id);

            if (deleted) {
                AppLogger.info(
                        "Видалено дерево з ID: " + id
                );
            }

            return deleted;

        } catch (SQLException e) {
            AppLogger.error(
                    "Помилка видалення дерева ID: " + id, e
            );
            throw e;
        }
    }

    // РОЗРАХУНОК ІНТЕГРАЛЬНОГО ПОКАЗНИКА РИЗИКУ
    //
    // F1 - технічний стан дерева
    // F2 - інтенсивність перебування людей
    // F3 - геометричність втручання
    //
    // I = 0.40 * F1 + 0.35 * F2 + 0.25 * F3

    public BigDecimal calculateRisk(
            long treeId,
            BigDecimal interventionGeometry) throws SQLException {

        Tree tree = findById(treeId);

        try {
            BigDecimal risk = riskCalculator.calculate(
                    tree.getTechnicalState(),
                    tree.getPeopleIntensity(),
                    interventionGeometry
            );

            AppLogger.info(
                    "Розраховано інтегральний показник ризику дерева ID: "
                            + treeId
                            + ", F1: " + tree.getTechnicalState()
                            + ", F2: " + tree.getPeopleIntensity()
                            + ", F3: " + interventionGeometry
                            + ", результат: " + risk
            );

            return risk;

        } catch (IllegalArgumentException e) {
            AppLogger.warning(
                    "Некоректні показники ризику дерева ID: "
                            + treeId + ". " + e.getMessage()
            );
            throw e;
        }
    }

    // VALIDATION
    private void validate(Tree tree) {

        if (tree == null) {
            throw new IllegalArgumentException(
                    "Об'єкт дерева не може бути null"
            );
        }

        if (tree.getSpecies() == null ||
                tree.getSpecies().isBlank() ||
                tree.getSpecies().length() > 120) {

            throw new IllegalArgumentException(
                    "Порода дерева повинна містити "
                            + "від 1 до 120 символів"
            );
        }

        if (tree.getAddress() == null ||
                tree.getAddress().isBlank() ||
                tree.getAddress().length() > 255) {

            throw new IllegalArgumentException(
                    "Адреса повинна містити "
                            + "від 1 до 255 символів"
            );
        }

        validateFactor(
                tree.getTechnicalState(),
                "Технічний стан"
        );

        validateFactor(
                tree.getPeopleIntensity(),
                "Інтенсивність перебування людей"
        );

        validateFactor(
                tree.getGreenArea(),
                "Показник зеленої зони"
        );

        validateRange(
                tree.getLatitude(),
                new BigDecimal("-90"),
                new BigDecimal("90"),
                "Широта"
        );

        validateRange(
                tree.getLongitude(),
                new BigDecimal("-180"),
                new BigDecimal("180"),
                "Довгота"
        );
    }

    // ПЕРЕВІРКА ПОКАЗНИКІВ
    private void validateFactor(
            BigDecimal value,
            String name) {

        if (value == null ||
                value.compareTo(BigDecimal.ZERO) < 0 ||
                value.compareTo(new BigDecimal("100")) > 0) {

            throw new IllegalArgumentException(
                    name + " повинен бути від 0 до 100"
            );
        }
    }

    // ПЕРЕВІРКА КООРДИНАТ
    private void validateRange(
            BigDecimal value,
            BigDecimal min,
            BigDecimal max,
            String name) {

        if (value != null &&
                (value.compareTo(min) < 0 ||
                        value.compareTo(max) > 0)) {

            throw new IllegalArgumentException(
                    name + " має недопустиме значення"
            );
        }
    }

    // ПЕРЕВІРКА ID
    private void validateId(long id) {

        if (id <= 0) {
            throw new IllegalArgumentException(
                    "ID повинен бути додатним"
            );
        }
    }
}