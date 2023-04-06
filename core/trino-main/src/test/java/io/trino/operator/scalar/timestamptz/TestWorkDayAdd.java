/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.trino.operator.scalar.timestamptz;

import io.trino.Session;
import io.trino.spi.type.SqlTimestampWithTimeZone;
import io.trino.spi.type.TimeZoneKey;
import io.trino.sql.query.QueryAssertions;
import io.trino.testing.QueryRunner;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.time.ZonedDateTime;
import java.util.function.BiFunction;

import static io.trino.spi.type.TimeZoneKey.getTimeZoneKey;
import static io.trino.type.DateTimes.MILLISECONDS_PER_SECOND;
import static io.trino.type.DateTimes.PICOSECONDS_PER_MILLISECOND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

@TestInstance(PER_CLASS)
public class TestWorkDayAdd
{
    private QueryAssertions assertions;

    @BeforeAll
    public void init()
    {
        System.out.println("111111");
        assertions = new QueryAssertions();
        System.out.println("222222");
    }

    @AfterAll
    public void teardown()
    {
        assertions.close();
        assertions = null;
    }

    @Test
    public void testWorkdayAdd()
    {
        System.out.println("AAAAAAAAA");
        assertions = new QueryAssertions();
        assertThat(assertions.function("workday_add", "TIMESTAMP '2020-05-01 12:34:56 Asia/Tokyo'", "3", "ARRAY[DATE '2020-5-02', DATE '2020-05-03']"))
                .isEqualTo(timestampWithTimeZone(0, 2020, 6, 1, 12, 34, 56, 0, getTimeZoneKey("Asia/Tokyo")));
    }

    private BiFunction<Session, QueryRunner, Object> timestampWithTimeZone(int precision, int year, int month, int day, int hour, int minute, int second, long picoOfSecond, TimeZoneKey timeZoneKey)
    {
        return (session, queryRunner) -> {
            ZonedDateTime base = ZonedDateTime.of(year, month, day, hour, minute, second, 0, timeZoneKey.getZoneId());

            long epochMillis = base.toEpochSecond() * MILLISECONDS_PER_SECOND + picoOfSecond / PICOSECONDS_PER_MILLISECOND;
            int picosOfMilli = (int) (picoOfSecond % PICOSECONDS_PER_MILLISECOND);

            return SqlTimestampWithTimeZone.newInstance(precision, epochMillis, picosOfMilli, timeZoneKey);
        };
    }
}
