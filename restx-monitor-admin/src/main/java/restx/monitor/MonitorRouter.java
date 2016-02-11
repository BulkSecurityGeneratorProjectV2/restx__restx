package restx.monitor;

import static restx.security.Permissions.hasRole;


import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.collect.ImmutableMap;
import restx.*;
import restx.admin.AdminModule;
import restx.factory.Component;
import restx.http.HttpStatus;
import restx.metrics.codahale.CodahaleMetricRegistry;
import restx.security.RestxSecurityManager;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * User: xavierhanin
 * Date: 2/9/13
 * Time: 12:44 AM
 */
@Component
public class MonitorRouter extends RestxRouter {
    public MonitorRouter(final restx.common.metrics.api.MetricRegistry metricsRegistry,
                         final RestxSecurityManager securityManager) {
        super("restx-admin", "MonitorRouter",
                getMonitorUIRoute(),
                getGetMonitorRoute(metricsRegistry, securityManager));
    }

    private static StdRoute getGetMonitorRoute(restx.common.metrics.api.MetricRegistry metricRegistry, final RestxSecurityManager securityManager) {
        if (!(metricRegistry instanceof CodahaleMetricRegistry)){
            throw new IllegalStateException("restx-monitor-admin expects that module restx-monitor-codahale is loaded");
        }
        CodahaleMetricRegistry codahaleMetricRegistry = (CodahaleMetricRegistry) metricRegistry;
        final MetricRegistry metrics = codahaleMetricRegistry.getCodahaleMetricRegistry();

        return new StdRoute("MonitorRoute", new StdRestxRequestMatcher("GET", "/@/monitor")) {
            @Override
            public void handle(RestxRequestMatch match, RestxRequest req, RestxResponse resp, RestxContext ctx) throws IOException {
                securityManager.check(req, hasRole(AdminModule.RESTX_ADMIN_ROLE));
                resp.setStatus(HttpStatus.OK);
                resp.setContentType("application/json");
                resp.getWriter().print("[");
                int i = 0;
                for (Map.Entry<String, Timer> timerEntry : metrics.getTimers().entrySet()) {
                    String label = timerEntry.getKey();
                    if (label.endsWith("/@/monitor")) {
                        // not include monitor information on this route itself
                        continue;
                    }

                    Timer timer = timerEntry.getValue();

                    if (i != 0) {
                        resp.getWriter().print(",");
                    }
                    double nsPerMs = 1000000;
                    resp.getWriter().print(String.format(Locale.ENGLISH,
                            "{ \"id\": %s, \"label\": \"%s\", \"hits\": %s, \"avg\": %.2f, \"lastVal\": %.2f, \"min\": %.2f, \"max\": %.2f," +
                                    " \"active\": %.2f, \"avgActive\": %.2f }",
                            i++,
                            label,
                            timer.getCount(),
                            timer.getSnapshot().getMean() / nsPerMs,
                            timer.getSnapshot().getMedian() / nsPerMs,
                            timer.getSnapshot().getMin() / nsPerMs,
                            timer.getSnapshot().getMax() / nsPerMs,
                            timer.getOneMinuteRate(),
                            timer.getMeanRate()
                    ));
                }
                resp.getWriter().print("]");
            }
        };
    }

    private static ResourcesRoute getMonitorUIRoute() {
        return new ResourcesRoute("MonitorUIRoute", "/@/ui/monitor",
            MonitorRouter.class.getPackage().getName(), ImmutableMap.of("", "index.html"));
    }
}
