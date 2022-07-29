package com.settop.LogisticsWoW.WispNetwork;

import com.settop.LogisticsWoW.Wisps.WispInteractionNodeBase;
import net.minecraft.core.Direction;

import javax.annotation.Nonnull;

public class SourcedReservation
{
    public final Reservation reservation;
    public final WispInteractionNodeBase inventoryNode;
    public final Direction inventoryDirection;

    public SourcedReservation(@Nonnull Reservation reservation, @Nonnull WispInteractionNodeBase inventoryNode, Direction inventoryDirection)
    {
        this.reservation = reservation;
        this.inventoryNode = inventoryNode;
        this.inventoryDirection = inventoryDirection;
    }

    public boolean IsValid()
    {
        return reservation.IsValid();
    }
}
