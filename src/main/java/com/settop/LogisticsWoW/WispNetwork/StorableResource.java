package com.settop.LogisticsWoW.WispNetwork;

import org.checkerframework.checker.nullness.qual.NonNull;

public interface StorableResource<T> extends StoreableResourceMatcher<T>
{
    @NonNull T GetStack();
    int GetAmount();
}
