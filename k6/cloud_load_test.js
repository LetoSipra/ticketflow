import http from "k6/http";
import { check, sleep } from "k6";

export const options = {
  discardResponseBodies: true,
  scenarios: {
    contacts: {
      executor: "ramping-vus",
      startVUs: 0,
      stages: [
        { duration: "15s", target: 250 }, // Ramp up to 250 users
        { duration: "30s", target: 250 }, // Sustain cloud pressure
        { duration: "10s", target: 0 }, // Cool down
      ],
      gracefulRampDown: "5s",
    },
  },
  thresholds: {
    // 95% of requests should be faster than 2 seconds (cloud latency is real)
    http_req_duration: ["p(95)<2000"],
  },
};

export default function () {
  const vu = __VU;
  const iter = __ITER;
  const userId = `cloud-user-${vu}`;
  const requestId = `cloud-req-${vu}-${iter}-${Math.random()
    .toString(36)
    .substring(7)}`;

  const params = {
    headers: {
      "X-User-ID": userId,
      "X-Request-ID": requestId,
      "Content-Type": "application/json",
    },
  };

  // TARGET YOUR LIVE URL
  const res = http.post(
    "https://ticketflow-b415.onrender.com/api/tickets/book/1",
    null,
    params
  );

  check(res, {
    "status is success or expected logic": (r) =>
      [200, 409, 429].includes(r.status),
  });

  // Check for instance overload
  if (res.status === 503 || res.status === 504) {
    console.log("Render Free Tier is overloaded (503/504)");
  }

  sleep(0.2); // Slower sleep to account for cloud network overhead
}
