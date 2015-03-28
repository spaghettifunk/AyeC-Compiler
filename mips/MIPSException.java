package rtl;

public class MIPSException extends Exception
{
	private String message;

    public MIPSException(String message) 
    {
        this.message = message;
    }

    public MIPSException(String message, Position p) 
    {
        if (p != null)
        	this.message = message + " @ " + p.toString();
        else
        	this.message = message + " @ unknown";
    }

    @Override
    public String getMessage() {
    	return message;
    }
}