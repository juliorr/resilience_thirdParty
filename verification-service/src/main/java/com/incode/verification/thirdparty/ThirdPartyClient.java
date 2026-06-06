package com.incode.verification.thirdparty;

import com.incode.verification.domain.Source;

public interface ThirdPartyClient {

    Source source();

    ThirdPartyResult search(String query);
}
