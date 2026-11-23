
package ua.edu.nupp.kpp.server.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class RiskCalculator {

    private static final BigDecimal WEIGHT_F1 =
            new BigDecimal("0.40");

    private static final BigDecimal WEIGHT_F2 =
            new BigDecimal("0.35");

    private static final BigDecimal WEIGHT_F3 =
            new BigDecimal("0.25");

    private static final BigDecimal MIN_VALUE =
            BigDecimal.ZERO;

    private static final BigDecimal MAX_VALUE =
            new BigDecimal("100");

    public BigDecimal calculate(
            BigDecimal technicalState,
            BigDecimal peopleIntensity,
            BigDecimal interventionGeometry) {

        validateIndicator(technicalState, "Технічний стан (F1)");
        validateIndicator(peopleIntensity, "Інтенсивність перебування людей (F2)");
        validateIndicator(interventionGeometry, "Геометричність втручання (F3)");

        return technicalState.multiply(WEIGHT_F1)
                .add(peopleIntensity.multiply(WEIGHT_F2))
                .add(interventionGeometry.multiply(WEIGHT_F3))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private void validateIndicator(BigDecimal value, String name) {

        if (value == null) {
            throw new IllegalArgumentException(
                    name + " не може бути порожнім");
        }

        if (value.compareTo(MIN_VALUE) < 0
                || value.compareTo(MAX_VALUE) > 0) {

            throw new IllegalArgumentException(
                    name + " повинен бути в межах від 0 до 100");
        }
    }
}
