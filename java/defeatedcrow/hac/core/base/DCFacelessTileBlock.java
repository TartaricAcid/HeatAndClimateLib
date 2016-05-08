package defeatedcrow.hac.core.base;

import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import defeatedcrow.hac.api.climate.ClimateAPI;
import defeatedcrow.hac.api.climate.DCAirflow;
import defeatedcrow.hac.api.climate.DCHeatTier;
import defeatedcrow.hac.api.climate.DCHumidity;
import defeatedcrow.hac.api.climate.IClimate;
import defeatedcrow.hac.api.recipe.IClimateObject;
import defeatedcrow.hac.api.recipe.IClimateSmelting;
import defeatedcrow.hac.api.recipe.RecipeAPI;
import defeatedcrow.hac.core.ClimateCore;

//TESR持ちブロックのベース
public class DCFacelessTileBlock extends BlockContainer implements IClimateObject, INameSuffix {

	protected Random rand = new Random();

	// Type上限
	public final int maxMeta;
	public final boolean forceUpdate;

	// 同系ブロック共通ﾌﾟﾛﾊﾟﾁｰ
	public static final PropertyInteger TYPE = PropertyInteger.create("type", 0, 15);

	public DCFacelessTileBlock(Material m, String s, int max, boolean force) {
		super(m);
		this.setCreativeTab(ClimateCore.climate);
		this.setUnlocalizedName(s);
		this.setHardness(0.5F);
		this.setResistance(10.0F);
		this.setDefaultState(this.blockState.getBaseState().withProperty(TYPE, 0));
		this.maxMeta = max;
		forceUpdate = force;
	}

	public int getMaxMeta() {
		return maxMeta;
	}

	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumFacing side, float hitX,
			float hitY, float hitZ) {
		return false;
	}

	@Override
	public void onBlockClicked(World world, BlockPos pos, EntityPlayer player) {
	}

	@Override
	public TileEntity createNewTileEntity(World worldIn, int meta) {
		return new DCTileEntity();
	}

	@Override
	public boolean isFullBlock() {
		return false;
	}

	@Override
	public boolean isFullCube() {
		return false;
	}

	@Override
	public boolean isOpaqueCube() {
		return false;
	}

	@Override
	public int getRenderType() {
		return 2;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean addDestroyEffects(World world, BlockPos pos, net.minecraft.client.particle.EffectRenderer effectRenderer) {
		effectRenderer.addBlockDestroyEffects(pos, Blocks.bedrock.getStateFromMeta(0));
		return true;
	}

	@Override
	public void getSubBlocks(Item item, CreativeTabs tab, List<ItemStack> list) {
		for (int i = 0; i < maxMeta + 1; i++) {
			list.add(new ItemStack(this, 1, i));
		}
	}

	// 設置・破壊処理
	@Override
	public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
		TileEntity tile = world.getTileEntity(pos);
		DCTileEntity te = null;
		if (tile != null && tile instanceof DCTileEntity) {
			te = (DCTileEntity) tile;
		}

		if (te != null) {
			NBTTagCompound tag = stack.getTagCompound();
			if (tag != null) {
				te.setNBT(tag);
			}
		}
	}

	@Override
	public void breakBlock(World world, BlockPos pos, IBlockState state) {
		TileEntity tile = world.getTileEntity(pos);
		DCTileEntity te = null;
		if (tile instanceof DCTileEntity) {
			te = (DCTileEntity) tile;
		}
		int i = this.damageDropped(state);
		if (te != null) {
			ItemStack drop = new ItemStack(this, 1, i);
			NBTTagCompound tag = new NBTTagCompound();
			tag = te.getNBT(tag);
			if (tag != null)
				drop.setTagCompound(tag);
			EntityItem entityitem = new EntityItem(world, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, drop);
			float f3 = 0.05F;
			entityitem.motionX = (float) this.rand.nextGaussian() * f3;
			entityitem.motionY = (float) this.rand.nextGaussian() * f3 + 0.2F;
			entityitem.motionZ = (float) this.rand.nextGaussian() * f3;
			world.spawnEntityInWorld(entityitem);
		}
		world.updateComparatorOutputLevel(pos, state.getBlock());
	}

	@Override
	public int damageDropped(IBlockState state) {
		int i = state.getValue(TYPE);
		if (i > maxMeta)
			i = maxMeta;
		return i;
	}

	@Override
	public int quantityDropped(Random random) {
		return 0;
	}

	@Override
	public Item getItemDropped(IBlockState state, Random rand, int fortune) {
		return null;
	}

	// state関連
	@Override
	public IBlockState getStateFromMeta(int meta) {
		int i = meta & 15;
		if (i > maxMeta)
			i = maxMeta;
		IBlockState state = this.getDefaultState().withProperty(TYPE, i);
		return state;
	}

	// state
	@Override
	public int getMetaFromState(IBlockState state) {
		int i = 0;

		i = state.getValue(TYPE);
		if (i > maxMeta)
			i = maxMeta;

		return i;
	}

	@Override
	public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
		return state;
	}

	@Override
	protected BlockState createBlockState() {
		return new BlockState(this, new IProperty[] { TYPE });
	}

	/* climate */
	@Override
	public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand) {
		super.updateTick(worldIn, pos, state, rand);
		if (!worldIn.isRemote && state != null && state.getBlock() != null) {
			IClimate clm = this.onUpdateClimate(worldIn, pos, state);
			if (!this.onClimateChange(worldIn, pos, state, clm) && this.isForcedTickUpdate()) {
				worldIn.scheduleUpdate(pos, this, this.tickRate(worldIn) + rand.nextInt(20));
			}
		}
	}

	@Override
	public IClimate onUpdateClimate(World world, BlockPos pos, IBlockState state) {
		DCHeatTier heat = ClimateAPI.calculator.getHeatTier(world, pos, checkingRange()[0], false);
		DCHumidity hum = ClimateAPI.calculator.getHumidity(world, pos, checkingRange()[1], false);
		DCAirflow air = ClimateAPI.calculator.getAirflow(world, pos, checkingRange()[2], false);
		IClimate c = ClimateAPI.register.getClimateFromParam(heat, hum, air);
		return c;
	}

	@Override
	public boolean onClimateChange(World world, BlockPos pos, IBlockState state, IClimate clm) {
		if (clm != null) {
			DCHeatTier heat = clm.getHeat();
			DCHumidity hum = clm.getHumidity();
			DCAirflow air = clm.getAirflow();
			int meta = this.getMetaFromState(state);
			ItemStack check = new ItemStack(this, 1, meta);
			IClimateSmelting recipe = RecipeAPI.registerSmelting.getRecipe(clm, check);
			if (recipe != null) {
				ItemStack output = recipe.getOutput();
				if (output != null && output.getItem() instanceof ItemBlock) {
					Block ret = ((ItemBlock) output.getItem()).block;
					IBlockState retS = ret.getStateFromMeta(output.getItemDamage());
					if (world.setBlockState(pos, retS, 3)) {
						world.markBlockForUpdate(pos);
						// DCLogger.debugLog("smelting!" + ret.getRegistryName());

						// 効果音
						if (playSEOnChanging(meta)) {
							double d0 = pos.getX();
							double d1 = pos.getY();
							double d2 = pos.getZ();
							world.playSoundEffect(d0 + 0.5D, d1 + 0.5D, d2 + 0.5D, getSEName(meta), 0.8F, 2.0F);
						}
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	public String getSEName(int meta) {
		return "random.fizz";
	}

	@Override
	public boolean playSEOnChanging(int meta) {
		return true;
	}

	@Override
	public String[] getNameSuffix() {
		return null;
	}

	@Override
	public boolean isForcedTickUpdate() {
		return forceUpdate;
	}

	@Override
	public int[] checkingRange() {
		return new int[] {
				2,
				1,
				1 };
	}

}