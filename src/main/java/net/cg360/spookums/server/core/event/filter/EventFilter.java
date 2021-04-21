package net.cg360.spookums.server.core.event.filter;

import net.cg360.spookums.server.core.event.BaseEvent;

public interface EventFilter {

    boolean checkEvent(BaseEvent eventIn);

}
