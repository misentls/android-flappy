package egx.game.fbird.yaoming;
/**
 * Copyright (C) 2013 Martin Varga <android@kul.is>
 */
import egx.game.fbird.yaoming.R;
import com.startapp.android.publish.StartAppAd;

import java.io.IOException;

import org.andengine.engine.Engine;
import org.andengine.engine.LimitedFPSEngine;
import org.andengine.engine.camera.Camera;
import org.andengine.engine.handler.timer.ITimerCallback;
import org.andengine.engine.handler.timer.TimerHandler;
import org.andengine.engine.options.EngineOptions;
import org.andengine.engine.options.ScreenOrientation;
import org.andengine.engine.options.WakeLockOptions;
import org.andengine.engine.options.resolutionpolicy.FillResolutionPolicy;
import org.andengine.engine.options.resolutionpolicy.IResolutionPolicy;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.sprite.Sprite;
import org.andengine.opengl.texture.TextureOptions;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory;
import org.andengine.opengl.texture.region.TextureRegion;
import org.andengine.opengl.util.GLState;
import org.andengine.ui.activity.LayoutGameActivity;
import org.andengine.util.debug.Debug;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public class GameActivity extends LayoutGameActivity {

	private Camera camera;

	private GameScene gameScene;
	private Scene splashScene;
	private Sprite splash;

	private BitmapTextureAtlas texImage;
	private TextureRegion regImage;

	SharedPreferences prefs;
	private StartAppAd startAppAd;

	@Override
	public Engine onCreateEngine(EngineOptions pEngineOptions) {
		startAppAd = new StartAppAd(this);
		Engine engine = new LimitedFPSEngine(pEngineOptions,
				Constants.FPS_LIMIT);
		return engine;
	}

	public void setHighScore(int score) {
		SharedPreferences.Editor settingsEditor = prefs.edit();
		settingsEditor.putInt(Constants.KEY_HISCORE, score);
		settingsEditor.commit();
	}

	public int getHighScore() {
		return prefs.getInt(Constants.KEY_HISCORE, 0);
	}

	@Override
	public EngineOptions onCreateEngineOptions() {

		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		camera = new FollowCamera(0, 0, Constants.CW, Constants.CH);
		IResolutionPolicy resolutionPolicy = new FillResolutionPolicy();
		EngineOptions engineOptions = new EngineOptions(true,
				ScreenOrientation.PORTRAIT_FIXED, resolutionPolicy, camera);
		engineOptions.getAudioOptions().setNeedsMusic(true).setNeedsSound(true);
		engineOptions.setWakeLockOptions(WakeLockOptions.SCREEN_ON);
		return engineOptions;
	}

	@Override
	public void onCreateResources(
			OnCreateResourcesCallback pOnCreateResourcesCallback)
			throws IOException {
		ResourceManager.getInstance().create(this, getEngine(), camera,
				getVertexBufferObjectManager());
		ResourceManager.getInstance().loadFont();
		ResourceManager.getInstance().loadGameResources();

		texImage = new BitmapTextureAtlas(this.getTextureManager(), 1024, 1024,
				TextureOptions.BILINEAR_PREMULTIPLYALPHA);
		regImage = BitmapTextureAtlasTextureRegionFactory.createFromAsset(
				texImage, this.getAssets(), "splash.jpg", 0, 0);
		texImage.load();
		pOnCreateResourcesCallback.onCreateResourcesFinished();
	}

	@Override
	public void onCreateScene(OnCreateSceneCallback pOnCreateSceneCallback)
			throws IOException {
		StartAppAd.init(this, "103326818", "203475122");
		initSplashScene();
		pOnCreateSceneCallback.onCreateSceneFinished(this.splashScene);
	}

	@Override
	public void onPopulateScene(Scene pScene,
			OnPopulateSceneCallback pOnPopulateSceneCallback)
			throws IOException {

		mEngine.registerUpdateHandler(new TimerHandler(1.5f,
				new ITimerCallback() {
					public void onTimePassed(final TimerHandler pTimerHandler) {
						mEngine.unregisterUpdateHandler(pTimerHandler);
						loadScenes();
						splash.detachSelf();
						mEngine.setScene(gameScene);
						if (gameScene != null)
							gameScene.reset();
					}
				}));
		pOnPopulateSceneCallback.onPopulateSceneFinished();
	}

	@Override
	public synchronized void onResumeGame() {
		super.onResumeGame();
		startAppAd.onResume();
		if (gameScene != null)
			gameScene.resume();
	}

	@Override
	public void onBackPressed() {
		startAppAd.onBackPressed();
		super.onBackPressed();
	}

	private void loadScenes() {
		gameScene = new GameScene();
	}

	@Override
	public synchronized void onPauseGame() {
		super.onPauseGame();
		// startAppAd.onPause();
		if (gameScene != null)
			gameScene.pause();
	}

	private void initSplashScene() {
		Log.d("-------initSplashScene()---------", " ");
		splashScene = new Scene();
		splash = new Sprite(0, 0, regImage, ResourceManager.getInstance().vbom) {
			@Override
			protected void preDraw(GLState pGLState, Camera pCamera) {
				super.preDraw(pGLState, pCamera);
				pGLState.enableDither();
			}
		};

		splash.setPosition(Constants.CW / 2, (Constants.CH / 2));
		splashScene.attachChild(splash);
	}

	public void gotoPlayStore() {
		try {
			Intent i = new Intent(Intent.ACTION_VIEW,
					Uri.parse("market://details?id="
							+ getString(R.string.google_play_app_id)));
			ResourceManager.getInstance().activity.startActivity(i);

		} catch (Exception ex) {
			Debug.w("Google Play Store not installed");
			Intent i = new Intent(Intent.ACTION_VIEW,
					Uri.parse("http://play.google.com/store/apps/details?id="
							+ getString(R.string.google_play_app_id)));
			ResourceManager.getInstance().activity.startActivity(i);
		}

	}

	public boolean isVisibleFacebook() {
		PackageManager pkManager = getPackageManager();
		try {
			PackageInfo pkgInfo = pkManager.getPackageInfo(
					"com.facebook.katana", 0);
			String getPkgInfo = pkgInfo.toString();
			if (getPkgInfo.equals("com.facebook.katana")) {
				toastOnUiThread(
						"Facebook not found, Please install app before using action",
						Toast.LENGTH_SHORT);
				return false;
			} else {
				return true;
			}
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		return false;
	}

	public void faceShare() {
		String urlToShare = "http://play.google.com/store/apps/details?id="
				+ getString(R.string.google_play_app_id);
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.setType("text/plain");
		intent.putExtra(Intent.EXTRA_TEXT, urlToShare);
		intent.setPackage("com.facebook.katana");
		startActivity(intent);
	}

	@Override
	protected int getLayoutID() {
		// TODO Auto-generated method stub
		return R.layout.activity_game;
	}

	@Override
	protected int getRenderSurfaceViewID() {
		// TODO Auto-generated method stub
		return R.id.game_rendersurfaceview;
	}

}
