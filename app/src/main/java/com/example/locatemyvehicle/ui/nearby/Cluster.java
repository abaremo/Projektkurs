package com.example.locatemyvehicle.ui.nearby;

import org.osmdroid.util.GeoPoint;
import java.util.ArrayList;
import java.util.List;

public class Cluster {
    private GeoPoint center;
    private List<GeoPoint> points;

    public Cluster(GeoPoint center) {
        this.center = center;
        this.points = new ArrayList<>();
    }

    public void addPoint(GeoPoint point) {
        points.add(point);
    }

    public int getSize() {
        return points.size();
    }

    public GeoPoint getCenter() {
        return center;
    }

    // Eventuella ytterligare metoder eller egenskaper kan läggas till här
}
