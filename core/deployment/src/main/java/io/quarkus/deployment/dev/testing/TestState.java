package io.quarkus.deployment.dev.testing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.UniqueId;

public class TestState {

    final Map<String, Map<UniqueId, TestResult>> resultsByClass = new HashMap<>();

    public List<String> getClassNames() {
        return new ArrayList<>(resultsByClass.keySet()).stream().sorted().collect(Collectors.toList());
    }

    public List<PerClassResult> getPassingClasses() {
        List<PerClassResult> ret = new ArrayList<>();
        for (Map.Entry<String, Map<UniqueId, TestResult>> i : resultsByClass.entrySet()) {
            List<TestResult> passing = new ArrayList<>();
            List<TestResult> failing = new ArrayList<>();
            for (TestResult j : i.getValue().values()) {
                if (j.getTestExecutionResult().getStatus() == TestExecutionResult.Status.FAILED) {
                    failing.add(j);
                } else {
                    passing.add(j);
                }
            }
            if (failing.isEmpty()) {
                PerClassResult p = new PerClassResult(i.getKey(), passing, failing);
                ret.add(p);
            }
        }

        Collections.sort(ret);
        return ret;
    }

    public List<PerClassResult> getFailingClasses() {
        List<PerClassResult> ret = new ArrayList<>();
        for (Map.Entry<String, Map<UniqueId, TestResult>> i : resultsByClass.entrySet()) {
            List<TestResult> passing = new ArrayList<>();
            List<TestResult> failing = new ArrayList<>();
            for (TestResult j : i.getValue().values()) {
                if (j.getTestExecutionResult().getStatus() == TestExecutionResult.Status.FAILED) {
                    failing.add(j);
                } else {
                    passing.add(j);
                }
            }
            if (!failing.isEmpty()) {
                PerClassResult p = new PerClassResult(i.getKey(), passing, failing);
                ret.add(p);
            }
        }
        Collections.sort(ret);
        return ret;
    }

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

    public static class PerClassResult implements Comparable<PerClassResult> {
        final String className;
        final List<TestResult> passing;
        final List<TestResult> failing;

        public PerClassResult(String className, List<TestResult> passing, List<TestResult> failing) {
            this.className = className;
            this.passing = passing;
            this.failing = failing;
        }

        public String getClassName() {
            return className;
        }

        public List<TestResult> getPassing() {
            return passing;
        }

        public List<TestResult> getFailing() {
            return failing;
        }

        @Override
        public int compareTo(PerClassResult o) {
            return className.compareTo(o.className);
        }
    }
}
