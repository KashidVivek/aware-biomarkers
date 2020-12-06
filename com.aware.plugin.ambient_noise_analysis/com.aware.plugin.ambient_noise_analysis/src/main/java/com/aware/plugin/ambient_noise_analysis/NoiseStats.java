package com.aware.plugin.ambient_noise;

import android.content.ContentResolver;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public class NoiseStats {
    private final ContentResolver resolver;
    private long lowNoiseTime;
    private long normalNoiseTime;
    private long highNoiseTime;
    private boolean isharmfulInDay;

    public NoiseStats(ContentResolver resolver) {
        this.resolver = resolver;
    }

    public void generateStats() {
        long previousDay = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000).getTime();
        long currentTime = new Date(System.currentTimeMillis()).getTime();
        lowNoiseTime = TimeUnit.MILLISECONDS.toMinutes(Stats.getTimeLow(resolver, previousDay, currentTime));
        normalNoiseTime=TimeUnit.MILLISECONDS.toMinutes(Stats.getTimeNormal(resolver, previousDay, currentTime));
        highNoiseTime = TimeUnit.MILLISECONDS.toMinutes(Stats.getTimeHigh(resolver, previousDay, currentTime));
        isharmfulInDay = highNoiseTime>120;
    }

    public long getlowNoiseTime() {
        return lowNoiseTime;
    }

    public long getnormalNoiseTime() {
        return normalNoiseTime;
    }

    public long gethighNoiseTime() {
        return highNoiseTime;
    }

    public boolean isharmfulInDay() {
        return isharmfulInDay;
    }
}
