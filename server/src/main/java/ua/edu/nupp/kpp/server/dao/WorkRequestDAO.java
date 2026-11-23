
package ua.edu.nupp.kpp.server.dao;

import ua.edu.nupp.kpp.server.DBUtil;
import ua.edu.nupp.kpp.server.model.WorkRequest;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class WorkRequestDAO {

    // CREATE
    public long create(WorkRequest request) throws SQLException {
        String sql = """
            INSERT INTO work_requests
            (tree_id, description, status, planned_date)
            VALUES (?, ?, ?, ?)
            """;

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, request.getTreeId());
            ps.setString(2, request.getDescription());
            ps.setString(3, request.getStatus());
            setDate(ps, 4, request.getPlannedDate());

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    request.setId(id);
                    return id;
                }
            }

            throw new SQLException("Не вдалося отримати ID заявки");
        }
    }

    // READ BY ID
    public WorkRequest findById(long id) throws SQLException {
        String sql = "SELECT * FROM work_requests WHERE id = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRequest(rs) : null;
            }
        }
    }

    // READ ALL
    public List<WorkRequest> findAll() throws SQLException {
        List<WorkRequest> requests = new ArrayList<>();
        String sql = "SELECT * FROM work_requests ORDER BY id";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                requests.add(mapRequest(rs));
            }
        }

        return requests;
    }

    // UPDATE
    public boolean update(WorkRequest request) throws SQLException {
        String sql = """
            UPDATE work_requests SET
            tree_id = ?, description = ?, status = ?,
            planned_date = ?
            WHERE id = ?
            """;

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, request.getTreeId());
            ps.setString(2, request.getDescription());
            ps.setString(3, request.getStatus());
            setDate(ps, 4, request.getPlannedDate());
            ps.setLong(5, request.getId());

            return ps.executeUpdate() > 0;
        }
    }

    // DELETE
    public boolean delete(long id) throws SQLException {
        String sql = "DELETE FROM work_requests WHERE id = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    // Преобразование ResultSet в WorkRequest
    private WorkRequest mapRequest(ResultSet rs)
            throws SQLException {

        WorkRequest request = new WorkRequest();

        request.setId(rs.getLong("id"));
        request.setTreeId(rs.getLong("tree_id"));
        request.setDescription(rs.getString("description"));
        request.setStatus(rs.getString("status"));

        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) {
            request.setCreatedAt(created.toLocalDateTime());
        }

        Date planned = rs.getDate("planned_date");
        if (planned != null) {
            request.setPlannedDate(planned.toLocalDate());
        }

        return request;
    }

    // Обработка необязательной даты
    private void setDate(
            PreparedStatement ps,
            int index,
            java.time.LocalDate date) throws SQLException {

        if (date == null) {
            ps.setNull(index, Types.DATE);
        } else {
            ps.setDate(index, Date.valueOf(date));
        }
    }
}
