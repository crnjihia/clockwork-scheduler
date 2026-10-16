package com.clockwork.scheduler.cron;

import com.cronutils.descriptor.CronDescriptor;
import com.cronutils.model.Cron;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.scheduling.support.CronExpression;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Locale;

import static com.cronutils.model.CronType.QUARTZ;
import static org.junit.jupiter.api.Assertions.*;

class CronParsingTest {

    private final CronParser quartzParser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(QUARTZ));
    private final CronDescriptor descriptor = CronDescriptor.instance(Locale.ENGLISH);

    @ParameterizedTest
    @ValueSource(strings = {
            "0 0 2 * * ?",        // 2:00 AM daily (Nightly M-Pesa recon)
            "0 30 1 * * ?",       // 1:30 AM daily (Nightly DB snapshot)
            "0 0 6 1 * ?",        // 6:00 AM on 1st of month (Monthly statements)
            "0 */15 * * * ?",     // Every 15 minutes
            "0 0/5 * * * ?",      // Every 5 minutes
            "0 0 12 ? * MON-FRI"  // Noon on weekdays
    })
    @DisplayName("Valid Quartz-style cron expressions parse and validate successfully")
    void testValidQuartzCronExpressions(String cronString) {
        assertTrue(CronExpression.isValidExpression(cronString), "Expression should be valid: " + cronString);

        CronExpression springCron = CronExpression.parse(cronString);
        assertNotNull(springCron);

        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Africa/Nairobi"));
        ZonedDateTime nextRun = springCron.next(now);
        assertNotNull(nextRun);
        assertTrue(nextRun.isAfter(now));

        // Parse with cron-utils
        Cron quartzCron = quartzParser.parse(cronString);
        assertNotNull(quartzCron);
        String description = descriptor.describe(quartzCron);
        assertNotNull(description);
        assertFalse(description.isBlank());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "invalid cron text",
            "0 0 99 * * ?",       // Invalid hour 99
            "0 65 * * * ?",       // Invalid minute 65
            ""
    })
    @DisplayName("Invalid cron expressions are correctly rejected")
    void testInvalidCronExpressions(String invalidCron) {
        assertFalse(CronExpression.isValidExpression(invalidCron));
    }
}
