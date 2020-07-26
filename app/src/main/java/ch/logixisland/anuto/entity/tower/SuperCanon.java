package ch.logixisland.anuto.entity.tower;

import android.graphics.Canvas;

import java.util.ArrayList;
import java.util.List;

import ch.logixisland.anuto.R;
import ch.logixisland.anuto.engine.logic.GameEngine;
import ch.logixisland.anuto.engine.logic.entity.Entity;
import ch.logixisland.anuto.engine.logic.entity.EntityFactory;
import ch.logixisland.anuto.engine.render.Layers;
import ch.logixisland.anuto.engine.render.sprite.AnimatedSprite;
import ch.logixisland.anuto.engine.render.sprite.SpriteInstance;
import ch.logixisland.anuto.engine.render.sprite.SpriteTemplate;
import ch.logixisland.anuto.engine.render.sprite.SpriteTransformation;
import ch.logixisland.anuto.engine.render.sprite.SpriteTransformer;
import ch.logixisland.anuto.engine.render.sprite.StaticSprite;
import ch.logixisland.anuto.engine.sound.Sound;
import ch.logixisland.anuto.entity.enemy.Enemy;
import ch.logixisland.anuto.entity.enemy.WeaponType;
import ch.logixisland.anuto.entity.shot.CanonShotMg;
import ch.logixisland.anuto.entity.shot.Shot;
import ch.logixisland.anuto.util.RandomUtils;
import ch.logixisland.anuto.util.math.MathUtils;
import ch.logixisland.anuto.util.math.Vector2;

public class SuperCanon extends Tower implements SpriteTransformation {

    public final static String ENTITY_NAME = "superCanon";
    private final static float SHOT_SPAWN_OFFSET = 0.7f;
    private final static float MG_ROTATION_SPEED = 3f;

    private final static TowerProperties TOWER_PROPERTIES = new TowerProperties.Builder()
            .setValue(94200)
            .setDamage(110000)
            .setRange(4.0f)
            .setReload(0.5f)
            .setMaxLevel(30)
            .setWeaponType(WeaponType.Bullet)
            .setEnhanceBase(1.5f)
            .setEnhanceCost(1250)
            .setEnhanceDamage(750)
            .setEnhanceRange(0.1f)
            .setEnhanceReload(0.01f)
            .build();

    public static class Factory extends EntityFactory {
        @Override
        public Entity create(GameEngine gameEngine) {
            return new Canon(gameEngine);
        }
    }

    public static class Persister extends TowerPersister {

    }

    private static class StaticData {
        SpriteTemplate mSpriteTemplateBase;
        SpriteTemplate mSpriteTemplateCanon;
    }

    private float mAngle = 90f;
    private boolean mReboundActive;
    private final Aimer mAimer = new Aimer(this);

    private SampledFunction mReboundFunction;

    private StaticSprite mSpriteBase;
    private StaticSprite mSpriteCanon;

    private Sound mSound;

    private Canon(GameEngine gameEngine) {
        super(gameEngine, TOWER_PROPERTIES);
        StaticData s = (StaticData) getStaticData();

        mReboundFunction = Function.sine()
                .multiply(REBOUND_RANGE)
                .stretch(GameEngine.TARGET_FRAME_RATE * REBOUND_DURATION / (float) Math.PI)
                .sample();

        mSpriteBase = getSpriteFactory().createStatic(Layers.TOWER_BASE, s.mSpriteTemplateBase);
        mSpriteBase.setListener(this);
        mSpriteBase.setIndex(RandomUtils.next(4));

        mSpriteCanon = getSpriteFactory().createStatic(Layers.TOWER, s.mSpriteTemplateCanon);
        mSpriteCanon.setListener(this);
        mSpriteCanon.setIndex(RandomUtils.next(4));

        mSound = getSoundFactory().createSound(R.raw.gun3_dit);
        mSound.setVolume(0.5f);
    }

    @Override
    public String getEntityName() {
        return ENTITY_NAME;
    }

    @Override
    public Object initStatic() {
        StaticData s = new StaticData();

        s.mSpriteTemplateBase = getSpriteFactory().createTemplate(R.attr.base1, 4);
        s.mSpriteTemplateBase.setMatrix(1f, 1f, null, null);

        s.mSpriteTemplateCanon = getSpriteFactory().createTemplate(R.attr.canon, 4);
        s.mSpriteTemplateCanon.setMatrix(0.4f, 1.0f, new Vector2(0.2f, 0.2f), -90f);

        return s;
    }

    @Override
    public void init() {
        super.init();

        getGameEngine().add(mSpriteBase);
        getGameEngine().add(mSpriteCanon);
    }

    @Override
    public void clean() {
        super.clean();

        getGameEngine().remove(mSpriteBase);
        getGameEngine().remove(mSpriteCanon);
    }

    @Override
    public void tick() {
        super.tick();
        mAimer.tick();

        if (mAimer.getTarget() != null) {
            mAngle = getAngleTo(mAimer.getTarget());

            if (isReloaded()) {
                Shot shot = new CanonShot(this, getPosition(), mAimer.getTarget(), getDamage());
                shot.move(Vector2.polar(SHOT_SPAWN_OFFSET, mAngle));
                getGameEngine().add(shot);
                mSound.play();

                setReloaded(false);
                mReboundActive = true;
            }
        }

        if (mReboundActive) {
            mReboundFunction.step();
            if (mReboundFunction.getPosition() >= GameEngine.TARGET_FRAME_RATE * REBOUND_DURATION) {
                mReboundFunction.reset();
                mReboundActive = false;
            }
        }
    }

    @Override
    public Aimer getAimer() {
        return mAimer;
    }

    @Override
    public void draw(SpriteInstance sprite, SpriteTransformer transformer) {
        transformer.translate(getPosition());
        transformer.rotate(mAngle);

        if (sprite == mSpriteCanon && mReboundActive) {
            transformer.translate(-mReboundFunction.getValue(), 0);
        }
    }

    @Override
    public void preview(Canvas canvas) {
        mSpriteBase.draw(canvas);
        mSpriteCanon.draw(canvas);
    }

    @Override
    public List<TowerInfoValue> getTowerInfoValues() {
        List<TowerInfoValue> properties = new ArrayList<>();
        properties.add(new TowerInfoValue(R.string.damage, getDamage()));
        properties.add(new TowerInfoValue(R.string.reload, getReloadTime()));
        properties.add(new TowerInfoValue(R.string.range, getRange()));
        properties.add(new TowerInfoValue(R.string.inflicted, getDamageInflicted()));
        return properties;
    }
}
