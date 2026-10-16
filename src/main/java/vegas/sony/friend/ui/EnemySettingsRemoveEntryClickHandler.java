package vegas.sony.friend.ui;

import vegas.sony.SonyVegas;
import vegas.sony.friend.Enemy;
import vegas.sony.friend.ui.EnemySettingsFrame;
import vegas.sony.ui.click.component.GuiClickListener;

class EnemySettingsRemoveEntryClickHandler
implements GuiClickListener {
    final Enemy enemy;
    final EnemySettingsFrame frame;

    @Override
    public void onPrimaryClick() {
        SonyVegas.INSTANCE.saveAndStop();
        SonyVegas.INSTANCE.getEnemyManager().removeEnemy(this.enemy);
        this.frame.refreshEntries();
    }

    EnemySettingsRemoveEntryClickHandler(EnemySettingsFrame enemySettingsFrame, Enemy enemy) {
        this.frame = enemySettingsFrame;
        this.enemy = enemy;
    }
}
