package net.cg360.spookums.server.auth.record;

import net.cg360.spookums.server.util.clean.Check;

public class StoredIdentity {

    private final String accountID;
    private final String passwordHash;
    private final String passwordSalt;
    private final long accountCreationTime;

    public StoredIdentity(String accountID, String passwordHash, String passwordSalt, long accountCreationTime) {
        Check.nullParam(accountID, "accountID");
        Check.nullParam(passwordHash, "passwordHash");
        Check.nullParam(passwordSalt, "passwordSalt");
        Check.nullParam(accountCreationTime, "accountCreationTime");

        this.accountID = accountID;
        this.passwordHash = passwordHash;
        this.passwordSalt = passwordSalt;
        this.accountCreationTime = accountCreationTime;
    }



    public String getAccountID() {
        return accountID;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getPasswordSalt() {
        return passwordSalt;
    }

    public long getAccountCreationTime() {
        return accountCreationTime;
    }
}
