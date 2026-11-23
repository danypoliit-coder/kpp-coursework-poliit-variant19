
package ua.edu.nupp.kpp.server.service;

import ua.edu.nupp.kpp.server.dao.*;
import ua.edu.nupp.kpp.server.model.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public class DAOTestRunner {

    private static final TreeDAO treeDAO = new TreeDAO();
    private static final InspectionDAO inspectionDAO =
            new InspectionDAO();
    private static final WorkRequestDAO requestDAO =
            new WorkRequestDAO();
    private static final WorkOrderDAO orderDAO =
            new WorkOrderDAO();

    public static void main(String[] args) throws Exception {

        Long treeId = null;
        Long inspectionId = null;
        Long requestId = null;
        Long orderId = null;

        System.out.println("=== ПОЧАТОК CRUD ТЕСТУВАННЯ ===");

        try {
            // 1. TREE
            System.out.println("\n--- TREE ---");

            Tree tree = new Tree();
            tree.setSpecies("Тестова липа");
            tree.setAddress("Тестова адреса");
            tree.setLatitude(new BigDecimal("49.588300"));
            tree.setLongitude(new BigDecimal("34.551400"));
            tree.setTechnicalState(new BigDecimal("70.00"));
            tree.setPeopleIntensity(new BigDecimal("60.00"));
            tree.setGreenArea(new BigDecimal("50.00"));

            treeId = treeDAO.create(tree);
            check(treeId > 0, "Tree CREATE");

            Tree savedTree = treeDAO.findById(treeId);
            check(savedTree != null &&
                            savedTree.getSpecies().equals("Тестова липа"),
                    "Tree READ");

            check(treeDAO.findAll().stream()
                    .anyMatch(t -> treeIdEquals(t.getId(),
                            tree.getId())), "Tree READ ALL");

            savedTree.setSpecies("Оновлена липа");
            check(treeDAO.update(savedTree), "Tree UPDATE");

            check(treeDAO.findById(treeId)
                            .getSpecies().equals("Оновлена липа"),
                    "Tree UPDATE VERIFY");

            // 2. INSPECTION
            System.out.println("\n--- INSPECTION ---");

            Inspection inspection = new Inspection();
            inspection.setTreeId(treeId);
            inspection.setInspectionDate(LocalDate.now());
            inspection.setNotes("Тестовий огляд");
            inspection.setTechnicalState(
                    new BigDecimal("75.00"));
            inspection.setInspector("Тестовий інспектор");

            inspectionId = inspectionDAO.create(inspection);
            check(inspectionId > 0, "Inspection CREATE");

            Inspection savedInspection =
                    inspectionDAO.findById(inspectionId);

            check(savedInspection != null &&
                            savedInspection.getTreeId().equals(treeId),
                    "Inspection READ");

            check(inspectionDAO.findAll().stream()
                            .anyMatch(i -> treeIdEquals(i.getId(),
                                    inspection.getId())),
                    "Inspection READ ALL");

            savedInspection.setNotes("Огляд оновлено");
            check(inspectionDAO.update(savedInspection),
                    "Inspection UPDATE");

            check(inspectionDAO.findById(inspectionId)
                            .getNotes().equals("Огляд оновлено"),
                    "Inspection UPDATE VERIFY");

            // 3. WORK REQUEST
            System.out.println("\n--- WORK REQUEST ---");

            WorkRequest request = new WorkRequest();
            request.setTreeId(treeId);
            request.setDescription("Тестове обрізання гілок");
            request.setStatus("СТВОРЕНО");

            requestId = requestDAO.create(request);
            check(requestId > 0, "Request CREATE");

            WorkRequest savedRequest =
                    requestDAO.findById(requestId);

            check(savedRequest != null &&
                            savedRequest.getStatus().equals("СТВОРЕНО"),
                    "Request READ");

            check(requestDAO.findAll().stream()
                            .anyMatch(r -> treeIdEquals(r.getId(),
                                    request.getId())),
                    "Request READ ALL");

            savedRequest.setDescription("Оновлена заявка");
            check(requestDAO.update(savedRequest),
                    "Request UPDATE");

            check(requestDAO.findById(requestId)
                            .getDescription().equals("Оновлена заявка"),
                    "Request UPDATE VERIFY");

            // 4. WORK ORDER
            System.out.println("\n--- WORK ORDER ---");

            WorkOrder order = new WorkOrder();
            order.setRequestId(requestId);
            order.setAssignedTo("Тестова бригада");
            order.setOrderDate(LocalDate.now());
            order.setCompletionNotes("Тестовий наряд");

            orderId = orderDAO.create(order);
            check(orderId > 0, "Order CREATE");

            WorkOrder savedOrder =
                    orderDAO.findById(orderId);

            check(savedOrder != null &&
                            savedOrder.getRequestId().equals(requestId),
                    "Order READ");

            check(orderDAO.findAll().stream()
                            .anyMatch(o -> treeIdEquals(o.getId(),
                                    order.getId())),
                    "Order READ ALL");

            savedOrder.setCompletionNotes("Роботу перевірено");
            check(orderDAO.update(savedOrder),
                    "Order UPDATE");

            check(orderDAO.findById(orderId)
                            .getCompletionNotes()
                            .equals("Роботу перевірено"),
                    "Order UPDATE VERIFY");

        } finally {
            // Видаляємо тестові записи у зворотному порядку
            System.out.println("\n--- CLEANUP ---");

            if (orderId != null) {
                check(orderDAO.delete(orderId),
                        "Order DELETE");
                check(orderDAO.findById(orderId) == null,
                        "Order DELETE VERIFY");
            }

            if (requestId != null) {
                check(requestDAO.delete(requestId),
                        "Request DELETE");
                check(requestDAO.findById(requestId) == null,
                        "Request DELETE VERIFY");
            }

            if (inspectionId != null) {
                check(inspectionDAO.delete(inspectionId),
                        "Inspection DELETE");
                check(inspectionDAO.findById(inspectionId) == null,
                        "Inspection DELETE VERIFY");
            }

            if (treeId != null) {
                check(treeDAO.delete(treeId),
                        "Tree DELETE");
                check(treeDAO.findById(treeId) == null,
                        "Tree DELETE VERIFY");
            }
        }

        System.out.println(
                "\n=== УСІ CRUD ТЕСТИ ПРОЙДЕНО ===");
    }

    private static boolean treeIdEquals(Long a, Long b) {
        return a != null && a.equals(b);
    }

    private static void check(boolean condition, String name) {
        if (!condition) {
            throw new IllegalStateException(
                    "FAILED: " + name);
        }
        System.out.println("OK: " + name);
    }
}
