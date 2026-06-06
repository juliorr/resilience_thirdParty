package com.incode.verification.thirdparty;

import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "third-party", url = "${app.third-party.base-url}")
public interface ThirdPartyApi {

    @GetMapping("/free-third-party")
    List<FreeCompany> searchFree(@RequestParam("query") String query);

    @GetMapping("/premium-third-party")
    List<PremiumCompany> searchPremium(@RequestParam("query") String query);
}
