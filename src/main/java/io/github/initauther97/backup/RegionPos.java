package io.github.initauther97.backup;

public record RegionPos(int x, int y) {
    public String getFileName() {
        return "r."+x+"."+y+".mca";
    }
}
