import http from "k6/http";
import { check, sleep } from "k6";

export const options = {
  // Discard response bodies to save memory
  discardResponseBodies: true,

  scenarios: {
    contacts: {
      executor: "ramping-vus",
      startVUs: 0,
      stages: [
        { duration: "10s", target: 2000 }, // 1. BLITZ: Rush to 2000 users in 10s
        { duration: "50s", target: 2000 }, // 2. SUSTAIN: Hammer the system for nearly a minute
        { duration: "10s", target: 0 }, // 3. COOL DOWN
      ],
      gracefulRampDown: "5s",
    },
  },
};

export default function () {
  const vu = __VU;
  const iter = __ITER;

  // 1. Generate Headers
  // User ID: Identifies the customer
  const userId = `user-${vu}`;

  // Request ID: MUST be unique for every single attempt to simulate distinct sales.
  // Combine VU, Iteration, and a random number to be safe.
  const requestId = `req-${vu}-${iter}-${Math.random()
    .toString(36)
    .substring(7)}`;

  const params = {
    headers: {
      "X-User-ID": userId,
      "X-Request-ID": requestId, // 👈 The new Idempotency Key
      "Content-Type": "application/json",
    },
  };

  // 2. Send Request
  const res = http.post(
    "http://host.docker.internal:8080/api/tickets/book/1",
    null, // (assuming no payload needed)
    params
  );

  check(res, {
    // 200 OK = Success (User bought a ticket)
    "is booked": (r) => r.status === 200,

    // 429 Too Many Requests = Success (User was correctly queued/throttled)
    "is queued": (r) => r.status === 429,

    // 409 Conflict = Success (User was correctly told it's sold out)
    "is sold out": (r) => r.status === 409,
  });

  if (res.status === 500) {
    console.log("500 Error received! Body: " + res.body);
  }

  sleep(0.1);
}
