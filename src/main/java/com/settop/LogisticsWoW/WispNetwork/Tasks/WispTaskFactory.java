package com.settop.LogisticsWoW.WispNetwork.Tasks;

import com.settop.LogisticsWoW.LogisticsWoW;
import com.settop.LogisticsWoW.WispNetwork.WispNetwork;
import net.minecraft.nbt.CompoundTag;

import java.util.HashMap;

public abstract class WispTaskFactory
{
    private static final HashMap<String, WispTaskFactory> factories = new HashMap<>();

    public static synchronized void RegisterFactory(String typeName, WispTaskFactory factory)
    {
        if(factories.put(typeName, factory) != null)
        {
            throw new RuntimeException("Registering a wisp task factory that is already registered(%s)".formatted(typeName) );
        }
    }

    static SerialisableWispTask Read(WispNetwork network, CompoundTag nbt)
    {
        String type = nbt.getString("type");
        WispTaskFactory taskFactory = factories.get(type);
        if(taskFactory == null)
        {
            LogisticsWoW.LOGGER.error("Failed to find wisp task type {}", type);
            return null;
        }
        return taskFactory.CreateAndRead(network, nbt);
    }

    public abstract SerialisableWispTask CreateAndRead(WispNetwork network, CompoundTag nbt);
}
