package com.xbk.agent.framework.reflection.domain.memory;

import java.util.ArrayList;
import java.util.List;

/**
 * Reflection 记忆容器
 *
 * 职责：累加保存每一轮“代码 -> 评审”记录，供最终结果回放
 *
 * @author xiexu
 */
public class ReflectionMemory {

    private final List<ReflectionTurnRecord> turnRecords = new ArrayList<ReflectionTurnRecord>();

    /**
     * 追加一轮记录。
     *
     * @param turnRecord 单轮记录
     */
    public void addTurnRecord(ReflectionTurnRecord turnRecord) {
        if (turnRecord == null) {
            return;
        }
        turnRecords.add(turnRecord);
    }

    /**
     * 返回当前全部轮次记录。
     *
     * @return 轮次记录快照
     */
    public List<ReflectionTurnRecord> snapshot() {
        return List.copyOf(turnRecords);
    }

    /**
     * 返回当前记录数量。
     *
     * @return 记录数量
     */
    public int size() {
        return turnRecords.size();
    }
}
