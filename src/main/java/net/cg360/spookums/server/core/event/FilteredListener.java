package net.cg360.spookums.server.core.event;

import net.cg360.spookums.server.core.event.filter.EventFilter;
import net.cg360.spookums.server.core.event.handler.HandlerMethodPair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FilteredListener extends Listener {

    private List<EventFilter> filters;

    public FilteredListener(Object sourceObject, EventFilter... eventFilters) {
        super(sourceObject);
        this.filters = new ArrayList<>();

        this.filters.addAll(Arrays.asList(eventFilters));
    }

    @Override
    public ArrayList<HandlerMethodPair> getEventMethods(BaseEvent event) {
        ArrayList<HandlerMethodPair> methods = super.getEventMethods(event);
        // Pass to filters. They can edit the map.
        return methods;
    }

    public List<EventFilter> getFilters() {
        return new ArrayList<>(filters);
    }
}
