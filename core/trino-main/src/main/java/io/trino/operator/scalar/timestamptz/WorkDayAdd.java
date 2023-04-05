package io.trino.operator.scalar.timestamptz;
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
import io.trino.spi.block.Block;
import io.trino.spi.function.Description;
import io.trino.spi.function.LiteralParameter;
import io.trino.spi.function.LiteralParameters;
import io.trino.spi.function.ScalarFunction;
import io.trino.spi.function.SqlType;
import io.trino.spi.type.StandardTypes;
import org.joda.time.chrono.ISOChronology;

import static io.trino.spi.type.DateTimeEncoding.unpackMillisUtc;
import static io.trino.spi.type.DateTimeEncoding.updateMillisUtc;
import static io.trino.spi.type.IntegerType.INTEGER;
import static io.trino.spi.type.Timestamps.round;
import static io.trino.util.DateTimeZoneIndex.unpackChronology;
import static java.lang.Math.toIntExact;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class WorkDayAdd
{
    private WorkDayAdd() {}

    @ScalarFunction("workday_add")
    @LiteralParameters("p")
    @SqlType("timestamp(p) with time zone")
    @Description("Add the specified amount of time to the given timestamp excluding non-workdays")
    public static long add(
            @LiteralParameter("p") long precision,
            @SqlType("timestamp(p) with time zone") long packedEpochMillis,
            @SqlType(StandardTypes.BIGINT) long value,
            @SqlType("array(date)") Block nonWorkdays)
    {
        if (value == 0) {
            return packedEpochMillis;
        }

        long epochMillis = unpackMillisUtc(packedEpochMillis);

        ISOChronology chronology = unpackChronology(packedEpochMillis);
        epochMillis = addWithWorkdays(chronology, value, epochMillis, nonWorkdays);
        epochMillis = round(epochMillis, (int) (3 - precision));

        return updateMillisUtc(epochMillis, packedEpochMillis);
    }

    private static long addWithWorkdays(ISOChronology chronology, long value, long date, Block nonWorkdays)
    {
        long countdown = Math.abs(value);
        long signum = Long.signum(value);

        long duration = signum;
        long current = addUnderChronology(chronology, date, duration);

        while (countdown != 0) {
            if (isWorkday(current, nonWorkdays)) {
                countdown--;
            }

            if (countdown == 0) {
                break;
            }

            // Increment the direction the current date is updated toward.
            duration += signum;
            current = addUnderChronology(chronology, date, duration);
        }
        return current;
    }

    private static long addUnderChronology(ISOChronology chronology, long date, long duration)
    {
        return chronology.dayOfMonth().add(date, toIntExact(duration));
    }

    private static boolean isWorkday(long day, Block nonWorkdays)
    {
        for (int i = 0; i < nonWorkdays.getPositionCount(); i++) {
            if (nonWorkdays.isNull(i)) {
                continue;
            }

            long nonWorkday = INTEGER.getLong(nonWorkdays, i);
            if (MILLISECONDS.toDays(day) == nonWorkday) {
                return false;
            }
        }

        return true;
    }
}
