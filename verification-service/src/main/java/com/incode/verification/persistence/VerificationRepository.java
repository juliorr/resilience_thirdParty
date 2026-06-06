package com.incode.verification.persistence;

import com.incode.verification.domain.VerificationRecord;
import java.util.Optional;

public interface VerificationRepository {

    void save(VerificationRecord record);

    Optional<VerificationRecord> findById(String verificationId);
}
