import { check } from "k6";
import http from "k6/http";

export default function () {
  const url = "http://host.docker.internal:8080/api/tickets/book/1";
  const payload = JSON.stringify({});

  // Static ID for both calls to simulate a double-click
  const params = {
    headers: {
      "Content-Type": "application/json",
      "X-User-ID": "user-test-1",
      "X-Request-ID": "unique-request-id-123", // <--- SAME ID
    },
  };

  // --- CALL 1 ---
  let res1 = http.post(url, payload, params);
  check(res1, {
    "First call is 200 OK": (r) => r.status === 200, // Should succeed
  });

  // --- CALL 2 (The Duplicate) ---
  let res2 = http.post(url, payload, params);
  check(res2, {
    // EXPECT 409 Conflict here, because that's how we protected the API
    "Second call is 409 Conflict": (r) => r.status === 409,
    "Body says already booked": (r) => r.body.includes("already booked"),
  });
}
