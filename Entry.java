package net.bzstudios.leaderboards;

import java.util.UUID;

public record Entry(UUID uuid, String name, double value) { }
