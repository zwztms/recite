package cn.bugstack.recite.domain.achievement.model.valueobj;

import java.util.function.Function;

/**
 * 徽章定义值对象 — 硬编码 46 条.
 *
 * <p>condition 接收 UserStatsVO 快照，返回是否满足条件.</p>
 */
public class BadgeDefinition {

    private final String key;
    private final String name;
    private final String description;
    private final String icon;
    private final String category;
    private final boolean hidden;
    private final Function<UserStatsVO, Boolean> condition;

    public BadgeDefinition(String key, String name, String description,
                           String icon, String category, boolean hidden,
                           Function<UserStatsVO, Boolean> condition) {
        this.key = key;
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.category = category;
        this.hidden = hidden;
        this.condition = condition;
    }

    /** 评估当前统计是否满足此徽章条件 */
    public boolean evaluate(UserStatsVO stats) {
        return condition.apply(stats);
    }

    // ---- getters ----

    public String getKey() { return key; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getIcon() { return icon; }
    public String getCategory() { return category; }
    public boolean isHidden() { return hidden; }
    public Function<UserStatsVO, Boolean> getCondition() { return condition; }
}
