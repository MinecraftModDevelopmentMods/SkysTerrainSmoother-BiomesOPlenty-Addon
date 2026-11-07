package zone.moddev.mc.skysterrainsmootherbop.probe;

/** Scoped failure diagnostics; it observes loads without requesting any chunks. */
final class ChunkLoadTrace {
    static boolean active;

    @net.neoforged.bus.api.SubscribeEvent
    public static void loaded(net.neoforged.neoforge.event.level.ChunkEvent.Load event) {
        if (active) {
            zone.moddev.mc.skysterrainsmoother.SkysTerrainSmoother.logger.info("BOP_PROBE_CHUNK_LOAD " + event.getChunk().getPos());
            new Exception("Scoped chunk-load caller").printStackTrace(System.out);
        }
    }
}
