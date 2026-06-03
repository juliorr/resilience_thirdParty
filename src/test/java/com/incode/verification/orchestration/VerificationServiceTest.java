package com.incode.verification.orchestration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.incode.verification.domain.Company;
import com.incode.verification.domain.Source;
import com.incode.verification.domain.VerificationRecord;
import com.incode.verification.domain.VerificationResult.Match;
import com.incode.verification.domain.VerificationResult.NoResults;
import com.incode.verification.domain.VerificationResult.ThirdPartiesDown;
import com.incode.verification.persistence.VerificationRepository;
import com.incode.verification.thirdparty.FreeThirdPartyClient;
import com.incode.verification.thirdparty.PremiumThirdPartyClient;
import com.incode.verification.thirdparty.ThirdPartyResult;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VerificationServiceTest {

    private static final String ID = "11111111-1111-1111-1111-111111111111";

    @Mock
    private FreeThirdPartyClient freeClient;

    @Mock
    private PremiumThirdPartyClient premiumClient;

    @Mock
    private VerificationRepository repository;

    private VerificationService service;

    private static Company active(String cin) {
        return new Company(cin, "Name " + cin, "2020-01-01", "Address " + cin, true);
    }

    private static Company inactive(String cin) {
        return new Company(cin, "Name " + cin, "2020-01-01", "Address " + cin, false);
    }

    @BeforeEach
    void setUp() {
        service = new VerificationService(freeClient, premiumClient, repository);
    }

    @Test
    void usesFreeWhenItHasActiveMatchAndDoesNotCallPremium() {
        when(freeClient.search("q")).thenReturn(ThirdPartyResult.available(List.of(active("A1"))));

        VerificationRecord record = service.verify(ID, "q");

        assertThat(record.source()).isEqualTo(Source.FREE);
        assertThat(record.result()).isInstanceOf(Match.class);
        assertThat(((Match) record.result()).company().cin()).isEqualTo("A1");
        verify(premiumClient, never()).search(any());
        verify(repository).save(record);
    }

    @Test
    void fallsBackToPremiumWhenFreeIsUnavailable() {
        when(freeClient.search("q")).thenReturn(ThirdPartyResult.unavailable());
        when(premiumClient.search("q")).thenReturn(ThirdPartyResult.available(List.of(active("P1"))));

        VerificationRecord record = service.verify(ID, "q");

        assertThat(record.source()).isEqualTo(Source.PREMIUM);
        assertThat(((Match) record.result()).company().cin()).isEqualTo("P1");
    }

    @Test
    void fallsBackToPremiumWhenFreeIsEmpty() {
        when(freeClient.search("q")).thenReturn(ThirdPartyResult.available(List.of()));
        when(premiumClient.search("q")).thenReturn(ThirdPartyResult.available(List.of(active("P2"))));

        VerificationRecord record = service.verify(ID, "q");

        assertThat(record.source()).isEqualTo(Source.PREMIUM);
        assertThat(((Match) record.result()).company().cin()).isEqualTo("P2");
    }

    @Test
    void returnsThirdPartiesDownWhenBothUnavailable() {
        when(freeClient.search("q")).thenReturn(ThirdPartyResult.unavailable());
        when(premiumClient.search("q")).thenReturn(ThirdPartyResult.unavailable());

        VerificationRecord record = service.verify(ID, "q");

        assertThat(record.result()).isInstanceOf(ThirdPartiesDown.class);
        assertThat(record.source()).isNull();
    }

    @Test
    void returnsNoResultsWhenFreeReturnsOnlyInactiveAndDoesNotFallBack() {
        when(freeClient.search("q")).thenReturn(ThirdPartyResult.available(List.of(inactive("I1"))));

        VerificationRecord record = service.verify(ID, "q");

        assertThat(record.result()).isInstanceOf(NoResults.class);
        assertThat(record.source()).isEqualTo(Source.FREE);
        verify(premiumClient, never()).search(any());
    }

    @Test
    void selectsFirstActiveAndExcludesInactiveAndCollectsOtherResults() {
        when(freeClient.search("q"))
                .thenReturn(ThirdPartyResult.available(
                        List.of(inactive("I1"), active("A1"), active("A2"), inactive("I2"))));

        VerificationRecord record = service.verify(ID, "q");

        Match match = (Match) record.result();
        assertThat(match.company().cin()).isEqualTo("A1");
        assertThat(match.otherResults()).extracting(Company::cin).containsExactly("A2");
    }
}
