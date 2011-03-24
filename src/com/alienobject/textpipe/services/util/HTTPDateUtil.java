package com.alienobject.textpipe.services.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

class HTTPDateUtil {

    /**
     * Date format pattern used to generate the header in RFC 1123 format.
     */
    private static final String PATTERN_RFC1123 = "EEE, dd MMM yyyy HH:mm:ss zzz";

    /**
     * The time zone to use in the date header.
     */
    private static final TimeZone GMT = TimeZone.getTimeZone("GMT");

    private static final DateFormat dateformat = new SimpleDateFormat(PATTERN_RFC1123, Locale.US);

    static {
        dateformat.setTimeZone(GMT);
    }

    public static String formatDate(Date date) {
        return dateformat.format(date);
    }

    public static Date parseDate(String dateString) throws ParseException {
        return dateformat.parse(dateString);
    }
}

