package net.cg360.spookums.server.core.data.json.io.parse;

import net.cg360.spookums.server.Server;
import net.cg360.spookums.server.core.data.json.Json;
import net.cg360.spookums.server.core.data.json.JsonArray;
import net.cg360.spookums.server.core.data.json.JsonObject;
import net.cg360.spookums.server.core.data.json.io.error.JsonFormatException;

import java.util.ArrayList;
import java.util.regex.Pattern;

public class ArrayParsingFrame extends ParsingFrame {

    protected Json<JsonArray> holdingArray = null;
    protected State currentReaderState;

    // number recognition
    protected StringBuilder foundDigits;
    protected boolean foundFloatingPoint;

    // boolean recognition
    protected StringBuilder foundBoolLetters;
    protected static final String FALSE = "false";
    protected static final String TRUE  = "true";

    // both
    protected int valueLineNum;



    public ArrayParsingFrame() {
        this.holdingArray = Json.from(new JsonArray());
        this.currentReaderState = State.FIRST_VALUE;

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

            case FIRST_VALUE:
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
        String construct = foundDigits.toString();
        Number number;

        try {
            if (foundFloatingPoint) number = Float.parseFloat(construct);
            else                    number = Integer.parseInt(construct);

        } catch (Exception err) {
            throw new JsonFormatException(String.format("Invalid element [Is decimal? %s] - unable to parse number: %s ", foundFloatingPoint?"true":"false", err.getMessage()) + getErrorLineNumber());
        }

        Json<Number> numberJson = Json.from(number);
        this.holdingArray.getValue().addChild(numberJson);

        this.foundDigits = new StringBuilder();
        this.foundFloatingPoint = false;
    }

    protected void parseAndAddCollectedBool() {
        String construct = foundBoolLetters.toString();

        if(construct.equalsIgnoreCase(TRUE))       this.holdingArray.getValue().addChild(Json.from(true));
        else if(construct.equalsIgnoreCase(FALSE)) this.holdingArray.getValue().addChild(Json.from(false));
        else throw new JsonFormatException("Invalid element - unable to parse boolean "+getErrorLineNumber());

        this.foundBoolLetters = new StringBuilder();
    }


    protected boolean hasFoundNumber() {
        return foundDigits.length() > 0;
    }

    protected boolean hasFoundBoolean() {
        return foundBoolLetters.length() > 0;
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
