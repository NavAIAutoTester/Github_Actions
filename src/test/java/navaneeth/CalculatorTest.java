package navaneeth;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CalculatorTest {

    private final Calculator calculator = new Calculator();

    @Test
    @DisplayName("add returns the sum of its operands")
    void addReturnsSum() {
        assertEquals(5, calculator.add(2, 3), "2 + 3 should equal 5");
        assertEquals(0, calculator.add(-1, 1), "-1 + 1 should equal 0");
    }

    @Test
    @DisplayName("subtract returns the difference of its operands")
    void subtractReturnsDifference() {
        assertEquals(6, calculator.subtract(10, 4), "10 - 4 should equal 6");
    }
}
