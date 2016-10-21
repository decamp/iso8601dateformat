/*
 * Copyright (c) 2016. SocialEmergence.org
 * This code is released under the MIT License
 * https://opensource.org/licenses/MIT
 */

package bits.util;

import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.*;

/**
 * Implementation of ISO-8601 date and time format. This version
 * is more complete/correct than either Java's java.text.SimpleDateFormat
 * and Jackson's ISO8601Utils ond ISO8601DateFormat. It is about 5x faster
 * than SimpleDateFormat and 2x faster than Jackson.
 * <p>
 * Full implementation of dates and times. Intervals are not implemented.
 *
 * @author Philip DeCamp
 */
public class Iso8601DateFormat extends DateFormat {

    public static final TimeZone UTC = TimeZone.getTimeZone( "UTC" );
    private static final Iso8601DateFormat INSTANCE = new Iso8601DateFormat();
    
    /**
     * Parses a ISO8601 string and converts to a
     * unix epoch millisecond using a static singleton
     * instance of this class.
     *
     * @param str ISO8601 formatted timestamp
     * @return equivalent timestamp in unix time milliseconds
     */
    public static Long stringToMillis( String str ) {
        return INSTANCE.parseToMillis( str );
    }

    /**
     * Converts a unix epoch millisecond timestamp into ISO8601
     * format using a static singleton instance of this class.
     * @see Iso8601DateFormat#format(long)
     * @param t Unix timestamp in milliseconds
     * @return equivalent timestamp in ISO8601 format
     */
    public static String millisToString( long t ) {
        return INSTANCE.format( t );
    }


    public static void format( Calendar cal, StringBuilder out ) {
        int zone = cal.get( Calendar.ZONE_OFFSET );
        out.ensureCapacity( out.length() + zone == 0 ? 24 : 29 );

        formatNum( cal.get( Calendar.YEAR ), 4, out );
        out.append( '-' );
        formatNum( cal.get( Calendar.MONTH ) + 1, 2, out );
        out.append( '-' );
        formatNum( cal.get( Calendar.DAY_OF_MONTH ), 2, out );
        out.append( 'T' );
        formatNum( cal.get( Calendar.HOUR_OF_DAY ), 2, out );
        out.append( ':' );
        formatNum( cal.get( Calendar.MINUTE ), 2, out );
        out.append( ':' );
        formatNum( cal.get( Calendar.SECOND ), 2, out );
        out.append( '.' );
        formatNum( cal.get( Calendar.MILLISECOND ), 3, out );

        if( zone == 0 ) {
            out.append( 'Z' );
            return;
        }

        boolean positive = true;
        if( zone < 0 ) {
            positive = false;
            zone = -zone;
        }

        if( zone == 0 ) {
            out.append( 'Z' );
            return;
        }
        
        int hour   = zone / ( 60 * 60 * 1000 );
        int minute = ( zone % ( 60 * 60 * 1000 ) ) / ( 60 * 1000 );
        
        if( hour == 0 && minute == 0 ) {
            out.append( 'Z' );
            return;
        }

        out.append( positive ? '+' : '-' );
        formatNum( hour, 2, out );
        out.append( ':' );
        formatNum( minute, 2, out );
    }

    /**
     * Parses string containing ISO8601 formatted value. Valid formats include: <ul>
     * <li>[date]
     * <li>[date]T[time]
     * <li>[date]T[time][zone]
     * </ul>
     * Where: <ul>
     * <li>[date] = Date component (see {@link #parseDate})
     * <li>T = Time component indicator. May be 'T', 't', or ' ', although only 'T' is 8601 compliant.
     * <li>[time] = Time component (see {@link #parseTime})
     * <li>[zone] = Zone Offset component( see {@link #parseZoneOffset}).
     * </ul>
     *
     * @param s   Input string.
     * @param off Offset into string.
     * @param out Calendar to use for parsing. WILL be cleared.
     * @return On success, position where parsing finished (always last character of string).
     *         On error, (-errPos-1), where errPos is the string position where parsing failed.
     *
     * @see #parseDate
     * @see #parseTime
     * @see #parseZoneOffset
     */
    public static int parse( CharSequence s, final int off, Calendar out ) {
        int pos = off;
        int end = s.length();

        out.clear();
        pos = parseDate( s, pos, out );
        if( pos < 0 || pos == end ) {
            return pos;
        }

        // Check for date-time separator.
        // Formally, we should only accept a 'T' char, but
        // I'm also allowing a lower-case 't' or a space.
        switch( s.charAt( pos++ ) ) {
        case ' ':
        case 'T':
        case 't':
            pos = parseTime( s, pos, out );
            // Return if error or no more data.
            if( pos < 0 || pos == end ) {
                return pos;
            }

            pos = parseZoneOffset( s, pos, out );
            // Return if error or no more data.
            if( pos < 0 || pos == end ) {
                return pos;
            }

            return -pos - 1;

        default:
            return -pos;
        }
    }

    /**
     * Parses date component. The following formats are valid: <ul>
     * <li>yyyy
     * <li>yyyy-MM
     * <li>yyyy-MM-dd
     * <li>yyyy-'W'ww
     * <li>yyyy-'W'ww-d
     * <li>yyyy-ddd
     * <li>yyyyMMdd
     * <li>yyyy'W'ww
     * <li>yyyy'W'wwd
     * <li>yyyyDDD
     * </ul>
     * Where: <ul>
     * <li>y = year digit
     * <li>M = month digit
     * <li>w = week digit ('W' means the letter W)
     * <li>D = day-of-year digit
     * <li>d = day-of-month digit
     * </ul>
     * Note that yyyyMM is not a valid format!
     *
     * @param s   Input string
     * @param pos Position into string.
     * @param out Calendar to receive [YEAR,MONTH,WEEK,DAY_OF_YEAR,DAY_OF_MONTH] values.
     *            These fields are assumed to be clear, otherwise results are undefined.
     * @return On success, position where parsing ended.
     *         On error, (-errPos - 1), where errPos is the string position where parsing failed.
     */
    public static int parseDate( CharSequence s, int pos, Calendar out ) {
        int end = s.length();
        int val = parseNum( s, pos, 4 );
        if( val < 0 ) {
            // Error. No year.
            return val;
        }

        out.set( Calendar.YEAR, val );
        pos += 4;
        if( pos == end ) {
            // Done at year.
            return pos;
        }

        // Check if dashes are being used between date components.
        boolean useDash = s.charAt( pos++ ) == '-';
        if( !useDash ) {
            pos--;
        }

        char c = s.charAt( pos );
        if( c == 'w' || c == 'W' ) {
            // Week format: yyyy-Www OR yyyyWww
            val = parseNum( s, ++pos, 2 );
            if( val < 0 ) {
                // Invalid week. Error.
                return val;
            }
            pos += 2;
            out.set( Calendar.WEEK_OF_YEAR, val );

            if( pos == end ) {
                // Done at week.
                return pos;
            }

            // Day of week format:
            // yyyy-Www-d OR yyyyWwwd
            if( useDash ) {
                if( s.charAt( pos++ ) != '-' ) {
                    // Stop at week.
                    return pos - 1;
                }
                val = parseNum( s, pos, 1 );
                if( val < 0 ) {
                    // Invalid day. Error.
                    return val;
                }
            } else {
                val = parseNum( s, pos, 1 );
                if( val < 0) {
                    // No error.
                    return pos;
                }
            }

            // Done at day.
            pos++;
            val = ( val % 7 ) + 1;
            out.set( Calendar.DAY_OF_WEEK, val );
            return pos;
        }


        // Parse month (MM) OR day-of-year (ddd).
        // Possible formats:
        // yyyy-MM, yyyyMMdd, yyyy-MM-dd
        // yyyyDDD, yyyy-DDD
        // NOT yyyyMM (NB, the earlier check of length >= 7 will catch this case)
        switch( countDigits( s, pos, 4 ) ) {
        case 0:
            // Done at year.
            return pos;
        case 1:
            // Invalid month or day-of-year value. Error.
            return -pos-2;
        case 2:
        case 4:
            // Two-digit month
            val = parseNum( s, pos, 2 );
            pos += 2;
            out.set( Calendar.MONTH, val - 1 );
            break;
        case 3:
            // Three-digit day-of-year
            val = parseNum( s, pos, 3 );
            pos += 3;
            out.set( Calendar.DAY_OF_MONTH, val );
            // Done at day-of-year.
            return pos;
        }

        if( pos == end ) {
            // Done at month.
            return pos;
        }

        // Parse day of month.
        if( useDash ) {
            if( s.charAt( pos++ ) != '-' ) {
                // Done at month.
                return pos -1;
            }
            val = parseNum( s, pos, 2 );
            if( val < 0 ) {
                // Invalid day. Error.
                return val;
            }
        } else {
            val = parseNum( s, pos, 2 );
            if( val < 0 ) {
                // Done at month.
                return pos;
            }
        }

        // Done at day-of-month.
        pos += 2;
        out.set( Calendar.DAY_OF_MONTH, val );
        return pos;
    }

    /**
     * Parses time component. The following formats are valid: <ul>
     * <li>HH
     * <li>HH:mm
     * <li>HH:mm:ss
     * <li>HH:mm:ss.SSS
     * <li>HHmm
     * <li>HHmmss
     * <li>HHmmss.SSS
     * </ul>
     * Where: <ul>
     * <li>H = hour-of-day digit
     * <li>m = minute digit
     * <li>s = second digit
     * <li>S = millisecond digit
     * </ul>
     *
     * @param s   Input string
     * @param pos Position into string.
     * @param out Calendar to receive [HOUR_OF_DAY,MINUTE,SECOND,MILLISECOND] values.
     *            These fields are assumed to be clear, otherwise results are undefined.
     * @return On success, position where parsing ended.
     *         On error, (-errPos - 1), where errPos is the string position where parsing failed.
     */
    public static int parseTime( CharSequence s, int pos, Calendar out ) {
        final int end = s.length();

        // Parse hour.
        int val = parseNum( s, pos, 2 );
        if( val < 0 ) {
            // Invalid hour. Error.
            return val;
        }
        pos += 2;
        out.set( Calendar.HOUR_OF_DAY, val );

        // Check if done.
        if( pos == end ) {
            return pos;
        }

        // Check if using colon separators.
        boolean useColons = s.charAt( pos++ ) == ':';
        if( useColons ) {
            val = parseNum( s, pos, 2 );
            if( val < 0 ) {
                // Invalid minute. Error.
                return val;
            }
        } else {
            val = parseNum( s, --pos, 2 );
            if( val < 0 ) {
                // Done at hour. No error.
                return pos;
            }
        }

        pos += 2;
        out.set( Calendar.MINUTE, val );

        if( pos == end ) {
            // Done at minute.
            return pos;
        }

        if( useColons ) {
            if( s.charAt( pos++ ) != ':' ) {
                // Done at minute.
                return pos - 1;
            }

            val = parseNum( s, pos, 2 );
            if( val < 0 ) {
                // Invalid minute. Error.
                return val;
            }
        } else {
            val = parseNum( s, pos, 2 );
            if( val < 0 ) {
                // Done at minute.
                return pos;
            }
        }

        pos += 2;
        out.set( Calendar.SECOND, val );

        if( pos == end || s.charAt( pos ) != '.' ) {
            // Done at seconds.
            return pos;
        }

        val = parseNum( s, ++pos, 3 );
        if( val < 0 ) {
            // Invalid milliseconds. Error.
            return val;
        }

        pos += 3;
        out.set( Calendar.MILLISECOND, val );

        return pos;
    }

    /**
     * Parses zone offset component. The following formats are valid: <ul>
     * <li>Z
     * <li>z (not 8601 compliant, but accepted here)
     * <li>±HH:mm
     * <li>±HHmm
     * </ul>
     * Where: <ul>
     * <li>Z = the letter 'z', indicating no offset.
     * <li>± = a plus or minus, indicating sign of offset.
     * <li>H = hour digit
     * <li>m = minute digit
     * </ul>
     *
     * @param s   Input string
     * @param pos Position into string.
     * @param out Calendar to receive [ZONE_OFFSET] value.
     * @return On success, position where parsing ended.
     *         On error, (-errPos - 1), where errPos is the string position where parsing failed.
     */
    public static int parseZoneOffset( CharSequence s, int pos, Calendar out ) {
        final int end = s.length();
        if( pos == end ) {
            // Error. No data.
            return -pos - 1;
        }

        int sign = -1;

        switch( s.charAt( pos++ ) ) {
        case 'z':
        case 'Z':
            // Zulu / UTC time.
            return pos;

        case '+':
            sign = 1;
        case '-':
            break;

        default:
            // Invalid character.
            return -pos;
        }

        int val = parseNum( s, pos, 2 );
        if( val < 0 ) {
            // No hour. Error.
            return val;
        }
        pos += 2;
        int offset = val * ( 60 * 60 * 1000 );

        if( pos == end ) {
            // Done at hour.
            out.set( Calendar.ZONE_OFFSET, sign * offset );
            return pos;
        }

        if( s.charAt( pos++ ) == ':' ) {
            val = parseNum( s, pos, 2 );
            if( val < 0 ) {
                // Invalid minute. Error.
                return -pos;
            }
        } else {
            val = parseNum( s, --pos, 2 );
            if( val < 0 ) {
                // Done at hour.
                out.set( Calendar.ZONE_OFFSET, sign * offset );
                return pos;
            }
        }

        pos += 2;
        offset += val * ( 60 * 1000 );
        out.set( Calendar.ZONE_OFFSET, sign * offset );
        // Done at minute.
        return pos;
    }


    
    private static int parseNum( CharSequence s, int off, int len ) {
        if( off + len > s.length() ) {
            return -s.length() - 1;
        }

        int ret = 0;
        for( int i = 0; i < len; i++ ) {
            int d = s.charAt( off + i ) - '0';
            if( d < 0 || 9 < d ) {
                return -(off + i + 1);
            }
            ret = 10 * ret + d;
        }
        return ret;
    }


    private static int countDigits( CharSequence s, int off, int max ) {
        final int end = Math.min( s.length(), off + max );
        for( int i = 0; i < end - off; i++ ) {
            char c = s.charAt( i + off );
            if( c < '0' || '9' < c ) {
                return i;
            }
        }
        return end - off;
    }


    private static void formatNum( int num, int len, StringBuilder out ) {
        String s;
        if( num < 0 ) {
            s = "";
        } else {
            s = String.valueOf( num );
        }

        int lead = len - s.length();
        if( lead >= 0 ) {
            while( lead-- > 0 ) {
                out.append( '0' );
            }
            out.append( s );
        } else {
            for( int i = 0; i < len; i++ ) {
                out.append( '9' );
            }
        }
    }


    private TimeZone mZone = UTC;
    private final Calendar mCalendar = new GregorianCalendar( UTC, Locale.US );
    

    public synchronized Long parseToMillis( CharSequence s ) {
        int err = parse( s, 0, mCalendar );
        return err >= 0 ? mCalendar.getTimeInMillis() : null;
    }


    public String format( long millis ) {
        StringBuilder ret = new StringBuilder( 29 );
        format( millis, ret );
        return ret.toString();
    }


    public synchronized void format( long millis, StringBuilder out ) {
        mCalendar.setTimeZone( mZone );
        mCalendar.setTimeInMillis( millis );
        format( mCalendar, out );
    }

    @Override
    public StringBuffer format( Date date, StringBuffer toAppendTo, FieldPosition field ) {
        StringBuilder s = new StringBuilder();
        format( date.getTime(), s );
        final int off = s.length();
        toAppendTo.append( s );
        
        switch( field.getField() ) {
        case YEAR_FIELD:
            field.setBeginIndex( off + 0 );
            field.setEndIndex( off + 4 );
            break;
        case MONTH_FIELD:
            field.setBeginIndex( off + 5 );
            field.setEndIndex( off + 7 );
            break;
        case DATE_FIELD:
            field.setBeginIndex( off + 8 );
            field.setEndIndex( off + 10 );
            break;
        case HOUR_OF_DAY0_FIELD:
            field.setBeginIndex( off + 11 );
            field.setEndIndex( off + 13 );
            break;
        case MINUTE_FIELD:
            field.setBeginIndex( off + 14 );
            field.setEndIndex( off + 16 );
            break;
        case SECOND_FIELD:
            field.setBeginIndex( off + 17 );
            field.setEndIndex( off + 19 );
            break;
        case MILLISECOND_FIELD:
            field.setBeginIndex( off + 20 );
            field.setEndIndex( off + 23 );
            break;
        case TIMEZONE_FIELD:
            field.setBeginIndex( off + 23 );
            field.setEndIndex( s.length() );
        default:
            field.setBeginIndex( off + 0 );
            field.setEndIndex( off + 0 );
        }

        return toAppendTo;
    }

    @Override
    public synchronized Date parse( String source, ParsePosition pos ) {
        int err = parse( source, pos.getIndex(), mCalendar );
        if( err >= 0 ) {
            pos.setIndex( err );
            return new Date( mCalendar.getTimeInMillis() );
        }

        pos.setErrorIndex( -err - 1 );
        return null;
    }

    @Override
    public synchronized Date parseObject( String source ) throws ParseException {
        int err = parse( source, 0, mCalendar );
        if( err >= 0 ) {
            return new Date( mCalendar.getTimeInMillis() );
        }
        throw new ParseException( "Iso8601DateFormat.parseObject(String) failed", -err - 1 );
    }

    @Override
    public synchronized void setTimeZone( TimeZone zone ) {
        mZone = zone;
    }

    @Override
    public TimeZone getTimeZone() {
        return mZone;
    }

}
