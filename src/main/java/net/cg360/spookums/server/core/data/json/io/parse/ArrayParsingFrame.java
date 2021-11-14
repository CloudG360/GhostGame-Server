package net.cg360.spookums.server.core.data.json.io.parse;

import net.cg360.spookums.server.Server;
import net.cg360.spookums.server.core.data.json.Json;
import net.cg360.spookums.server.core.data.json.JsonArray;
import net.cg360.spookums.server.core.data.json.JsonObject;
import net.cg360.spookums.server.core.data.json.io.error.JsonFormatException;

import java.util.ArrayList;

public class ArrayParsingFrame extends ParsingFrame {

    protected Json<JsonArray> holdingArray = null;

    protected State currentReaderState;
    protected ArrayList<Character> foundDigits;


    public ArrayParsingFrame() {
        this.holdingArray = null;

        this.currentReaderState = State.FIRST_VALUE;
        this.foundDigits = new ArrayList<>();
    }



    @Override
    public boolean shouldHoldOnToCharacter(char character) {
        return false;
    }

    @Override
    public void processCharacter(char character) {
        if(character == ' ') return; // All spaces are legal

        State lastState = this.currentReaderState;

        switch (lastState) {

            case FIRST_VALUE:
            case VALUE: // can be on new line
                //TODO: Accept numbers - CHANGE TO "VALUE" WHEN FIRST NUMBER FOUND
                // with numbers, look for [0-9.]
                // this step can skip the comma stage if the comma ends a number, thus terminating the value check.
                // the comma stage more handles after strings or after numbers when spaces follow them.
                break;

            case COMMA: // can be on new line
                if(character == ',') {
                    this.currentReaderState = State.VALUE;
                } else throw new JsonFormatException("Unexpected character - expecting comma after entry "+getErrorLineNumber());
                break;

        }
    }

    @Override
    public void processConstructedInnerFrame(Json<?> frame) {
        if(frame == null) return; // Comments have no effect. Keeping this check here in case of future changes.
        State lastState = this.currentReaderState;

        switch (lastState) {
            case FIRST_VALUE:
            case VALUE:

                this.holdingArray.getValue().addChild(frame);
                this.currentReaderState = State.COMMA;
                break;

            case COMMA:
                throw new JsonFormatException("Unexpected element - expecting comma "+getErrorLineNumber());
        }
    }

    @Override
    public void initFrame() {
        //Server.getMainLogger().info(String.format("[Parser] '[' used to open array frame (depth %s)", jsonIOInstance.getParseFrameStack().getSize()));
    }

    @Override
    public Json<?> terminateFrame() {
        //Server.getMainLogger().info(String.format("[Parser] ']' used to close array frame (depth %s)", jsonIOInstance.getParseFrameStack().getSize()));
        if((this.currentReaderState == State.VALUE) && hasFoundNumber()) {
            parseAndAddCollectedDigits();
            return holdingArray;
        }

        if(this.currentReaderState == State.FIRST_VALUE || this.currentReaderState == State.COMMA) return holdingArray;
        else throw new JsonFormatException("Unexpected ending - the object was closed after an invalid character "+getErrorLineNumber());
        //TODO: Lock the result
    }


    protected void parseAndAddCollectedDigits() {

    }

    protected boolean hasFoundNumber() {
        return foundDigits.size() > 0;
    }


    @Override
    public char getOpeningCharacter() {
        return '[';
    }

    @Override
    public char getClosingCharacter() {
        return ']';
    }



    protected enum State {

        FIRST_VALUE, // can terminate here, otherwise, awaiting_identifier.
        VALUE, // waiting for identifier (string)
        COMMA, // looking for comma or termination.

    }

}
