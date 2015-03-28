// rtl package containing all intermediate instructions
package rtl; 

class SemanticException extends Exception {
    
	private String message;

    public SemanticException(String message) {
        this.message = message;
    }

    public SemanticException(String message, Position p) {
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