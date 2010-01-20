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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.tranche.hash.BigHash;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class PassphrasePool {

    private static final Map<BigHash, String> passphrases = new HashMap<BigHash, String>();
    private static final List<PassphrasePoolListener> listeners = new ArrayList<PassphrasePoolListener>();

    public synchronized static void set(BigHash hash, String passphrase) {
        if (passphrase == null || passphrase.equals("") || hash == null) {
            return;
        }
        boolean isNew = passphrases.containsKey(hash);
        passphrases.put(hash, passphrase);
        for (PassphrasePoolListener listener : listeners) {
            if (isNew) {
                listener.passphraseAdded(hash);
            } else {
                listener.passphraseUpdated(hash);
            }
        }
    }

    public synchronized static void remove(BigHash hash) {
        passphrases.remove(hash);
        for (PassphrasePoolListener listener : listeners) {
            listener.passphraseRemoved(hash);
        }
    }

    public synchronized static boolean contains(BigHash hash) {
        return passphrases.containsKey(hash);
    }

    public synchronized static int size() {
        return passphrases.size();
    }

    public synchronized static String get(BigHash hash) {
        return passphrases.get(hash);
    }

    public synchronized static Map<BigHash, String> getAll() {
        Map<BigHash, String> map = new HashMap<BigHash, String>();
        for (BigHash hash : passphrases.keySet()) {
            map.put(hash, passphrases.get(hash));
        }
        return Collections.unmodifiableMap(map);
    }

    public synchronized static void addListener(PassphrasePoolListener listener) {
        listeners.add(listener);
    }
}
