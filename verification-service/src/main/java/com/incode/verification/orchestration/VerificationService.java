package com.incode.verification.orchestration;

import com.incode.verification.domain.Company;
import com.incode.verification.domain.Source;
import com.incode.verification.domain.VerificationRecord;
import com.incode.verification.domain.VerificationResult;
import com.incode.verification.domain.VerificationResult.Match;
import com.incode.verification.domain.VerificationResult.NoResults;
import com.incode.verification.domain.VerificationResult.ThirdPartiesDown;
import com.incode.verification.metrics.MetricsRecorder;
import com.incode.verification.persistence.VerificationRepository;
import com.incode.verification.thirdparty.FreeThirdPartyClient;
import com.incode.verification.thirdparty.PremiumThirdPartyClient;
import com.incode.verification.thirdparty.ThirdPartyResult;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

@Service
public class VerificationService {

    private static final Logger log = LoggerFactory.getLogger(VerificationService.class);

    private final FreeThirdPartyClient freeClient;
    private final PremiumThirdPartyClient premiumClient;
    private final VerificationRepository repository;
    private final MetricsRecorder metrics;

    public VerificationService(
            FreeThirdPartyClient freeClient,
            PremiumThirdPartyClient premiumClient,
            VerificationRepository repository,
            MetricsRecorder metrics) {
        this.freeClient = freeClient;
        this.premiumClient = premiumClient;
        this.repository = repository;
        this.metrics = metrics;
    }

    public VerificationRecord verify(String verificationId, String query) {
        MDC.put("verificationId", verificationId);
        MDC.put("query", query);
        try {
            Resolution resolution = resolve(query);
            if (resolution.source() != null) {
                MDC.put("source", resolution.source().name());
            }
            metrics.result(metricType(resolution.result()));
            VerificationRecord record = new VerificationRecord(
                    verificationId, query, Instant.now(), resolution.source(), resolution.result());
            repository.save(record);
            log.info("Verification persisted with result {}", metricType(resolution.result()));
            return record;
        } finally {
            MDC.clear();
        }
    }

    private Resolution resolve(String query) {
        ThirdPartyResult free = freeClient.search(query);
        if (free.hasMatches()) {
            return new Resolution(Source.FREE, select(free.companies()));
        }

        metrics.fallback();
        log.info("FREE returned no matches, falling back to PREMIUM");
        ThirdPartyResult premium = premiumClient.search(query);
        if (premium.available()) {
            return new Resolution(Source.PREMIUM, select(premium.companies()));
        }
        log.warn("Both providers unavailable, returning ThirdPartiesDown");
        return new Resolution(null, new ThirdPartiesDown());
    }

    private VerificationResult select(List<Company> companies) {
        List<Company> active = companies.stream().filter(Company::active).toList();
        if (active.isEmpty()) {
            return new NoResults();
        }
        Company first = active.getFirst();
        List<Company> others = active.subList(1, active.size());
        return new Match(first, List.copyOf(others));
    }

    private String metricType(VerificationResult result) {
        return switch (result) {
            case Match ignored -> "match";
            case NoResults ignored -> "no_results";
            case ThirdPartiesDown ignored -> "down";
        };
    }

    private record Resolution(Source source, VerificationResult result) {}
}
