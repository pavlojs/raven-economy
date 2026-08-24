package net.whiteravens.ravencoin;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The White Ravens Forge economy.
 *
 * <p>Everything here is server-authoritative: balances live in the server's own
 * saved data and a client is never trusted with a number it could edit. The
 * client half exists only to draw the ATM screen and the shop screen.
 *
 * <p>Scope for the first version is set out in the README. The short form: one
 * currency that is also a physical item, an ATM that moves coins between hand
 * and account, player and server shops, ranks bought through the LuckPerms API,
 * and a leaderboard by balance. Every feature is switchable off, because the
 * first live season is what will tell us which of them were a good idea.
 */
@Mod(RavenCoin.MOD_ID)
public class RavenCoin {
    public static final String MOD_ID = "ravencoin";
    public static final Logger LOG = LoggerFactory.getLogger("RavenCoin");

    public RavenCoin(IEventBus modBus, ModContainer container) {
        LOG.info("RavenCoin loading");
    }
}
