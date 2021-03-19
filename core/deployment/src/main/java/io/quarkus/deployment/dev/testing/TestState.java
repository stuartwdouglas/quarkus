package io.quarkus.deployment.dev.testing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.UniqueId;

public class TestState {

    final Map<String, Map<UniqueId, TestResult>> resultsByClass = new HashMap<>();

    public synchronized void updateResults(Map<String, Map<UniqueId, TestResult>> latest) {
        for (Map.Entry<String, Map<UniqueId, TestResult>> entry : latest.entrySet()) {
            Map<UniqueId, TestResult> existing = this.resultsByClass.get(entry.getKey());
            if (existing == null) {
                resultsByClass.put(entry.getKey(), entry.getValue());
            } else {
                existing.putAll(entry.getValue());
            }
        }
    }

    public synchronized void classesRemoved(Set<String> classNames) {
        for (String i : classNames) {
            resultsByClass.remove(i);
        }
    }

    public Map<String, Map<UniqueId, TestResult>> getCurrentResults() {
        return Collections.unmodifiableMap(resultsByClass);
    }

    public int getTotalFailures() {
        int count = 0;
        for (Map<UniqueId, TestResult> i : resultsByClass.values()) {
            for (TestResult j : i.values()) {
                if (j.getTestExecutionResult().getStatus() == TestExecutionResult.Status.FAILED) {
                    count++;
                }
            }
        }
        return count;
    }

    public List<TestResult> getHistoricFailures(Map<String, Map<UniqueId, TestResult>> currentResults) {
        List<TestResult> ret = new ArrayList<>();
        for (Map.Entry<String, Map<UniqueId, TestResult>> entry : resultsByClass.entrySet()) {
            for (TestResult j : entry.getValue().values()) {
                if (j.getTestExecutionResult().getStatus() == TestExecutionResult.Status.FAILED) {
                    if (currentResults.containsKey(entry.getKey())) {
                        if (currentResults.get(entry.getKey()).containsKey(j.uniqueId)) {
                            continue;
                        }
                    }
                    ret.add(j);
                }
            }
        }
        return ret;
    }
}
