package be.cytomine.Exception;

public class SoftwareParameterConstraintException extends CytomineException {

    public static int CODE = 400;

    /**
     * Message map with this exception
     * @param msg  Message
     */
    public SoftwareParameterConstraintException(String msg) {
        super(msg, CODE);
    }

}
