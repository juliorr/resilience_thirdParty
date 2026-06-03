package com.incode.verification.orchestration;

import com.incode.verification.domain.Company;
import com.incode.verification.domain.Source;
import com.incode.verification.domain.VerificationRecord;
import com.incode.verification.domain.VerificationResult;
import com.incode.verification.domain.VerificationResult.Match;
import com.incode.verification.domain.VerificationResult.NoResults;
import com.incode.verification.domain.VerificationResult.ThirdPartiesDown;
import com.incode.verification.persistence.VerificationRepository;
import com.incode.verification.thirdparty.FreeThirdPartyClient;
import com.incode.verification.thirdparty.PremiumThirdPartyClient;
import com.incode.verification.thirdparty.ThirdPartyResult;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class VerificationService {

    private final FreeThirdPartyClient freeClient;
    private final PremiumThirdPartyClient premiumClient;
    private final VerificationRepository repository;

    public VerificationService(
            FreeThirdPartyClient freeClient, PremiumThirdPartyClient premiumClient, VerificationRepository repository) {
        this.freeClient = freeClient;
        this.premiumClient = premiumClient;
        this.repository = repository;
    }

    public VerificationRecord verify(String verificationId, String query) {
        Resolution resolution = resolve(query);
        VerificationRecord record =
                new VerificationRecord(verificationId, query, Instant.now(), resolution.source(), resolution.result());
        repository.save(record);
        return record;
    }

    private Resolution resolve(String query) {
        ThirdPartyResult free = freeClient.search(query);
        if (free.hasMatches()) {
            return new Resolution(Source.FREE, select(free.companies()));
        }

        ThirdPartyResult premium = premiumClient.search(query);
        if (premium.available()) {
            return new Resolution(Source.PREMIUM, select(premium.companies()));
        }
        if (free.available()) {
            return new Resolution(Source.FREE, select(free.companies()));
        }
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

    private record Resolution(Source source, VerificationResult result) {}
}
