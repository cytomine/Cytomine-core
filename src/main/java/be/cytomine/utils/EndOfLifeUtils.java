package be.cytomine.utils;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.Month;

@Slf4j
public class EndOfLifeUtils {

    public static LocalDateTime END_OF_LIFE = LocalDateTime.of(2024, Month.JANUARY, 15, 0 , 0, 0);

    public static void blockApplicationIfEndOfLifeHasBeenReached() {
        if (LocalDateTime.now().isAfter(END_OF_LIFE)) {
            String message = "CYTOMINE End of life has been reached (" + END_OF_LIFE + "), please contact Cytomine company";
            log.error(message);
            throw new RuntimeException(message);
        }
    }

}
