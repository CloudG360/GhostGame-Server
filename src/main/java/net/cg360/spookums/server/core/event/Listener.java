package net.cg360.spookums.server.core.event;


import net.cg360.spookums.server.core.event.handler.EventHandler;
import net.cg360.spookums.server.core.event.handler.HandlerMethodPair;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;

// This whole class probably isn't an efficient EventListener
// implementation. If anyone has any suggestions, create a PR. All
// that needs to stay intact is the FilteredListener's added filter
// behaviour
//TODO: Rewrite this as an interface maybe? Would me more in-line
public class Listener {

    // Event Class -> Method+Annotation list
    private HashMap<Class<? extends BaseEvent>, ArrayList<HandlerMethodPair>> listenerMethods;
    private Object sourceObject;

    public Listener(Object sourceObject) {
        this.listenerMethods = new HashMap<>();
        this.sourceObject = sourceObject;

        // Get event listening methods.
        for(Method method : this.sourceObject.getClass().getMethods()) {

            if(method.isAnnotationPresent(EventHandler.class)) {
                EventHandler annotation = method.getAnnotation(EventHandler.class);
                Parameter[] parameters = method.getParameters();

                if(parameters.length == 1){
                    Class<?> type = parameters[0].getType();
                    ArrayList<Class<? extends BaseEvent>> eventClasses = new ArrayList<>();

                    HandlerMethodPair pair = new HandlerMethodPair(annotation, method);
                    checkAndAdd(type, eventClasses); // Get all the categories this method would be in.

                    for(Class<? extends BaseEvent> cls: eventClasses) {

                        if(!listenerMethods.containsKey(cls)){
                            listenerMethods.put(cls, new ArrayList<>()); // Create new handler list if it doesn't exist.
                        }
                        listenerMethods.get(cls).add(pair);
                    }
                }
            }
        }
    }

    /**
     * Calls EventManager's #addListener() method.
     * @param manager the manager to add this listener to.
     */
    public final void registerListener(EventManager manager) {
        manager.addListener(this);
    }

    /**
     * Calls EventManager's #removeListener() method.
     * @param manager the manager to remove this listener from.
     */
    public final void unregisterListener(EventManager manager) {
        unregisterListener(manager, true);
    }

    /**
     * Calls EventManager's #removeListener() method.
     * @param manager the manager to remove this listener from.
     * @param removeFromChildren should child managers have this listener removed?
     */
    public final void unregisterListener(EventManager manager, boolean removeFromChildren) {
        manager.removeListener(this, removeFromChildren);
    }

    /**
     * Checks to see if a class extends an event, adding it to a list of
     * identified classes. It crawls through all the superclasses and interfaces
     * of a class with recursion.
     * @param classIn the class to be checked.
     * @param list a list of all the previously checked classes.
     */
    @SuppressWarnings("unchecked") // It's checked with Class#isAssaignableFrom() :)
    private static void checkAndAdd(Class<?> classIn, ArrayList<Class<? extends BaseEvent>> list) {
        if(classIn == null) return;

        if(BaseEvent.class.isAssignableFrom(classIn)){
            list.add((Class<? extends BaseEvent>) classIn);
            checkAndAdd(classIn.getSuperclass(), list);

            for(Class<?> cls: classIn.getInterfaces()) {
                checkAndAdd(cls, list);
            }
        }
    }

    /**
     * @param event the event received.
     * @return all the methods that are triggered by the specified event.
     */
    public ArrayList<HandlerMethodPair> getEventMethods(BaseEvent event) {
        ArrayList<HandlerMethodPair> ls = listenerMethods.get(event.getClass());

        return ls == null ? new ArrayList<>() : new ArrayList<>(ls); // Empty list if no methods exist. Otherwise clone.
    }

    /**
     * @return the source object this listener hooks into the event system.
     */
    public Object getSourceObject() {
        return sourceObject;
    }

    @Override
    public boolean equals(Object o) {

        if(o instanceof Listener) {
            Listener oListener = (Listener) o;

            // Compare the source objects instead.
            return oListener.getSourceObject().equals( this.getSourceObject() );
        }
        return false;
    }
}
