"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { movieApi } from "@/lib/api";
import { Movie } from "@/types";
import MovieCard from "@/components/movies/MovieCard";
import HeroBanner from "@/components/home/HeroBanner";

export default function HomePage() {
  const [featuredMovies, setFeaturedMovies] = useState<Movie[]>([]);
  const [allMovies, setAllMovies] = useState<Movie[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function fetchData() {
      try {
        const [featuredRes, moviesRes] = await Promise.all([
          movieApi.getFeatured().catch(() => ({ data: { data: [] } })),
          movieApi.getMovies({ page: 0, size: 12 }).catch(() => ({
            data: { data: { content: [] } },
          })),
        ]);

        setFeaturedMovies(featuredRes.data?.data || []);
        setAllMovies(moviesRes.data?.data?.content || []);
      } catch (error) {
        console.error("Failed to fetch movies:", error);
      } finally {
        setLoading(false);
      }
    }

    fetchData();
  }, []);

  return (
    <div>
      <HeroBanner movies={featuredMovies} />

      {/* Now Showing */}
      <section className="mx-auto max-w-7xl px-4 py-10">
        <div className="mb-6 flex items-center justify-between">
          <h2 className="text-2xl font-bold text-gray-900">
            Recommended Movies
          </h2>
          <Link
            href="/movies"
            className="text-sm font-medium text-primary-500 hover:text-primary-600"
          >
            See All →
          </Link>
        </div>

        {loading ? (
          <div className="grid grid-cols-2 gap-6 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5">
            {Array.from({ length: 5 }).map((_, i) => (
              <div
                key={i}
                className="h-80 animate-pulse rounded-lg bg-gray-200"
              />
            ))}
          </div>
        ) : (
          <div className="grid grid-cols-2 gap-6 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5">
            {allMovies.slice(0, 10).map((movie) => (
              <MovieCard key={movie.id} movie={movie} />
            ))}
          </div>
        )}
      </section>

      {/* Premieres */}
      <section className="bg-gray-900 py-10">
        <div className="mx-auto max-w-7xl px-4">
          <h2 className="mb-6 text-2xl font-bold text-white">
            🎬 Premieres
          </h2>
          <p className="mb-6 text-gray-400">
            Brand new releases every Friday
          </p>

          <div className="grid grid-cols-2 gap-6 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5">
            {allMovies.slice(0, 5).map((movie) => (
              <MovieCard key={movie.id} movie={movie} variant="dark" />
            ))}
          </div>
        </div>
      </section>
    </div>
  );
}
