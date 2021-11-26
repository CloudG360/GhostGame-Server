package net.cg360.spookums.server.network.packet.auth;

import net.cg360.spookums.server.network.VanillaProtocol;
import net.cg360.spookums.server.network.packet.NetworkPacket;
import net.cg360.spookums.server.util.MicroBoolean;
import net.cg360.spookums.server.util.clean.Check;

// This packet contains a lot of optional peices of data. They are
// ordered based on their apperence in the flags.
public class PacketInUpdateAccount extends NetworkPacket {

    protected boolean createNewAccount = false;

    protected String existingPassword = null;
    protected String newPassword = null;

    protected String existingUsername = null;
    protected String newUsername = null;


    @Override
    protected byte getPacketTypeID() {
        return VanillaProtocol.PACKET_UPDATE_ACCOUNT;
    }

    @Override
    protected int encodeBody() {
        return 0;
    }

    @Override
    protected void decodeBody(int inboundSize) {
        this.getBodyData().reset();

        if(this.getBodyData().canReadBytesAhead(1)) {
            MicroBoolean flags1 = MicroBoolean.from(getBodyData().get());
            // -- Flags:
            //createNewAccount: Is this packet meant to create a new account?
            //existingPassword: Depending on the data, is the original password required for further verification?
            //newPassword: Is a new password being set? (Requires original password if updating)
            //existingUsername: Used to indicate which account is being updated.
            //newUsername: Used to set a new username for an update/new account.

            this.createNewAccount = flags1.getValue(0);

            ifStringFlagTrue(flags1, 1, () -> this.existingPassword = this.getBodyData().getSmallUTF8String());
            ifStringFlagTrue(flags1, 2, () -> this.newPassword = this.getBodyData().getSmallUTF8String());
            ifStringFlagTrue(flags1, 3, () -> this.existingUsername = this.getBodyData().getSmallUTF8String());
            ifStringFlagTrue(flags1, 4, () -> this.newUsername = this.getBodyData().getSmallUTF8String());
            //ifStringFlagTrue(flags1, 5, () -> this.??? = this.getBodyData().getSmallUTF8String());
            //ifStringFlagTrue(flags1, 6, () -> this.??? = this.getBodyData().getSmallUTF8String());
            //ifStringFlagTrue(flags1, 7, () -> this.??? = this.getBodyData().getSmallUTF8String());
        }
    }


    protected void ifStringFlagTrue(MicroBoolean flags, int index, Runnable doThis) {
        if(flags.getValue(index) && this.getBodyData().canReadBytesAhead(1)) {
            doThis.run();
        }
    }


    public boolean isValid() {
        if(this.isCreatingNewAccount())
            return !(Check.isNull(this.getNewUsername()) || Check.isNull(this.getNewPassword()));
        else
            return !Check.isNull(this.getExistingUsername()); // Some changes don't require the password.
    }


    public boolean isCreatingNewAccount() { return createNewAccount; }

    public String getExistingPassword() { return existingPassword; }
    public String getNewPassword() { return newPassword; }

    public String getExistingUsername() { return existingUsername; }
    public String getNewUsername() { return newUsername; }


    protected static boolean isFilledString(String string) {
        return (string != null) && (string.length() > 0);
    }
}
