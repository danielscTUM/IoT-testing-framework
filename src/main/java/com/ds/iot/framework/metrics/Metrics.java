package com.ds.iot.framework.metrics;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import lombok.Getter;
import org.json.JSONArray;
import org.json.JSONObject;

@Getter
public class Metrics {

    private MetricRegistry metrics = new MetricRegistry();
    private final Timer responses = metrics.timer("responses");
    private final Counter failedConnections = metrics.counter("failedConnections");
    private final Histogram responseDuration = metrics.histogram("responseDuration");

    public void merge(JSONObject metricsObj2) {
        JSONObject metrics2 = metricsObj2.getJSONObject("metrics").getJSONObject("metrics");
        JSONArray durationValues = metrics2
            .getJSONObject("responseDuration")
            .getJSONObject("snapshot")
            .getJSONArray("values");
        for(int k= 0; k < durationValues.length(); k++){
            this.getResponseDuration().update(durationValues.getLong(k));
        }
        long failedConnections2 = metrics2.getJSONObject("failedConnections")
            .getLong("count");
        this.getFailedConnections().inc(failedConnections2);
    }
}
