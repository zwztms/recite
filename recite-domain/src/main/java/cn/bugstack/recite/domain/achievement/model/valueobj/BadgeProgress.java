package cn.bugstack.recite.domain.achievement.model.valueobj;

/**
 * 徽章进度值对象.
 */
public class BadgeProgress {

    private final int current;
    private final int target;
    private final int percent;

    public BadgeProgress(int current, int target) {
        this.current = current;
        this.target = target;
        this.percent = target > 0 ? Math.min(100, current * 100 / target) : 0;
    }

    public int getCurrent() { return current; }
    public int getTarget() { return target; }
    public int getPercent() { return percent; }
}
