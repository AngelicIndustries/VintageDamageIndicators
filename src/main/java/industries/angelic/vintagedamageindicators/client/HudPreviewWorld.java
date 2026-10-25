package industries.angelic.vintagedamageindicators.client;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.IProgressUpdate;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.MinecraftException;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.chunk.storage.IChunkLoader;
import net.minecraft.world.storage.IPlayerFileData;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.WorldInfo;

public final class HudPreviewWorld extends World {

    private static final WorldSettings DEFAULT_SETTINGS = new WorldSettings(
        1L,
        WorldSettings.GameType.SURVIVAL,
        true,
        false,
        WorldType.DEFAULT);
    public static final HudPreviewWorld INSTANCE = new HudPreviewWorld();
    private boolean forceDark;

    private HudPreviewWorld() {
        super(new PreviewSaveHandler(), "VDIPreview", new WorldProviderSurface(), DEFAULT_SETTINGS, new Profiler());
        this.provider.setDimension(Integer.MAX_VALUE);
        int providerDim = this.provider.dimensionId;
        this.provider.registerWorld(this);
        this.provider.setDimension(providerDim);
        this.chunkProvider = this.createChunkProvider();
        this.calculateInitialSkylight();
        if (this.difficultySetting == null) {
            this.difficultySetting = EnumDifficulty.NORMAL;
        }
    }

    @Override
    public void notifyBlocksOfNeighborChange(int x, int y, int z, Block block, int side) {}

    @Override
    public void markAndNotifyBlock(int x, int y, int z, Chunk chunk, Block oldBlock, Block newBlock, int flag) {}

    @Override
    public void markBlockForUpdate(int x, int y, int z) {}

    @Override
    public void markBlockRangeForRenderUpdate(int x1, int y1, int z1, int x2, int y2, int z2) {}

    @Override
    public void notifyBlocksOfNeighborChange(int x, int y, int z, Block block) {}

    @Override
    protected IChunkProvider createChunkProvider() {
        return new PreviewChunkProvider(this);
    }

    @Override
    public boolean updateLightByType(EnumSkyBlock type, int x, int y, int z) {
        return true;
    }

    @Override
    protected int func_152379_p() {
        return -1;
    }

    @Override
    public Entity getEntityByID(int id) {
        return null;
    }

    @Override
    public float getLightBrightness(int x, int y, int z) {
        if (this.forceDark) {
            return 0.0F;
        }
        return 1.0F;
    }

    @Override
    public int getLightBrightnessForSkyBlocks(int x, int y, int z, int lightValue) {
        if (this.forceDark) {
            return 0;
        }
        return 15728880;
    }

    @Override
    public int getBlockLightValue(int x, int y, int z) {
        if (this.forceDark) {
            return 0;
        }
        return 15;
    }

    @Override
    public int getBlockLightValue_do(int x, int y, int z, boolean useNeighborBrightness) {
        if (this.forceDark) {
            return 0;
        }
        return 15;
    }

    @Override
    public int getSkyBlockTypeBrightness(EnumSkyBlock type, int x, int y, int z) {
        if (this.forceDark) {
            return 0;
        }
        return 15;
    }

    @Override
    public int getSavedLightValue(EnumSkyBlock type, int x, int y, int z) {
        if (this.forceDark) {
            return 0;
        }
        return 15;
    }

    public boolean isForceDark() {
        return this.forceDark;
    }

    public void setForceDark(boolean forceDark) {
        this.forceDark = forceDark;
    }

    private static final class PreviewChunkProvider implements IChunkProvider {

        private final World world;
        private final Map<Long, Chunk> loadedChunks = new HashMap<>();

        private PreviewChunkProvider(World world) {
            this.world = world;
        }

        @Override
        public boolean chunkExists(int x, int z) {
            return true;
        }

        @Override
        public Chunk provideChunk(int x, int z) {
            Long key = Long.valueOf(chunkKey(x, z));
            Chunk chunk = this.loadedChunks.get(key);
            if (chunk == null) {
                chunk = new Chunk(this.world, x, z);
                this.loadedChunks.put(key, chunk);
            }
            return chunk;
        }

        @Override
        public Chunk loadChunk(int x, int z) {
            return this.loadedChunks.get(Long.valueOf(chunkKey(x, z)));
        }

        @Override
        public void populate(IChunkProvider provider, int x, int z) {}

        @Override
        public boolean saveChunks(boolean saveAll, IProgressUpdate progress) {
            return false;
        }

        @Override
        public boolean unloadQueuedChunks() {
            return false;
        }

        @Override
        public boolean canSave() {
            return false;
        }

        @Override
        public String makeString() {
            return "VDIPreview";
        }

        @Override
        public List getPossibleCreatures(EnumCreatureType creatureType, int x, int y, int z) {
            return Collections.emptyList();
        }

        @Override
        public ChunkPosition func_147416_a(World world, String structureName, int x, int y, int z) {
            return null;
        }

        @Override
        public int getLoadedChunkCount() {
            return this.loadedChunks.size();
        }

        @Override
        public void recreateStructures(int x, int z) {}

        @Override
        public void saveExtraData() {}

        private static long chunkKey(int x, int z) {
            return ((long) x & 0xffffffffL) | (((long) z & 0xffffffffL) << 32);
        }
    }

    private static final class PreviewSaveHandler implements ISaveHandler, IPlayerFileData, IChunkLoader {

        @Override
        public WorldInfo loadWorldInfo() {
            return null;
        }

        @Override
        public void checkSessionLock() throws MinecraftException {}

        @Override
        public IChunkLoader getChunkLoader(WorldProvider provider) {
            return this;
        }

        @Override
        public void saveWorldInfoWithPlayer(WorldInfo worldInformation, NBTTagCompound tagCompound) {}

        @Override
        public void saveWorldInfo(WorldInfo worldInformation) {}

        @Override
        public IPlayerFileData getSaveHandler() {
            return this;
        }

        @Override
        public void flush() {}

        @Override
        public File getWorldDirectory() {
            return null;
        }

        @Override
        public File getMapFileFromName(String mapName) {
            return null;
        }

        @Override
        public String getWorldDirectoryName() {
            return "";
        }

        @Override
        public void writePlayerData(EntityPlayer player) {}

        @Override
        public NBTTagCompound readPlayerData(EntityPlayer player) {
            return null;
        }

        @Override
        public String[] getAvailablePlayerDat() {
            return new String[0];
        }

        @Override
        public Chunk loadChunk(World world, int x, int z) throws IOException {
            return null;
        }

        @Override
        public void saveChunk(World world, Chunk chunk) throws MinecraftException, IOException {}

        @Override
        public void saveExtraChunkData(World world, Chunk chunk) {}

        @Override
        public void chunkTick() {}

        @Override
        public void saveExtraData() {}
    }
}
