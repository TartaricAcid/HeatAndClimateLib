package defeatedcrow.hac.core.event;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.MapGenCaves;

/**
 * MapGenCavesと同様の内容だが、一部置き換えブロックが異なるようになっている
 */
public class MapGenCaveDC extends MapGenCaves {
	protected static final IBlockState BLK_WATER = Blocks.FLOWING_WATER.getDefaultState();

	@Override
	protected boolean isOceanBlock(ChunkPrimer data, int x, int y, int z, int chunkX, int chunkZ) {
		return y > 10 && super.isOceanBlock(data, x, y, z, chunkX, chunkZ);
	}

	@Override
	protected void digBlock(ChunkPrimer data, int x, int y, int z, int chunkX, int chunkZ, boolean foundTop,
			IBlockState state, IBlockState up) {
		net.minecraft.world.biome.Biome biome = world.getBiome(new BlockPos(x + chunkX * 16, 0, z + chunkZ * 16));
		IBlockState top = biome.topBlock;
		IBlockState filler = biome.fillerBlock;

		if (this.canReplaceBlock(state, up) || state.getBlock() == top.getBlock()
				|| state.getBlock() == filler.getBlock()) {
			if (y - 1 < 10) {
				if (biome.getRainfall() >= 0.85F) {
					data.setBlockState(x, y, z, BLK_WATER);
				} else {
					data.setBlockState(x, y, z, BLK_LAVA);
				}
			} else {
				data.setBlockState(x, y, z, BLK_AIR);

				if (foundTop && data.getBlockState(x, y - 1, z).getBlock() == filler.getBlock()) {
					data.setBlockState(x, y - 1, z, top.getBlock().getDefaultState());
				}
			}
		}
	}
}
