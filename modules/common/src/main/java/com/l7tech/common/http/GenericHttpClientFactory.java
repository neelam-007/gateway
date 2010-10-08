package com.l7tech.common.http;

/**
 * Factory for GenericHttpClient instances.
 *
 * <p>A client created by the factory may or may not be suitable for use from
 * multiple threads.</p>
 */
public interface GenericHttpClientFactory {

    /**
     * Create a client with default settings.
     *
     * <p>This will create a new GenericHttpClient instance ready to make
     * outgoing requests, possibly already including any SSL setup required
     * for the current environment.</p>
     *
     * @return The new client.
     */
    GenericHttpClient createHttpClient();

    /**
     * Create a client with the given settings.
     *
     * <p>This will create a new GenericHttpClient instance ready to make
     * outgoing requests, possibly already including any SSL setup required
     * for the current environment.</p>
     *
     * <p>Some factories may not support all configuration settings. In this
     * case defaults will be used (or the settings ignored if not meaningful
     * for the client implementation)</p>
     *
     * @param hostConnections The maximum number of connections per host (-1 for default)
     * @param totalConnections The maximum number of connections (-1 for default)
     * @param connectTimeout The socket timeout for connections (-1 for default)
     * @param timeout The socket timeout for reads (-1 for default)
     * @param identity The identity to bind (may be null)
     * @return The new client.
     */
    GenericHttpClient createHttpClient(int hostConnections,
                                       int totalConnections,
                                       int connectTimeout,
                                       int timeout,
                                       Object identity);
}
