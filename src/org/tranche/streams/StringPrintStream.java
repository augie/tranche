/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.tranche.streams;

import java.io.OutputStream;
import java.io.PrintStream;

/**
 * <p>Returns contents of everything writen to print stream as string.</p>
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class StringPrintStream extends PrintStream {

    final StringOutputStream sos;

    /**
     * <p>Creates a enw StringPrintStream object.</p>
     * <p>A StringPrintStream can be used as a PrintStream. All it does is store all written data as a string, which can be later retrieved using getOutput().</p>
     * <p>This is especially useful when want to log an activity.</p>
     * @return
     */
    public static StringPrintStream create() {
        StringOutputStream newSos = new StringOutputStream();
        return new StringPrintStream(newSos);
    }
    
    private StringPrintStream(StringOutputStream sos) {
        super(sos);
        this.sos = sos;
    }
    
    public String getOutput() {
        return sos.buffer.toString();
    }

    private static class StringOutputStream extends OutputStream {
        
        final StringBuffer buffer;
        
        StringOutputStream() {
            buffer = new StringBuffer();
        }

        @Override()
        public void close() {
        }

        @Override()
        public void flush() {
        }

        @Override()
        public void write(byte[] b) {
            String str = new String(b);
            buffer.append(str);
        }

        @Override()
        public void write(byte[] b, int off, int len) {
            byte[] bytes = new byte[len];
            System.arraycopy(b, off, bytes, 0, len);
            String str = new String(bytes);
            buffer.append(str);
        }

        public void write(int b) {
            buffer.append(b);
        }
    }
}
