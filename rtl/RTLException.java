package rtl;

class RTLException extends Exception {
    
	private String message;

    public RTLException(String message) 
    {
        this.message = message;
    }

    public RTLException(String message, Position p) 
    {
        if (p != null)
        	this.message = message + " @ " + p.toString();
        else
        	this.message = message + " @ unknown";
    }

    @Override
    public String getMessage() 
    {
    	return message;
    }
}