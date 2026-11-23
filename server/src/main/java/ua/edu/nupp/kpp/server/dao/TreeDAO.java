
package ua.edu.nupp.kpp.server.dao;

import ua.edu.nupp.kpp.server.DBUtil;
import ua.edu.nupp.kpp.server.model.Tree;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TreeDAO {

    // CREATE
    public long create(Tree tree) throws SQLException {
        String sql = """
            INSERT INTO trees
            (species, address, latitude, longitude,
             technical_state, people_intensity, green_area)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            setTreeFields(ps, tree);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    tree.setId(id);
                    return id;
                }
            }
            throw new SQLException("Не вдалося отримати ID дерева");
        }
    }

    // READ BY ID
    public Tree findById(long id) throws SQLException {
        String sql = "SELECT * FROM trees WHERE id = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapTree(rs) : null;
            }
        }
    }

    // READ ALL
    public List<Tree> findAll() throws SQLException {
        List<Tree> trees = new ArrayList<>();
        String sql = "SELECT * FROM trees ORDER BY id";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                trees.add(mapTree(rs));
            }
        }
        return trees;
    }

    // UPDATE
    public boolean update(Tree tree) throws SQLException {
        String sql = """
            UPDATE trees SET
            species = ?, address = ?, latitude = ?,
            longitude = ?, technical_state = ?,
            people_intensity = ?, green_area = ?
            WHERE id = ?
            """;

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            setTreeFields(ps, tree);
            ps.setLong(8, tree.getId());

            return ps.executeUpdate() > 0;
        }
    }

    // DELETE
    public boolean delete(long id) throws SQLException {
        String sql = "DELETE FROM trees WHERE id = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private void setTreeFields(
            PreparedStatement ps, Tree tree) throws SQLException {

        ps.setString(1, tree.getSpecies());
        ps.setString(2, tree.getAddress());
        ps.setBigDecimal(3, tree.getLatitude());
        ps.setBigDecimal(4, tree.getLongitude());
        ps.setBigDecimal(5, tree.getTechnicalState());
        ps.setBigDecimal(6, tree.getPeopleIntensity());
        ps.setBigDecimal(7, tree.getGreenArea());
    }

    private Tree mapTree(ResultSet rs) throws SQLException {
        Tree tree = new Tree();

        tree.setId(rs.getLong("id"));
        tree.setSpecies(rs.getString("species"));
        tree.setAddress(rs.getString("address"));
        tree.setLatitude(rs.getBigDecimal("latitude"));
        tree.setLongitude(rs.getBigDecimal("longitude"));
        tree.setTechnicalState(
                rs.getBigDecimal("technical_state"));
        tree.setPeopleIntensity(
                rs.getBigDecimal("people_intensity"));
        tree.setGreenArea(rs.getBigDecimal("green_area"));

        return tree;
    }
}
