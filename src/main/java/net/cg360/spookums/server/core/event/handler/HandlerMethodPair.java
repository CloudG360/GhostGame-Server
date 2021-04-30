package net.cg360.spookums.server.core.event.handler;

import java.lang.reflect.Method;

public final class HandlerMethodPair {

    private EventHandler annotation;
    private Method method;

    public HandlerMethodPair(EventHandler annotation, Method method) {
        this.annotation = annotation;
        this.method = method;
    }

    public EventHandler getAnnotation() { return annotation; }
    public Method getMethod() { return method; }

}
