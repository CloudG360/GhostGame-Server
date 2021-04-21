package net.cg360.spookums.server.core.data.id;

import java.util.Objects;
import java.util.regex.Pattern;

public class Namespace {

    private String namespace;

    public Namespace(String namespace) {

         if(nullCheck(namespace)) {

            if(namespace.contains(":")) {
                String n = namespace.split(Pattern.quote(":"))[0];

                if(nullCheck(n)){
                    this.namespace = n.trim().toLowerCase();
                }

            } else {
                // Not spaces, no colons, all checks passed.
                this.namespace = namespace.trim().toLowerCase();
            }
        }

    }

    /**
     * Little constructor utility method for checking if the namespace is
     * valid, else replacing it with unknown.
     * @param n the namespace in
     * @return has the namespace been left the same?
     */
    protected boolean nullCheck(String n) {

        // Check it isn't null, length 0, or just spaces.
        if(n == null || n.trim().length() == 0) {
            namespace = "unknown";
            return false;
        }
        return true;
    }

    /**
     * @param id the id to be tied to the namespace.
     * @return an identifier using this namespace.
     */
    public Identifier id(String id) {
        return new Identifier(this, id);
    }



    /** @return the string of the namespace. */
    public String getNamespaceString() {
        return namespace;
    }


    /** @return the result of Namespace#getNamespaceString() */
    @Override
    public String toString() {
        return getNamespaceString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Namespace namespace1 = (Namespace) o;
        return Objects.equals(namespace, namespace1.namespace);
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespace);
    }
}
