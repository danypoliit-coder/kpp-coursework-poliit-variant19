
package ua.edu.nupp.kpp.server.service;

import org.junit.jupiter.api.Test;
import ua.edu.nupp.kpp.server.model.Tree;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class TreeServiceTest {

    private final TreeService service = new TreeService();

    private Tree validTree() {
        Tree tree = new Tree();

        tree.setSpecies("Липа");
        tree.setAddress("вул. Соборності, 1");

        tree.setTechnicalState(new BigDecimal("80"));
        tree.setPeopleIntensity(new BigDecimal("65"));
        tree.setGreenArea(new BigDecimal("70"));

        return tree;
    }

    @Test
    void rejectNullTree() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.create(null)
        );
    }

    @Test
    void rejectEmptySpecies() {
        Tree tree = validTree();
        tree.setSpecies("");

        assertThrows(
                IllegalArgumentException.class,
                () -> service.create(tree)
        );
    }

    @Test
    void rejectEmptyAddress() {
        Tree tree = validTree();
        tree.setAddress("");

        assertThrows(
                IllegalArgumentException.class,
                () -> service.create(tree)
        );
    }

    @Test
    void rejectInvalidTechnicalState() {
        Tree tree = validTree();
        tree.setTechnicalState(new BigDecimal("120"));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.create(tree)
        );
    }

    @Test
    void rejectInvalidPeopleIntensity() {
        Tree tree = validTree();
        tree.setPeopleIntensity(new BigDecimal("-5"));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.create(tree)
        );
    }
}
