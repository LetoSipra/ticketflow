import http from "k6/http";
import { check, sleep } from "k6";

export const options = {
  stages: [
    { duration: "10s", target: 500 }, // Ramp-up to 500 users over 10 seconds
    { duration: "10s", target: 500 }, // Stay at 500 users for 10 seconds
    { duration: "5s", target: 0 }, // Ramp-down to 0 users over 5 seconds
  ],
};

export default function () {
  // Make a POST request to book a ticket
  const res = http.post("http://host.docker.internal:8080/api/tickets/book/1");

  check(res, {
    "is status 200": (r) => r.status === 200,
    "is success": (r) => r.body && r.body.includes("Success"),
  });

  sleep(0.1); // Wait 100ms and hit it again
}
