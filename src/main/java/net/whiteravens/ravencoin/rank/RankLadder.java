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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.neoforged.fml.loading.FMLPaths;
import net.whiteravens.ravencoin.RavenCoin;

/**
 * The rank ladder, kept in {@code config/ravencoin-ranks.json}.
 *
 * <p>A plain JSON file rather than the mod's {@code ModConfigSpec}, because a
 * ladder is a list of records and that format only really carries scalars — the
 * usual workaround is to encode each rank into a delimited string, which an
 * operator then has to edit by hand without a typo. This file can also be
 * rewritten by {@code /rc rank set}, so an operator never has to leave the game.
 *
 * <p>Order in the file is the order of the ladder. There is no priority field to
 * disagree with it.
 */
public final class RankLadder {
    private static final String FILE_NAME = "ravencoin-ranks.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final List<Rank> ranks;

    private RankLadder(List<Rank> ranks) {
        this.ranks = new ArrayList<>(ranks);
    }

    /** {@return the ladder on disk, creating an empty one the first time} */
    public static RankLadder load() {
        Path path = path();
        if (!Files.exists(path)) {
            RankLadder empty = new RankLadder(List.of());
            empty.save();
            RavenCoin.LOG.info(
                    "No rank ladder yet — wrote an empty {}. Add rungs with /rc rank set <id> <group> <price>.",
                    FILE_NAME);
            return empty;
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonArray array = root.getAsJsonArray("ranks");
            List<Rank> parsed = new ArrayList<>();
            for (int i = 0; array != null && i < array.size(); i++) {
                JsonObject entry = array.get(i).getAsJsonObject();
                String id = entry.get("id").getAsString();
                String group = entry.get("group").getAsString();
                long price = entry.get("price").getAsLong();
                String name = entry.has("name") ? entry.get("name").getAsString() : id;
                String requires = entry.has("requires") && !entry.get("requires").isJsonNull()
                        ? entry.get("requires").getAsString()
                        : null;
                parsed.add(new Rank(id, group, price, name, requires));
            }
            return new RankLadder(parsed);
        } catch (IOException | RuntimeException broken) {
            // A malformed file must not silently become an empty ladder that then
            // gets saved over the operator's work.
            RavenCoin.LOG.error("Could not read {} — rank sales are off until it is fixed", FILE_NAME, broken);
            return null;
        }
    }

    public void save() {
        JsonArray array = new JsonArray();
        for (Rank rank : this.ranks) {
            JsonObject entry = new JsonObject();
            entry.addProperty("id", rank.id());
            entry.addProperty("group", rank.group());
            entry.addProperty("price", rank.price());
            entry.addProperty("name", rank.name());
            if (rank.requires() != null) {
                entry.addProperty("requires", rank.requires());
            }
            array.add(entry);
        }
        JsonObject root = new JsonObject();
        root.add("ranks", array);

        try (Writer writer = Files.newBufferedWriter(path(), StandardCharsets.UTF_8)) {
            GSON.toJson(root, writer);
        } catch (IOException failed) {
            RavenCoin.LOG.error("Could not write {}", FILE_NAME, failed);
        }
    }

    public List<Rank> ranks() {
        return List.copyOf(this.ranks);
    }

    public Optional<Rank> find(String id) {
        return this.ranks.stream().filter(rank -> rank.id().equals(id)).findFirst();
    }

    /** {@return the rung below this one, or empty if it is the first} */
    public Optional<Rank> below(String id) {
        for (int i = 1; i < this.ranks.size(); i++) {
            if (this.ranks.get(i).id().equals(id)) {
                return Optional.of(this.ranks.get(i - 1));
            }
        }
        return Optional.empty();
    }

    /**
     * {@return true if making {@code id} require {@code target} would close a loop}
     *
     * <p>Walks the prerequisite chain from the proposed target. A cycle would not
     * hang anything — a purchase only ever checks one level — but it would make
     * every rank in the loop permanently unbuyable, with nothing in the game to
     * explain why. Better to refuse it at the moment it is typed.
     */
    public boolean wouldCycle(String id, String target) {
        String current = target;
        for (int guard = 0; current != null && guard <= this.ranks.size(); guard++) {
            if (current.equals(id)) {
                return true;
            }
            current = this.find(current).map(Rank::requires).orElse(null);
        }
        return false;
    }

    /** Adds a rung, or replaces one that already has this id without moving it. */
    public void put(Rank rank) {
        for (int i = 0; i < this.ranks.size(); i++) {
            if (this.ranks.get(i).id().equals(rank.id())) {
                this.ranks.set(i, rank);
                this.save();
                return;
            }
        }
        this.ranks.add(rank);
        this.save();
    }

    public boolean remove(String id) {
        boolean removed = this.ranks.removeIf(rank -> rank.id().equals(id));
        if (removed) {
            this.save();
        }
        return removed;
    }

    private static Path path() {
        return FMLPaths.CONFIGDIR.get().resolve(FILE_NAME);
    }
}
