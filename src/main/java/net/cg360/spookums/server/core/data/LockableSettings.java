package net.cg360.spookums.server.core.data;

import net.cg360.spookums.server.util.clean.Immutable;
import net.cg360.spookums.server.core.data.keyvalue.Key;

/**
 * A way of storing settings/properties with
 * the option to lock them.
 *
 * Uses String keys rather than Identifier keys.
 */
public final class LockableSettings extends Settings {

    private boolean isLocked;

    public LockableSettings() {
        super();
        this.isLocked = false;
    }

    /** Used to duplicate a Settings instance.*/
    public LockableSettings(Settings duplicate, boolean lock) {
        super(duplicate);
        this.isLocked = lock;
    }



    /** Prevents setting data within this Settings object. */
    public LockableSettings lock() {
        // Duplicate maps + make them unmodifiable if unlocked still.
        if(!this.isLocked) {
            this.dataMap = Immutable.uMap(this.dataMap, true);
            this.isLocked = true;
        }
        return this;
    }


    /** Sets a key within the settings if they are unlocked. */
    public <T> LockableSettings set(Key<T> key, T value) {
        if(!isLocked) {
            super.set(key, value);
        }
        return this;
    }


    /**
     * Returns a property with the same type as the key. If not
     * present, the object from the 2nd parameter is returned.
     */
    public <T> T getOrElse(Key<T> id, T orElse) {
        return super.getOrElse(id, orElse);
    }

    /**
     * Returns a property with the same type as the key. If not
     * present, null is returned.
     */
    public <T> T get(Key<T> id) {
        return getOrElse(id, null);
    }

    /** Complete duplicate of settings, lock status included. */
    @Override
    public LockableSettings getCopy() {
        return new LockableSettings(this, this.isLocked);
    }

    /** @return a copy of these settings which is unlocked. */
    public LockableSettings getUnlockedCopy() {
        return new LockableSettings(this, false);
    }

    public boolean isLocked() { return isLocked; }
}
