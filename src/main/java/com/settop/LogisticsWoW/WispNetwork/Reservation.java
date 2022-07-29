package com.settop.LogisticsWoW.WispNetwork;

import com.settop.LogisticsWoW.Utils.Invalidable;

public class Reservation extends Invalidable
{
    public final int reservedCount;

    public Reservation(int reservedCount)
    {
        assert reservedCount != 0;
        this.reservedCount = reservedCount;
    }

    public boolean IsInsertReservation() {
        return reservedCount > 0;
    }

    public boolean IsExtractReservation() {
        return reservedCount < 0;
    }

    public int GetInsertCount()
    {
        assert IsInsertReservation();
        return reservedCount;
    }

    public int GetExtractCount()
    {
        assert IsExtractReservation();
        return -reservedCount;
    }
}
