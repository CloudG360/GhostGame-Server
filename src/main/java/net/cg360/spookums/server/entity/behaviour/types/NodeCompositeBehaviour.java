package net.cg360.spookums.server.entity.behaviour.types;

import com.google.gson.JsonObject;
import net.cg360.spookums.server.entity.Entity;

public abstract class NodeCompositeBehaviour extends NodeBaseBehaviour {

    @Override
    protected boolean loadParametersFromJson(JsonObject object) {
        return false;
    }
}