package com.incode.verification.mock;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;
import org.springframework.stereotype.Component;

@Component
public class FailureSimulator {

    private final DoubleSupplier randomSource;

    public FailureSimulator() {
        this(() -> ThreadLocalRandom.current().nextDouble());
    }

    public FailureSimulator(DoubleSupplier randomSource) {
        this.randomSource = randomSource;
    }

    public boolean shouldFail(double probability) {
        return randomSource.getAsDouble() < probability;
    }
}
