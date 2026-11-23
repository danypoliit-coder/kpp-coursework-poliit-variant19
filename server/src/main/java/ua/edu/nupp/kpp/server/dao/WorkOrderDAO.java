
package ua.edu.nupp.kpp.server.dao;

import ua.edu.nupp.kpp.server.DBUtil;
import ua.edu.nupp.kpp.server.model.WorkOrder;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class WorkOrderDAO {

    // CREATE
    public long create(WorkOrder order) throws SQLException {
        String sql = """
            INSERT INTO work_orders
            (request_id, assigned_to, order_date,
             completion_notes)
            VALUES (?, ?, ?, ?)
            """;

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            setFields(ps, order);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    order.setId(id);
                    return id;
                }
            }

            throw new SQLException(
                    "Не вдалося отримати ID наряду");
        }
    }

    // READ BY ID
    public WorkOrder findById(long id) throws SQLException {
        String sql = "SELECT * FROM work_orders WHERE id = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapOrder(rs) : null;
            }
        }
    }

    // READ ALL
    public List<WorkOrder> findAll() throws SQLException {
        List<WorkOrder> orders = new ArrayList<>();

        String sql = "SELECT * FROM work_orders ORDER BY id";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                orders.add(mapOrder(rs));
            }
        }

        return orders;
    }

    // UPDATE
    public boolean update(WorkOrder order) throws SQLException {
        String sql = """
            UPDATE work_orders SET
            request_id = ?, assigned_to = ?,
            order_date = ?, completion_notes = ?
            WHERE id = ?
            """;

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            setFields(ps, order);
            ps.setLong(5, order.getId());

            return ps.executeUpdate() > 0;
        }
    }

    // DELETE
    public boolean delete(long id) throws SQLException {
        String sql = "DELETE FROM work_orders WHERE id = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    // Заполнение параметров SQL
    private void setFields(
            PreparedStatement ps,
            WorkOrder order) throws SQLException {

        ps.setLong(1, order.getRequestId());
        ps.setString(2, order.getAssignedTo());

        if (order.getOrderDate() == null) {
            ps.setNull(3, Types.DATE);
        } else {
            ps.setDate(3,
                    Date.valueOf(order.getOrderDate()));
        }

        ps.setString(4, order.getCompletionNotes());
    }

    // Преобразование ResultSet в WorkOrder
    private WorkOrder mapOrder(ResultSet rs)
            throws SQLException {

        WorkOrder order = new WorkOrder();

        order.setId(rs.getLong("id"));
        order.setRequestId(rs.getLong("request_id"));
        order.setAssignedTo(rs.getString("assigned_to"));

        Date date = rs.getDate("order_date");
        if (date != null) {
            order.setOrderDate(date.toLocalDate());
        }

        order.setCompletionNotes(
                rs.getString("completion_notes"));

        return order;
    }
}
