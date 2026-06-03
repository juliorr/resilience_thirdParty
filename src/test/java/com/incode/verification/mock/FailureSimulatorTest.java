package com.incode.verification.mock;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FailureSimulatorTest {

    @Test
    void failsWhenRandomBelowProbability() {
        FailureSimulator simulator = new FailureSimulator(() -> 0.05);

        assertThat(simulator.shouldFail(0.40)).isTrue();
        assertThat(simulator.shouldFail(0.10)).isTrue();
    }

    @Test
    void doesNotFailWhenRandomAboveProbability() {
        FailureSimulator simulator = new FailureSimulator(() -> 0.95);

        assertThat(simulator.shouldFail(0.40)).isFalse();
        assertThat(simulator.shouldFail(0.10)).isFalse();
    }

    @Test
    void neverFailsWithZeroProbability() {
        FailureSimulator simulator = new FailureSimulator(() -> 0.0);

        assertThat(simulator.shouldFail(0.0)).isFalse();
    }

    @Test
    void alwaysFailsWithFullProbability() {
        FailureSimulator simulator = new FailureSimulator(() -> 0.99);

        assertThat(simulator.shouldFail(1.0)).isTrue();
    }
}
