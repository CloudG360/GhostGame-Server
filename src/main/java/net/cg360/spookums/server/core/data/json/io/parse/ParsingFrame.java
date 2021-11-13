package net.cg360.spookums.server.core.data.json.io.parse;

import net.cg360.spookums.server.core.data.json.Json;
import net.cg360.spookums.server.core.data.json.io.JsonIO;

public abstract class ParsingFrame {

    protected JsonIO jsonIOInstance = null;

    public final void acceptJsonIOInstance(JsonIO io) {
        if(this.jsonIOInstance == null) this.jsonIOInstance = io;
    }

    /**
     * Check if a character is exempt from forming a new parsing frame.
     * An example being an { in a string.
     *
     * @return true to block the character from starting a new frame.
     */
    public abstract boolean shouldHoldOnToCharacter(char character);

    /** Processes a single character from the input. */
    public abstract void processCharacter(char character);

    /** Processes the formation of an encapsulated inner frame. */
    public abstract void processConstructedInnerFrame(Json<?> frame);


    public abstract void initFrame();

    /**
     * Notifies the parsing frame that its closing character has been reached.
     * @return the completed frame.
     */
    public abstract Json<?> terminateFrame();


    public abstract char getOpeningCharacter();
    public abstract char getClosingCharacter();


    public JsonIO getJsonIO() {
        return jsonIOInstance;
    }

    public final String getErrorLineNumber() {
        return getJsonIO().getErrorLineNumber();
    }
}
