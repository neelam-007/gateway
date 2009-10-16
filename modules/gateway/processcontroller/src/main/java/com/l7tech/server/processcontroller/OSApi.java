package com.l7tech.server.processcontroller;

import com.l7tech.common.io.ProcResult;

import javax.jws.WebService;
import java.io.IOException;

/**
 * Operating System API.
 */
@WebService
public interface OSApi {

    /**
     * Executes a program with the given parameters and (optionally) within a given timeout and returns the result.
     *
     * @param cwd     The working directory for the program.
     * @param program The program to be executed.
     * @param args    The program's argument list.
     * @param stdin   Input data to be passed to program's stdin.
     * @param timeoutMillis Maximum execution time (in milliseconds) allowed for the program.
     *                      If the program has not finished within timeoutMillis the process running the program is
     *                      destroyed and IOException is thrown. If timeoutMillis is not positive the call will block
     *                      waiting for the program to finish.
     * @return        ProcResult containing the program's exit code, stdout and stderr.
     * @throws IOException if there an error launching the program or if the program has not finished executing within the allowed timeout.
     */
    public ProcResult execute(String cwd, String program, String[] args, byte[] stdin, long timeoutMillis) throws IOException;
}
