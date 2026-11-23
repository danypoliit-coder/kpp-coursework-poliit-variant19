package ua.edu.nupp.kpp.server.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class RiskCalculatorTest {

    private final RiskCalculator calculator =
            new RiskCalculator();

    @Test
    void calculateNormalRisk() {
        BigDecimal result = calculator.calculate(
                new BigDecimal("80"),
                new BigDecimal("65"),
                new BigDecimal("90")
        );

        assertEquals(
                new BigDecimal("77.25"),
                result
        );
    }

    @Test
    void calculateMinimumRisk() {
        BigDecimal result = calculator.calculate(
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );

        assertEquals(
                new BigDecimal("0.00"),
                result
        );
    }

    @Test
    void calculateMaximumRisk() {
        BigDecimal result = calculator.calculate(
                new BigDecimal("100"),
                new BigDecimal("100"),
                new BigDecimal("100")
        );

        assertEquals(
                new BigDecimal("100.00"),
                result
        );
    }

    @Test
    void calculateOnlyTechnicalState() {
        BigDecimal result = calculator.calculate(
                new BigDecimal("100"),
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );

        assertEquals(
                new BigDecimal("40.00"),
                result
        );
    }

    @Test
    void calculateOnlyPeopleIntensity() {
        BigDecimal result = calculator.calculate(
                BigDecimal.ZERO,
                new BigDecimal("100"),
                BigDecimal.ZERO
        );

        assertEquals(
                new BigDecimal("35.00"),
                result
        );
    }

    @Test
    void calculateOnlyInterventionGeometry() {
        BigDecimal result = calculator.calculate(
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("100")
        );

        assertEquals(
                new BigDecimal("25.00"),
                result
        );
    }

    @Test
    void rejectNegativeTechnicalState() {
        assertThrows(
                IllegalArgumentException.class,
                () -> calculator.calculate(
                        new BigDecimal("-1"),
                        new BigDecimal("50"),
                        new BigDecimal("50")
                )
        );
    }

    @Test
    void rejectTooHighPeopleIntensity() {
        assertThrows(
                IllegalArgumentException.class,
                () -> calculator.calculate(
                        new BigDecimal("50"),
                        new BigDecimal("101"),
                        new BigDecimal("50")
                )
        );
    }

    @Test
    void rejectTooHighInterventionGeometry() {
        assertThrows(
                IllegalArgumentException.class,
                () -> calculator.calculate(
                        new BigDecimal("50"),
                        new BigDecimal("50"),
                        new BigDecimal("101")
                )
        );
    }

    @Test
    void rejectNullIndicator() {
        assertThrows(
                IllegalArgumentException.class,
                () -> calculator.calculate(
                        null,
                        new BigDecimal("50"),
                        new BigDecimal("50")
                )
        );
    }
}
