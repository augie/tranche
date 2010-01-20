/*
 *    Copyright 2005 The Regents of the University of Michigan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tranche.clc;

/**
 * A Tranch Server CLC item attribute class.
 * @author Jayson Falkner - jfalkner@umich.edu
 */
public class TrancheServerCommandLineClientItemAttribute {
    String name;
    String description;
    boolean required;
     
    /**
     * Create a CLC item attribute with a name, description defaulting that the 
     * attribute is not required.
     * @param   name            the name of the attribute 
     * @param   description     the description of the attribute
     *
     */
    public TrancheServerCommandLineClientItemAttribute(String name, String description) {
        this(name, description, false);
    }
    
    /** 
     * Createa a CLC item attribute with a name, description, and marking if it is
     * a required attribute.
     * @param   name            the name of the attribute 
     * @param   description     the description of the attribute 
     * @param   required        the required flag
     * 
     */
    public TrancheServerCommandLineClientItemAttribute(String name, String description, boolean required) {
        this.name = name;
        this.description = description;
        this.required = required;
    }
    
    /**
     * Retrieve the name of the item attribute.
     * @return  the name of an attribute
     * 
     */
    public String getName(){
        return name;
    }
    
    /**
     * Retrieve the description of the item attribute.
     * @return  the description of an attribute
     * 
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Return true if item attribute is required; otherwise false.
     * @return  <code>true</code> is an attribute is required;
     *          <code>false</code> otherwise
     *
     */
    public boolean isRequired(){
        return required;
    }
}
