package net.cg360.spookums.server.util;

import java.util.regex.Pattern;

public class Identifier {

    private String namespace;
    private String id;

    public Identifier(String namespace, String id) {
        this.namespace = namespace.toLowerCase().trim();
        this.id = id.toLowerCase().trim().replaceAll(Pattern.quote(" "), "_");
    }

    public String get() { return namespace + ":" +id; }

    public String getNamespaceComponent() { return namespace; }
    public String getIdentifierComponent() { return id; }
}
