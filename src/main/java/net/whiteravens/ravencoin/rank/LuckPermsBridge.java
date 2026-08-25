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

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.InheritanceNode;

/**
 * Every line in this mod that touches the LuckPerms API.
 *
 * <p><b>Nothing outside {@link RankService} may reference this class.</b> It is
 * kept in one file so that the JVM only ever loads it after something has
 * checked LuckPerms is installed — a class is verified when it is first used,
 * and a single stray reference from a class that always loads would turn a
 * missing optional dependency into a {@code NoClassDefFoundError} at startup.
 *
 * <p>Ranks go through the API rather than through {@code lp} typed at the
 * console, because a console command reports failure by printing a line nobody
 * reads, and a rank that was paid for and not granted has to be detectable.
 */
final class LuckPermsBridge {
    /** {@return true if this group exists, so an operator cannot sell nothing} */
    static boolean groupExists(String group) {
        return luckPerms().getGroupManager().getGroup(group) != null;
    }

    /** {@return true if the player already has this group, directly or by inheritance} */
    static boolean hasGroup(UUID playerId, String group) {
        User user = luckPerms().getUserManager().getUser(playerId);
        if (user == null) {
            return false;
        }
        for (Group inherited : user.getInheritedGroups(user.getQueryOptions())) {
            if (inherited.getName().equalsIgnoreCase(group)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds the group and saves.
     *
     * <p>{@code modifyUser} loads, changes and persists in one go, and the future
     * it returns is the only honest signal that the change survived. The caller
     * waits on it before treating the sale as done.
     */
    static CompletableFuture<Void> grant(UUID playerId, String group) {
        InheritanceNode node = InheritanceNode.builder(group).build();
        return luckPerms().getUserManager().modifyUser(playerId, user -> user.data().add(node));
    }

    private static LuckPerms luckPerms() {
        return LuckPermsProvider.get();
    }

    private LuckPermsBridge() {}
}
