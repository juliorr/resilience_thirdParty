package com.incode.verification.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.incode.verification.domain.Company;
import com.incode.verification.domain.VerificationResult.Match;
import com.incode.verification.domain.VerificationResult.NoResults;
import com.incode.verification.domain.VerificationResult.ThirdPartiesDown;
import java.util.List;
import org.junit.jupiter.api.Test;

class VerificationResultCodecTest {

    private final VerificationResultCodec codec = new VerificationResultCodec(new ObjectMapper());

    @Test
    void roundTripsMatchWithOtherResults() {
        Company first = new Company("A1", "Acme", "2021-01-01", "Addr1", true);
        Company other = new Company("A2", "Beta", "2022-02-02", "Addr2", true);
        Match match = new Match(first, List.of(other));

        String type = codec.type(match);
        String json = codec.toJson(match);
        Match restored = (Match) codec.fromJson(type, json);

        assertThat(type).isEqualTo("MATCH");
        assertThat(restored.company()).isEqualTo(first);
        assertThat(restored.otherResults()).containsExactly(other);
    }

    @Test
    void roundTripsNoResults() {
        NoResults result = new NoResults();

        assertThat(codec.type(result)).isEqualTo("NO_RESULTS");
        assertThat(codec.fromJson("NO_RESULTS", codec.toJson(result))).isInstanceOf(NoResults.class);
    }

    @Test
    void roundTripsThirdPartiesDown() {
        ThirdPartiesDown result = new ThirdPartiesDown();

        assertThat(codec.type(result)).isEqualTo("THIRD_PARTIES_DOWN");
        assertThat(codec.fromJson("THIRD_PARTIES_DOWN", codec.toJson(result))).isInstanceOf(ThirdPartiesDown.class);
    }
}
