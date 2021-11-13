package net.cg360.spookums.server.core.data.json.io.parse;

import net.cg360.spookums.server.Server;
import net.cg360.spookums.server.core.data.json.Json;
import net.cg360.spookums.server.core.data.json.JsonObject;
import net.cg360.spookums.server.core.data.json.io.JsonIO;
import net.cg360.spookums.server.core.data.json.io.error.JsonFormatException;

public abstract class StringParsingFrame extends ParsingFrame {

    protected StringBuilder buildString;
    protected int parseLine;

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
        if(parseLine != getJsonIO().getCurrentLine()) throw new JsonFormatException("String types must not extend over multiple physical lines "+getErrorLineNumber());
        this.buildString.append(character);
    }

    @Override
    public void processConstructedInnerFrame(Json<?> frame) {
        throw new IllegalStateException("Implementation error - Processing of inner frames should not occur inside strings");
    }

    @Override
    public void initFrame() {
        this.parseLine = getJsonIO().getCurrentLine();
        //Server.getMainLogger().info(String.format("[Parser] quotation used to open string frame (depth %s)", jsonIOInstance.getParseFrameStack().getSize()));
    }

    @Override
    public Json<String> terminateFrame() {
        Json<String> stringJson = Json.from(buildString.toString());
        //Server.getMainLogger().info(String.format("[Parser] quotation used to close string frame (depth %s | result: %s)", jsonIOInstance.getParseFrameStack().getSize(), stringJson.getValue()));
        return stringJson;
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
