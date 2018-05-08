package com.l7tech.server.identity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created for failed authentications.
 */
public class AuthenticationResultFailure {

    private final Long cacheFailureTime;
    private final String failureReason;

    /**
     * Create an authentication result for failed authentications.
     *
     * @param currTimeMilliseconds The CacheFailureTime (required)
     * @param failureReason The reason why the authentication failed.
     */
    public AuthenticationResultFailure(@NotNull final Long currTimeMilliseconds, @Nullable final String failureReason){

        this.cacheFailureTime = currTimeMilliseconds;
        this.failureReason = failureReason;
    }

    public Long getCacheFailureTime(){
        return this.cacheFailureTime;
    }

    public String getFailureReason(){
        return this.failureReason;
    }
}
