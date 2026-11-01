import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  cacheComponents: true,
  partialPrefetching: true,
  reactCompiler: true,
  images: {
    // Product photos currently come from the BILLZ CDN; Cloudflare R2 will be added later.
    remotePatterns: [
      new URL("https://fra1.digitaloceanspaces.com/billz2-minio-billz/**"),
      // Temporary home page mood photos; object form because the URLs carry query params.
      { protocol: "https", hostname: "images.unsplash.com", pathname: "/**" },
    ],
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
