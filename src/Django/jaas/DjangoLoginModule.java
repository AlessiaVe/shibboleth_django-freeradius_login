package Django.jaas;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

// import for the library Unirest
import com.mashape.unirest.http.*;
import com.mashape.unirest.http.exceptions.UnirestException;
import java.util.HashMap;
import org.json.JSONObject;

/**
 * Extends the basic login module to do django based authentication.
 *
 * @author Alessia Ventani
 */

public class DjangoLoginModule implements LoginModule {

    protected Subject subject = null;
    protected CallbackHandler callbackhandler = null;
    protected Callback[] callbacks = null;
    protected Map sharedState = null;
    protected Map options = null;
    protected String username = null;
    protected String password = null;
    protected Set subjectPrincipals = null;
    protected String djangoUrl = "";

    private boolean loginState = false;
    private boolean commitState = false;

    /**
     * default constructor
     */
    public DjangoLoginModule() {
	super();
    }

    /**
     * default life cycle method
     */
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
        this.subject = subject;
        this.callbackhandler = callbackHandler;
        this.sharedState = sharedState;
        this.options = options;

	this.djangoUrl = getOption("djangoUrl", "url");
        if (this.djangoUrl == "url") throw new Error("No Url for the authentiction specified (djangoUrl=?)");

    }

    /**
     * The actual authentication logic comes here.
     */
    public boolean login() throws LoginException {
        // Do the callbacks to take the info from Shibboleth
        Callback callbacks[] = null;
        this.buildCallbacks();
        this.execCallbacks();
        this.username = this.getUsername();
        this.password = this.getPassword();

        //only if session and authentication token is not null
        if ( this.username != null && this.password != null ) {
	    // do the authentication code here...

            // check the result of the Django Authentication
	    if ( this.passesDjangoCheck(this.username, this.password ) ) {
		//this.subjectPrincipals = this.findPrincipals();
		this.loginState = true;
	    }
        }
	else {
            throw new FailedLoginException("Failed login procedure as either the username or password token was not presented");
        }
	return this.loginState;
    }

    /**
     * Standard lifecycle method. If the login has successed it simply adds the principals to the Subject 
     */
    public boolean commit() throws LoginException {
        if ( this.loginState == true ) {
            //add principals
            this.subject.getPrincipals().add(new DjangoPrincipal(this.username));
            //this.subject.getPrincipals().addAll(this.subjectPrincipals);
            this.commitState = true;
        }
	return this.commitState;
    }

    /**
     * Invoked when the login context determines that it cant continue with the login process 
     */
    public boolean abort() throws LoginException {
        //should be same as logout
        return this.logout();
    }

    /**
     * Invoked when the subject is being logged out
     */
    public boolean logout() throws LoginException {
        this.commitState = false;
        this.loginState = false;
        subject.getPrincipals().removeAll(this.subjectPrincipals);
        return true;
    }

    /**
     * Helper method to build the call backs to be passed to the call back handler.
     */
    protected void buildCallbacks() {
        this.callbacks = new Callback[2];
        this.callbacks[0] = new NameCallback("Username: ");
        this.callbacks[1] = new PasswordCallback("Password: ", false);
    }

    /**
     * Passes the call backs to the handler to determine user input
     * @throws LoginException when the call back handler isnt able to execute all the callbacks
     */
    protected void execCallbacks() throws LoginException {
        try {
            this.callbackhandler.handle(this.callbacks);
        } catch(IOException ie) {
            throw new LoginException("Caught an IOException while executing callbacks");
        } catch(UnsupportedCallbackException uce) {
            throw new LoginException("Caught an UnsupportedCallbackException while executing callbacks");
        }
    }

    /**
     * helper method to retrieve the users username
     * @return the radius username of the authenticating user
     */
    protected String getUsername() {
        String user = null;
        for ( int i = 0; i < this.callbacks.length; i++){
            if ( this.callbacks[i] instanceof NameCallback ) {
                user = ((NameCallback)this.callbacks[i]).getName();
            }
        }
        return user;
    }

    /**
     * helper method to retrieve the password entered by the user
     * @return password given by the user
     */
    protected String getPassword() {
        String pass = null;
        for ( int i = 0; i < this.callbacks.length; i++){
            if ( this.callbacks[i] instanceof PasswordCallback) {
                pass = new String(((PasswordCallback)this.callbacks[i]).getPassword());
            }
        }
	return pass;
    }

    /**
     * creates a connection to DjangoServer and post the data for the  authentication
     */
    protected boolean passesDjangoCheck(String user, String pass) throws LoginException {

        boolean isAuthenticated = false;
	String result = "";
	Map<String, Object> fields = new HashMap<>();
        fields.put("username", user);
        fields.put("password", pass);
        try{
        	HttpResponse<String> request = Unirest.post(this.djangoUrl)
                		         .header("Accept", "application/json")
                                         .fields(fields)
                                         .asString();
                JSONObject json = new JSONObject(request.getBody());
                result  =  json.getString("control:Auth-Type");
                if("Accept".equals(result)){
			isAuthenticated = true;
		}
        }catch(UnirestException ex){
           throw new LoginException("Error of connection ");
        }
        return isAuthenticated;
    }


   /**
   * get options from the jaas.config file
   */

   // boolean option
   protected boolean getOption(String name, boolean dflt)
   {
	String opt = ((String) options.get(name));

	if (opt == null) return dflt;

	opt = opt.trim();
	if (opt.equalsIgnoreCase("true") || opt.equalsIgnoreCase("yes") || opt.equals("1"))
		return true;
	else if (opt.equalsIgnoreCase("false") || opt.equalsIgnoreCase("no") || opt.equals("0"))
		return false;
	else
		return dflt;
   }

   // int option
   protected int getOption(String name, int dflt)
   {
	String opt = ((String) options.get(name));
	if (opt == null) return dflt;
	try { dflt = Integer.parseInt(opt); } catch (Exception e) { }
	return dflt;
   }

   // string option
   protected String getOption(String name, String dflt)
   {
	String opt = (String) options.get(name);
	return opt == null ? dflt : opt;
   }
}















