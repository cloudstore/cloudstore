package co.codewizards.cloudstore.core.oio;

public interface FileFilter {

    /**
     * Tests whether or not the specified file should be
     * included in a file list.
     *
     * @param file the file to be tested. Never <code>null</code>.
     * @return <code>true</code> if and only if <code>file</code> should be included.
     */
    boolean accept(File file);
}

