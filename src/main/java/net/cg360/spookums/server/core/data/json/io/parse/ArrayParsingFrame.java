package net.cg360.spookums.server.core.data.json.io.parse;

import net.cg360.spookums.server.core.data.json.Json;
import net.cg360.spookums.server.core.data.json.JsonArray;
import net.cg360.spookums.server.core.data.json.JsonObject;

public class ArrayParsingFrame extends ParsingFrame {

    protected Json<JsonArray> holdingArray = null;

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
        return holdingArray;
    }


    @Override
    public char getOpeningCharacter() {
        return '[';
    }

    @Override
    public char getClosingCharacter() {
        return ']';
    }

}
