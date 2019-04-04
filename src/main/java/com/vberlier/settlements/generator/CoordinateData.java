package com.vberlier.settlements.generator;

public class CoordinateData {
    private boolean containsLiquids = false;
    private int decorationHeight;

    public void setContainsLiquids(boolean containsLiquids) {
        this.containsLiquids = containsLiquids;
    }

    public boolean containsLiquids() {
        return containsLiquids;
    }

    public void setDecorationHeight(int decorationHeight) {
        this.decorationHeight = decorationHeight;
    }

    public int getDecorationHeight() {
        return decorationHeight;
    }
}
