package exceptions;

public class InvalidPositionException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public InvalidPositionException(String msg)
		{	super(msg); }
	
}