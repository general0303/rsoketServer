package com.example.rsoketserver;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RsoketServerApplicationTests {

    @Test
    void testFact() {
        Integer[] tests = new Integer[]{1, 5, 10};
        Integer[] results = new Integer[]{1, 120, 3628800};
        RSocketController controller = new RSocketController();
        for (int i=0; i<3; i++){
            assertEquals(controller.factorial(tests[i]), results[i]);
        }
    }

    @Test
    void testFib(){
        long[] tests = new long[]{1, 2, 5, 10};
        Integer[] results = new Integer[]{1, 1, 5, 55};
        RSocketController controller = new RSocketController();
        for (int i=0; i<4; i++){
            assertEquals(controller.fib(tests[i]), results[i]);
        }
    }
}
