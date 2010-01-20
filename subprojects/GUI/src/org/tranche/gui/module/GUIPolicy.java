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
package org.tranche.gui.module;

/**
 * Used to represent an actions GUI policy, built using annotations
 * @author Bryan Smith <bryanesmith at gmail.com>
 */
public class GUIPolicy {

    private static int LEFT_MENU_PROJECT = 1 << 0; // 1
    private static int LEFT_MENU_FILES = 1 << 1;  // 2
    private static int POPUP_MENU_PROJECT = 1 << 2; // 4
    private static int POPUP_MENU_FILES = 1 << 3; // 8
    public static final int SINGLE_FILE_ONLY = 1 << 4, // 16
             MULTI_FILES_ONLY = 1 << 5, // 32
             ANY_NUMBER_FILES = 1 << 6, // 64
             NO_FILES = 1 << 7; //
    // The value stored as single byte
    private int policy;

    /**
     * Create a policy with no permissions.
     */
    public GUIPolicy() {
        policy = 0;
    }

    private GUIPolicy(byte policy) {
        this.policy = policy;
    }

    public boolean isForProjects() {
        return isForProjectLeftMenu() || isForProjectPopupMenu();
    }

    public boolean isForProjectLeftMenu() {
        return (this.policy & this.LEFT_MENU_PROJECT) == this.LEFT_MENU_PROJECT;
    }

    public boolean isForProjectPopupMenu() {
        return (this.policy & this.POPUP_MENU_PROJECT) == this.POPUP_MENU_PROJECT;
    }

    public boolean isForFiles() {
        return isForFilesLeftMenu() || isForFilesPopupMenu();
    }

    public boolean isForFilesLeftMenu() {
        return (this.policy & this.LEFT_MENU_FILES) == this.LEFT_MENU_FILES;
    }

    public boolean isForFilesPopupMenu() {
        return (this.policy & this.POPUP_MENU_FILES) == this.POPUP_MENU_FILES;
    }

    public boolean anyNumberOfFiles() {
        return (policy & ANY_NUMBER_FILES) == ANY_NUMBER_FILES;
    }

    public boolean singleFileOnly() {
        return (policy & SINGLE_FILE_ONLY) == SINGLE_FILE_ONLY;
    }

    public boolean multiFileOnly() {
        return (policy & MULTI_FILES_ONLY) == MULTI_FILES_ONLY;
    }

    public boolean noFileOnly() {
        return (policy & NO_FILES) == NO_FILES;
    }

    public String toString() {
        return String.valueOf(policy);
    }

    public static GUIPolicy createFromString(String str) {
        return new GUIPolicy(Byte.parseByte(str));
    }

    public static GUIPolicy extractPolicy(TrancheMethodAnnotation ma, LeftMenuAnnotation la, PopupMenuAnnotation pa) {

        byte policy = 0;

        // Get selection mode first
        String mode = ma.selectionMode().trim().toLowerCase();

        if (mode.equals("single")) {
            policy += SINGLE_FILE_ONLY;
        } else if (mode.equals("multiple")) {
            policy += MULTI_FILES_ONLY;
        } else if (mode.equals("any")) {
            policy += ANY_NUMBER_FILES;
        } else if (mode.equals("none")) {
            policy += NO_FILES;
        }

        // Left menu annotation
        if (la != null) {
            if (la.scope().trim().toLowerCase().contains("projects")) {
                policy += LEFT_MENU_PROJECT;
            }
            if (la.scope().trim().toLowerCase().contains("files")) {
                policy += LEFT_MENU_FILES;
            }
        }

        // Popup menu annotation
        if (pa != null) {
            if (pa.scope().trim().toLowerCase().contains("projects")) {
                policy += POPUP_MENU_PROJECT;
            }
            if (pa.scope().trim().toLowerCase().contains("files")) {
                policy += POPUP_MENU_FILES;
            }
        }

        return new GUIPolicy(policy);
    }
}
