package net.cg360.spookums.server.core.event;

import net.cg360.spookums.server.Server;
import net.cg360.spookums.server.core.event.filter.EventFilter;
import net.cg360.spookums.server.core.event.handler.HandlerMethodPair;
import net.cg360.spookums.server.core.event.type.Cancellable;

import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;

public class EventManager {

    private static EventManager primaryManager;

    protected ArrayList<EventFilter> filters; // Filter for EVERY listener.
    protected ArrayList<Listener> listeners;
    protected ArrayList<EventManager> children; // Send events to children too. Only sent if filter is passed.

    public EventManager(EventFilter... filters) {
        this.filters = new ArrayList<>();
        this.listeners = new ArrayList<>();
        this.children = new ArrayList<>();

        this.filters.addAll(Arrays.asList(filters));
    }


    /**
     * Sets the manager the result provided from EventManager#get() and
     * finalizes the instance to an extent.
     *
     * Cannot be changed once initially called.
     */
    public boolean setAsPrimaryManager(){
        if(primaryManager == null) {
            primaryManager = this;
            return true;
        }
        return false;
    }

    public synchronized void call(BaseEvent event) {
        ArrayList<HandlerMethodPair> callList = new ArrayList<>();
        ArrayList<Listener> callListeners = new ArrayList<>(); // Ordered to match call list

        // And this is the part where it's probably the least efficient.
        // Would be great to bake this but then I can't really use the FilteredListener.
        // Could maybe filter each method as I go?
        for(Listener listener: listeners) {

            for(HandlerMethodPair pair : listener.getEventMethods(event)) {

                // Handles generics in class parameters.
                // These would be a nightmare to integrate into the listener's map.
                TypeVariable<? extends Class<?>>[] source = event.getClass().getTypeParameters();
                TypeVariable<? extends Class<?>>[] target = pair.getMethod().getParameterTypes()[0].getTypeParameters();
                if(!(source.length == target.length)) continue; // Not even the right length!

                boolean fail = false;
                for(int i = 0; i < source.length; i++) {
                    TypeVariable<? extends Class<?>> sourceType = source[i];
                    TypeVariable<? extends Class<?>> targetType = target[i];

                    if(!sourceType.getGenericDeclaration().isAssignableFrom(targetType.getGenericDeclaration())){
                        fail = true;
                        break;
                    }
                }

                if(fail) continue;

                // ^ should just be skipped if generics aren't found ^


                boolean added = false;
                int pairPriority = pair.getAnnotation().priority().getValue();
                int originalSize = callList.size();

                for(int i = 0; i < originalSize; i++) {
                    HandlerMethodPair p = callList.get(i);

                    if(pairPriority > p.getAnnotation().priority().getValue()) {
                        callList.add(i, pair);
                        callListeners.add(i, listener);
                        added = true;
                        break;
                    }
                }

                if(!added){
                    callList.add(pair);
                    callListeners.add(listener);
                }
            }
        }

        // Separating them to save a tiny bit more time on each iteration.
        if(event instanceof Cancellable) {
            Cancellable cancellable = (Cancellable) event;

            for (int i = 0; i < callList.size(); i++) {
                HandlerMethodPair methodPair = callList.get(i);
                Listener sourceListener = callListeners.get(i);

                // Skip if cancelled and ignoring cancelled.
                if(cancellable.isCancelled() && methodPair.getAnnotation().ignoreIfCancelled()) continue;
                invokeEvent(sourceListener, event, methodPair);
            }

        } else {

            for (int i = 0; i < callList.size(); i++) {
                HandlerMethodPair methodPair = callList.get(i);
                Listener sourceListener = callListeners.get(i);

                invokeEvent(sourceListener, event, methodPair);
            }
        }

    }


    /**
     * Registers a listener to this EventManager
     * @param listener the listener to be registered.
     * @return listener for storing an instance.
     */
    public synchronized Listener addListener(Listener listener) {
        removeListener(listener, true);
        // Check that it isn't duped by clearing it.
        // If someone has used the same object instance to create two objects and overrided
        // #equals(), that's their problem.
        listeners.add(listener);
        return listener;
    }

    /**
     * Removes listener from this EventManager and any child EventManager's
     * @param listener the listener to be removed.
     */
    public synchronized void removeListener(Listener listener) {
        removeListener(listener, true);
    }

    /**
     * Removes a listener from the managers listener list.
     * @param listener the listener to be removed.
     * @param removeFromChildren should instances of this listener be removed in child EventManager's ?
     */
    public synchronized void removeListener(Listener listener, boolean removeFromChildren) {
        listeners.remove(listener);

        if(removeFromChildren) {

            for (EventManager child : children) {
                child.removeListener(listener, true); // Ensure children don't include it either.
            }
        }
    }


    private static void invokeEvent(Listener owningListener, BaseEvent event, HandlerMethodPair methodPair) {
        try {
            methodPair.getMethod().invoke(owningListener.getSourceObject(), event);

        } catch (Exception err) {
            Server.getMainLogger().error("An error was thrown during the invocation of an event.");
            err.printStackTrace();
        }
    }



    /** @return the primary instance of the EventManager. */
    public static EventManager get(){
        return primaryManager;
    }

    public synchronized EventFilter[] getFilters() { return filters.toArray(new EventFilter[0]); }
    public synchronized Listener[] getListeners() { return listeners.toArray(new Listener[0]); }
    public synchronized EventManager[] getChildren() { return children.toArray(new EventManager[0]); }
}
