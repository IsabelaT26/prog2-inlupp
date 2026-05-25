package se.su.inlupp;

public record RoadInfo(
        Place from,
        Place to,
        String name,
        int distance
) {}