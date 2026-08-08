package com.tom.wardrobe.filter;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class TraceIdFilterTest {

    private final TraceIdFilter traceIdFilter = new TraceIdFilter();

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void usesIncomingTraceIdAndReturnsItInResponseHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(TraceIdFilter.TRACE_ID_HEADER, "frontend-trace-001");
        AtomicReference<String> traceIdInsideChain = new AtomicReference<>();

        traceIdFilter.doFilter(request, response, captureTraceId(traceIdInsideChain));

        assertEquals("frontend-trace-001", traceIdInsideChain.get());
        assertEquals("frontend-trace-001", response.getHeader(TraceIdFilter.TRACE_ID_HEADER));
        assertNull(MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY));
    }

    @Test
    void generatesTraceIdWhenIncomingHeaderIsMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> traceIdInsideChain = new AtomicReference<>();

        traceIdFilter.doFilter(request, response, captureTraceId(traceIdInsideChain));

        String generatedTraceId = traceIdInsideChain.get();
        assertNotNull(generatedTraceId);
        assertTrue(generatedTraceId.matches("[A-Za-z0-9._-]{8,64}"));
        assertEquals(generatedTraceId, response.getHeader(TraceIdFilter.TRACE_ID_HEADER));
        assertNull(MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY));
    }

    @Test
    void replacesUnsafeIncomingTraceId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(TraceIdFilter.TRACE_ID_HEADER, "bad trace id with spaces");
        AtomicReference<String> traceIdInsideChain = new AtomicReference<>();

        traceIdFilter.doFilter(request, response, captureTraceId(traceIdInsideChain));

        assertNotEquals("bad trace id with spaces", traceIdInsideChain.get());
        assertEquals(traceIdInsideChain.get(), response.getHeader(TraceIdFilter.TRACE_ID_HEADER));
        assertNull(MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY));
    }

    private FilterChain captureTraceId(AtomicReference<String> traceIdInsideChain) {
        return (request, response) -> traceIdInsideChain.set(MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY));
    }
}
