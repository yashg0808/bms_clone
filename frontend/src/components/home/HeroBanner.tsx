"use client";

import { Movie } from "@/types";

interface HeroBannerProps {
  movies: Movie[];
}

export default function HeroBanner({ movies }: HeroBannerProps) {
  const bannerMovie = movies[0];

  return (
    <div className="relative h-[300px] overflow-hidden bg-gradient-to-r from-gray-900 to-gray-800 md:h-[400px]">
      {bannerMovie?.bannerUrl && (
        <img
          src={bannerMovie.bannerUrl}
          alt={bannerMovie.title}
          className="absolute inset-0 h-full w-full object-cover opacity-40"
        />
      )}

      <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-transparent to-transparent" />

      <div className="relative mx-auto flex h-full max-w-7xl items-end px-4 pb-8">
        <div className="text-white">
          <h1 className="mb-2 text-3xl font-bold md:text-5xl">
            {bannerMovie?.title || "Welcome to BookMyShow"}
          </h1>
          <p className="max-w-xl text-gray-300">
            {bannerMovie?.description?.substring(0, 150) ||
              "Book your favorite movies, events, and shows at the best prices!"}
            ...
          </p>
          {bannerMovie && (
            <div className="mt-4 flex gap-3">
              <span className="rounded bg-primary-500 px-3 py-1 text-sm font-medium">
                {bannerMovie.genre}
              </span>
              <span className="rounded bg-white/20 px-3 py-1 text-sm">
                {bannerMovie.language}
              </span>
              <span className="rounded bg-white/20 px-3 py-1 text-sm">
                {bannerMovie.rating}
              </span>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
