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
package org.tranche.gui.wizard;

import javax.swing.JPanel;

/**
 *
 * @author Bryan E. Smith - bryanesmith@gmail.com
 */
public class GenericWizardStep {

    private String title;
    private GenericWizardPanel panel;
    private String buttonLabel = "Next...";
    private GenericWizardStep previous = null;
    private GenericWizardStep next = null;
    private boolean isNextAvailableDefault = true;
    private boolean isBackDisabled = false;
    private boolean isNextDisabled = false;
    private JPanel icon = null;

    /**
     *
     * @param title The title for the step.
     * @param panel The panel with the content for the step.
     */
    public GenericWizardStep(String title, GenericWizardPanel panel) {
        this.title = title;
        this.panel = panel;
    }

    /**
     * Returns the title for this step in the wizard
     */
    public String getTitle() {
        return title;
    }

    /**
     * Set the title for this step in the wizard
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Returns the panel embedded in the wizard for this step.
     */
    public GenericWizardPanel getPanel() {
        return panel;
    }

    /**
     * Sets the panel for this step in the wizard.
     */
    public void setPanel(GenericWizardPanel panel) {
        this.panel = panel;
    }

    /**
     * <p>Returns the label used for the buttons that lead to this step (forward or backwards).</p>
     */
    public String getButtonLabel() {
        return buttonLabel;
    }

    /**
     * <p>Set the label used on the buttons that lead to this step (forwards or backwards).</p>
     */
    public void setButtonLabel(String buttonLabel) {
        this.buttonLabel = buttonLabel;
    }

    /**
     * <p>Return the previous step. If not one, returns null.</p>
     * <p>Note that the wizard uses this to navigate backwards and forwards.</p>
     */
    public GenericWizardStep getPrevious() {
        return previous;
    }

    /**
     * <p>Set the previous step. Note that steps are doubly-linked lists.</p>
     * <p>Ideally, you won't set these explicitly. If you add the steps in order, the GenericWizard will handle this for you.</p>
     */
    public void setPrevious(GenericWizardStep previous) {
        this.previous = previous;
    }

    /**
     * <p>Return the next step. If not one, returns null.</p>
     * <p>Note that the wizard uses this to navigate backwards and forwards.</p>
     */
    public GenericWizardStep getNext() {
        return next;
    }

    /**
     * <p>Set the next step. Note that steps are doubly-linked lists.</p>
     * <p>Ideally, you won't set these explicitly. If you add the steps in order, the GenericWizard will handle this for you.</p>
     */
    public void setNext(GenericWizardStep next) {
        this.next = next;
    }

    /**
     * Returns whether the back button is disabled for this step. Default is false.
     */
    public boolean isBackDisabled() {
        return isBackDisabled;
    }

    /**
     * Set whether to disable the back button for this step. Default is false.
     */
    public void setIsBackDisabled(boolean isBackDisabled) {
        this.isBackDisabled = isBackDisabled;
    }

    /**
     * Returns whether the forward button is disabled for this step. Default is false.
     */
    public boolean isNextDisabled() {
        return isNextDisabled;
    }

    /**
     * Set whether to disable the forward button for this step. Default is false.
     */
    public void setIsNextDisabled(boolean isNextDisabled) {
        this.isNextDisabled = isNextDisabled;
    }

    /**
     * <p>Returns array of JPanels with specific icons for this step. Default is null.</p>
     * <p>Note that GenericWizard may have default icons. Icons for a step will only override if not null.</p>
     */
    public JPanel getIcon() {
        return icon;
    }

    /**
     * <p>Set icon panels for specific step so different steps can have different icons.</p>
     * <p>Note that GenericWizard may have default icons. Icons for a step will only override if not null.</p>
     */
    public void setIcon(JPanel icon) {
        this.icon = icon;
    }

    public boolean isNextAvailableByDefault() {
        return isNextAvailableDefault;
    }

    public void setIsNextAvailableDefault(boolean isNextAvailableDefault) {
        this.isNextAvailableDefault = isNextAvailableDefault;
    }

    public String toString() {
        return this.title;
    }
}
