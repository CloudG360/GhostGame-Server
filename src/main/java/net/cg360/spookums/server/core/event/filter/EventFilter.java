package net.cg360.spookums.server.core.event.filter;

import net.cg360.spookums.server.core.event.type.Event;

public interface EventFilter {

    boolean checkEvent(Event eventIn);

}
