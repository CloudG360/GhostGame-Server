package net.cg360.spookums.server.core.data.json;

public class JsonContainerReachback {

    private Json<? extends JsonContainerReachback> selfContainer;

    protected final void setSelfContainer (Json<? extends JsonContainerReachback> container) {
        if(container.value.equals(this)) this.selfContainer = container;
    }

    protected Json<? extends JsonContainerReachback> getSelf() {
        return selfContainer;
    }
}
