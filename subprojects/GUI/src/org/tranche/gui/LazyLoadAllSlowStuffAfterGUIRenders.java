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

import java.util.LinkedList;
import java.util.List;
import org.tranche.gui.module.TrancheModulesUtil;

/**
 * Possibly my favorite design pattern. Or better known as lazy loading anything that could be slow. The goal is to have the GUI render as fast as it possibly can. *Anything* not related to layout is loaded lazily.
 * In order to prevent lazy loading from breaking functionality, anything that requires loading can use the lazyLoad() method of the related object to wait appropriately.
 *
 * @author Jayson Falkner - jfalkner@umich.edu
 * @author James "Augie" Hill - augman85@gmail.coms
 */
public class LazyLoadAllSlowStuffAfterGUIRenders {

    private static final List<LazyLoadable> thingsToLazyLoad = new LinkedList<LazyLoadable>();

    /**
     * 
     */
    private LazyLoadAllSlowStuffAfterGUIRenders() {
    }

    /**
     * This should be invoked immediately after the GUI's setVisible() is called.
     */
    public static final synchronized void lazyLoad() {
        LazyLoadable[] lazyLoadArray = thingsToLazyLoad.toArray(new LazyLoadable[0]);
        for (int i = 0; i < lazyLoadArray.length; i++) {
            try {
                // get the next thing to load
                LazyLoadable ll = lazyLoadArray[i];
                // load it
                ll.lazyLoad();
                // lazy loadable
                thingsToLazyLoad.remove(ll);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Force module load
        TrancheModulesUtil.loadModules();
    }

    /**
     * 
     * @param ll
     */
    public static final synchronized void add(LazyLoadable ll) {
        thingsToLazyLoad.add(ll);
    }
}
