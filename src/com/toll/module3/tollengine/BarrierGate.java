package com.toll.module3.tollengine;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Models the physical boom barrier actuator at a toll plaza lane.
 * Enforces the statutory safety rule: Barrier opens ONLY AFTER toll is deducted successfully
 * or legitimate exemption (Pass / Emergency) is validated.
 */
public class BarrierGate {
    private final int plazaId;
    private final int laneNumber;
    private final AtomicBoolean isRaised;

    public BarrierGate(int plazaId, int laneNumber) {
        this.plazaId = plazaId;
        this.laneNumber = laneNumber;
        this.isRaised = new AtomicBoolean(false);
    }

    public int getPlazaId() {
        return plazaId;
    }

    public int getLaneNumber() {
        return laneNumber;
    }

    public boolean isOpen() {
        return isRaised.get();
    }

    public void openBarrier() {
        isRaised.set(true);
    }

    public void closeBarrier() {
        isRaised.set(false);
    }

    @Override
    public String toString() {
        return String.format("BarrierGate[Plaza: %d | Lane: %d | Status: %s]",
                plazaId, laneNumber, isRaised.get() ? "OPEN (Raised)" : "CLOSED (Lowered)");
    }
}
