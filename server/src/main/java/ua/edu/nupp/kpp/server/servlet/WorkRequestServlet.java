
package ua.edu.nupp.kpp.server.servlet;

import ua.edu.nupp.kpp.server.model.WorkRequest;
import ua.edu.nupp.kpp.server.service.WorkRequestService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

public class WorkRequestServlet extends ApiServlet {

    private final WorkRequestService service =
            new WorkRequestService();

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
                    WorkRequest workRequest =
                            readBody(request, WorkRequest.class);

                    long id = service.create(workRequest);

                    sendJson(response, 201,
                            success("Заявку створено", "id", id));
                }

                case "update" -> {
                    WorkRequest workRequest =
                            readBody(request, WorkRequest.class);

                    boolean updated =
                            service.update(workRequest);

                    sendJson(response, 200,
                            success("Заявку оновлено",
                                    "updated", updated));
                }

                case "delete" -> {
                    long id = requiredId(
                            request.getParameter("id"));

                    boolean deleted = service.delete(id);

                    sendJson(response, 200,
                            success("Заявку видалено",
                                    "deleted", deleted));
                }

                case "changeStatus" -> {
                    long id = requiredId(
                            request.getParameter("id"));

                    String status =
                            request.getParameter("status");

                    if (status == null || status.isBlank()) {
                        throw new IllegalArgumentException(
                                "Необхідно вказати новий статус");
                    }

                    boolean changed =
                            service.changeStatus(id, status);

                    sendJson(response, 200,
                            success("Статус заявки змінено",
                                    "changed", changed));
                }

                default -> throw new IllegalArgumentException(
                        "Невідома дія: " + action);
            }
        });
    }
}
