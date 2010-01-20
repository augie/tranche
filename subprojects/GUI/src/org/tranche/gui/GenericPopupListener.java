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
package org.tranche.gui;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JPopupMenu;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class GenericPopupListener extends MouseAdapter {

    public static final int DOUBLE_CLICK_ONLY = 0;
    public static final int RIGHT_CLICK_ONLY = 1;
    public static final int RIGHT_OR_DOUBLE_CLICK = 2;
    private int displayMethod = RIGHT_OR_DOUBLE_CLICK;
    private JPopupMenu menu;

    public GenericPopupListener(JPopupMenu menu) {
        this.menu = menu;
    }

    public void setDisplayMethod(int displayMethod) {
        this.displayMethod = displayMethod;
    }

    public void mousePressed(MouseEvent e) {
        showPopup(e);
    }

    public void mouseReleased(MouseEvent e) {
        showPopup(e);
    }

    private void showPopup(final MouseEvent e) {
        if (menu == null) {
            return;
        }
        if ((e.getClickCount() == 2 && (displayMethod == DOUBLE_CLICK_ONLY || displayMethod == RIGHT_OR_DOUBLE_CLICK)) || (e.isPopupTrigger() && (displayMethod == RIGHT_CLICK_ONLY || displayMethod == RIGHT_OR_DOUBLE_CLICK))) {
            menu.show(e.getComponent(), e.getX(), e.getY());
        }
    }
}
