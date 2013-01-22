package jp.co.teng.android.moguratataki;

import jp.co.teng.android.moguratataki.R;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.util.Log;

/**
 * データをロードするためのクラス
 */
public class DataLoader {
	private GameView game;

	private final int imageIdBase = R.drawable.base;
	private final int imageIdMogura = R.drawable.mogura;
	private final int imageIdAtacked = R.drawable.atacked;

	/**
	 * DataLoaderクラスのコンストラクタ
	 * @param game GameViewクラスのインスタンス
	 */
	public DataLoader(GameView game) {
		this.game = game;
	}

	/**
	 * 画像データをロードする
	 * @param context コンテキスト
	 * @return true 正常にロードできた場合。 false 失敗した場合
	 */
	public boolean loadImages(Context context) {
		Resources res = context.getResources();

		//		Bitmap b;
		if ((game.imageBase =
				BitmapFactory.decodeResource(res,imageIdBase)) == null) {
			Log.e("Moguratataki","loadImages err:"+imageIdBase);
			return false;
		}

		//			Bitmap b;
		if ((game.imageMogura =
				BitmapFactory.decodeResource(res,imageIdMogura)) == null) {
			Log.e("Moguratataki","loadImages err:"+imageIdMogura);
			return false;
		}

		//		Bitmap b;
		if ((game.imageAtacked =
				BitmapFactory.decodeResource(res,imageIdAtacked)) == null) {
			Log.e("Moguratataki","loadImages err:"+imageIdAtacked);
			return false;
		}

		return true;
	}
}