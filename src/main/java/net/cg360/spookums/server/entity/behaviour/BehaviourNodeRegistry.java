package net.cg360.spookums.server.entity.behaviour;

import net.cg360.spookums.server.Server;
import net.cg360.spookums.server.entity.behaviour.types.NodeBaseBehaviour;
import net.cg360.spookums.server.entity.behaviour.types.NodeBlueprint;
import net.cg360.spookums.server.util.data.keyvalue.Key;
import net.cg360.spookums.server.util.id.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;

public class BehaviourNodeRegistry {

    private static BehaviourNodeRegistry primaryRegistry;

    private HashMap<String, NodeBlueprint<?>> nodeBlueprints;


    public BehaviourNodeRegistry() {
        this.nodeBlueprints = new HashMap<>();
    }

    /**
     * Sets the registry of the result provided from BehaviourNodeRegistry#get() and
     * finalizes the instance to an extent.
     *
     * Cannot be changed once initially called.
     */
    public void setAsPrimaryRegistry(){
        if(primaryRegistry == null) primaryRegistry = this;
    }



    /** @return the profile's key for use in key lists. Is silent if a duplicate occurs. */
    public <T extends NodeBaseBehaviour> Key<NodeBlueprint<T>> registerProfile(NodeBlueprint<T> blueprint) {
        register(blueprint);
        return blueprint.getKey();
    }

    /** @return true if there was not a profile already registered. */
    public boolean register(NodeBlueprint<?> blueprint) {
        String key = blueprint.getKey().get();

        if(!nodeBlueprints.containsKey(key)) {
            this.nodeBlueprints.put(key, blueprint);
            return true;
        }
        return false;
    }


    @SuppressWarnings("unchecked")
    public <T extends NodeBlueprint<?>> Optional<T> getProfile(Key<T> key) {
        String k = key.get();

        if(nodeBlueprints.containsKey(k)) {
            try {
                T profile = (T) nodeBlueprints.get(k);
                return Optional.of(profile);
            } catch (ClassCastException err) {
                Server.getMainLogger().warn("Tried accessing a NodeBlueprint with the key '%s' however it was the wrong type.");
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    public Optional<NodeBlueprint<?>> getProfile(Identifier identifier) {
        String k = identifier.getID();

        if(nodeBlueprints.containsKey(k)) {
            return Optional.of(nodeBlueprints.get(k));
        }
        return Optional.empty();
    }



    public ArrayList<NodeBlueprint<?>> getGameProfiles() {
        return new ArrayList<>(nodeBlueprints.values());
    }



    /** @return the primary version of this registry. */
    public static BehaviourNodeRegistry get() {
        return primaryRegistry;
    }

}
