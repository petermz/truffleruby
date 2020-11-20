/*
 * Copyright (c) 2013, 2019 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 2.0, or
 * GNU General Public License version 2, or
 * GNU Lesser General Public License version 2.1.
 */
package org.truffleruby.core.time;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.CachedLanguage;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.Shape;
import org.jcodings.specific.UTF8Encoding;
import org.truffleruby.RubyLanguage;
import org.truffleruby.builtins.CoreMethod;
import org.truffleruby.builtins.CoreMethodArrayArgumentsNode;
import org.truffleruby.builtins.CoreModule;
import org.truffleruby.builtins.Primitive;
import org.truffleruby.builtins.PrimitiveArrayArgumentsNode;
import org.truffleruby.core.exception.ErrnoErrorNode;
import org.truffleruby.core.klass.RubyClass;
import org.truffleruby.core.rope.CodeRange;
import org.truffleruby.core.rope.Rope;
import org.truffleruby.core.rope.RopeBuilder;
import org.truffleruby.core.rope.RopeNodes;
import org.truffleruby.core.string.RubyString;
import org.truffleruby.core.string.StringCachingGuards;
import org.truffleruby.core.string.StringNodes;
import org.truffleruby.core.string.StringOperations;
import org.truffleruby.core.string.StringUtils;
import org.truffleruby.core.time.RubyDateFormatter.Token;
import org.truffleruby.language.Nil;
import org.truffleruby.language.Visibility;
import org.truffleruby.language.control.RaiseException;
import org.truffleruby.language.objects.AllocateHelperNode;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

@CoreModule(value = "Time", isClass = true)
public abstract class TimeNodes {

    public static RubyString getShortZoneName(StringNodes.MakeStringNode makeStringNode, ZonedDateTime dt,
            TimeZoneAndName zoneAndName) {
        final String shortZoneName = zoneAndName.getName(dt);
        return makeStringNode.executeMake(shortZoneName, UTF8Encoding.INSTANCE, CodeRange.CR_UNKNOWN);
    }

    @CoreMethod(names = { "__allocate__", "__layout_allocate__" }, constructor = true, visibility = Visibility.PRIVATE)
    public abstract static class AllocateNode extends CoreMethodArrayArgumentsNode {

        private static final ZonedDateTime ZERO = ZonedDateTime.ofInstant(Instant.EPOCH, GetTimeZoneNode.UTC);

        @Child private AllocateHelperNode allocateNode = AllocateHelperNode.create();

        @Specialization
        protected RubyTime allocate(RubyClass rubyClass,
                @CachedLanguage RubyLanguage language) {
            final Shape shape = allocateNode.getCachedShape(rubyClass);
            final RubyTime instance = new RubyTime(rubyClass, shape, ZERO, nil, 0, false, false);
            allocateNode.trace(instance, this, language);
            return instance;
        }

    }

    @CoreMethod(names = "initialize_copy", required = 1)
    public abstract static class InitializeCopyNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        protected RubyTime initializeCopy(RubyTime self, RubyTime from) {
            self.dateTime = from.dateTime;
            self.offset = from.offset;
            self.zone = from.zone;
            self.relativeOffset = from.relativeOffset;
            self.isUtc = from.isUtc;
            return self;
        }

    }

    @Primitive(name = "time_localtime")
    public abstract static class LocalTimeNode extends PrimitiveArrayArgumentsNode {

        @Child private GetTimeZoneNode getTimeZoneNode = GetTimeZoneNodeGen.create();

        @Specialization
        protected RubyTime localtime(RubyTime time, Nil offset,
                @Cached StringNodes.MakeStringNode makeStringNode) {
            final TimeZoneAndName timeZoneAndName = getTimeZoneNode.executeGetTimeZone();
            final ZonedDateTime newDateTime = withZone(time.dateTime, timeZoneAndName.getZone());
            final RubyString zone = getShortZoneName(makeStringNode, newDateTime, timeZoneAndName);

            time.isUtc = false;
            time.relativeOffset = false;
            time.zone = zone;
            time.dateTime = newDateTime;

            return time;
        }

        @Specialization
        protected RubyTime localtime(RubyTime time, long offset) {
            final ZoneId zone = getDateTimeZone((int) offset);
            final ZonedDateTime dateTime = withZone(time.dateTime, zone);

            time.isUtc = false;
            time.relativeOffset = true;
            time.zone = nil;
            time.dateTime = dateTime;

            return time;
        }

        @TruffleBoundary
        public ZoneId getDateTimeZone(int offset) {
            try {
                return ZoneId.ofOffset("", ZoneOffset.ofTotalSeconds(offset));
            } catch (DateTimeException e) {
                throw new RaiseException(
                        getContext(),
                        getContext().getCoreExceptions().argumentError(e.getMessage(), this));
            }
        }

        @TruffleBoundary
        private ZonedDateTime withZone(ZonedDateTime dateTime, ZoneId zone) {
            return dateTime.withZoneSameInstant(zone);
        }

    }

    @Primitive(name = "time_add")
    public abstract static class TimeAddNode extends PrimitiveArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        protected RubyTime add(RubyTime time, long seconds, long nanoSeconds) {
            final ZonedDateTime dateTime = time.dateTime;
            time.dateTime = dateTime.plusSeconds(seconds).plusNanos(nanoSeconds);
            return time;
        }
    }

    @CoreMethod(names = { "gmtime", "utc" })
    public abstract static class GmTimeNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        protected RubyTime gmtime(RubyTime time) {
            final ZonedDateTime dateTime = time.dateTime;

            time.isUtc = true;
            time.relativeOffset = false;
            time.zone = coreStrings().UTC.createInstance(getContext());
            time.dateTime = inUTC(dateTime);

            return time;
        }

        @TruffleBoundary
        private ZonedDateTime inUTC(ZonedDateTime dateTime) {
            return dateTime.withZoneSameInstant(GetTimeZoneNode.UTC);
        }

    }

    @CoreMethod(names = "now", constructor = true)
    public static abstract class TimeNowNode extends CoreMethodArrayArgumentsNode {

        @Child private AllocateHelperNode allocateNode = AllocateHelperNode.create();
        @Child private GetTimeZoneNode getTimeZoneNode = GetTimeZoneNodeGen.create();
        @Child private StringNodes.MakeStringNode makeStringNode = StringNodes.MakeStringNode.create();

        @Specialization
        protected RubyTime timeNow(RubyClass timeClass,
                @CachedLanguage RubyLanguage language) {
            final TimeZoneAndName zoneAndName = getTimeZoneNode.executeGetTimeZone();
            final ZonedDateTime dt = now(zoneAndName.getZone());
            final RubyString zone = getShortZoneName(makeStringNode, dt, zoneAndName);
            final Shape shape = allocateNode.getCachedShape(timeClass);
            final RubyTime instance = new RubyTime(timeClass, shape, dt, zone, nil, false, false);
            allocateNode.trace(instance, this, language);
            return instance;

        }

        @TruffleBoundary
        private ZonedDateTime now(ZoneId timeZone) {
            return ZonedDateTime.now(timeZone);
        }

    }

    @Primitive(name = "time_at", lowerFixnum = 2)
    public static abstract class TimeAtPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @Child private GetTimeZoneNode getTimeZoneNode = GetTimeZoneNodeGen.create();
        @Child private AllocateHelperNode allocateNode = AllocateHelperNode.create();
        @Child private StringNodes.MakeStringNode makeStringNode = StringNodes.MakeStringNode.create();

        @Specialization
        protected RubyTime timeAt(RubyClass timeClass, long seconds, int nanoseconds,
                @CachedLanguage RubyLanguage language) {
            final TimeZoneAndName zoneAndName = getTimeZoneNode.executeGetTimeZone();
            final ZonedDateTime dateTime = getDateTime(seconds, nanoseconds, zoneAndName.getZone());
            final RubyString zone = getShortZoneName(makeStringNode, dateTime, zoneAndName);

            final Shape shape = allocateNode.getCachedShape(timeClass);
            final RubyTime instance = new RubyTime(
                    timeClass,
                    shape,
                    dateTime,
                    zone,
                    nil,
                    false,
                    false);
            allocateNode.trace(instance, this, language);
            return instance;
        }

        @TruffleBoundary
        private ZonedDateTime getDateTime(long seconds, int nanoseconds, ZoneId timeZone) {
            try {
                return ZonedDateTime.ofInstant(Instant.ofEpochSecond(seconds, nanoseconds), timeZone);
            } catch (DateTimeException e) {
                String message = StringUtils
                        .format("UNIX epoch + %d seconds out of range for Time (java.time limitation)", seconds);
                throw new RaiseException(getContext(), coreExceptions().rangeError(message, this));
            }
        }

    }

    @CoreMethod(names = { "to_i", "tv_sec" })
    public static abstract class TimeSecondsSinceEpochNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        protected long timeSeconds(RubyTime time) {
            return time.dateTime.toInstant().getEpochSecond();
        }

    }

    @CoreMethod(names = { "usec", "tv_usec" })
    public static abstract class TimeMicroSecondsNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        protected int timeUSec(RubyTime time) {
            return time.dateTime.getNano() / 1000;
        }

    }

    @CoreMethod(names = { "nsec", "tv_nsec" })
    public static abstract class TimeNanoSecondsNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        protected int timeNSec(RubyTime time) {
            return time.dateTime.getNano();
        }

    }

    @Primitive(name = "time_set_nseconds", lowerFixnum = 1)
    public static abstract class TimeSetNSecondsPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        protected long timeSetNSeconds(RubyTime time, int nanoseconds) {
            final ZonedDateTime dateTime = time.dateTime;
            time.dateTime = dateTime.plusNanos(nanoseconds - dateTime.getNano());
            return nanoseconds;
        }

    }

    @CoreMethod(names = { "utc_offset", "gmt_offset", "gmtoff" })
    public static abstract class TimeUTCOffsetNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        protected int timeUTCOffset(RubyTime time) {
            return time.dateTime.getOffset().getTotalSeconds();
        }
    }

    @CoreMethod(names = "sec")
    public static abstract class TimeSecNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        protected int timeSec(RubyTime time) {
            return time.dateTime.getSecond();
        }

    }

    @CoreMethod(names = "min")
    public static abstract class TimeMinNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        protected int timeMin(RubyTime time) {
            return time.dateTime.getMinute();
        }

    }

    @CoreMethod(names = "hour")
    public static abstract class TimeHourNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        protected int timeHour(RubyTime time) {
            return time.dateTime.getHour();
        }

    }

    @CoreMethod(names = { "day", "mday" })
    public static abstract class TimeDayNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        protected int timeDay(RubyTime time) {
            return time.dateTime.getDayOfMonth();
        }

    }

    @CoreMethod(names = { "mon", "month" })
    public static abstract class TimeMonthNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        protected int timeMonth(RubyTime time) {
            return time.dateTime.getMonthValue();
        }

    }

    @CoreMethod(names = "year")
    public static abstract class TimeYearNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        protected int timeYear(RubyTime time) {
            return time.dateTime.getYear();
        }

    }

    @CoreMethod(names = "wday")
    public static abstract class TimeWeekDayNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        protected int timeWeekDay(RubyTime time) {
            int wday = time.dateTime.getDayOfWeek().getValue();
            if (wday == 7) {
                wday = 0;
            }
            return wday;
        }

    }

    @CoreMethod(names = "yday")
    public static abstract class TimeYearDayNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        protected int timeYeayDay(RubyTime time) {
            return time.dateTime.getDayOfYear();
        }

    }

    @CoreMethod(names = { "dst?", "isdst" })
    public static abstract class TimeIsDSTNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        protected boolean timeIsDST(RubyTime time) {
            final ZonedDateTime dateTime = time.dateTime;
            return dateTime.getZone().getRules().isDaylightSavings(dateTime.toInstant());
        }

    }

    @CoreMethod(names = { "utc?", "gmt?" })
    public abstract static class IsUTCNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        protected boolean isUTC(RubyTime time) {
            return time.isUtc;
        }

    }

    @Primitive(name = "time_zone")
    public static abstract class TimeZoneNode extends PrimitiveArrayArgumentsNode {

        @Specialization
        protected Object timeZone(RubyTime time) {
            return time.zone;
        }

    }

    @Primitive(name = "time_strftime")
    @ImportStatic({ StringCachingGuards.class, StringOperations.class })
    public static abstract class TimeStrftimePrimitiveNode extends PrimitiveArrayArgumentsNode {

        @Child private StringNodes.MakeStringNode makeStringNode = StringNodes.MakeStringNode.create();
        @Child private ErrnoErrorNode errnoErrorNode = ErrnoErrorNode.create();

        @Specialization(
                guards = { "equalNode.execute(format.rope, cachedFormat)" },
                limit = "getContext().getOptions().TIME_FORMAT_CACHE")
        protected RubyString timeStrftime(VirtualFrame frame, RubyTime time, RubyString format,
                @Cached("privatizeRope(format)") Rope cachedFormat,
                @Cached("compilePattern(cachedFormat)") List<Token> pattern,
                @Cached RopeNodes.EqualNode equalNode) {
            return makeStringNode.fromBuilderUnsafe(formatTime(time, pattern), CodeRange.CR_UNKNOWN);
        }

        @Specialization
        protected RubyString timeStrftime(VirtualFrame frame, RubyTime time, RubyString format) {
            final List<Token> pattern = compilePattern(format.rope);
            return makeStringNode.fromBuilderUnsafe(formatTime(time, pattern), CodeRange.CR_UNKNOWN);
        }

        @TruffleBoundary
        protected List<Token> compilePattern(Rope format) {
            return RubyDateFormatter.compilePattern(format, false, getContext(), this);
        }

        private RopeBuilder formatTime(RubyTime time, List<Token> pattern) {
            return RubyDateFormatter.formatToRopeBuilder(
                    pattern,
                    time.dateTime,
                    time.zone,
                    getContext(),
                    this,
                    errnoErrorNode);
        }

    }

    @Primitive(
            name = "time_s_from_array",
            lowerFixnum = {
                    1 /* sec */,
                    2 /* min */,
                    3 /* hour */,
                    4 /* mday */,
                    5 /* month */,
                    6 /* year */,
                    7 /* nsec */,
                    8 /* isdst */ })
    public static abstract class TimeSFromArrayPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @Child private GetTimeZoneNode getTimeZoneNode = GetTimeZoneNodeGen.create();
        @Child private AllocateHelperNode allocateNode = AllocateHelperNode.create();
        @Child private StringNodes.MakeStringNode makeStringNode;

        @Specialization(guards = "(isutc || !isRubyDynamicObject(utcoffset)) || isNil(utcoffset)")
        protected RubyTime timeSFromArray(
                RubyClass timeClass,
                int sec,
                int min,
                int hour,
                int mday,
                int month,
                int year,
                int nsec,
                int isdst,
                boolean isutc,
                Object utcoffset,
                @CachedLanguage RubyLanguage language) {
            return buildTime(language, timeClass, sec, min, hour, mday, month, year, nsec, isdst, isutc, utcoffset);
        }

        @Specialization(guards = "!isInteger(sec) || !isInteger(nsec)")
        protected Object timeSFromArrayFallback(
                RubyClass timeClass,
                Object sec,
                int min,
                int hour,
                int mday,
                int month,
                int year,
                Object nsec,
                int isdst,
                boolean isutc,
                Object utcoffset) {
            return FAILURE;
        }

        @TruffleBoundary
        private RubyTime buildTime(RubyLanguage language, RubyClass timeClass, int sec, int min, int hour, int mday,
                int month,
                int year, int nsec, int isdst, boolean isutc, Object utcoffset) {
            if (sec < 0 || sec > 60 || // MRI accepts sec=60, whether it is a leap second or not
                    min < 0 || min > 59 ||
                    hour < 0 || hour > 23 ||
                    mday < 1 || mday > 31 ||
                    month < 1 || month > 12) {
                throw new RaiseException(getContext(), coreExceptions().argumentErrorOutOfRange(this));
            }

            final ZoneId zone;
            final boolean relativeOffset;
            Object zoneToStore = nil;
            TimeZoneAndName envZone = null;

            if (isutc) {
                zone = GetTimeZoneNode.UTC;
                relativeOffset = false;
                zoneToStore = coreStrings().UTC.createInstance(getContext());
            } else if (utcoffset == nil) {
                if (makeStringNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    makeStringNode = insert(StringNodes.MakeStringNode.create());
                }

                envZone = getTimeZoneNode.executeGetTimeZone();
                zone = envZone.getZone();
                relativeOffset = false;
            } else if (utcoffset instanceof Integer || utcoffset instanceof Long) {
                final int offset = ((Number) utcoffset).intValue();
                zone = getZoneOffset(offset);
                relativeOffset = true;
                zoneToStore = nil;
            } else {
                throw new UnsupportedOperationException(
                        StringUtils.format("%s %s %s %s", isdst, isutc, utcoffset, utcoffset.getClass()));
            }

            // java.time does not allow a sec value > 59. However, MRI allows a sec value of 60
            // to support leap seconds. In the case that leap seconds either aren't supported by
            // the underlying timezone or system (they seem to not be POSIX-compliant), MRI still
            // admits a sec value of 60 and just carries values forward to minutes, hours, day, etc.
            // To match MRI's behavior in that case, we build the java.time object with a sec value
            // of 0 and after it's constructed add 60 seconds to the time object, allowing the object
            // to carry values forward as necessary. This approach does not work in the cases where
            // MRI would properly handle a leap second, as java.time does not honor leap seconds.
            // Notably, this occurs when using non-standard timezone like TZ=right/UTC and specifying
            // a real leap second instant (a sec value of 60 is only valid on certain dates in certain
            // years at certain times). In these cases, MRI returns a time with HH:MM:60.
            final boolean sixtySeconds = sec == 60;
            if (sixtySeconds) {
                sec = 0;
            }

            ZonedDateTime dt;
            try {
                dt = ZonedDateTime.of(year, month, mday, hour, min, sec, nsec, zone);
            } catch (DateTimeException e) {
                // Time.new(1999, 2, 31) is legal and should return 1999-03-03
                dt = ZonedDateTime.of(year, 1, 1, hour, min, sec, nsec, zone).plusMonths(month - 1).plusDays(mday - 1);
            }

            if (sixtySeconds) {
                dt = dt.plusSeconds(60);
            }

            if (isdst == 0) {
                dt = dt.withLaterOffsetAtOverlap();
            } else if (isdst == 1) {
                dt = dt.withEarlierOffsetAtOverlap();
            }

            if (envZone != null) {
                zoneToStore = getShortZoneName(makeStringNode, dt, envZone);
            }
            final Shape shape = allocateNode.getCachedShape(timeClass);
            final RubyTime instance = new RubyTime(
                    timeClass,
                    shape,
                    dt,
                    zoneToStore,
                    utcoffset,
                    relativeOffset,
                    isutc);
            allocateNode.trace(instance, this, language);
            return instance;
        }

        private ZoneOffset getZoneOffset(int offset) {
            try {
                return ZoneOffset.ofTotalSeconds(offset);
            } catch (DateTimeException e) {
                throw new RaiseException(getContext(), coreExceptions().argumentError(e.getMessage(), this));
            }
        }

    }

}
