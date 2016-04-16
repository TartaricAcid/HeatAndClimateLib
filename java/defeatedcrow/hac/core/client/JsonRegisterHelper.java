package defeatedcrow.hac.core.client;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.statemap.StateMap;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.google.gson.Gson;

import defeatedcrow.hac.core.ClimateCore;
import defeatedcrow.hac.core.DCInit;
import defeatedcrow.hac.core.DCLogger;
import defeatedcrow.hac.core.base.DCTileBlock;
import defeatedcrow.hac.core.base.ITexturePath;

/**
 * Jsonの登録と生成を行うクラス。<br>
 * 他MODから使用する場合は、現在の開発環境ディレクトリパスを用いてインスタンスを生成して下さい。
 */
@SideOnly(Side.CLIENT)
public class JsonRegisterHelper {
	private final String basePath;

	public JsonRegisterHelper(String s) {
		basePath = s;
	}

	public static final JsonRegisterHelper INSTANCE = new JsonRegisterHelper("E:\\forge1.8.9\\HandC\\src\\main\\resources");

	public void load() {
		regSimpleItem(DCInit.climate_checker, ClimateCore.PACKAGE_ID, "checker", "tool", 0);
	}

	/**
	 * 一枚絵アイコンItemのJson生成と登録をまとめて行うメソッド。
	 */
	public void regSimpleItem(Item item, String domein, String name, String dir, int max) {
		int m = 0;
		while (m < max + 1) {
			INSTANCE.checkAndBuildJson(item, domein, name, dir, m);
			ModelLoader
					.setCustomModelResourceLocation(item, m, new ModelResourceLocation(domein + ":" + dir + "/" + name + m, "inventory"));
			m++;
		}
	}

	/**
	 * 汎用Tile使用メソッド
	 * 外見は仮のJsonファイルに紐付け、TESRで描画する
	 */
	public void regTEBlock(Block block, String domein, String name, String dir, int maxMeta) {
		ModelLoader.setCustomStateMapper(block, (new StateMap.Builder()).ignore(((DCTileBlock) block).FACING, ((DCTileBlock) block).TYPE)
				.build());
		ModelBakery.registerItemVariants(Item.getItemFromBlock(block), new ModelResourceLocation(domein + ":" + "basetile"));
		if (maxMeta == 0) {
			INSTANCE.checkAndBuildJson(block, domein, name, dir, 0);
			ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(block), 0, new ModelResourceLocation(domein + ":" + dir + "/"
					+ name, "inventory"));
		} else {
			for (int i = 0; i < maxMeta + 1; i++) {
				INSTANCE.checkAndBuildJson(block, domein, name, dir, i);
				ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(block), i, new ModelResourceLocation(domein + ":" + dir
						+ "/" + name + i, "inventory"));
			}
		}
	}

	/**
	 * メタ持ちブロックのJson登録とItemBlock用Json生成を行う。
	 * Block、Blockstate用Jsonはここでは生成しない。
	 */
	public void regSimpleBlock(Block block, String domein, String name, String dir, int maxMeta) {
		if (maxMeta == 0) {
			ModelBakery.registerItemVariants(Item.getItemFromBlock(block), new ModelResourceLocation(domein + ":" + dir + "/" + name,
					"type"));
			INSTANCE.checkAndBuildJsonItemBlock(domein, name, dir, 0);
			ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(block), 0, new ModelResourceLocation(domein + ":" + dir + "/"
					+ name, "inventory"));
		} else {
			ModelResourceLocation[] models = new ModelResourceLocation[maxMeta + 1];
			for (int i = 0; i < maxMeta + 1; i++) {
				models[i] = new ModelResourceLocation(domein + ":" + dir + "/" + name + i, "type");
			}
			ModelBakery.registerItemVariants(Item.getItemFromBlock(block), models);
			for (int i = 0; i < maxMeta + 1; i++) {
				INSTANCE.checkAndBuildJsonItemBlock(domein, name, dir, i);
				ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(block), i, new ModelResourceLocation(domein + ":" + dir
						+ "/" + name + i, "inventory"));
			}
		}
	}

	/**
	 * Jsonあれ 1<br>
	 * デバッグモード時に限り、標準的な一枚絵アイコン用のJsonを生成する。既に生成済みの場合は生成処理を行わない。<br>
	 * テクスチャの取得にITexturePathを使用するため、登録するItemに実装する。
	 */
	public void checkAndBuildJson(Object item, String domein, String name, String dir, int meta) {
		if (!(item instanceof ITexturePath))
			return;

		String filePath = null;
		File gj = null;
		boolean find = false;
		boolean tool = false;
		if (item instanceof Item && ((Item) item).isFull3D())
			tool = true;

		try {
			Path path = Paths.get(basePath);
			path.normalize();
			filePath = path.toString() + "\\assets\\" + domein + "\\models\\item\\";
			if (dir != null) {
				filePath += dir + "\\";
			}
			// DCLogger.debugLog("dcs_climate", "current pass " + filePath.toString());
			if (filePath != null) {
				gj = new File(filePath + name + meta + ".json");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (gj.exists()) {
			find = true;
			DCLogger.debugLog("File is found! " + gj.getName());
		}

		if (!find) {
			ITexturePath tex = (ITexturePath) item;

			try {
				Map<String, Object> jsonMap = new HashMap<String, Object>();
				PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(gj.getPath())));
				if (tool) {
					Textures textures = new Textures(tex.getTexPath(meta, false));
					Disp display = new Disp();
					jsonMap.put("parent", "builtin/generated");
					jsonMap.put("textures", textures);
					jsonMap.put("display", display);
				} else {
					Textures textures = new Textures(tex.getTexPath(meta, false));
					Disp2 display = new Disp2();
					jsonMap.put("parent", "builtin/generated");
					jsonMap.put("textures", textures);
					jsonMap.put("display", display);
				}

				Gson gson = new Gson();
				String output = gson.toJson(jsonMap);
				pw.println(output);
				pw.close();
				output = "";
				DCLogger.debugLog("File writed! " + gj.getPath());

			} catch (FileNotFoundException e) {
				DCLogger.debugLog("File not found! " + gj.getPath());
			} catch (IOException e) {
				DCLogger.debugLog("fail");
			}
		}
	}

	/**
	 * Jsonあれ 2<br>
	 * parentに同名のblockmodelを要求するItemBlock用Jsonを生成する。<br>
	 * blockstate用Jsonは生成しないため、ぬくもりある手作りを用いて下さい。
	 */
	public void checkAndBuildJsonItemBlock(String domein, String name, String dir, int meta) {

		String filePath = null;
		File gj = null;
		boolean find = false;
		try {
			Path path = Paths.get(basePath);
			path.normalize();
			filePath = path.toString() + "\\assets\\" + domein + "\\models\\item\\";
			if (dir != null) {
				filePath += dir + "\\";
			}
			// DCLogger.debugLog("dcs_climate", "current pass " + filePath.toString());
			if (filePath != null) {
				gj = new File(filePath + name + meta + ".json");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (gj.exists()) {
			find = true;
			DCLogger.debugLog("File is found! " + gj.getName());
		}

		if (!find) {

			try {
				PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(gj.getPath())));
				Map<String, Object> jsonMap = new HashMap<String, Object>();
				jsonMap.put("parent", domein + ":block/" + dir + "/" + name + meta);

				Gson gson = new Gson();
				String output = gson.toJson(jsonMap);
				pw.println(output);
				pw.close();
				output = "";
				DCLogger.debugLog("File writed! " + gj.getPath());

			} catch (FileNotFoundException e) {
				DCLogger.debugLog("File not found! " + gj.getPath());
			} catch (IOException e) {
				DCLogger.debugLog("fail");
			}
		}
	}

	/**
	 * Jsonあれ 3<br>
	 * デバッグモード時に限り、前面同テクスチャのCubeモデルのJsonを生成する。既に生成済みの場合は生成処理を行わない。<br>
	 * テクスチャの取得にITexturePathを使用するため、登録するblockに実装する。
	 */
	public void checkAndBuildJsonCube(ITexturePath block, String domein, String name, String dir, int meta) {
		if (block == null)
			return;

		String filePath = null;
		File gj = null;
		boolean find = false;
		try {
			Path path = Paths.get(basePath);
			path.normalize();
			filePath = path.toString() + "\\assets\\" + domein + "\\models\\block\\";
			if (dir != null) {
				filePath += dir + "\\";
			}
			// DCLogger.debugLog("dcs_climate", "current pass " + filePath.toString());
			if (filePath != null) {
				gj = new File(filePath + name + meta + ".json");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (gj.exists()) {
			find = true;
			DCLogger.debugLog("File is found! " + gj.getName());
		}

		if (!find) {

			try {
				PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(gj.getPath())));
				Map<String, Object> jsonMap = new HashMap<String, Object>();
				BlockTex textures = new BlockTex(block.getTexPath(meta, false));
				jsonMap.put("parent", domein + ":block/dcs_cube_all");
				jsonMap.put("textures", textures);

				Gson gson = new Gson();
				String output = gson.toJson(jsonMap);
				pw.println(output);
				pw.close();
				output = "";
				DCLogger.debugLog("File writed! " + gj.getPath());

			} catch (FileNotFoundException e) {
				DCLogger.debugLog("File not found! " + gj.getPath());
			} catch (IOException e) {
				DCLogger.debugLog("fail");
			}
		}
	}

	private class Textures {
		String layer0;

		private Textures(String tex) {
			layer0 = tex;
		}
	}

	private class BlockTex {
		String all;

		private BlockTex(String tex) {
			all = tex;
		}
	}

	private class Disp {
		Third thirdperson = new Third();
		First firstperson = new First();
	}

	private class Third {
		int[] rotation = new int[] {
				-90,
				0,
				0 };
		int[] translation = new int[] {
				0,
				1,
				-3 };
		double[] scale = new double[] {
				0.55D,
				0.55D,
				0.55D };
	}

	private class Disp2 {
		Third2 thirdperson = new Third2();
		First firstperson = new First();
	}

	private class Third2 {
		int[] rotation = new int[] {
				0,
				90,
				-35 };
		double[] translation = new double[] {
				0,
				1.25D,
				-3.5D };
		double[] scale = new double[] {
				0.85D,
				0.85D,
				0.85D };
	}

	private class First {
		int[] rotation = new int[] {
				0,
				-135,
				25 };
		int[] translation = new int[] {
				0,
				4,
				2 };
		double[] scale = new double[] {
				1.7D,
				1.7D,
				1.7D };
	}

	/** めもめも。 https://gist.github.com/aksource/9be70a0bef9a46eec468 */

}
