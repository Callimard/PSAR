package common.message;

import java.util.Set;

import peersim.core.Node;

public class LoanRequest extends Request {

	// Private.
	
	private double mark;
	
	private Set<Integer> missingResource;
	
	// Constructors.

	public LoanRequest(double mark, Set<Integer> missingResource, int resourceID, int requestID, Node sender, Node receiver) {
		super(resourceID, requestID, sender, receiver);
		
		this.mark = mark;
		this.missingResource = missingResource;
	}
	
	// Getters and Setters.
	
	public double getMark() {
		return this.mark;
	}
	
	public Set<Integer> getMissingResource() {
		return this.missingResource;
	}

}
