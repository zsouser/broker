/**
 * Broker Implementation Class
 * @author Zach Souser
 * @version 2/21/13
 */

class BrokerImplementation implements Broker {
	
	/** Front-of-house supply */
	
	private int[] supply;
	
	/** Back-of-house reserves */
	
	private int[] reserves;
	
	/** The specialty */
	
	private int specialty;

	/**
	 * Constructor for BrokerImplementation
	 * @param specialty the specialty for the broker
	 */

	public BrokerImplementation(int specialty) {
		this.specialty = specialty;
		supply = new int[3];
		reserves = new int[3];
	}

	/**
	 * Receive a delivery from a refiner
	 * @param ounces the amount to be delivered
	 */

	public synchronized void deliver(int ounces) {
		supply[specialty] += ounces;
		notifyAll();
	}

	/**
	 * Fulfill a request for a metal. While you cannot satisfy
	 * the order from reserves, attempt to reserve the order 
	 * as it becomes available. 
	 *
	 * If there is no deficit information, there is not enough
	 * specialty metal on hand to swap and we must wait.
	 *
	 * Otherwise, swa for non-specialty metals with their respective brokers.
	 * 
	 * @param order the order to be processed
	 */
	
	public void get(int[] order) throws StopThread {
		while (!fill(order)) {
			int[] deficit = reserve(order);
			if (deficit == null) doWait();
			else {
				for (int i = 0; i < deficit.length; i++) {
					if (i != specialty && deficit[i] > 0) {
						Project2.specialist(i).swap(specialty,deficit[i]);
						reserveSwap(i,deficit[i]);
					}				
				}
			} 
		}
	}

	/**
	 * Attempt to fill the order. If there is not enough of each
	 * metal on hand to immediately fill the order, return false. 
	 * If the order can be filled, remove it from reserves and return
	 * true.
	 *
	 * @param order the order to be filled
	 * @return boolean success flag
	 */

	public synchronized boolean fill(int[] order) throws StopThread {
		boolean enough = true;
		for (int i = 0; i < order.length; i++) {
			enough &= reserves[i] >= order[i];
		}
		if (!enough) return false;
		for (int i = 0; i < order.length; i++) {
			reserves[i] -= order[i];
		}
		return true;
	}
	
	/**
	 * Attempt to reserve an order.
	 * If there is not enough metal on hand to attempt a swap,
	 * indicate this by returning null. Otherwise, reserve all available
	 * metal and return an array of deficit information for swaps.
	 *
	 * @param order the order to be reserved
	 * @return the array of deficit values
	 */

	public synchronized int[] reserve(int[] order) throws StopThread {
		int total = 0;
		int[] deficit = new int[order.length];
		for (int i = 0; i < order.length; i++) {
			int difference = order[i] - supply[i];
			if (i == specialty) total += order[i];
			else if (difference > 0) total += difference;
		}

		if (total > supply[specialty]) return null;

		for (int i = 0; i < order.length; i++) {
			if (i == specialty) {
				reserve(i,total);
			}	
			else if (supply[i] >= order[i]) {
				deficit[i] = 0;
				reserve(i,order[i]);
			}	
			else {
				deficit[i] = order[i] - supply[i];
				reserve(i,supply[i]);
			}
		}
		return deficit;
	}

	/**
	 * Reserve helper function.
	 *
	 * @param what the metal to be reserved
	 * @param ounces the number of ounces to be reserved
	 */

	public synchronized void reserve(int what, int ounces) throws StopThread {
		if (supply[what] < ounces) throw new StopThread();
		reserves[what] += ounces;
		supply[what] -= ounces;
	}

	/**
	 * Perform the swap functionality when called from another Broker.
	 * Increments the supply of what in exchange for ounces of specialty.
	 *
	 * @param what the metal to be reserved
	 * @param ounces the number of ounces to be reserved
	 */		
	
	public synchronized void swap(int what, int ounces) throws StopThread {
   		while (supply[specialty] < ounces) doWait();
		supply[what] += ounces;
		supply[specialty] -= ounces;	
	}

	/**
	 * Perform a swap on reserves
	 *
	 * @param what the metal to be reserved
	 * @param ounces the number of ounces to be reserved
	 */
	
	private synchronized void reserveSwap (int what, int ounces) {
		reserves[what] += ounces;
		reserves[specialty] -= ounces;
	}	

	/**
	 * Wait wrapper function
	 */

	private synchronized void doWait() throws StopThread {
		try {
			wait();
		} catch (InterruptedException e) { throw new StopThread(); }
	}

	/**
	 * Get the amount on hand and insert it into inventory
	 * @param inventory the inventory
	 */
						  	
	public void getAmountOnHand(int[] inventory) {
		for (int i = 0; i < inventory.length; i++) {
			inventory[i] = supply[i] + reserves[i];
		}
	}
}
