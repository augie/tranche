/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.tranche.exceptions;

/**
 * <p>If no host provided for request, but host required, throw this exception.</p>
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class NoHostProvidedException extends RuntimeException {
    public static final String MESSAGE = "At least one host is required for this function, but none has been provided.";
    
    public NoHostProvidedException() {
        super(MESSAGE);
    }
}
