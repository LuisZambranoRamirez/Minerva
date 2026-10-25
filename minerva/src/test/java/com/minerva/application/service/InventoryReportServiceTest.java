package com.minerva.application.service;

import com.minerva.application.exceptions.ReportRowLimitExceededException;
import com.minerva.application.exceptions.UnauthorizedActionException;
import com.minerva.application.model.report.GeneratedReport;
import com.minerva.application.model.report.InventoryReportQuery;
import com.minerva.application.model.report.InventoryReportRow;
import com.minerva.application.model.report.ReportFormat;
import com.minerva.application.port.driven.AuditService;
import com.minerva.application.port.driven.InventoryReportRepository;
import com.minerva.application.port.driven.InventoryReportWriter;
import com.minerva.application.port.driven.UserContext;
import com.minerva.domain.constants.AuditEventType;
import com.minerva.domain.constants.Permission;
import com.minerva.domain.constants.ProductCategory;
import com.minerva.domain.entities.auditEvent.Attribute;
import com.minerva.domain.entities.auditEvent.AuditEvent;
import com.minerva.domain.entities.inventory.InventoryMovementSource;
import com.minerva.domain.entities.inventory.InventoryMovementType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class InventoryReportServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-10-24T22:00:00Z"), ZoneOffset.UTC);

    @Test
    void authorizedExportSelectsWriterQueriesFiveThousandAndOneAndAuditsAfterWriting() {
        Fixture fixture = new Fixture("ROLE_ADMIN", List.of(row()));

        GeneratedReport result = fixture.service().generate(query(ReportFormat.CSV));

        assertEquals("report.csv", result.filename());
        assertEquals(5_001, fixture.repository.requestedLimit);
        assertEquals(List.of("write", "audit"), fixture.events);
        assertEquals(1, fixture.audit.events.size());
        AuditEvent auditEvent = fixture.audit.events.getFirst();
        assertEquals(Permission.PRODUCT_EXPORT_INVENTORY_REPORT, auditEvent.getPermission());
        assertEquals(AuditEventType.READ, auditEvent.getEventType());
        assertEquals("INVENTORY_REPORTS", auditEvent.getAuditable().getAuditSubjectName());
        assertEquals("csv", attribute(auditEvent, "format"));
        assertEquals("1", attribute(auditEvent, "rowCount"));
        assertEquals("SUCCESS", attribute(auditEvent, "outcome"));
    }

    @Test
    void emptyExportProducesFileAndAuditsZeroRows() {
        Fixture fixture = new Fixture("VENDEDOR", List.of());

        GeneratedReport result = fixture.service().generate(query(ReportFormat.XLSX));

        assertEquals(0, result.rowCount());
        assertEquals("0", attribute(fixture.audit.events.getFirst(), "rowCount"));
    }

    @Test
    void exactlyFiveThousandRowsAreGenerated() {
        Fixture fixture = new Fixture("ALMACENISTA", Collections.nCopies(5_000, row()));

        GeneratedReport result = fixture.service().generate(query(ReportFormat.CSV));

        assertEquals(5_000, result.rowCount());
        assertEquals(1, fixture.writer.calls);
        assertEquals(1, fixture.audit.events.size());
    }

    @Test
    void fiveThousandAndOneRowsFailBeforeWritingOrAuditing() {
        Fixture fixture = new Fixture("ADMIN", Collections.nCopies(5_001, row()));

        assertThrows(ReportRowLimitExceededException.class,
                () -> fixture.service().generate(query(ReportFormat.CSV)));

        assertEquals(0, fixture.writer.calls);
        assertTrue(fixture.audit.events.isEmpty());
    }

    @Test
    void writerFailureDoesNotRegisterSuccessfulAudit() {
        Fixture fixture = new Fixture("ADMIN", List.of(row()));
        fixture.writer.failure = new IllegalStateException("writer failed");

        assertThrows(IllegalStateException.class,
                () -> fixture.service().generate(query(ReportFormat.CSV)));

        assertEquals(1, fixture.writer.calls);
        assertTrue(fixture.audit.events.isEmpty());
    }

    @Test
    void selectsOnlyWriterSupportingRequestedFormat() {
        Fixture fixture = new Fixture("ADMIN", List.of(row()));
        FakeWriter unsupported = new FakeWriter(fixture.events) {
            @Override
            public boolean supports(ReportFormat format) {
                return false;
            }
        };
        InventoryReportService service = new InventoryReportService(
                () -> new UserContext("technicaluser", "ADMIN"),
                fixture.repository,
                List.of(unsupported, fixture.writer),
                fixture.audit,
                FIXED_CLOCK
        );

        service.generate(query(ReportFormat.XLSX));

        assertEquals(0, unsupported.calls);
        assertEquals(1, fixture.writer.calls);
    }

    @Test
    void clienteIsRejectedBeforeQueryingOrWriting() {
        Fixture fixture = new Fixture("CLIENTE", List.of(row()));

        assertThrows(UnauthorizedActionException.class,
                () -> fixture.service().generate(query(ReportFormat.CSV)));

        assertEquals(0, fixture.repository.calls);
        assertEquals(0, fixture.writer.calls);
        assertTrue(fixture.audit.events.isEmpty());
    }

    @Test
    void nullQueryIsRejectedBeforeReadingCurrentUser() {
        Fixture fixture = new Fixture("ADMIN", List.of());

        assertThrows(NullPointerException.class, () -> fixture.service().generate(null));

        assertEquals(0, fixture.currentUserCalls);
        assertEquals(0, fixture.repository.calls);
        assertTrue(fixture.audit.events.isEmpty());
    }

    @Test
    void invertedRangeIsRejectedBeforeServiceDependenciesCanRun() {
        Fixture fixture = new Fixture("ADMIN", List.of());

        assertThrows(IllegalArgumentException.class, () -> new InventoryReportQuery(
                ReportFormat.CSV,
                null,
                null,
                null,
                LocalDateTime.of(2026, 10, 25, 0, 0),
                LocalDateTime.of(2026, 10, 24, 0, 0)
        ));

        assertEquals(0, fixture.currentUserCalls);
        assertEquals(0, fixture.repository.calls);
        assertTrue(fixture.audit.events.isEmpty());
    }

    private static String attribute(AuditEvent event, String name) {
        return event.getAuditable().getAuditData().stream()
                .filter(candidate -> candidate.getName().equals(name))
                .map(Attribute::getAttributeValue)
                .map(value -> value.orElseThrow().toString())
                .findFirst()
                .orElseThrow();
    }

    private static InventoryReportQuery query(ReportFormat format) {
        return new InventoryReportQuery(format, null, null, null, null, null);
    }

    private static InventoryReportRow row() {
        return new InventoryReportRow(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                LocalDateTime.of(2026, 10, 24, 10, 0),
                "SKU-1",
                "Producto",
                ProductCategory.ABARROTES_SECOS,
                InventoryMovementType.ENTRADA,
                InventoryMovementSource.STOCK_ENTRY,
                BigDecimal.ONE,
                BigDecimal.ZERO,
                BigDecimal.ONE,
                "admin"
        );
    }

    private static final class Fixture {
        private final List<String> events = new ArrayList<>();
        private final FakeRepository repository;
        private final FakeWriter writer;
        private final FakeAudit audit;
        private final String role;
        private int currentUserCalls;

        private Fixture(String role, List<InventoryReportRow> rows) {
            this.role = role;
            this.repository = new FakeRepository(rows);
            this.writer = new FakeWriter(events);
            this.audit = new FakeAudit(events);
        }

        private InventoryReportService service() {
            return new InventoryReportService(
                    () -> {
                        currentUserCalls++;
                        return new UserContext("technicaluser", role);
                    },
                    repository,
                    List.of(writer),
                    audit,
                    FIXED_CLOCK
            );
        }
    }

    private static final class FakeRepository implements InventoryReportRepository {
        private final List<InventoryReportRow> rows;
        private int calls;
        private int requestedLimit;

        private FakeRepository(List<InventoryReportRow> rows) {
            this.rows = rows;
        }

        @Override
        public List<InventoryReportRow> findForReport(InventoryReportQuery query, int limit) {
            calls++;
            requestedLimit = limit;
            return rows;
        }
    }

    private static class FakeWriter implements InventoryReportWriter {
        private final List<String> events;
        private int calls;
        private RuntimeException failure;

        private FakeWriter(List<String> events) {
            this.events = events;
        }

        @Override
        public boolean supports(ReportFormat format) {
            return true;
        }

        @Override
        public GeneratedReport write(List<InventoryReportRow> rows, InventoryReportQuery query) {
            calls++;
            events.add("write");
            if (failure != null) {
                throw failure;
            }
            return new GeneratedReport(new byte[]{1}, query.format().mediaType(),
                    "report." + query.format().extension(), rows.size());
        }
    }

    private static final class FakeAudit implements AuditService {
        private final List<String> order;
        private final List<AuditEvent> events = new ArrayList<>();

        private FakeAudit(List<String> order) {
            this.order = order;
        }

        @Override
        public void register(AuditEvent auditEvent) {
            order.add("audit");
            events.add(auditEvent);
        }
    }
}
