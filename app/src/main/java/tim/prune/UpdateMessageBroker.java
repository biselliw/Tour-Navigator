package tim.prune;

// Basic class required for Android app
// @since WB

/**
 * Class responsible for distributing update information
 * to all registered listeners
 */


 public class UpdateMessageBroker {


	/**
	 * Send a message to all subscribers that
	 * the data has been updated
	 */
	public static void informSubscribers() {};



	/**
	 * Send message to all subscribers
	 * @param inChange Change that occurred
	 */
	public static void informSubscribers(byte inChange) {};


	/**
	 * Send message to all subscribers
	 * @param inMessage message to display informing of action completed
	 */
	public void informSubscribers(String inMessage) {};

}

