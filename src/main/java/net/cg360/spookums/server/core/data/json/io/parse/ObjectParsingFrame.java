package net.cg360.spookums.server.core.data.json.io.parse;

import net.cg360.spookums.server.core.data.json.Json;
import net.cg360.spookums.server.core.data.json.JsonObject;
import net.cg360.spookums.server.core.data.json.io.error.JsonFormatException;

import java.util.ArrayList;
import java.util.regex.Pattern;

public class ObjectParsingFrame extends ParsingFrame {

    protected Json<JsonObject> holdingObject;
    protected State currentReaderState;

    protected String lastFoundIdentifier;

    // number recognition
    protected StringBuilder foundDigits;
    protected boolean foundFloatingPoint;

    // boolean recognition
    protected StringBuilder foundBoolLetters;
    protected static final String FALSE = "false";
    protected static final String TRUE  = "true";

    // both
    protected int valueLineNum;


    public ObjectParsingFrame() {
        this.holdingObject = Json.from(new JsonObject());
        this.currentReaderState = State.FIRST_AWAITING_IDENTIFIER;

        this.lastFoundIdentifier = null;

        this.foundDigits = new StringBuilder();
        this.foundFloatingPoint = false;

        this.foundBoolLetters = new StringBuilder();

        this.valueLineNum = -1;
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
                } else throw new JsonFormatException("Unexpected character - expecting colon after identifier "+getErrorLineNumber());
                break;


            case VALUE: // can be on new line
                if(hasFoundNumber()) {
                    if(this.valueLineNum != getJsonIO().getCurrentLine()) {
                        this.parseAndAddCollectedDigits();
                        this.currentReaderState = State.COMMA;
                        processCharacter(character);
                        return;
                    }

                    if(character == ' ') {
                        parseAndAddCollectedDigits();
                        this.currentReaderState = State.COMMA;
                        return;
                    }

                    if(character == ',') {
                        parseAndAddCollectedDigits();
                        this.currentReaderState = State.VALUE;
                        return;
                    }

                    if(character == '.') {
                        if(foundFloatingPoint)
                            throw new JsonFormatException("Unexpected character - number has more than one point "+getErrorLineNumber());

                        this.foundFloatingPoint = true;
                        this.foundDigits.append(character);
                        return;
                    }

                    String charSting = Character.toString(character);
                    if (Pattern.matches("[0-9]", charSting)) {
                        this.foundDigits.append(character);
                        return;
                    }


                } else if(hasFoundBoolean()) {

                    if(this.valueLineNum != getJsonIO().getCurrentLine())
                        throw new JsonFormatException("Unexpected line break - expecting boolean/string type "+getErrorLineNumber());

                    this.foundBoolLetters.append(character);
                    String currentTerm = this.foundBoolLetters.toString().toLowerCase();

                    if(TRUE.equalsIgnoreCase(currentTerm) || FALSE.equalsIgnoreCase(currentTerm)) {
                        parseAndAddCollectedBool();
                        this.currentReaderState = State.COMMA;
                        return;
                    }

                    if(! (TRUE.startsWith(currentTerm) || FALSE.startsWith(currentTerm)))
                        throw new JsonFormatException("Unexpected character - expecting boolean/string type " + getErrorLineNumber());


                } else {

                    String charSting = Character.toString(character);
                    if (Pattern.matches("[0-9-]", charSting)) {
                        this.foundDigits.append(character);
                        this.valueLineNum = this.getJsonIO().getCurrentLine();
                        this.currentReaderState = State.VALUE;
                        return;
                    }

                    if(charSting.equalsIgnoreCase("t") || charSting.equalsIgnoreCase("f")) {
                        this.foundBoolLetters.append(charSting);
                        this.valueLineNum = this.getJsonIO().getCurrentLine();
                        this.currentReaderState = State.VALUE;
                        return;
                    }

                    if(character != ' ')
                        throw new JsonFormatException("Unexpected character - expecting a value "+getErrorLineNumber());

                }
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
        String construct = foundDigits.toString();
        Number number;

        try {
            if (foundFloatingPoint) number = Float.parseFloat(construct);
            else                    number = Integer.parseInt(construct);

        } catch (Exception err) {
            throw new JsonFormatException(String.format("Invalid element [Is decimal? %s] - unable to parse number: %s ", foundFloatingPoint?"true":"false", err.getMessage()) + getErrorLineNumber());
        }

        Json<Number> numberJson = Json.from(number);
        this.holdingObject.getValue().addChild(lastFoundIdentifier, numberJson);

        this.foundDigits = new StringBuilder();
        this.foundFloatingPoint = false;
        this.lastFoundIdentifier = null;
    }

    protected void parseAndAddCollectedBool() {
        String construct = foundBoolLetters.toString();

        if(construct.equalsIgnoreCase(TRUE))       this.holdingObject.getValue().addChild(lastFoundIdentifier, Json.from(true));
        else if(construct.equalsIgnoreCase(FALSE)) this.holdingObject.getValue().addChild(lastFoundIdentifier, Json.from(false));
        else throw new JsonFormatException("Invalid element - unable to parse boolean "+getErrorLineNumber());

        this.foundBoolLetters = new StringBuilder();
        this.lastFoundIdentifier = null;
    }


    protected boolean hasFoundNumber() {
        return foundDigits.length() > 0;
    }

    protected boolean hasFoundBoolean() {
        return foundBoolLetters.length() > 0;
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
