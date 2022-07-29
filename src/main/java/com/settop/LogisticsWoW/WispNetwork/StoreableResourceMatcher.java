package com.settop.LogisticsWoW.WispNetwork;

import net.minecraft.nbt.CompoundTag;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.ArrayList;

public interface StoreableResourceMatcher<T>
{
    @NonNull Object GetType();
    //full match including nbt checks
    boolean Matches(StoreableResourceMatcher<T> other);
    boolean Matches(T other);
}
