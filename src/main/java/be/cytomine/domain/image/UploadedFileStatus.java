package be.cytomine.domain.image;

import java.util.Arrays;
import java.util.Optional;

public enum UploadedFileStatus {
    /**
     * Even codes lower than 100 => information
     * Even codes greater or equal to 100 => success
     * Odd codes => error
     */
    UPLOADED (0),

    DETECTING_FORMAT (10),
    ERROR_FORMAT (11), // 3

    EXTRACTING_DATA (20),
    ERROR_EXTRACTION (21),

    CONVERTING (30),
    ERROR_CONVERSION (31), // 4

    DEPLOYING (40),
    ERROR_DEPLOYMENT (41), // 8

    DEPLOYED (100),
    EXTRACTED (102),
    CONVERTED (104);

    private final int code;

    UploadedFileStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    static Optional<UploadedFileStatus> findByCode(int code) {
        return Arrays.stream(values()).filter(x -> x.getCode() == code).findFirst();
    }
}
