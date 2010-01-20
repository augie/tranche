/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.tranche.exceptions;

/**
 * <p>Used if client request is improperly formatted.</p>
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class TrancheProtocolException extends RuntimeException {
    public static final String MESSAGE = "The request is not properly formatted and cannot be serviced.";
    
    public TrancheProtocolException() {
        super(MESSAGE);
    }
}
