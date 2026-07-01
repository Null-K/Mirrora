package com.puddingkc.Mirrora.model;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;


public class MirrorSelection {

    private Block pos1;
    private BlockFace face1;
    private Block pos2;
    private BlockFace face2;

    public Block getPos1() {
        return pos1;
    }

    public BlockFace getFace1() {
        return face1;
    }

    public Block getPos2() {
        return pos2;
    }

    public BlockFace getFace2() {
        return face2;
    }

    public void setPos1(Block pos1, BlockFace face1) {
        this.pos1 = pos1;
        this.face1 = face1;
    }

    public void setPos2(Block pos2, BlockFace face2) {
        this.pos2 = pos2;
        this.face2 = face2;
    }

    public boolean isComplete() {
        return pos1 != null && pos2 != null;
    }

    public boolean isFaceConsistent() {
        return face1 != null && face1 == face2;
    }
}
