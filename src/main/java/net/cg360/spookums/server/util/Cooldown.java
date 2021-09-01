package net.cg360.spookums.server.util;

import java.util.UUID;

public class Cooldown {

    protected UUID cooldownToken;
    protected float cooldownDelay;
    protected boolean isCooldownActive;

    protected Runnable actionExpire; // Cooldown expired with a new timer
    protected Runnable actionReplaced; // Cooldown is replaced with a newer timer

    public Cooldown self() { return this; }
    public Cooldown() {
        this.cooldownDelay = 5;
        this.isCooldownActive = false;

        this.actionExpire = () -> { };
        this.actionReplaced = () -> { };
    }


    public synchronized void start() {
        UUID token = UUID.randomUUID();
        float cooldown = self().cooldownDelay;

        new Thread() {
            @Override
            public void run() {
                try {
                    self().isCooldownActive = true;
                    self().cooldownToken = token;

                    long timer = (long) (cooldown * 1000);
                    if(timer > 0) synchronized (this) { this.wait(timer); }

                    synchronized (self()) {
                        if (self().cooldownToken.equals(token)) {
                            self().isCooldownActive = false;
                            self().actionExpire.run();
                        } else {
                            self().actionReplaced.run();
                        }
                    }

                } catch (InterruptedException ignored) {
                } catch (Exception err) {
                    err.printStackTrace();
                }
            }
        }.start();
    }

    public synchronized void invalidate() {
        this.cooldownToken = UUID.randomUUID();
    }


    public Cooldown setCooldownDelay(float cooldownDelay) {
        if(cooldownDelay > 0) this.cooldownDelay = cooldownDelay;
        return this;
    }

    public Cooldown setExpireAction(Runnable actionExpire) {
        this.actionExpire = actionExpire == null ? () -> { } : actionExpire;
        return this;
    }

    public Cooldown setReplacedAction(Runnable actionReplaced) {
        this.actionReplaced = actionReplaced == null ? () -> { } : actionReplaced;
        return this;
    }
}
