import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      "/auth": "http://localhost:8080",
      "/users": "http://localhost:8080",
      "/programs": "http://localhost:8080",
      "/screenings": "http://localhost:8080",
      "/public": "http://localhost:8080",
    },
  },
});
