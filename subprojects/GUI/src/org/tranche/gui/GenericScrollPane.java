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

import java.awt.Component;
import javax.swing.JScrollPane;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class GenericScrollPane extends JScrollPane {

    public GenericScrollPane() {
        super();
        init();
    }

    public GenericScrollPane(Component c) {
        super(c);
        init();
    }

    public GenericScrollPane(Component c, int verticalRule, int horizontalRule) {
        super(c, verticalRule, horizontalRule);
        init();
    }

    private void init() {
        setVerticalScrollBar(new GenericScrollBar());
        setHorizontalScrollBar(new GenericScrollBar(GenericScrollBar.HORIZONTAL));
        setBorder(Styles.BORDER_BLACK_1);
    }
}
