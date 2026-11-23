
package ua.edu.nupp.kpp.server.dao;

import ua.edu.nupp.kpp.server.DBUtil;
import ua.edu.nupp.kpp.server.model.Inspection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class InspectionDAO {

    // CREATE
    public long create(Inspection inspection) throws SQLException {
        String sql = """
            INSERT INTO inspections
            (tree_id, inspection_date, notes,
             technical_state, inspector)
            VALUES (?, ?, ?, ?, ?)
            """;

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            setFields(ps, inspection);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    inspection.setId(id);
                    return id;
                }
            }

            throw new SQLException("Не вдалося отримати ID огляду");
        }
    }

    // READ BY ID
    public Inspection findById(long id) throws SQLException {
        String sql = "SELECT * FROM inspections WHERE id = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapInspection(rs) : null;
            }
        }
    }

    // READ ALL
    public List<Inspection> findAll() throws SQLException {
        List<Inspection> inspections = new ArrayList<>();

        String sql = "SELECT * FROM inspections ORDER BY id";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                inspections.add(mapInspection(rs));
            }
        }

        return inspections;
    }

    // UPDATE
    public boolean update(Inspection inspection) throws SQLException {
        String sql = """
            UPDATE inspections SET
            tree_id = ?, inspection_date = ?, notes = ?,
            technical_state = ?, inspector = ?
            WHERE id = ?
            """;

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            setFields(ps, inspection);
            ps.setLong(6, inspection.getId());

            return ps.executeUpdate() > 0;
        }
    }

    // DELETE
    public boolean delete(long id) throws SQLException {
        String sql = "DELETE FROM inspections WHERE id = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    // Заполнение параметров SQL
    private void setFields(
            PreparedStatement ps,
            Inspection inspection) throws SQLException {

        ps.setLong(1, inspection.getTreeId());

        ps.setDate(2,
                Date.valueOf(inspection.getInspectionDate()));

        ps.setString(3, inspection.getNotes());

        ps.setBigDecimal(4,
                inspection.getTechnicalState());

        ps.setString(5, inspection.getInspector());
    }

    // Преобразование ResultSet в Java-объект
    private Inspection mapInspection(ResultSet rs)
            throws SQLException {

        Inspection inspection = new Inspection();

        inspection.setId(rs.getLong("id"));
        inspection.setTreeId(rs.getLong("tree_id"));

        Date date = rs.getDate("inspection_date");
        inspection.setInspectionDate(
                date != null ? date.toLocalDate() : null);

        inspection.setNotes(rs.getString("notes"));

        inspection.setTechnicalState(
                rs.getBigDecimal("technical_state"));

        inspection.setInspector(rs.getString("inspector"));

        return inspection;
    }
}
