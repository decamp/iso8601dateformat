# ISO-8601 Date Format

Implementation of ISO-8601 date and time format. This version
is more complete and correct than either Java's java.text.SimpleDateFormat
and Jackson's ISO8601DateFormat. It is about 5x faster
than SimpleDateFormat and 2x faster than Jackson.

All the static methods should be solid. 
However, override java.text.DateFormat is a pain
in the ass and there might be compliance issues when accessing the more archaic DateFormat
features.

This library should parse any valid ISO-8601 date or time. 

Formatting of dates are limited to one of two formats. 
1. If there is no time offset:   
**2016-01-01T04:22:30.123Z**   
2. If there any time offset (even if less than a minute!):    
**2016-01-01T04:22:30.123Z-03:00**    

This class does not implement ISO-8601 intervals.

### License  
Copyright (c) 2016. SocialEmergence.org
This code is released under the MIT License
https://opensource.org/licenses/MIT


### Author  
Philip DeCamp



