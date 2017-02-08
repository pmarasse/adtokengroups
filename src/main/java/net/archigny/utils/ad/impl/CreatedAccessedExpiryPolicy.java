package net.archigny.utils.ad.impl;

import java.io.Serializable;

import javax.cache.expiry.Duration;
import javax.cache.expiry.ExpiryPolicy;

public class CreatedAccessedExpiryPolicy implements ExpiryPolicy, Serializable {

    /** Serial */
    private static final long serialVersionUID = -3045147819105891618L;

    /** Durée de vie maximale d'un élément */
    private Duration          timeToLive;

    /** Durée de vie d'un élément après accès */
    private Duration          timeToIdle;

    public CreatedAccessedExpiryPolicy(Duration timeToLive, Duration timeToIdle) {
        super();
        this.timeToLive = timeToLive;
        this.timeToIdle = timeToIdle;
    }

    @Override
    public Duration getExpiryForCreation() {

        // TODO Auto-generated method stub
        return timeToLive;
    }

    @Override
    public Duration getExpiryForAccess() {

        // TODO Auto-generated method stub
        return timeToIdle;
    }

    @Override
    public Duration getExpiryForUpdate() {

        // TODO Auto-generated method stub
        return null;
    }

}
