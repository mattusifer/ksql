/*
 * Copyright 2022 Confluent Inc.
 *
 * Licensed under the Confluent Community License (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 *
 * http://www.confluent.io/confluent-community-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package io.confluent.ksql.function.udaf.min;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;

import io.confluent.ksql.GenericKey;
import io.confluent.ksql.function.AggregateFunctionInitArguments;
import io.confluent.ksql.function.KsqlAggregateFunction;
import io.confluent.ksql.schema.ksql.SqlArgument;
import io.confluent.ksql.schema.ksql.types.SqlTypes;
import java.util.Collections;
import org.apache.kafka.streams.kstream.Merger;
import org.junit.Test;

public class StringMinKudafTest {

  @Test
  public void shouldFindCorrectMin() {
    final MinKudaf<String> stringMinKudaf = getStringMinKudaf();
    final String[] values = new String[]{"C", "F", "B", "E", "A", "D", "B"};
    String currentMin = "D";
    for (final String val : values) {
      currentMin = stringMinKudaf.aggregate(val, currentMin);
    }
    assertThat("A", equalTo(currentMin));
  }

  @Test
  public void shouldHandleNull() {
    final MinKudaf<String> stringMinKudaf = getStringMinKudaf();
    final String[] values = new String[]{"C", "F", "B", "E", "A", "D", "B"};
    String currentMin = null;

    // null before any aggregation
    currentMin = stringMinKudaf.aggregate(null, currentMin);
    assertThat(null, equalTo(currentMin));

    // now send each value to aggregation and verify
    for (final String val : values) {
      currentMin = stringMinKudaf.aggregate(val, currentMin);
    }
    assertThat("A", equalTo(currentMin));

    // null should not impact result
    currentMin = stringMinKudaf.aggregate(null, currentMin);
    assertThat("A", equalTo(currentMin));
  }

  @Test
  public void shouldFindCorrectMinForMerge() {
    final MinKudaf stringMinKudaf = getStringMinKudaf();
    final Merger<GenericKey, String> merger = stringMinKudaf.getMerger();
    final String mergeResult1 = merger.apply(null, "B", "D");
    assertThat(mergeResult1, equalTo("B"));
    final String mergeResult2 = merger.apply(null, "P", "F");
    assertThat(mergeResult2, equalTo("F"));
    final String mergeResult3 = merger.apply(null, "A", "K");
    assertThat(mergeResult3, equalTo("A"));
  }

  private MinKudaf getStringMinKudaf() {
    final KsqlAggregateFunction aggregateFunction = new MinAggFunctionFactory()
        .createAggregateFunction(Collections.singletonList(SqlArgument.of(SqlTypes.STRING)),
            AggregateFunctionInitArguments.EMPTY_ARGS);
    assertThat(aggregateFunction, instanceOf(MinKudaf.class));
    return  (MinKudaf) aggregateFunction;
  }

}
