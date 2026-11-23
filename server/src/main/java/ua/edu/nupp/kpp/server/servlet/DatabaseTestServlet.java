
package ua.edu.nupp.kpp.server.servlet;

import ua.edu.nupp.kpp.server.DBUtil;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseTestServlet extends HttpServlet {

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");

        try (Connection connection = DBUtil.getConnection()) {

            response.setStatus(HttpServletResponse.SC_OK);

            response.getWriter().write(
                    "{\"success\":true,"
                            + "\"message\":\"Підключення до MySQL успішне\"}"
            );

        } catch (SQLException e) {

            e.printStackTrace();

            response.setStatus(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR
            );

            response.getWriter().write(
                    "{\"success\":false,"
                            + "\"message\":\"Помилка підключення до MySQL\"}"
            );
        }
    }
}
