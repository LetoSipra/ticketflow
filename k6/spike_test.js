import http from "k6/http";
import { check, sleep } from "k6";

export const options = {
  stages: [
    { duration: "5s", target: 100 }, // Ramp up to 100 users
    { duration: "10s", target: 100 }, // Stay at 100 users
    { duration: "5s", target: 0 }, // Cool down
  ],
};

export default function () {
  const userId = "user-" + __VU;

  const params = {
    headers: {
      "X-User-ID": userId,
      "Content-Type": "application/json",
    },
  };

  const res = http.post(
    "http://host.docker.internal:8080/api/tickets/book/1",
    null,
    params
  );

  check(res, {
    // Accept 200 (Success), 429 (Wait), or 409 (Sold Out)
    "is status expected": (r) =>
      r.status === 200 || r.status === 429 || r.status === 409,
    "is booked": (r) => r.status === 200 && r.body.includes("Success"),
    "is queued": (r) => r.status === 429,
  });

  // Short sleep to avoid overwhelming the system too quickly
  sleep(0.1);
}
