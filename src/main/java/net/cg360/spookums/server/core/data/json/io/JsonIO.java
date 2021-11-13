package net.cg360.spookums.server.core.data.json.io;

import net.cg360.spookums.server.core.data.Stack;
import net.cg360.spookums.server.core.data.json.Json;
import net.cg360.spookums.server.core.data.json.JsonObject;
import net.cg360.spookums.server.core.data.json.io.error.JsonFormatException;
import net.cg360.spookums.server.core.data.json.io.error.JsonParseException;
import net.cg360.spookums.server.core.data.json.io.parse.ArrayParsingFrame;
import net.cg360.spookums.server.core.data.json.io.parse.ObjectParsingFrame;
import net.cg360.spookums.server.core.data.json.io.parse.ParsingFrame;
import net.cg360.spookums.server.core.data.json.io.parse.StringParsingFrame;
import net.cg360.spookums.server.util.clean.Check;

import java.io.*;
import java.util.HashMap;
import java.util.Iterator;

public class JsonIO {

    // { : },   [ : ], maybe other ones? Idk, versatile.
    // Pairs the opening character of a frame with a pair holding the closing character and its processing loop.
    protected HashMap<Character,  Class<? extends ParsingFrame>> boundaries;

    // configuration
    protected CommentPolicy commentPolicy; // read
    protected int stackDepth;
    //protected boolean isPrettyPrintingEnabled; // write


    // processing - These could be local variables but I want the frames to have the
    // potential to access them.
    protected Stack<ParsingFrame> parseFrameStack;
    protected JsonObject root;
    protected int currentLine;



    public JsonIO() {
        this.boundaries = new HashMap<>();
        this.boundaries.put('{', ObjectParsingFrame.class);
        this.boundaries.put('[', ArrayParsingFrame.class);
        this.boundaries.put('\'', StringParsingFrame.SingleQuotes.class);
        this.boundaries.put('"', StringParsingFrame.DoubleQuotes.class);

        this.commentPolicy = CommentPolicy.NOT_PERMITTED;
        this.stackDepth = 512;
        //this.isPrettyPrintingEnabled = false;

    }


    // As it's the root, it does not need a Json<?> container to hold a parent.
    public JsonObject read(File file) throws FileNotFoundException {
        Check.nullParam(file, "file");
        FileReader standardReader = new FileReader(file);
        BufferedReader read = new BufferedReader(standardReader);
        Iterator<String> lines = read.lines().iterator();

        StringBuilder compiledText = new StringBuilder();
        while (lines.hasNext()) {
            String line = lines.next();
            if(line.trim().startsWith("//")) {
                if (commentPolicy == CommentPolicy.NOT_PERMITTED)
                    throw new JsonFormatException("Comments are not permitted with the current JsonIO configuration");

            } else {
                compiledText.append(line).append("\n");
            }
        }

        return read(compiledText.toString());
    }

    // newline check -> https://stackoverflow.com/questions/454908/split-java-string-by-new-line
    public JsonObject read(String text) {
        String[] lines  = text.split("\\r?\\n");

        if(lines.length == 0) throw new JsonFormatException("No content was provided");

        this.parseFrameStack = Stack.ofLength(512);
        this.currentLine = 0;
        this.root = null;

        boolean escapeNextCharacter;

        full_loop:
        for(String line: lines) {
            this.currentLine++;
            escapeNextCharacter = false; // escapes don't run over lines.

            for(char letter: line.toCharArray()) {
                if (this.parseFrameStack.isEmpty()) {

                    // Json Object found, stop reading if no more frames are in.
                    if(this.root != null) break full_loop;

                    if(letter != ' ') {
                        if(letter != '{') throw new JsonFormatException("Illegal character found "+getErrorLineNumber());

                        //TODO: Support JsonArray roots
                        ObjectParsingFrame rootParsingFrame = createAndPushNewFrame(ObjectParsingFrame.class);
                    }


                } else {

                    ParsingFrame currentFrame = this.parseFrameStack.peek();

                    // ensures the character is exempt from forming a new frame.
                    if(! (escapeNextCharacter || currentFrame.shouldHoldOnToCharacter(letter)) ) {

                        // ---- check for escape backslashes ----
                        // Forces the next character to be read as a letter if so.
                        // TODO: Factor in for special codes such as \n. \\n should be equivalent to two characters
                        //       "\" and "n" whereas \n should be equivalent to a newline.
                        if(letter == '\\') {
                            escapeNextCharacter = true;
                            continue;
                        }


                        // ---- check for new frame opening (continue) ----
                        // Open a new frame if a type exists for the character.
                        // Then push it so its used.
                        Class<? extends ParsingFrame> frameClass = this.boundaries.get(letter);

                        if(frameClass != null) {
                            createAndPushNewFrame(frameClass);
                            continue;
                        }


                        // ---- if not an opening, check for a closing (continue) ----
                        // Fetch the frame of the top of the stack and terminate it.
                        // Then

                        if(letter == currentFrame.getClosingCharacter()) {
                            ParsingFrame terminatingFrame = this.parseFrameStack.pop();
                            Json<?> branch = terminatingFrame.terminateFrame();

                            // That was the root element, finish up.
                            if(this.parseFrameStack.isEmpty()) {
                                if(branch == null) throw new JsonParseException("Root element parser returned a null tree");

                                //TODO: Support JsonArray roots
                                if(branch.getValue() instanceof JsonObject) {

                                    this.root = (JsonObject) branch.getValue();
                                    return this.root;

                                } else throw new JsonParseException("Root element should've been of type JsonObject");

                            // Otherwise, parent this frame to its parent. Null branches are allowed for
                            // content that shouldn't be added to the tree such as comments.
                            } else {

                                ParsingFrame parentFrame = this.parseFrameStack.peek();
                                if(branch != null) parentFrame.processConstructedInnerFrame(branch); // should I pass this in if null?
                                continue;

                            }
                        }


                        // ---- if neither, pass as letter in next section ----
                    }

                    currentFrame.processCharacter(letter);

                }
            }
        }

        throw new JsonFormatException(String.format("Missing %s closing characters - Check for brackets and quotation marks", this.parseFrameStack.getPointerPos() + 1));
    }


    protected <T extends ParsingFrame> T createAndPushNewFrame(Class<T> frameType) {
        Check.nullParam(frameType, "frameType");

        try {
            T frame = frameType.newInstance();
            frame.acceptJsonIOInstance(this);

            parseFrameStack.push(frame);

            return frame;
        } catch (Exception err) {
            throw new JsonParseException("Broken parsing frame: " + err.getMessage());
        }
    }


    public JsonIO setCommentPolicy(CommentPolicy commentPolicy) {
        this.commentPolicy = commentPolicy;
        return this;
    }

    public JsonIO setStackDepth(int stackDepth) {
        this.stackDepth = stackDepth;
        return this;
    }



    public Stack<ParsingFrame> getParseFrameStack() {
        return parseFrameStack;
    }

    public HashMap<Character, Class<? extends ParsingFrame>> getBoundaries() {
        return boundaries;
    }

    public int getCurrentLine() {
        return currentLine;
    }

    public String getErrorLineNumber() {
        return "(ln "+this.getCurrentLine()+")";
    }
}
