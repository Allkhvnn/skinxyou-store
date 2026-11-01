import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  cacheComponents: true,
  partialPrefetching: true,
  reactCompiler: true,
  images: {
    // Product photos currently come from the BILLZ CDN; Cloudflare R2 will be added later.
    remotePatterns: [new URL("https://fra1.digitaloceanspaces.com/billz2-minio-billz/**")],
  },
  turbopack: {
    rules: {
      "*.css": {
        loaders: ["@tailwindcss/turbopack"],
        as: "*.css",
      },
    },
  },
};

export default nextConfig;
