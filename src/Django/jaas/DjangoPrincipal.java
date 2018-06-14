package Django.jaas;

import java.security.Principal;

/**
 * Extends the standard jaas principal and is used by the DjangoLoginModule
 * @author Alessia Ventani
 */

public class DjangoPrincipal implements Principal {
    protected String name = null;
    protected String username = null;

    public DjangoPrincipal(String username) {
        super();
        this.name = "person";
        this.username = username;
    }

    /**
     * returns the principals name
     */
    public String getName() {
        return this.name;
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        return this.name.equals(((DjangoPrincipal)obj).getName());
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return this.name.hashCode();
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return "Principal [" + this.name + "] associated with user [" + this.username + "]";
    }
}
