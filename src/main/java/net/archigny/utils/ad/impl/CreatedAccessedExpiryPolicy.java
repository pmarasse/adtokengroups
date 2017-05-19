package net.archigny.utils.ad.impl;

import java.io.Serializable;

import javax.cache.configuration.Factory;
import javax.cache.configuration.FactoryBuilder;
import javax.cache.expiry.Duration;
import javax.cache.expiry.ExpiryPolicy;

public class CreatedAccessedExpiryPolicy implements ExpiryPolicy, Serializable {

    /** Serial */
    private static final long serialVersionUID = -3045147819105891618L;

    /** The {@link Duration} a cache entry should be available before it expires. */
    private Duration          timeToLive;

    /** The {@link Duration} a cache entry should be available, without be accessed, before it expires. */
    private Duration          timeToIdle;

    /**
     * Obtains a {@link Factory} for a CreatedAccessed {@link ExpiryPolicy}.
     * 
     * @param timeToLive
     *            the {@link Duration} a Cache Entry should exist before it expires after being created
     * @param timeToIdle
     *            the {@link Duration} a Cache Entry should exist before it expires after being accessed
     * @return a {@link Factory} for CreatedAccessed {@link ExpiryPolicy}.
     */
    public static Factory<ExpiryPolicy> factoryOf(Duration timeToLive, Duration timeToIdle) {

        return new FactoryBuilder.SingletonFactory<ExpiryPolicy>(new CreatedAccessedExpiryPolicy(timeToLive, timeToIdle));
    }

    /**
     * Construct an {@link ExpiryPolicy} based on a maximum TTL and an idle TTL
     * 
     * @param timeToLive
     *            the {@link Duration} a Cache Entry should exist before it expires after being created
     * @param timeToIdle
     *            the {@link Duration} a Cache Entry should exist before it expires after being accessed
     */
    public CreatedAccessedExpiryPolicy(Duration timeToLive, Duration timeToIdle) {
        super();
        this.timeToLive = timeToLive;
        this.timeToIdle = timeToIdle;
    }

    @Override
    public Duration getExpiryForCreation() {

        return timeToLive;
    }

    @Override
    public Duration getExpiryForAccess() {

        return timeToIdle;
    }

    @Override
    public Duration getExpiryForUpdate() {

        return null;
    }

    @Override
    public int hashCode() {

        final int prime = 31;
        int result = 1;
        result = prime * result + ((timeToIdle == null) ? 0 : timeToIdle.hashCode());
        result = prime * result + ((timeToLive == null) ? 0 : timeToLive.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        CreatedAccessedExpiryPolicy other = (CreatedAccessedExpiryPolicy) obj;
        if (timeToIdle == null) {
            if (other.timeToIdle != null) return false;
        } else if (!timeToIdle.equals(other.timeToIdle)) return false;
        if (timeToLive == null) {
            if (other.timeToLive != null) return false;
        } else if (!timeToLive.equals(other.timeToLive)) return false;
        return true;
    }

}
