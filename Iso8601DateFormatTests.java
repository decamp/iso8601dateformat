/*
 * Copyright (c) 2016. SocialEmergence.org
 * This code is released under the MIT License
 * https://opensource.org/licenses/MIT
 */
import org.junit.Ignore;
import org.junit.Test;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.junit.Assert.*;


//import com.fasterxml.jackson.databind.util.ISO8601DateFormat;
//import com.fasterxml.jackson.databind.util.ISO8601Utils;


/**
 * @author Philip DeCamp
 */
@SuppressWarnings( "deprecation" )
public class Iso8601DateFormatTests {

    @Test
    public void testParse() throws ParseException {
        final Random rand = new Random( 2 );

        List<DateFormat> formatsWithZones = new ArrayList<>();
        List<DateFormat> formats = new ArrayList<>();
        genFormats( formats, formatsWithZones );

        Calendar cal = new GregorianCalendar( UTC, Locale.US );
        long millis = System.currentTimeMillis();

        for( DateFormat format: formats ) {
            format.setTimeZone( UTC );
            String str = format.format( new Date( millis ) );

            long refAnswer = format.parse( str ).getTime();
            assertTrue( Iso8601DateFormat.parse( str, 0, cal ) >= 0 );
            long isoAnswer = cal.getTimeInMillis();
            assertEquals( isoAnswer, refAnswer );
        }

        Iso8601DateFormat mine = new Iso8601DateFormat();

        long minTime  = 0;
        long maxTime  = mine.parseToMillis( "9998-12-31T23:59:59.999" );
        int minOffset = -24 * 60 * 60 * 1000;
        int maxOffset = 24 * 60 * 60 * 1000;

        for( int trial = 0; trial < 1000; trial++ ) {
            int offset = minOffset + rand.nextInt( maxOffset - minOffset );
            TimeZone tz = new SimpleTimeZone( offset, "__test__" );
            millis = rand.nextLong() % ( maxTime - minTime ) + minTime;

            for( DateFormat format: formatsWithZones ) {
                format.setTimeZone( tz );
                String str = format.format( new Date( millis ) );

                long refAnswer = format.parse( str ).getTime();
                assertTrue( Iso8601DateFormat.parse( str, 0, cal ) >= 0 );
                long isoAnswer = cal.getTimeInMillis();
                assertEquals( isoAnswer, refAnswer );
            }
        }

    }

    @Test
    public void testParseCanonical() throws ParseException {
        Iso8601DateFormat mine = new Iso8601DateFormat();
        Random            rand = new Random( 8 );

        long minTime  = 0;
        long maxTime  = mine.parseToMillis( "9998-12-31T23:59:59.999" );

        for( int trial = 0; trial < 1000; trial++ ) {
            long millis = rand.nextLong() % ( maxTime - minTime ) + minTime;

            String str = CANONICAL.format( new Date( millis ) );
            assertEquals( str, mine.format( millis ) );

            long refAnswer = CANONICAL.parse( str ).getTime();
            Long isoAnswer = mine.parseToMillis( str );
            assertNotNull( isoAnswer );

            if( isoAnswer != refAnswer ) {
                System.out.println( str + " -> " + ( isoAnswer - refAnswer ) + "    " + refAnswer );
                //IsoDateFormat.parse( str, 0, cal );
            }
            assertEquals( isoAnswer.longValue(), refAnswer );
        }
    }

    @Ignore
    @Test
    public void testParseSpeed() throws ParseException {
        Iso8601DateFormat mine = new Iso8601DateFormat();
        Random            rand = new Random( 8 );

        long t0 = 0;
        long t1 = 0;
        long scrap = 0;

        long minTime  = 0;
        long maxTime  = mine.parseToMillis( "9998-12-31T23:59:59.999" );

        for( int trial = 0; trial < 100000; trial++ ) {
            long millis = rand.nextLong() % ( maxTime - minTime ) + minTime;
            String str = CANONICAL.format( new Date( millis ) );

            long startNanos = System.nanoTime();
            for( int i = 0; i < 10; i++ ) {
                scrap += CANONICAL.parse( str ).getTime();
            }
            t0 += System.nanoTime() - startNanos;

            startNanos = System.nanoTime();
            for( int i = 0; i < 10; i++ ) {
                scrap += mine.parseToMillis( str );
            }
            t1 += System.nanoTime() - startNanos;

            millis = rand.nextLong() % ( maxTime - minTime ) + minTime;
            str = mine.format( millis );

            startNanos = System.nanoTime();
            for( int i = 0; i < 10; i++ ) {
                scrap += mine.parseToMillis( str );
            }
            t1 += System.nanoTime() - startNanos;

            startNanos = System.nanoTime();
            for( int i = 0; i < 10; i++ ) {
                scrap += CANONICAL.parse( str ).getTime();
            }
            t0 += System.nanoTime() - startNanos;
        }

        System.out.println( "SimpleDateFormat: " + ( t0 / 1000000000.0 ) );
        System.out.println( "IsoDateFormat: " + ( t1 / 1000000000.0 ) );
    }

//    @Ignore @Test
//    public void testJacksonSpeed() throws ParseException {
//        IsoDateFormat mine = new IsoDateFormat();
//        Random rand = new Random( 10 );
//
//        long t0 = 0;
//        long t1 = 0;
//        long scrap = 0;
//
//        long minTime  = mine.parseToMillis( "1900-12-31T23:59:59.999" );
//        long maxTime  = mine.parseToMillis( "2900-12-31T23:59:59.999" );
//
//        ISO8601DateFormat ref = new ISO8601DateFormat();
//
//        for( int trial = 0; trial < 100000; trial++ ) {
//            long millis = rand.nextLong() % ( maxTime - minTime ) + minTime;
//            String str = ISO8601Utils.format( new Date( millis ), true );
//
//            long startNanos = System.nanoTime();
//            for( int i = 0; i < 10; i++ ) {
//                scrap += ref.parse( str ).getTime();
//            }
//            t0 += System.nanoTime() - startNanos;
//
//            startNanos = System.nanoTime();
//            for( int i = 0; i < 10; i++ ) {
//                scrap += mine.parseToMillis( str );
//            }
//            t1 += System.nanoTime() - startNanos;
//
//            millis = rand.nextLong() % ( maxTime - minTime ) + minTime;
//            str = mine.format( millis );
//
//            startNanos = System.nanoTime();
//            for( int i = 0; i < 10; i++ ) {
//                scrap += mine.parseToMillis( str );
//            }
//            t1 += System.nanoTime() - startNanos;
//
//            startNanos = System.nanoTime();
//            for( int i = 0; i < 10; i++ ) {
//                scrap += ref.parse( str ).getTime();
//            }
//            t0 += System.nanoTime() - startNanos;
//        }
//
//        System.out.println( "ISO8601DateFormat: " + ( t0 / 1000000000.0 ) );
//        System.out.println( "IsoDateFormat: " + ( t1 / 1000000000.0 ) );
//    }


    private static final TimeZone UTC = TimeZone.getTimeZone( "UTC" );
    private static final DateFormat CANONICAL = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US );

    static {
        CANONICAL.setTimeZone( UTC );
    }


    private static final String[] DATE_FORMATS = {
            "yyyy" ,
            "yyyy-MM" ,
            "yyyy-MM-dd" ,
            "yyyy-'W'ww" ,
            "yyyy-'W'ww-u" ,
            "yyyy-DDD" ,
            "yyyyMMdd" ,
            "yyyy'W'ww" ,
            "yyyy'W'wwu" ,
            "yyyyDDD"
    };


    private static final String[] TIME_PREFIXES = {
            " ",
            "'T'",
            "'t'"
    };

    private static final String[] TIME_FORMATS = {
            "HH",
            "HH:mm",
            "HH:mm:ss",
            "HH:mm:ss.SSS",
            "HHmm",
            "HHmmss",
            "HHmmss.SSS"
    };

    private static final String[] ZONE_FORMATS = {
            "X",
            "XX",
            "XXX"
    };


    /**
     * Generates all valid permutations of valid dates.
     */
    static void genFormats( List<DateFormat> outAll, List<DateFormat> outWithZones ) {
        for( int date = 0; date < DATE_FORMATS.length; date++ ) {
            for( int pref = -1; pref < TIME_PREFIXES.length; pref++ ) {
                if( pref == -1 ) {
                    StringBuilder s = new StringBuilder();
                    s.append( DATE_FORMATS[date] );
                    outAll.add( new SimpleDateFormat( s.toString(), Locale.US ) );
                    continue;
                }

                for( int time = 0; time < TIME_FORMATS.length; time++ ) {
                    for( int zone = -1; zone < ZONE_FORMATS.length; zone++ ) {
                        StringBuilder s = new StringBuilder();
                        s.append( DATE_FORMATS[date] );
                        s.append( TIME_PREFIXES[pref] );
                        s.append( TIME_FORMATS[time] );

                        if( zone < 0 ) {
                            outAll.add( new SimpleDateFormat( s.toString(), Locale.US )  );
                        } else {
                            s.append( ZONE_FORMATS[zone] );
                            DateFormat format = new SimpleDateFormat( s.toString(), Locale.US );
                            outAll.add( format );
                            outWithZones.add( format );
                        }
                    }
                }
            }
        }
    }

}
