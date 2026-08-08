import http from "k6/http";
import { check, sleep } from "k6";
import { Rate } from "k6/metrics";

const endpointFailureRate = new Rate("endpoint_failure_rate");

export const options = {
  scenarios: {
    read_smoke: {
      executor: "ramping-vus",
      stages: [
        { duration: "30s", target: 10 },
        { duration: "1m", target: 30 },
        { duration: "30s", target: 0 },
      ],
    },
  },
  thresholds: {
    http_req_failed: ["rate<0.01"],
    http_req_duration: ["p(95)<800"],
    endpoint_failure_rate: ["rate<0.01"],
  },
};

const baseUrl = __ENV.BASE_URL || "http://localhost:8081";

export default function () {
  const endpoints = [
    "/actuator/health",
    "/clothes",
    "/clothes/1",
    "/clothes/type/1",
    "/clothes/search?keyword=T",
  ];

  for (const endpoint of endpoints) {
    const response = http.get(`${baseUrl}${endpoint}`, {
      headers: {
        "X-Trace-Id": `k6-${__VU}-${__ITER}`,
      },
      tags: {
        endpoint,
      },
    });
    const ok = check(response, {
      "status is 2xx": (r) => r.status >= 200 && r.status < 300,
      "trace id returned": (r) => Boolean(r.headers["X-Trace-Id"]),
    }, { endpoint });
    endpointFailureRate.add(!ok, { endpoint, status: String(response.status) });
  }

  sleep(1);
}
