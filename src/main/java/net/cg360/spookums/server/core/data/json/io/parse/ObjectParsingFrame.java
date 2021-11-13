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

    @Override
    public void processCharacter(char character) {

    }

    @Override
    public void processConstructedInnerFrame(Json<?> frame) {

    }

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
