package com.puddingkc.Mirrora.manager;

import com.puddingkc.Mirrora.model.MirrorSelection;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


public class SelectionManager {

    private final Map<UUID, MirrorSelection> selections = new ConcurrentHashMap<>();

    public MirrorSelection getOrCreate(UUID playerId) {
        return selections.computeIfAbsent(playerId, id -> new MirrorSelection());
    }

    public MirrorSelection get(UUID playerId) {
        return selections.get(playerId);
    }

    public void clear(UUID playerId) {
        selections.remove(playerId);
    }
}
