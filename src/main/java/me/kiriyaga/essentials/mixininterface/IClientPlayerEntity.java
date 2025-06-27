package me.kiriyaga.essentials.mixininterface;

public interface IClientPlayerEntity {
    float getLastYawClient();
    void setLastYawClient(float val);

    float getLastPitchClient();
    void setLastPitchClient(float val);

    double getLastXClient();
    void setLastXClient(double val);

    double getLastYClient();
    void setLastYClient(double val);

    double getLastZClient();
    void setLastZClient(double val);

    int getTicksSinceLastPositionPacketSent();
    void setTicksSinceLastPositionPacketSent(int val);
}
