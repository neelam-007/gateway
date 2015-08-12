package com.l7tech.internal.signer;

/**
 * List of known Skar Signer exit codes.
 */
public abstract class SignerErrorCodes {
    public static final int SUCCESS = 0;
    public static final int PRINT_HELP = 1;
    public static final int PARSING_ERROR = 2;
    public static final int INVALID_ARG = 3;
    public static final int IO_ERROR_WHILE_SIGNING = 4;
    public static final int ERROR_SIGNING = 5;
    public static final int ERROR_ENCODING_PASSWORD = 6;
}
