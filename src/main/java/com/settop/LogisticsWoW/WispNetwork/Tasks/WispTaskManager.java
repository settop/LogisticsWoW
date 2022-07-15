package com.settop.LogisticsWoW.WispNetwork.Tasks;

import com.settop.LogisticsWoW.LogisticsWoW;
import com.settop.LogisticsWoW.WispNetwork.Tasks.WispTask;
import com.settop.LogisticsWoW.WispNetwork.WispNetwork;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.*;

public class WispTaskManager
{
    protected static class QueuedWispTask implements Comparable<QueuedWispTask>
    {
        WispTask task;
        int tickTime;

        @Override
        public int compareTo(@NotNull QueuedWispTask o)
        {
            return o.tickTime - tickTime;
        }
    }
    //sorted in reverse order, the first task to finish should be at the end
    protected ArrayList<QueuedWispTask> tasks = new ArrayList<>();
    protected ArrayList<QueuedWispTask> newTasks = new ArrayList<>();
    protected boolean inAdvance = false;

    private void HandleLostItemStack(@Nonnull WispNetwork network, ItemStack itemStack)
    {

    }

    public void Advance(@Nonnull TickEvent.ServerTickEvent tickEvent, @Nonnull WispNetwork network, int currentTickTime)
    {
        //make sure that we at least advance a little bit, even in the case tickEvent.haveTime() is false
        //advance at least 10% of the tasks
        int minProcessAmount = Math.max(tasks.size() / 10, 1);
        int processCount = 0;

        boolean needsSort = false;
        inAdvance = true;
        //reverse iterator
        for(ListIterator<QueuedWispTask> it = tasks.listIterator(tasks.size()); it.hasPrevious(); ++processCount)
        {
            if(processCount >= minProcessAmount)
            {
                if(!tickEvent.haveTime())
                {
                    break;
                }
            }
            QueuedWispTask queuedTask = it.previous();
            if(currentTickTime >= queuedTask.tickTime)
            {
                OptionalInt nextTickTime = OptionalInt.empty();
                try
                {
                    nextTickTime = queuedTask.task.Tick(tickEvent, network, currentTickTime, queuedTask.tickTime - currentTickTime);
                }
                catch (Exception ex)
                {
                    LogisticsWoW.LOGGER.error("Task tick failed.", ex);
                    if(queuedTask.task instanceof ItemHoldingTask)
                    {
                        HandleLostItemStack(network, ((ItemHoldingTask) queuedTask.task).heldItemStack);
                    }
                }
                if(nextTickTime.isPresent())
                {
                    queuedTask.tickTime = nextTickTime.getAsInt();
                    needsSort = true;
                }
                else
                {
                    //task is done
                    it.remove();
                }
            }
            else
            {
                //done processing for now, all other tasks will be later as well
                break;
            }
        }
        inAdvance = false;

        if(!newTasks.isEmpty())
        {
            tasks.addAll(newTasks);
            newTasks.clear();
            needsSort = true;
        }
        if(needsSort)
        {
            tasks.sort(null);
        }
    }

    public boolean HasAnyTasks()
    {
        return !tasks.isEmpty();
    }

    public int GetSoonestTickTime()
    {
        if(tasks.isEmpty())
        {
            return 0;
        }
        else
        {
            return tasks.get(tasks.size() - 1).tickTime;
        }
    }

    public void StartTask(@Nonnull WispTask task, @Nonnull WispNetwork network, int startTickTime)
    {
        int firstTickTime = task.Start(network, startTickTime);
        QueuedWispTask queuedTask = new QueuedWispTask();
        queuedTask.task = task;
        queuedTask.tickTime = firstTickTime;

        if(inAdvance)
        {
            newTasks.add(queuedTask);
        }
        else
        {
            //find the index to insert on to retain the correct ordering
            int begin = 0;
            int end = tasks.size();
            while(begin != end)
            {
                int testIndex = (begin + end) / 2;
                int comp = queuedTask.compareTo(tasks.get(testIndex));
                if(comp == 0)
                {
                    //insert it here
                    tasks.add(testIndex, queuedTask);
                    return;
                }
                else if(comp < 0)
                {
                    end = testIndex;
                }
                else
                {
                    begin = testIndex + 1;
                }
            }
            if(begin == tasks.size())
            {
                //adding to the end
                tasks.add(queuedTask);
            }
            else
            {
                tasks.add(begin, queuedTask);
            }
        }
    }

    public CompoundTag SerialiseNBT(WispNetwork network, int currentTickTime)
    {
        ListTag taskListNBT = new ListTag();

        for(QueuedWispTask task : tasks)
        {
            if(task.task instanceof SerialisableWispTask)
            {
                SerialisableWispTask serialisableTask = (SerialisableWispTask)task.task;
                CompoundTag taskNBT = serialisableTask.SerialiseNBT(network);
                if(taskNBT != null)
                {
                    taskNBT.putInt("nextTickOffset", task.tickTime - currentTickTime);
                    taskListNBT.add(taskNBT);
                }
            }
        }

        if(taskListNBT.isEmpty())
        {
            return null;
        }
        CompoundTag nbt = new CompoundTag();
        nbt.put("taskList", taskListNBT);

        return nbt;
    }

    public void DeserialiseNBT(WispNetwork network, int currentTickTime, CompoundTag nbt)
    {
        if(nbt == null)
        {
            return;
        }
        ListTag taskListNBT = nbt.getList("taskList", nbt.getId());
        for(int i = 0; i < taskListNBT.size(); ++i)
        {
            CompoundTag taskNBT = taskListNBT.getCompound(i);
            WispTask task = null;
            try
            {
                task = WispTaskFactory.Read(network, taskNBT);
                int tickOffset = taskNBT.getInt("nextTickOffset");
                QueuedWispTask queuedWispTask = new QueuedWispTask();
                queuedWispTask.tickTime = currentTickTime + tickOffset;
                queuedWispTask.task = task;
                tasks.add(queuedWispTask);
            }
            catch (Exception ex)
            {
                LogisticsWoW.LOGGER.error("Error loading task. Task NBT: \"{}\".", taskNBT.toString(), ex);
                if(task instanceof ItemHoldingTask)
                {
                    HandleLostItemStack(network, ((ItemHoldingTask) task).heldItemStack);
                }
            }
        }
        tasks.sort(null);
    }
}
