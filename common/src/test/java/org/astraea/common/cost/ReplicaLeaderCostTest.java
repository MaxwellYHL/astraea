/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.astraea.common.cost;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.astraea.common.admin.ClusterInfo;
import org.astraea.common.admin.NodeInfo;
import org.astraea.common.admin.Replica;
import org.astraea.common.metrics.BeanObject;
import org.astraea.common.metrics.broker.ServerMetrics;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class ReplicaLeaderCostTest {
  private final Dispersion dispersion = Dispersion.cov();

  @Test
  void testNoMetrics() {
    var replicas =
        List.of(
            Replica.builder()
                .topic("topic")
                .partition(0)
                .nodeInfo(NodeInfo.of(10, "broker0", 1111))
                .path("/tmp/aa")
                .buildLeader(),
            Replica.builder()
                .topic("topic")
                .partition(0)
                .nodeInfo(NodeInfo.of(10, "broker0", 1111))
                .path("/tmp/aa")
                .buildLeader(),
            Replica.builder()
                .topic("topic")
                .partition(0)
                .nodeInfo(NodeInfo.of(11, "broker1", 1111))
                .path("/tmp/aa")
                .buildLeader());
    var clusterInfo =
        ClusterInfo.of(
            "fake",
            List.of(
                NodeInfo.of(10, "host1", 8080),
                NodeInfo.of(11, "host1", 8080),
                NodeInfo.of(12, "host1", 8080)),
            Map.of(),
            replicas);
    var brokerCost = ReplicaLeaderCost.leaderCount(clusterInfo);
    var clusterCost =
        dispersion.calculate(
            brokerCost.values().stream().map(x -> (double) x).collect(Collectors.toSet()));
    Assertions.assertTrue(brokerCost.containsKey(10));
    Assertions.assertTrue(brokerCost.containsKey(11));
    Assertions.assertEquals(3, brokerCost.size());
    Assertions.assertTrue(brokerCost.get(10) > brokerCost.get(11));
    Assertions.assertEquals(brokerCost.get(12), 0);
    Assertions.assertEquals(clusterCost, 0.816496580927726);
  }

  private ServerMetrics.ReplicaManager.Gauge mockResult(String name, int count) {
    var result = Mockito.mock(ServerMetrics.ReplicaManager.Gauge.class);
    var bean = Mockito.mock(BeanObject.class);
    Mockito.when(result.beanObject()).thenReturn(bean);
    Mockito.when(bean.properties()).thenReturn(Map.of("name", name, "type", "ReplicaManager"));
    Mockito.when(result.value()).thenReturn(count);
    return result;
  }
}
