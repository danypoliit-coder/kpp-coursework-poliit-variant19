
package ua.edu.nupp.kpp.server.servlet;

import ua.edu.nupp.kpp.server.model.WorkOrder;
import ua.edu.nupp.kpp.server.service.WorkOrderService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

public class WorkOrderServlet extends ApiServlet {

    private final WorkOrderService service =
            new WorkOrderService();

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        execute(request, response, () -> {

            String action = request.getParameter("action");

            if (action != null && !action.isBlank()) {
                throw new IllegalArgumentException(
                        "Невідома дія: " + action);
            }

            String id = request.getParameter("id");

            if (id == null) {
                sendJson(response, 200, service.findAll());
            } else {
                sendJson(response, 200,
                        service.findById(requiredId(id)));
            }
        });
    }

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        execute(request, response, () -> {

            String action = requiredAction(
                    request.getParameter("action"));

            switch (action) {

                case "create" -> {
                    WorkOrder order =
                            readBody(request, WorkOrder.class);

                    long id = service.create(order);

                    sendJson(response, 201,
                            success("Наряд створено", "id", id));
                }

                case "update" -> {
                    WorkOrder order =
                            readBody(request, WorkOrder.class);

                    boolean updated = service.update(order);

                    sendJson(response, 200,
                            success("Наряд оновлено",
                                    "updated", updated));
                }

                case "delete" -> {
                    long id = requiredId(
                            request.getParameter("id"));

                    boolean deleted = service.delete(id);

                    sendJson(response, 200,
                            success("Наряд видалено",
                                    "deleted", deleted));
                }

                default -> throw new IllegalArgumentException(
                        "Невідома дія: " + action);
            }
        });
    }
}
