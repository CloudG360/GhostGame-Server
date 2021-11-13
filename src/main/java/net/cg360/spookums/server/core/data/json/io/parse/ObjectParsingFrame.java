package net.cg360.spookums.server.core.data.json.io.parse;

import net.cg360.spookums.server.core.data.json.Json;
import net.cg360.spookums.server.core.data.json.JsonObject;
import net.cg360.spookums.server.core.data.json.io.JsonIO;

public class ObjectParsingFrame extends ParsingFrame {

    protected Json<JsonObject> holdingObject = null;

    @Override
    public boolean shouldHoldOnToCharacter(char character) {
        return false;
    }

    // TODO - Each line must either be empty space or be a definition line.
    // {

    @Override
    public void processCharacter(char character) {

    }

    @Override
    public void processConstructedInnerFrame(Json<?> frame) {
        // update the current "parameter" line
    }

    @Override
    public void initFrame() {

    }

    // }

    @Override
    public Json<?> terminateFrame() {
        return holdingObject;
    }


    @Override
    public char getOpeningCharacter() {
        return '{';
    }

    @Override
    public char getClosingCharacter() {
        return '}';
    }

}
