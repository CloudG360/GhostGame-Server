package net.cg360.spookums.server.core.data.json.io.parse;

import net.cg360.spookums.server.core.data.json.Json;
import net.cg360.spookums.server.core.data.json.JsonObject;
import net.cg360.spookums.server.core.data.json.io.JsonIO;

public abstract class StringParsingFrame extends ParsingFrame {

    protected StringBuilder buildString;

    public StringParsingFrame() {
        this.buildString = new StringBuilder();
    }


    @Override
    public boolean shouldHoldOnToCharacter(char character) {
        // All characters should be within the string, except the terminator.
        // The terminator is processed elsewhere.
        return true;
    }

    @Override
    public void processCharacter(char character) {
        this.buildString.append(character);
    }

    @Override
    public void processConstructedInnerFrame(Json<?> frame) {
        throw new IllegalStateException("Processing of inner frames should not occur inside strings. Implementation error.");
    }

    @Override
    public Json<String> terminateFrame() {
        return Json.from(buildString.toString());
    }


    public static class SingleQuotes extends StringParsingFrame {

        @Override public char getOpeningCharacter() { return '\''; }
        @Override public char getClosingCharacter() { return '\''; }

    }

    public static class DoubleQuotes extends StringParsingFrame {

        @Override public char getOpeningCharacter() { return '"'; }
        @Override public char getClosingCharacter() { return '"'; }

    }

}
