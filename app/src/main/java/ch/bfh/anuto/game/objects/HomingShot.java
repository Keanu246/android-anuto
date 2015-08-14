package ch.bfh.anuto.game.objects;

import ch.bfh.anuto.game.GameEngine;

public abstract class HomingShot extends Shot implements GameObject.Listener {

    /*
    ------ Members ------
     */

    protected Enemy mTarget;
    private boolean mReached;

    /*
    ------ Methods ------
     */

    @Override
    public void onClean() {
        super.onClean();
        setTarget(null);
    }

    @Override
    public void onTick() {
        super.onTick();

        if (mEnabled && hasTarget() && getDistanceTo(mTarget) <= mSpeed / GameEngine.TARGET_FRAME_RATE) {
            mReached = true;
            onTargetReached();
        }
    }


    public void setTarget(Enemy target) {
        if (mTarget != null) {
            mTarget.removeListener(this);
        }

        mTarget = target;
        mReached = false;

        if (mTarget != null) {
            mTarget.addListener(this);
        }
    }

    public boolean hasTarget() {
        return mTarget != null;
    }

    protected abstract void onTargetReached();

    protected abstract void onTargetLost();

    /*
    ------ GameObject Listener ------
     */

    @Override
    public void onObjectAdded(GameObject obj) {
    }

    @Override
    public void onObjectRemoved(GameObject obj) {
        if (!mReached) {
            onTargetLost();
        }
    }
}