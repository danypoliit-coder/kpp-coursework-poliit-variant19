
package ua.edu.nupp.kpp.server.servlet;

import ua.edu.nupp.kpp.server.model.Inspection;
import ua.edu.nupp.kpp.server.service.InspectionService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

public class InspectionServlet extends ApiServlet {

    private final InspectionService service =
            new InspectionService();

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
                    Inspection inspection =
                            readBody(request, Inspection.class);

                    long id = service.create(inspection);

                    sendJson(response, 201,
                            success("Огляд створено", "id", id));
                }

                case "update" -> {
                    Inspection inspection =
                            readBody(request, Inspection.class);

                    boolean updated = service.update(inspection);

                    sendJson(response, 200,
                            success("Огляд оновлено",
                                    "updated", updated));
                }

                case "delete" -> {
                    long id = requiredId(
                            request.getParameter("id"));

                    boolean deleted = service.delete(id);

                    sendJson(response, 200,
                            success("Огляд видалено",
                                    "deleted", deleted));
                }

                default -> throw new IllegalArgumentException(
                        "Невідома дія: " + action);
            }
        });
    }
}
