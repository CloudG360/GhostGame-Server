package net.cg360.spookums.server.core.data.json.io.parse;

import net.cg360.spookums.server.core.data.json.Json;
import net.cg360.spookums.server.core.data.json.JsonObject;
import net.cg360.spookums.server.core.data.json.io.error.JsonFormatException;

import java.util.ArrayList;

public class ObjectParsingFrame extends ParsingFrame {

    protected Json<JsonObject> holdingObject;

    protected State currentReaderState;

    protected String lastFoundIdentifier;
    protected ArrayList<Character> foundDigits;


    public ObjectParsingFrame() {
        this.holdingObject = Json.from(new JsonObject());

        this.currentReaderState = State.FIRST_AWAITING_IDENTIFIER;

        this.lastFoundIdentifier = null;
        this.foundDigits = new ArrayList<>();
    }


    @Override
    public boolean shouldHoldOnToCharacter(char character) {
        return false;
    }

    @Override
    public void processCharacter(char character) {

        State lastState = this.currentReaderState;

        switch (lastState) {

            case FIRST_AWAITING_IDENTIFIER:
            case AWAITING_IDENTIFIER:
                if(character == ' ') return;
                 throw new JsonFormatException("Unexpected character - expecting quotes for identifier "+getErrorLineNumber());

            case COLON: // can be on new line
                if(character == ' ') return;
                if(character == ':') {
                    this.currentReaderState = State.VALUE;
                    this.foundDigits.clear();
                } else throw new JsonFormatException("Unexpected character - expecting colon after identifier "+getErrorLineNumber());
                break;

            case VALUE: // can be on new line
                //TODO: Accept numbers & update found number
                // with numbers, look for [0-9] initially but continue with [0-9.]
                // this step can skip the comma stage if the comma ends a number, thus terminating the value check.
                // the comma stage more handles after strings or after numbers when spaces follow them.
                break;

            case COMMA: // can be on new line
                if(character == ' ') return;
                if(character == ',') {
                    this.currentReaderState = State.AWAITING_IDENTIFIER;
                } else throw new JsonFormatException("Unexpected character - expecting comma after entry "+getErrorLineNumber());
                break;

        }
    }

    @Override
    public void processConstructedInnerFrame(Json<?> frame) {
        if(frame == null) return; // Comments have no effect. Keeping this check here in case of future changes.
        State lastState = this.currentReaderState;

        switch (lastState) {
            case FIRST_AWAITING_IDENTIFIER:
            case AWAITING_IDENTIFIER:
                if(frame.getValue() instanceof String) {
                    this.lastFoundIdentifier = (String) frame.getValue();
                    this.currentReaderState = State.COLON;

                } else throw new JsonFormatException("Unexpected element - expecting string identifier for entry "+getErrorLineNumber());
                break;

            case COLON:
            case COMMA:
                throw new JsonFormatException("Unexpected element - expecting divider "+getErrorLineNumber());

            case VALUE:

                if(this.hasFoundNumber()) throw new JsonFormatException("Unexpected element - number preceding json element "+getErrorLineNumber());

                this.holdingObject.getValue().addChild(lastFoundIdentifier, frame);
                this.lastFoundIdentifier = null;
                this.currentReaderState = State.COMMA;
                break;
        }
    }

    @Override
    public void initFrame() {
        //Server.getMainLogger().info(String.format("[Parser] '{' used to open object frame (depth %s)", jsonIOInstance.getParseFrameStack().getSize()));
    }


    @Override
    public Json<?> terminateFrame() {
        //Server.getMainLogger().info(String.format("[Parser] '}' used to close object frame (depth %s)", jsonIOInstance.getParseFrameStack().getSize()));
        if(this.currentReaderState == State.FIRST_AWAITING_IDENTIFIER || this.currentReaderState == State.COMMA) return holdingObject;
        else {
            if(this.currentReaderState == State.VALUE && hasFoundNumber()) {
                parseAndAddCollectedDigits();
                return holdingObject;
            }
            throw new JsonFormatException("Unexpected ending - the object was closed after an invalid character "+getErrorLineNumber());
        }
        //TODO: Lock the result
    }

    protected void parseAndAddCollectedDigits() {

    }

    protected boolean hasFoundNumber() {
        return foundDigits.size() > 0;
    }


    @Override
    public char getOpeningCharacter() {
        return '{';
    }

    @Override
    public char getClosingCharacter() {
        return '}';
    }


    protected enum State {

        FIRST_AWAITING_IDENTIFIER, // can terminate here, otherwise, awaiting_identifier.
        AWAITING_IDENTIFIER, // waiting for identifier (string)
        COLON, // waiting for a colon to show up.
        VALUE, // waiting reading a value
        COMMA, // looking for comma or termination.

    }
}
