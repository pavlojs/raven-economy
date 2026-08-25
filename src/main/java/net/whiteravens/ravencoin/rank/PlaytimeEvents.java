/*
 * Copyright 2026 pavlojs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.whiteravens.ravencoin.rank;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.whiteravens.ravencoin.RavenCoin;

/**
 * Watches how long people have played and hands out the rungs they have earned.
 *
 * <p>Kept apart from the economy's own event class because it is a different
 * concern with a different failure mode: nothing here touches money, so a
 * failure costs nobody anything and is simply retried.
 */
@EventBusSubscriber(modid = RavenCoin.MOD_ID)
public final class PlaytimeEvents {
    /**
     * How often to look, in ticks. Half a minute.
     *
     * <p>Not a config value on purpose. The thresholds are measured in hours, so
     * the only thing this number changes is how long after crossing one a player
     * waits for the message — and every value anyone would pick is fine for that.
     * A knob here would be a knob nobody can set wrongly and nobody needs to set.
     */
    private static final int CHECK_TICKS = 20 * 30;

    /**
     * Promotes on login as well as on the timer.
     *
     * <p>Playtime only accrues while connected, so nobody crosses a threshold
     * while away — but an operator who has just lowered one, or a grant that
     * failed last session, is put right the moment the player is back rather
     * than half a minute later.
     */
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            RankService.promoteForPlaytime(player);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % CHECK_TICKS != 0) {
            return;
        }
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            RankService.promoteForPlaytime(player);
        }
    }

    private PlaytimeEvents() {}
}
