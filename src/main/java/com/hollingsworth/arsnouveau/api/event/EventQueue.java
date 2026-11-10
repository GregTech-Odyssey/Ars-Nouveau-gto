package com.hollingsworth.arsnouveau.api.event;

import appeng.hooks.ticking.TickHandler;
import com.hollingsworth.arsnouveau.ArsNouveau;
import com.suried.gtoutils.schedule.Cadence;
import com.suried.gtoutils.schedule.Schedulers;
import com.suried.gtoutils.schedule.Task;
import it.unimi.dsi.fastutil.objects.Reference2ObjectLinkedOpenHashMap;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * For queuing deferred or over-time tasks. Tick refers to the Server or Client Tick event.
 */
@SuppressWarnings("ForLoopReplaceableByForEach")
@Mod.EventBusSubscriber(modid = ArsNouveau.MODID)
public class EventQueue {
    private static final Consumer<ITimedEvent> SERVER_TICK = event -> event.tick(true);
    private static final Consumer<ITimedEvent> CLIENT_TICK = event -> event.tick(false);

    @NotNull List<ITimedEvent> events = new ArrayList<>();;
    private final Reference2ObjectLinkedOpenHashMap<ITimedEvent, Task> levelEvents = new Reference2ObjectLinkedOpenHashMap<>();
    private final Predicate<ITimedEvent> expired = event -> {
        if (!event.isExpired()) return false;
        levelEvents.remove(event);
        return true;
    };

    public void tick(TickEvent tickEvent) {
        if (events.isEmpty()) {
            return;
        }

        List<ITimedEvent> stale = new ArrayList<>();
        // Enhanced-for or iterator will cause a concurrent modification.
        for (int i = 0; i < events.size(); i++) {
            ITimedEvent event = events.get(i);
            if (event.isExpired()) {
                stale.add(event);
            } else {
                event.tickEvent(tickEvent);
            }
        }
        this.events.removeAll(stale);
    }

    public void addEvent(ITimedEvent event) {
        Level level = event.level();
        if (level == null) {
            events.add(event);
            return;
        }
        Task task = Schedulers.of(level).repeat(Cadence.EVERY_TICK, event, level.isClientSide ? CLIENT_TICK : SERVER_TICK, expired);
        if (task.isActive()) levelEvents.put(event, task);
    }

    public static EventQueue getServerInstance() {
        if (serverQueue == null)
            serverQueue = new EventQueue();
        return serverQueue;
    }

    public static EventQueue getClientQueue() {
        if (clientQueue == null)
            clientQueue = new EventQueue();
        return clientQueue;
    }


    // Tear down on world unload
    public void clear() {
        for(ITimedEvent event : events){
            event.onServerStopping();
        }
        this.events = new ArrayList<>();
        var it = levelEvents.reference2ObjectEntrySet().fastIterator();
        while (it.hasNext()) {
            var entry = it.next();
            entry.getValue().cancel();
            entry.getKey().onServerStopping();
        }
        levelEvents.clear();
    }

    private void releaseLevel(Level level) {
        if (levelEvents.isEmpty()) return;
        var it = levelEvents.reference2ObjectEntrySet().fastIterator();
        while (it.hasNext()) {
            var entry = it.next();
            if (entry.getKey().level() != level) continue;
            it.remove();
            entry.getValue().cancel();
            entry.getKey().onServerStopping();
        }
    }

    // Split these because our integrated servers are CURSED and both tick.
    private static EventQueue serverQueue;
    private static EventQueue clientQueue;

    private EventQueue() {
        events = new ArrayList<>();
    }

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent e) {

        if (e.phase != TickEvent.Phase.END)
            return;

        EventQueue.getServerInstance().tick(e);
    }

    @SubscribeEvent
    public static void clientTickEvent(TickEvent.ClientTickEvent e) {

        if (e.phase != TickEvent.Phase.END)
            return;
        TickHandler.INSTANCE.tickCounter++;
        EventQueue.getClientQueue().tick(e);
    }

    @SubscribeEvent
    public static void levelUnload(LevelEvent.Unload e) {
        if (!(e.getLevel() instanceof Level level)) return;
        (level.isClientSide ? getClientQueue() : getServerInstance()).releaseLevel(level);
    }
}
