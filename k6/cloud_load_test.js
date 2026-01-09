import http from "k6/http";
import { check, sleep } from "k6";
import { Counter } from "k6/metrics";

// Custom Metric to track actual sales
const soldTickets = new Counter("successful_bookings");
const soldOutEvents = new Counter("sold_out_responses");

export const options = {
  discardResponseBodies: true,
  scenarios: {
    contacts: {
      executor: "ramping-vus",
      startVUs: 0,
      stages: [
        { duration: "20s", target: 50 }, // Smooth ramp up to 50 users
        { duration: "3m", target: 50 }, // Hold steady pressure to drain stock
        { duration: "10s", target: 0 }, // Cool down
      ],
      gracefulRampDown: "10s",
    },
  },
  thresholds: {
    // We want 95% of requests to be handled (success or sold out) in under 5s
    http_req_duration: ["p(95)<5000"],
    // Ensure we don't have too many 500 errors (server crashes)
    http_req_failed: ["rate<0.05"], // Allow 409/429 but fail on 500s
  },
};

export default function () {
  const vu = __VU;
  const iter = __ITER;
  const userId = `load-tester-${vu}`;
  const requestId = `req-${vu}-${iter}-${Math.random()
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

  // 1. Count actual sales
  if (res.status === 200) {
    soldTickets.add(1);
  }

  // 2. Count "Sold Out" responses
  if (res.status === 409) {
    soldOutEvents.add(1);
  }

  // 3. Verify logic
  check(res, {
    "status is legal (200, 409, 429)": (r) =>
      [200, 409, 429].includes(r.status),
    "Ticket Bought (200)": (r) => r.status === 200,
    "Sold Out (409)": (r) => r.status === 409,
  });

  // Adaptive sleep: If server is struggling (429), sleep longer
  if (res.status === 429) {
    sleep(1);
  } else {
    sleep(0.1);
  }
}
