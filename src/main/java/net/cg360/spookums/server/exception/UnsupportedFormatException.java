package net.cg360.spookums.server.exception;

/**
 * For use when a format/version of a file is unsupported
 * @author CG360
 */
public class UnsupportedFormatException extends RuntimeException {

    public UnsupportedFormatException() { super(); }
    public UnsupportedFormatException(String str) { super(str); }

}
