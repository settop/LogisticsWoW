package com.settop.LogisticsWoW.WispNetwork.Tasks;

import com.settop.LogisticsWoW.WispNetwork.Tasks.WispTask;
import com.settop.LogisticsWoW.WispNetwork.WispNetwork;
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

    public void Advance(@Nonnull TickEvent.ServerTickEvent tickEvent, @Nonnull WispNetwork network, int currentTickTime)
    {
        //make sure that we at least advance a little bit, even in the case tickEvent.haveTime() is false
        //advance at least 10% of the tasks
        int minProcessAmount = Math.max(tasks.size() / 10, 1);
        int processCount = 0;

        boolean needsSort = false;
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
                OptionalInt nextTickTime = queuedTask.task.Tick(tickEvent, network, currentTickTime, queuedTask.tickTime - currentTickTime);
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
