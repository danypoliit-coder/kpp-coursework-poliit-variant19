
package ua.edu.nupp.kpp.server.service;

import ua.edu.nupp.kpp.server.dao.TreeDAO;
import ua.edu.nupp.kpp.server.dao.WorkRequestDAO;
import ua.edu.nupp.kpp.server.dao.WorkOrderDAO;
import ua.edu.nupp.kpp.server.model.*;
import ua.edu.nupp.kpp.server.service.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ServiceTestRunner {

    private static final TreeService treeService =
            new TreeService();

    private static final InspectionService inspectionService =
            new InspectionService();

    private static final WorkRequestService requestService =
            new WorkRequestService();

    private static final WorkOrderService orderService =
            new WorkOrderService();

    public static void main(String[] args) throws Exception {

        Long treeId = null;
        Long requestId = null;
        Long orderId = null;

        System.out.println("=== SERVICE TESTS ===");

        try {
            // 1. Перевірка валідації дерева
            Tree invalidTree = new Tree();
            invalidTree.setSpecies("");

            expectFailure(
                    () -> treeService.create(invalidTree),
                    "Порожня порода дерева");

            // 2. Перевірка неіснуючого дерева
            Inspection invalidInspection = new Inspection();
            invalidInspection.setTreeId(Long.MAX_VALUE);
            invalidInspection.setInspectionDate(LocalDate.now());
            invalidInspection.setInspector("Тестовий інспектор");
            invalidInspection.setTechnicalState(
                    new BigDecimal("50.00"));
            invalidInspection.setNotes("Тест");

            expectFailure(
                    () -> inspectionService.create(
                            invalidInspection),
                    "Огляд неіснуючого дерева");

            // 3. Створення тестового дерева
            Tree tree = new Tree();
            tree.setSpecies("Тестовий дуб");
            tree.setAddress("Тестова адреса Service");
            tree.setLatitude(new BigDecimal("49.588300"));
            tree.setLongitude(new BigDecimal("34.551400"));
            tree.setTechnicalState(new BigDecimal("60.00"));
            tree.setPeopleIntensity(new BigDecimal("70.00"));
            tree.setGreenArea(new BigDecimal("50.00"));

            treeId = treeService.create(tree);
            check(treeId > 0, "Створення дерева");

            // 4. Створення заявки
            WorkRequest request = new WorkRequest();
            request.setTreeId(treeId);
            request.setDescription("Тестове обрізання гілок");
            request.setStatus(WorkRequestService.CREATED);
            request.setPlannedDate(LocalDate.now().plusDays(1));

            requestId = requestService.create(request);
            check(requestId > 0, "Створення заявки");

            final long id = requestId;

            // 5. Заборонений перехід
            expectFailure(
                    () -> requestService.changeStatus(
                            id, WorkRequestService.COMPLETED),
                    "Заборона пропуску статусів");

            // 6. Наряд для неперевіреної заявки
            WorkOrder order = new WorkOrder();
            order.setRequestId(requestId);
            order.setAssignedTo("Тестова бригада");
            order.setOrderDate(LocalDate.now().plusDays(1));
            order.setCompletionNotes("Тестовий наряд");

            expectFailure(
                    () -> orderService.create(order),
                    "Заборона передчасного наряду");

            // 7. Правильні переходи статусів
            check(requestService.changeStatus(
                            id, WorkRequestService.VERIFIED),
                    "СТВОРЕНО -> ПЕРЕВІРЕНО");

            check(requestService.changeStatus(
                            id, WorkRequestService.PLANNED),
                    "ПЕРЕВІРЕНО -> ЗАПЛАНОВАНО");

            // 8. Створення наряду
            orderId = orderService.create(order);
            check(orderId > 0,
                    "Створення наряду для запланованої заявки");

            // 9. Завершення заявки
            check(requestService.changeStatus(
                            id, WorkRequestService.COMPLETED),
                    "ЗАПЛАНОВАНО -> ВИКОНАНО");

            // 10. Заборона редагування завершеної заявки
            WorkRequest completed = requestService.findById(id);
            completed.setDescription("Змінений опис");

            expectFailure(
                    () -> requestService.update(completed),
                    "Заборона редагування виконаної заявки");

            // 11. Заборона видалення завершеної заявки
            expectFailure(
                    () -> requestService.delete(id),
                    "Заборона видалення виконаної заявки");

            System.out.println(
                    "\n=== УСІ SERVICE ТЕСТИ ПРОЙДЕНО ===");

        } finally {
            // Прибираємо тільки створені тестові записи.
            // DAO використовується тут виключно для очищення.
            System.out.println("\n--- CLEANUP ---");

            if (orderId != null) {
                new WorkOrderDAO().delete(orderId);
            }

            if (requestId != null) {
                new WorkRequestDAO().delete(requestId);
            }

            if (treeId != null) {
                new TreeDAO().delete(treeId);
            }

            System.out.println("Тестові записи очищено");
        }
    }

    @FunctionalInterface
    interface TestAction {
        void run() throws Exception;
    }

    private static void expectFailure(
            TestAction action, String name) throws Exception {

        try {
            action.run();
        } catch (IllegalArgumentException |
                 IllegalStateException expected) {

            System.out.println("OK: " + name);
            return;
        }

        throw new AssertionError(
                "FAILED: " + name +
                        " — операція мала бути заборонена");
    }

    private static void check(
            boolean condition, String name) {

        if (!condition) {
            throw new AssertionError("FAILED: " + name);
        }

        System.out.println("OK: " + name);
    }
}
