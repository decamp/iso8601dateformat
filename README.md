# ISO-8601 Date Format

- **Parses any valid ISO-8601 date/time string.**  
- **Formats into valid ISO-8601 strings.**  
- **5x faster parsing than java.text.SimpleDateFormat**  
- **2x faster parsing than Jackson's ISO8601DateFormat**    
- **Unit tests cover parsing of all possible permutations of ISO-8601 formats.**  
- **Does not implement ISO-8601 Intervals**  

Formatting of dates are limited to one of two formats.    
1. If there is no time offset:   
**2016-01-01T04:22:30.123Z**  
2. If there any time offset (even if less than a minute!):      
**2016-01-01T04:22:30.123Z-03:00**   

### Caveats  
All the static methods should be solid. 
However, subclassing java.text.DateFormat is a pain
and there might be potential issues with 
some of the DateFormat methods. Like there hasn't been
any testing to make sure that any ParseExceptions report
the correct string locations, etc. 

### License   
Copyright (c) 2016. SocialEmergence.org
This code is released under the MIT License
https://opensource.org/licenses/MIT

### Author  
Philip DeCamp



