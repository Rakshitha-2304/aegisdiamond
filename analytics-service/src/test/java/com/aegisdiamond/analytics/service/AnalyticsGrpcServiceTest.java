package com.aegisdiamond.analytics.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AnalyticsGrpcServiceTest {

    @Test
    void sanityCheck() {
        AnalyticsGrpcService service = new AnalyticsGrpcService();
        assertNotNull(service);
    }
}
